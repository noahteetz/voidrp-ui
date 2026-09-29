# TMB-Fork von VoidRP UI

Dieser Fork ([noahteetz/voidrp-ui](https://github.com/noahteetz/voidrp-ui)) liefert die
grafischen Oberflächen für das Burg-Event
([TMB-Castle-Event](https://github.com/noahteetz/TMB-Castle-Event)). Das Original ist
[VOIDRP-MINECRAFT/voidrp-ui](https://github.com/VOIDRP-MINECRAFT/voidrp-ui) (MIT).

**Wir bleiben dauerhaft beim Fork.** Pull Requests ans Original sind nicht geplant;
Neuerungen des Originals ziehen wir bei Bedarf nach.

## Warum ein Fork

VoidRP UI legte die Schrift fest im eigenen Jar fest (nur Inter, keine Umlaute). Der Fork
hat das behoben: Latin-1 in den Schriftbögen, eigene Schrift über `theme.yml`, Item-Icons
in Größe 48 und eigene Pack-Dateien aus `plugins/VoidRpUI/pack/`.

**Seit 0.3.19 steckt all das im Original** (Changelog 0.3.19, „From the fork by Noah
Teetz“). `tmb` weicht derzeit nur um diese Datei und die Versionsnummer vom Original ab.
Der Fork bleibt trotzdem die Quelle für das Burg-Plugin, damit künftige eigene Änderungen
wieder hier entstehen können, ohne auf ein Release des Originals zu warten.

## Wo was liegt

| Was | Wo |
| --- | --- |
| Dieser Fork, lokal | `voidrp-ui/` — neben dem Burg-Repo, kein Unterordner |
| Burg-Plugin, lokal | `TMB-Castle-Event/` |
| Remote `origin` | `github.com/noahteetz/voidrp-ui` (unser Fork) |
| Remote `upstream` | `github.com/VOIDRP-MINECRAFT/voidrp-ui` (nur lesen, Push gesperrt) |

Auf einem neuen Rechner:

```bash
git clone -b tmb https://github.com/noahteetz/voidrp-ui.git
cd voidrp-ui
git remote add upstream https://github.com/VOIDRP-MINECRAFT/voidrp-ui.git
git remote set-url --push upstream DISABLE
```

Der Klon gehört neben das Burg-Repo, sonst findet dessen Schalter `-PvoidrpLokal` ihn
nicht. Gebraucht wird er nur, wer am Fork arbeitet; das Burg-Plugin allein holt die
Fork-Version über JitPack und das Release.

## Branches

| Branch | Inhalt |
| --- | --- |
| `tmb` | Standard-Branch. Unsere Fassung: das Original plus alle eigenen Änderungen. Diese läuft auf dem Burg-Server. |
| `main` | unverändert wie das Original. Nur per `git fetch upstream` + Merge bewegen, nie selbst darauf committen. |

Größere Änderungen dürfen auf einem eigenen Branch entstehen und werden nach `tmb` gemergt.

## Abläufe

**Original nachziehen**

```bash
git fetch upstream --tags
git checkout main && git merge --ff-only upstream/main && git push origin main
git checkout tmb && git merge main
./gradlew test
```

**Neue Version für das Burg-Plugin**

1. In `build.gradle.kts` die Version hochzählen: `<Original-Version>-tmb.<n>`, etwa
   `0.3.19-tmb.2`.
2. Committen, Tag `v0.3.19-tmb.2` auf `tmb` setzen, beides pushen.
3. Der Workflow `.github/workflows/release.yml` baut das Jar und hängt es an das
   GitHub-Release. JitPack baut die Abhängigkeit beim ersten Abruf von selbst.
4. Im Burg-Repo `voidrpUiVersion` in `gradle.properties` auf die neue Version setzen.

**Ausprobieren ohne Tag**

Im Burg-Repo baut `-PvoidrpLokal` gegen diesen Klon: Tests, `uiVorschau` und
`runServer` nutzen dann den aktuellen Stand von `../voidrp-ui` — `runServer` mit dem
hier frisch gebauten `build/libs/*-all.jar`.

## Bauen und prüfen

```bash
./gradlew test       # misst jedes Zeichen aus den gebauten Bildern nach
./gradlew preview    # PNGs der Seiten nach build/preview
./gradlew shadowJar  # Plugin-Jar nach build/libs/*-all.jar
```
