# In-App-Updates

Die einzige OWNER/REPO-Konfiguration liegt in `updates/Release.kt`: `UpdateConfig.REPOSITORY = "KingMezzo/GymMane-Scanner"`. Metadaten kommen ohne Anmeldung von `https://api.github.com/repos/KingMezzo/GymMane-Scanner/releases/latest`.

## Verhalten

- Automatische Prüfung standardmäßig an, in Einstellungen deaktivierbar. Der Zeitpunkt eines Versuchs wird vor der Anfrage gespeichert, einschließlich fehlgeschlagener Anfragen. Automatisch höchstens einmal innerhalb von 24 Stunden; Bildschirmdrehung startet keinen zweiten Check.
- Die manuelle Aktion in Einstellungen umgeht das 24-Stunden-Intervall, respektiert aber GitHub-Rate-Limits. HTTP-/Verbindungsfehler bleiben bei automatischen Checks still. Manuelle Checks melden Fehler, Rate-Limits, fehlende APK oder den aktuellen Versionsstand.
- SemVer-2.0-Vergleich einschließlich Vorabversionsreihenfolge, optionalem `v` und ignorierten Build-Metadaten. Kein lexikografischer Vergleich. Drafts und als Prerelease markierte Releases werden ignoriert.
- Nur neuere, gültige Releases mit APK werden angeboten. Universelle APKs werden bevorzugt; eindeutig erkannte, nicht unterstützte ABI-Varianten werden ausgeschlossen. Release-Body wird als Text angezeigt; die Darstellung ist zum Schutz vor übergroßen Textlayouts auf 30.000 Zeichen begrenzt.
- „Aktualisieren“ startet erst auf Nutzertipp einen Download mit Fortschritt. „Später“ schließt das Angebot und bricht gegebenenfalls den Download ab. Erneut angeboten wird die Version frühestens beim nächsten Check.

## Installation

Die APK wird intern unter `cache/updates` geladen, mit Größenlimit und gegebenenfalls GitHub-SHA-256-Digest geprüft. Vor dem Systeminstaller werden Paketname, zur Release-Version passender `versionName`, höherer `versionCode` und kompatible Signatur geprüft. Debug- und Release-Schlüssel müssen beachtet werden: Ein Debug-Build kann eine anders signierte Release-APK nicht als normales Update installieren.

Der nicht exportierte FileProvider gibt ausschließlich das Update-Cacheverzeichnis frei. Der Installer erhält `ACTION_VIEW`, den APK-MIME-Typ, eine `content://`-URI und temporäres Leserecht. Es gibt keine Speicher- oder Benachrichtigungsberechtigung und keinen DownloadManager.

Fehlt „Unbekannte Apps installieren“, wird die entsprechende Android-Einstellungsseite geöffnet. Nach Rückkehr wird fortgesetzt, wenn die Berechtigung erteilt wurde. Ein lokaler Pending-Marker ermöglicht die Fortsetzung auch nach einem Prozessneustart. Bei Verweigerung entsteht keine Einstellungs-Endlosschleife. Android bestätigt und prüft die Installation weiterhin selbst; die App installiert nichts unbeaufsichtigt.

## Was ist neu?

Nach einer aktualisierten Installation wird einmal ein eigener Versionshinweis-Bildschirm angezeigt. Gespeichert wird der zuletzt angezeigte `versionCode`. Eine frische Installation bekommt diesen Bildschirm nicht zusätzlich zum Onboarding. Hinweise stammen bevorzugt aus den vor dem Update gespeicherten Release-Notes, andernfalls aus dem zur installierten Version passenden GitHub-Tag. Offline oder bei abgeschalteten automatischen Checks stehen lokal mitgelieferte Hinweise bereit.

## Veröffentlichung und Prüfung

Ein Release muss öffentlich über die GitHub-API erreichbar sein, einen SemVer-Tag wie `v1.4.0` und mindestens eine passende `.apk` enthalten. Der APK-`versionName` muss diesem Tag entsprechen; `versionCode` muss erhöht und derselbe Signierschlüssel beziehungsweise eine gültige Signaturrotation verwendet werden.

Quellcodeprüfung ohne APK-Bau:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:lintDebug
```

Tests decken SemVer, Intervall-/Rate-Limit-Logik, Release-/Asset-Auswahl, anonyme Requests, Downloadfortschritt, Dateigröße und Prüfsummen ab. Installer-URI-Tests sind als Android-Testquellen enthalten. Eine echte Installation und die Rückkehr aus den Systemberechtigungseinstellungen müssen zusätzlich auf einem Gerät oder Emulator geprüft werden.

Es werden nur öffentliche GitHub-Anfragen mit den üblichen API-Headern und einem App-User-Agent gesendet: keine Barcodes, Produktdaten, Kontodaten, Analytics oder Tracking-IDs. Scan- und GymMane-Teilen-Logik bleiben unverändert.
