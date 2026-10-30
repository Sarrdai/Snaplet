# TinyCam

Eine Kamera-App für Kinder (Android). Ein großer Auslöser an der Seite, eine Vorschau-Leiste mit den
letzten Bildern und ein Eltern-Bereich mit Lernstufen, Limits und elf Designs
(Klassisch, Regenbogen, Raumschiff, Dino, Leopard, Einhorn, Hai, Roboter, Feuerwehr, Monster, Eiscreme).

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
| Foto ↔ Video | Umschalter 1,5 s halten. |
| Galerie, Selfie, Filter & Rahmen, Selbstauslöser, Design | Werkzeug-Knopf neben dem Auslöser 1 s halten. |
| Zoom | Knopf des großen Schiebereglers ziehen (kleiner Punkt = weit, großer Punkt = nah). |
| Design wechseln | Paletten-Knopf 1 s halten, dann ein Auslöser-Bild antippen. Antippen daneben schließt. |
| Selbstauslöser abbrechen | Während des Countdowns den Auslöser erneut antippen. |
| Eltern-Bereich | Schloss in der unteren Ecke 3 s halten. |

Das Livebild reagiert auf keine Berührung. Liegen mehrere Finger oder der Handballen auf dem
Bildschirm, wird nichts ausgelöst. Hinweise erscheinen als Bild (Finger mit Ring = gedrückt halten).

Ist ein Tageslimit für Aufnahmen gesetzt, zeigt der Auslöser die restlichen Aufnahmen als Symbole
(Sterne, Dino-Eier, Pfoten …), die nach und nach leer werden. Bis 10 Aufnahmen steht jedes Symbol
für eine Aufnahme. Ist alles aufgebraucht, wackelt der Auslöser.

Im Eltern-Bereich lässt sich jede Funktion einzeln einschalten: Videos, Galerie, Selfie-Kamera, Zoom,
Filter & Rahmen, Selbstauslöser und Design wählen (das Kind wechselt selbst zwischen den freigegebenen
Designs; welche angeboten werden, legen die Eltern fest). Werte wie Pause, Tageslimits, Videolänge, Countdown und Anzahl
Bilder in der Leiste stellen Schieberegler ein; abschaltbare Limits zeigen ihren Regler nur, wenn sie
an sind. Außerdem: Design, Auslöser-Position (links, rechts oder rund in der Mitte), Foto-Format
(4:3, 16:9, 1:1), Foto-Größe, Video-Qualität und eine Galerie mit Großansicht zum gezielten Teilen
oder Löschen.

**Zugang zum Eltern-Bereich** (nach 3 s Halten des Schlosses): nur Halten, die Bildschirmsperre des
Handys (PIN, Muster, Fingerabdruck; ideal bei geteiltem Handy) oder eine eigene 4-stellige PIN mit
der Geräte-Sperre als Rückweg.

**App fixieren**: per Knopf oder automatisch beim Start und beim Verlassen des Eltern-Bereichs.
Android fragt dabei jedes Mal nach einer Bestätigung. In den Android-Einstellungen unter
Sicherheit → App fixieren „Vor dem Lösen nach PIN fragen“ einschalten.

Filter und Rahmen gelten für Fotos; im Video-Modus sind sie ausgeblendet. Sie werden fest ins
gespeicherte Foto übernommen (auch beim Teilen). Das Original bleibt zusätzlich erhalten: In der
Großansicht schaltet der Zauberstab-Knopf (Kind) bzw. „Original“ / „Mit Filter“ (Eltern-Galerie)
zwischen beiden Versionen um.

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
