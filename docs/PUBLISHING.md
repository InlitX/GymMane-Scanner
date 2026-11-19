# Öffentlichen Quellcode vorbereiten

Diese Anleitung beschreibt, welche Dateien zum öffentlichen Quellcode gehören und welche ausschließlich lokal bleiben sollen.

## Was ins Repository gehört

- `app/src/`: Kotlin-Quellcode, Ressourcen und Tests
- Gradle-Builddateien, `gradle.properties`, `gradlew`, `gradlew.bat` und `gradle/wrapper/` einschließlich `gradle-wrapper.jar`
- `.gitignore`, `.gitattributes`, `README.md`, `docs/` und die leere `keystore.properties.example`

`gradle.properties` enthält hier ausschließlich öffentliche Build-Einstellungen und einen öffentlichen Repository-Link. Private Einstellungen gehören in ausgeschlossene lokale Dateien.

## Was lokal bleibt

| Dateien | Behandlung |
| --- | --- |
| `keystore.properties`, Keystores und private Schlüssel | Von Git ausgeschlossen; separat sicher aufbewahren |
| `local.properties`, IDE-Zustand und lokale Assistentenkonfiguration | Von Git ausgeschlossen |
| `.env`-Dateien, Zugangsdaten und Service-Account-Dateien | Von Git ausgeschlossen; nur leere Beispielvorlagen veröffentlichen |
| `.gradle/`, `.kotlin/`, `build/`, Logs und Speicherabbilder | Entbehrliche lokale Ausgaben; von Git ausgeschlossen |
| `artifacts/`, APKs, App-Bundles, Archive und R8-Mappings | Von Git ausgeschlossen; vorhandene Release-Archive bleiben lokal erhalten |
| Lokale Datenbanken, Sicherungen und temporäre Dateien | Von Git ausgeschlossen |

Die vorhandene Signierkonfiguration und Release-Archive werden nicht gelöscht, weil sie für spätere Updates und Fehleranalyse benötigt werden können. Build-Caches und Build-Ausgaben werden beim nächsten Build neu erzeugt.

## Vor dem ersten Upload

1. Nur die von Git erfassten Quellcodedateien veröffentlichen. Beim manuellen Browser-Upload eines ganzen Ordners oder ZIPs wird `.gitignore` nicht angewendet. Den unbearbeiteten Projektordner deshalb nicht als Ganzes hochladen.
2. Nach einer späteren lokalen Git-Initialisierung vor dem ersten Commit `git status --short --untracked-files=all` und `git add --dry-run .` prüfen. Nach dem Vormerken zeigen `git diff --cached --name-only` und `git diff --cached` den tatsächlichen Commit-Inhalt. Insbesondere `keystore.properties`, `artifacts/`, Speicherabbilder und Build-Ausgaben dürfen darin nicht auftauchen.
3. Keine privaten Angaben in Quellcode, Kommentare, Testdaten oder neue Dokumente schreiben. `.gitignore` schützt Dateinamen und Ordner, erkennt aber keine Zugangsdaten innerhalb öffentlicher Dateien. Auch Git-Autorname und E-Mail vor dem ersten Commit bewusst wählen.
4. Falls später eine bestehende Git-Historie importiert wird, auch diese vor einer Veröffentlichung auf private Inhalte prüfen. Ignore-Regeln entfernen keine früheren Commits oder bereits erfassten Dateien. Im Projektordner war bei der Vorbereitung kein `.git` vorhanden.
5. Vor einer Freigabe zur Nutzung und Weiterverteilung des eigenen Codes eine passende Lizenz festlegen. Es wurde keine Lizenz stellvertretend gewählt.

## Spätere App-Releases

1. `versionName` und `versionCode` in `app/build.gradle.kts` erhöhen und Versionshinweise unter `docs/` ergänzen.
2. Unit-Tests und Lint ausführen, anschließend mit dem bisherigen Release-Schlüssel bauen; siehe README.
3. Die APK als Datei eines GitHub-Releases mit passendem SemVer-Tag veröffentlichen, beispielsweise `v1.3.3` für Version 1.3.3. Für den Updater muss der Release öffentlich und regulär sein, also weder Draft noch Prerelease. GitHubs automatische Quellcode-Archive ersetzen die APK nicht.
4. Die zum Build gehörende R8-Mappingdatei privat sichern. Optional eine SHA-256-Prüfsumme der APK als Release-Datei bereitstellen.
5. Den Updateablauf auf einem Gerät mit einer älteren, mit demselben Schlüssel signierten Version prüfen.

Ein Quellcode-Upload veröffentlicht keine neue APK und keinen App-Release. Die öffentlichen Repository-Adressen in der App bleiben als bestehende Anwendungskonfiguration erhalten.
