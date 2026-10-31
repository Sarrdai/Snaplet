# TODO bis zum Play Store

Reihenfolge = empfohlene Abarbeitung. Abhaken mit `[x]`.

## 1. Package-ID festlegen (vor dem ersten Upload!)

Die Package-ID (`applicationId` in `app/build.gradle.kts`, heute `app.tinycam`) ist die technische, weltweit
eindeutige Kennung der App. Android erkennt daran Updates, Google Play die App
(`play.google.com/store/apps/details?id=<Package-ID>`). **Nach dem ersten Upload ist sie für immer fest.**
Der angezeigte Name („Snaplet“) lässt sich dagegen jederzeit ändern.

- [ ] Entscheiden: `app.tinycam` behalten oder passend zum neuen Namen ändern.
      Empfehlung: `de.kleinphilipp.snaplet` (umgekehrte eigene Domain, kollidiert garantiert mit niemandem).
- [ ] Falls geändert: `applicationId` in `app/build.gradle.kts` und `packageName` in
      `.github/workflows/release.yml` anpassen. `namespace` (Kotlin-Paket `app.tinycam`) kann bleiben.
      Achtung: Per APK installierte Testgeräte bekommen dann eine zweite, neue App; vorher Fotos sichern.
- [ ] Namen „Snaplet“ prüfen: Suche im Play Store, Markenregister
      ([DPMAregister](https://register.dpma.de), [TMview](https://www.tmdn.org/tmview)) in Klasse 9.

## 2. Upload-Schlüssel anlegen und als GitHub-Secrets hinterlegen

Google Play signiert die App mit einem eigenen Schlüssel („Play App Signing“). Unser Schlüssel ist nur der
**Upload-Schlüssel**, mit dem Google prüft, dass ein Upload von uns kommt. Geht er verloren, kann der
Play-Support ihn zurücksetzen – trotzdem sicher aufbewahren.

1. [ ] Schlüssel lokal erzeugen (Java/JDK nötig; `keytool` fragt Passwort und Namen ab):
   ```bash
   keytool -genkeypair -v -keystore snaplet-upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
   ```
   Für Keystore- und Schlüssel-Passwort darf dasselbe starke Passwort genutzt werden.
2. [ ] `snaplet-upload.jks` und das Passwort im Passwort-Manager sichern. **Nie ins Repo einchecken.**
3. [ ] Keystore als Base64 kodieren:
   ```bash
   base64 -w0 snaplet-upload.jks > snaplet-upload.b64
   ```
   (macOS: `base64 -i snaplet-upload.jks -o snaplet-upload.b64`)
4. [ ] Auf GitHub: Repository → **Settings → Secrets and variables → Actions → New repository secret**,
   vier Secrets anlegen:

   | Name | Wert |
   |---|---|
   | `UPLOAD_KEYSTORE_BASE64` | Inhalt von `snaplet-upload.b64` |
   | `UPLOAD_KEYSTORE_PASSWORD` | Keystore-Passwort |
   | `UPLOAD_KEY_ALIAS` | `upload` (bzw. der gewählte Alias) |
   | `UPLOAD_KEY_PASSWORD` | Schlüssel-Passwort |
5. [ ] `snaplet-upload.b64` löschen.
6. [ ] Testlauf: unter **Actions → Play release → Run workflow** Version `v1.0.0` eingeben. Ergebnis: Artefakt
   `Snaplet-v1.0.0-aab` mit `app-release.aab`. Fehlt ein Secret, bricht der Lauf mit einer klaren Meldung ab.
   Später reicht ein Tag: `git tag v1.0.1 && git push origin v1.0.1`.
   Versionsschema: `vX.Y.Z` → versionCode `X*10000 + Y*100 + Z` (Y und Z unter 100). Jede Play-Version braucht
   einen höheren versionCode, also für jeden Upload eine neue Version.
7. [ ] Optional, automatischer Upload aus GitHub: In der Google Cloud Console ein Dienstkonto mit JSON-Schlüssel
   anlegen, in der Play Console unter **Nutzer und Berechtigungen** mit Release-Rechten für die App einladen und
   den JSON-Inhalt als Secret `PLAY_SERVICE_ACCOUNT_JSON` speichern. Erst möglich, nachdem das erste AAB von Hand
   hochgeladen wurde. Bis die App einmal veröffentlicht ist, den Workflow mit Status `draft` laufen lassen.

## 3. Datenschutzerklärung veröffentlichen (GitHub Pages)

- [ ] Repository → **Settings → Pages → Build and deployment**: Source „Deploy from a branch“, Branch `main`,
      Ordner `/docs`. Pages braucht ein öffentliches Repository (oder einen kostenpflichtigen GitHub-Plan).
- [ ] Prüfen: `https://sarrdai.github.io/TinyCam/privacy.html` (Pfad = Repository-Name).
- [ ] Impressum in `docs/privacy.html` prüfen: Der **Ort fehlt** hinter der PLZ 55758 (fehlt auch auf
      rainbow.kleinphilipp.de). Kontakt-Adresse `mail+snaplet@…` muss zustellbar sein. Verweis jetzt auf
      § 5 DDG (ersetzt seit Mai 2024 § 5 TMG).

## 4. Play Console

- [ ] Entwicklerkonto anlegen (25 $ einmalig, Identitätsprüfung).
- [ ] App anlegen: Name „Snaplet“, Standardsprache Deutsch oder Englisch, App, kostenlos.
- [ ] **Play App Signing**: beim ersten Release den von Google erzeugten App-Signaturschlüssel akzeptieren.
- [ ] **Zielgruppe und Inhalte**: Altersgruppen wählen (z. B. „5 und jünger“, „6–8“, Eltern); damit gilt die
      Families-Richtlinie. Werbung: nein.
- [ ] **Datensicherheit**: „Keine Daten erhoben“, „Keine Daten geteilt“. Datenschutz-URL aus Punkt 3.
- [ ] **Inhaltseinstufung** (IARC-Fragebogen).
- [ ] **App-Zugriff**: Hinweis für Prüfer: „Parent area: hold the lock icon in the bottom corner for 3 s“ (in der Sprache der Prüfer, also Englisch).
- [ ] **Store-Eintrag**: Texte aus `store/listing/<sprache>/`, Bilder aus dem Workflow
      **Actions → Store assets** (Artefakt: Screenshots je Sprache, `feature_graphic.png`, `icon_512.png`).
      Optional eigenes Kamerabild: URL oder Pfad im Repo (z. B. `store/camera.jpg`) als Eingabe.
- [ ] Bei privatem Entwicklerkonto: **geschlossener Test mit mindestens 12 Testern über 14 Tage**, erst danach
      ist „Produktion“ freigeschaltet. Vorher interner Test zum eigenen Prüfen.
- [ ] Release-Build (R8) auf mehreren echten Geräten prüfen: Android 8, aktuelles Android, ein Tablet.

## 5. Weitere Sprachen

- [ ] **App übersetzen**, je Sprache:
  - `app/src/main/res/values-<sprache>/strings.xml` als Kopie von `values/strings.xml` (Englisch) anlegen,
    Einträge mit `translatable="false"` weglassen. `TranslationsTest` meldet fehlende oder überzählige Einträge.
  - In `StoreScreenshotTest.kt` eine Klasse `StoreScreenshot<Sprache>` und den Werbespruch in `Tagline` ergänzen.
  - `store/whatsnew/whatsnew-<sprache>-<LAND>` anlegen.
  - Kandidaten: Französisch, Spanisch, Italienisch, Niederländisch, Polnisch, Türkisch, Portugiesisch (BR).
- [ ] **Play-Store-Texte übersetzen**: `store/listing/<sprache>-<LAND>/` mit `title.txt` (max. 30 Zeichen),
      `short_description.txt` (max. 80) und `full_description.txt` (max. 4000) und in der Play Console unter
      „Store-Eintrag → Übersetzungen“ eintragen.
- [ ] Datenschutzerklärung (`docs/privacy.html`) um die neuen Sprachen ergänzen.
