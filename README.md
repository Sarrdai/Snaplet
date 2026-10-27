# TinyCam

Eine Kamera-App für Kinder (Android). Ein großer Auslöser an der Seite, eine Vorschau-Leiste mit den
letzten Bildern und ein Eltern-Bereich mit Lernstufen, Limits und fünf Designs
(Klassisch, Regenbogen, Raumschiff, Dino, Leopard).

## Installieren

1. Auf dem Handy die Seite **Releases → „TinyCam (neuester Stand)“** öffnen und `TinyCam.apk` laden.
   Alternativ unter **Actions** den letzten Lauf öffnen und das Artefakt herunterladen.
2. Installation aus unbekannten Quellen für den Browser bzw. Dateimanager erlauben.
3. Beim ersten Start Kamera und Mikrofon erlauben.

Updates lassen sich direkt drüber installieren, Fotos bleiben erhalten.

## Bedienung

| Was | Wie |
|---|---|
| Foto | Auslöser-Pille antippen. Sie füllt sich wieder, bis die nächste Aufnahme möglich ist. |
| Bild ansehen | Vorschaubild antippen, irgendwo tippen zum Schließen. |
| Foto ↔ Video (ab Stufe 2) | Umschalter 1,5 s halten. |
| Galerie (ab Stufe 3) | Galerie-Knopf 1 s halten. |
| Eltern-Bereich | Schloss in der unteren Ecke 3 s halten. |

Das Livebild reagiert auf keine Berührung. Liegen mehrere Finger oder der Handballen auf dem
Bildschirm, wird nichts ausgelöst.

Im Eltern-Bereich: Design, Lernstufe, Auslöser-Seite, Pause zwischen Aufnahmen, Aufnahmen pro Tag,
Nutzungszeit pro Tag, maximale Videolänge, Anzahl Bilder in der Leiste, Teilen/Löschen aller
Aufnahmen und App fixieren (Android-Bildschirmfixierung).

## Entwicklung

- Kotlin, Jetpack Compose, CameraX. Keine weiteren Laufzeit-Bibliotheken.
- `./gradlew assembleRelease` baut das signierte APK.
- `./gradlew testDebugUnitTest` führt Logik- und Screenshot-Tests aus.
- `./gradlew recordRoborazziDebug` rendert alle Designs nach `app/build/screenshots/`
  (zur Design-Kontrolle ohne Gerät).
- Die GitHub Action baut bei jedem Push. Auf dem Standard-Branch (oder per manuellem Start)
  wird das APK als Release „latest“ veröffentlicht.

### Signatur

`app/signing/throwaway.jks` ist ein absichtlich eingecheckter Wegwerf-Schlüssel, damit jeder
CI-Build das installierte App-Update akzeptiert. Vor einer Veröffentlichung (z. B. Play Store) durch
einen geheimen Schlüssel aus GitHub-Secrets ersetzen.

## Lizenzen

Schrift „Baloo 2“: SIL Open Font License, siehe `licenses/OFL-Baloo2.txt`.
