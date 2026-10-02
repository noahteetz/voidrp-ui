package ru.voidrp.ui

import java.lang.reflect.Proxy
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import ru.voidrp.ui.input.CursorPrefs
import ru.voidrp.ui.input.PacketAim
import ru.voidrp.ui.input.Pointer
import ru.voidrp.ui.layout.Direction
import ru.voidrp.ui.layout.Panel
import ru.voidrp.ui.layout.Size
import ru.voidrp.ui.pack.Glyphs
import ru.voidrp.ui.pack.Shaders
import ru.voidrp.ui.page.Button
import ru.voidrp.ui.page.CursorPage
import ru.voidrp.ui.page.Page
import ru.voidrp.ui.page.PageSession
import ru.voidrp.ui.render.BossBarRenderer

class DirectCursorTest {
    @Test
    fun `a flick reversal and stop follow the reading on the first frame at any ping`() {
        for (ping in listOf(0, 40, 250)) {
            val pointer = Pointer(500.0, 500.0, now = 0, smoothing = 3.0, prediction = 1.0)
            var now = 0L
            for ((x, y) in listOf(900.0 to 800.0, 80.0 to 40.0, 300.0 to 750.0)) {
                now += 50_000_000L
                pointer.sample(x, y, now)
                pointer.frame(now, ping, 1820, 1024, direct = true)
                assertEquals(x, pointer.x)
                assertEquals(y, pointer.y)
            }
            repeat(120) {
                now += 16_666_667L
                pointer.frame(now, ping, 1820, 1024, direct = true)
                assertEquals(300.0, pointer.x, "a stopped hand must not drift or bounce")
                assertEquals(750.0, pointer.y)
            }
        }
    }

    @Test
    fun `direct mode clamps both edges and discards old prediction when switched off`() {
        val pointer = Pointer(500.0, 500.0, now = 0)
        pointer.sample(-100.0, 2000.0, 50_000_000L)
        pointer.frame(50_000_000L, 100, 1820, 1024, direct = true)
        assertEquals(0.0, pointer.x)
        assertEquals(1023.0, pointer.y)
        pointer.sample(900.0, 300.0, 100_000_000L)
        pointer.frame(100_000_000L, 100, 1820, 1024, direct = true)
        pointer.frame(116_666_667L, 100, 1820, 1024)
        assertEquals(900.0, pointer.x)
        assertEquals(300.0, pointer.y)
    }

    @Test
    fun `a session sends exactly one static pointer despite motion capability and clock skew`() {
        val rig = Rig()
        rig.session.open()
        rig.session.tick() // Enables the old clock-dependent path if direct is not honoured.
        for (offset in listOf(-2.0, 0.0, 0.9, 4.0)) {
            rig.offset = offset
            rig.location.yaw += 5f
            rig.session.frame()
            val cursor = rig.bars.last().name()
            val glyphs = textRuns(cursor).filter { Glyphs.cursor() in it.content() }
            assertEquals(1, glyphs.sumOf { it.content().count { char -> char.toString() == Glyphs.cursor() } })
            val marker = glyphs.single().color()!!.value() shr 20
            assertTrue(marker == Shaders.MARKER || marker == Shaders.MARKER_SHIFTED,
                "direct cursor must not carry a motion timestamp")
            assertEquals(910 + (rig.location.yaw * 10).toInt(), rig.session.cursorX)
        }
        val stopped = rig.bars.last().name()
        repeat(12) { rig.session.frame() }
        assertEquals(stopped, rig.bars.last().name())
    }

    @Test
    fun `click follows the cursor sent between ticks instead of the previous hovered button`() {
        val rig = Rig()
        rig.session.open()
        assertEquals("right", rig.session.hovered)
        rig.location.yaw = -10f
        rig.session.frame()
        assertEquals(810, rig.session.cursorX)
        // No server tick since the new cursor was drawn; the cached hover still says right.
        assertEquals("right", rig.session.hovered)
        rig.session.click(Button.LEFT)
        assertEquals(listOf("left"), rig.clicks)
        assertEquals("left", rig.session.hovered)
    }

    @Test
    fun `switching from client motion at rest replaces the schedule on the next frame`() {
        val rig = Rig()
        rig.direct = false
        rig.session.open()
        rig.session.tick()
        rig.location.yaw = 12f
        rig.session.frame()
        assertTrue(textRuns(rig.bars.last().name()).any {
            Glyphs.cursor() in it.content() && (it.color()!!.value() shr 20) in Shaders.MARKER_MOTION_FIRST..Shaders.MARKER_MOTION_LAST
        })
        rig.direct = true
        rig.session.frame()
        assertEquals(1030, rig.session.cursorX)
        assertEquals(1, textRuns(rig.bars.last().name()).sumOf { run ->
            run.content().count { it.toString() == Glyphs.cursor() }
        })
        assertFalse(textRuns(rig.bars.last().name()).any {
            it.color()?.let { colour -> (colour.value() shr 20) in Shaders.MARKER_MOTION_FIRST..Shaders.MARKER_MOTION_LAST } == true
        })
    }

    @Test
    fun `existing preferences inherit direct mode and player selection survives reload and reset`() {
        val file = Files.createTempFile("cursor-prefs", ".yml").toFile()
        try {
            val id = UUID.randomUUID()
            file.writeText("$id:\n  motion: true\n  sensitivity: 12.0\n")
            val prefs = CursorPrefs(file).also { it.load() }
            assertEquals(null, prefs.of(id).direct)
            val page = CursorPage(prefs, id, { 10.0 }, { 0.9 }, { true })
            java.io.File("build/preview").mkdirs()
            ru.voidrp.ui.preview.Preview.render(page, java.io.File("build/preview/cursor-direct.png"))
            page.onClick("cur:frames", Button.LEFT)
            val reloaded = CursorPrefs(file).also { it.load() }
            assertEquals(false, reloaded.of(id).direct)
            assertEquals(false, reloaded.of(id).motion)
            ru.voidrp.ui.preview.Preview.render(page, java.io.File("build/preview/cursor-frames.png"))
            page.onClick("cur:smooth", Button.LEFT)
            ru.voidrp.ui.preview.Preview.render(page, java.io.File("build/preview/cursor-smooth.png"))
            page.onClick("cur:direct", Button.LEFT)
            assertEquals(true, CursorPrefs(file).also { it.load() }.of(id).direct)
            page.onClick("cur:reset", Button.LEFT)
            assertEquals(CursorPrefs.Prefs(), CursorPrefs(file).also { it.load() }.of(id))
        } finally {
            file.delete()
        }
    }

    private fun textRuns(component: Component): List<TextComponent> =
        listOfNotNull(component as? TextComponent) + component.children().flatMap { textRuns(it) }

    private class Rig {
        val bars = mutableListOf<BossBar>()
        val clicks = mutableListOf<String>()
        var offset = 0.9
        var direct = true
        private val config = YamlConfiguration()
        private val id = UUID.randomUUID()
        private val world = stub<World> { name, _ -> if (name == "getGameTime") 100L else null }
        val location = Location(world, 0.0, 64.0, 0.0)
        private val plugin = stub<Plugin> { name, _ -> if (name == "getConfig") config else null }
        private val player = stub<Player> { name, args ->
            when (name) {
                "getUniqueId" -> id
                "getLocation" -> location.clone()
                "getWorld" -> world
                "getPing" -> 40
                "activeBossBars" -> bars.toList()
                "showBossBar" -> { bars.add(args!![0] as BossBar); null }
                "setRotation" -> { location.yaw = args!![0] as Float; location.pitch = args[1] as Float; null }
                else -> null
            }
        }
        private val page = object : Page() {
            override val bleed = emptyList<ru.voidrp.ui.style.Paint>()
            override fun view() = Panel(direction = Direction.ROW, width = Size.Fixed(400), height = Size.Fixed(200),
                children = listOf(
                    Panel(id = "left", width = Size.Fixed(200), height = Size.Fill),
                    Panel(id = "right", width = Size.Fixed(200), height = Size.Fill),
                ))
            override fun onClick(id: String, button: Button) { clicks += id }
        }
        val session = PageSession(plugin, player, page, BossBarRenderer(), Sounds(plugin),
            { 10.0 }, { 1.5 }, { 0.5 }, { 19 }, { false }, PacketAim(),
            clientMotion = { true }, clockOffset = { offset }, frameRate = { 10 }, directCursor = { direct })
    }

    companion object {
        private inline fun <reified T> stub(crossinline call: (String, Array<out Any?>?) -> Any?): T =
            Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { proxy, method, args ->
                when (method.name) {
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.get(0)
                    "toString" -> "test ${T::class.java.simpleName}"
                    else -> call(method.name, args)
                }
            } as T
    }
}
