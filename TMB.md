# TMB-Fork von VoidRP UI

Dieser Fork ([noahteetz/voidrp-ui](https://github.com/noahteetz/voidrp-ui)) liefert die
grafischen Oberflächen für das Burg-Event
([TMB-Castle-Event](https://github.com/noahteetz/TMB-Castle-Event)). Das Original ist
[VOIDRP-MINECRAFT/voidrp-ui](https://github.com/VOIDRP-MINECRAFT/voidrp-ui) (MIT).

Diese Datei liegt nur auf dem Branch `tmb` und gehört in keinen Pull Request.

## Warum ein Fork

VoidRP UI legt die Schrift fest im eigenen Jar fest und bietet keine Einstellung dafür:

- **Nur Inter.** Die Burg soll wie ein Minecraft-Inventar aussehen, und dafür braucht es
  eine Pixelschrift.
- **Keine Umlaute.** Der Zeichensatz in `pack/TextFonts.kt` kennt ASCII und Kyrillisch,
  aber kein ä, ö, ü oder ß. Unbekannte Zeichen überspringt `render/Element.kt`
  stillschweigend, aus „Händler“ wird im Spiel „Hndler“.

Beides lässt sich nur im Code von VoidRP ändern. Jede Änderung, die auch anderen nützt,
wird dem Original als Pull Request angeboten. Wird sie übernommen, schrumpft der
Unterschied zum Original wieder.

## Wo was liegt

| Was | Wo |
| --- | --- |
| Dieser Fork, lokal | `C:\Dev\voidrp-ui` |
| Burg-Plugin, lokal | `C:\Dev\TMB-Castle-Event` (daneben, kein Unterordner) |
| Remote `origin` | `github.com/noahteetz/voidrp-ui` (unser Fork) |
| Remote `upstream` | `github.com/VOIDRP-MINECRAFT/voidrp-ui` (nur lesen, Push gesperrt) |

## Branches

| Branch | Inhalt |
| --- | --- |
| `main` | unverändert wie das Original. Nur per `git fetch upstream` + Merge bewegen, nie selbst darauf committen. |
| `<thema>` (z. B. `umlaute`, `pixelschrift`) | genau eine Änderung, abgezweigt von `main`. Daraus entsteht der Pull Request an das Original. |
| `tmb` | `main` plus alle eigenen Änderungen. Diese Fassung läuft auf dem Burg-Server. |

Einzelne Änderungen werden nie direkt auf `tmb` gebaut, sondern auf einem Themenbranch und
dann nach `tmb` gemergt. So bleibt jede Änderung sauber als Pull Request anbietbar. Baut
ein Thema auf einem anderen auf, zweigt es von dessen Branch ab; der Pull Request dafür
kommt erst, wenn der vorige angenommen ist.

### Stand der Themen

| Branch | Inhalt | Pull Request |
| --- | --- | --- |
| `umlaute` | Latin-1 (ä ö ü ß é …), deutsche Anführungszeichen und € in den Schriftbögen; Zeichen, die eine Schrift nicht kennt, landen nicht als Kästchen im Bogen | noch nicht gestellt |
| `pixelschrift` | baut auf `umlaute` auf: `font`-Abschnitt in `theme.yml` — eigene TTF-Dateien aus `plugins/VoidRpUI/fonts/`, `pixel: true` ohne Kantenglättung, eigene Größen (`docs/theming.md`) | nach `umlaute` |

## Abläufe

**Original nachziehen**

```bash
git fetch upstream --tags
git checkout main && git merge --ff-only upstream/main && git push origin main
git checkout tmb && git merge main
```

**Eigene Änderung**

```bash
git checkout -b pixelschrift main
# ändern, ./gradlew test, committen, pushen
git checkout tmb && git merge pixelschrift
```

Den Pull Request stellt man auf GitHub von `noahteetz:pixelschrift` nach
`VOIDRP-MINECRAFT:main`.

**Version für das Burg-Plugin**

Auf `tmb` wird ein Tag der Form `v<Original-Version>-tmb.<n>` gesetzt, etwa
`v0.3.17-tmb.1`. Der Workflow `.github/workflows/release.yml` baut das Jar beim Pushen des
Tags und hängt es an ein GitHub-Release. Dafür müssen die Actions im Fork aktiviert sein.
Das Burg-Plugin bezieht diese Version über JitPack; die Nummer steht dort in
`gradle.properties` (`voidrpUiVersion`).

## Bauen und prüfen

```bash
./gradlew test       # misst jedes Zeichen aus den gebauten Bildern nach
./gradlew preview    # PNGs der Seiten nach build/preview
./gradlew shadowJar  # Plugin-Jar nach build/libs/*-all.jar
```
