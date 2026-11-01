# TODO bis zum Play Store

Reihenfolge = empfohlene Abarbeitung. Abhaken mit `[x]`.

## 1. Package-ID festlegen (vor dem ersten Upload!)

Die Package-ID (`applicationId` in `app/build.gradle.kts`: `de.kleinphilipp.snaplet`) ist die technische, weltweit
eindeutige Kennung der App. Android erkennt daran Updates, Google Play die App
(`play.google.com/store/apps/details?id=<Package-ID>`). **Nach dem ersten Upload ist sie für immer fest.**
Der angezeigte Name („Snaplet“) lässt sich dagegen jederzeit ändern.

- [x] Entscheiden: `app.tinycam` behalten oder passend zum neuen Namen ändern.
      Empfehlung: `de.kleinphilipp.snaplet` (umgekehrte eigene Domain, kollidiert garantiert mit niemandem).
- [x] Falls geändert: `applicationId` in `app/build.gradle.kts` und `packageName` in
      `.github/workflows/release.yml` anpassen. `namespace` (Kotlin-Paket `app.tinycam`) kann bleiben.
      Achtung: Per APK installierte Testgeräte bekommen dann eine zweite, neue App; vorher Fotos sichern.
- [x] Namen „Snaplet“ prüfen: Suche im Play Store, Markenregister
      ([DPMAregister](https://register.dpma.de), [TMview](https://www.tmdn.org/tmview)) in Klasse 9.

## 2. Upload-Schlüssel anlegen und als GitHub-Secrets hinterlegen

Google Play signiert die App mit einem eigenen Schlüssel („Play App Signing“). Unser Schlüssel ist nur der
**Upload-Schlüssel**, mit dem Google prüft, dass ein Upload von uns kommt. Geht er verloren, kann der
Play-Support ihn zurücksetzen – trotzdem sicher aufbewahren.

1. [x] Schlüssel lokal erzeugen (Java/JDK nötig; `keytool` fragt Passwort und Namen ab):
   ```bash
   keytool -genkeypair -v -keystore snaplet-upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
   ```
   Für Keystore- und Schlüssel-Passwort darf dasselbe starke Passwort genutzt werden.
2. [x] `snaplet-upload.jks` und das Passwort im Passwort-Manager sichern. **Nie ins Repo einchecken.**
3. [x] Keystore als Base64 kodieren:
   ```bash
   base64 -w0 snaplet-upload.jks > snaplet-upload.b64
   ```
   (macOS: `base64 -i snaplet-upload.jks -o snaplet-upload.b64`)
4. [x] Auf GitHub: Repository → **Settings → Secrets and variables → Actions → New repository secret**,
   vier Secrets anlegen:

   | Name | Wert |
   |---|---|
   | `UPLOAD_KEYSTORE_BASE64` | Inhalt von `snaplet-upload.b64` |
   | `UPLOAD_KEYSTORE_PASSWORD` | Keystore-Passwort |
   | `UPLOAD_KEY_ALIAS` | `upload` (bzw. der gewählte Alias) |
   | `UPLOAD_KEY_PASSWORD` | Schlüssel-Passwort |
5. [x] `snaplet-upload.b64` löschen.
6. [x] Testlauf: unter **Actions → Play release → Run workflow** Version `v1.0.0` eingeben. Ergebnis: Artefakt
   `Snaplet-v1.0.0-aab` mit `app-release.aab`. Fehlt ein Secret, bricht der Lauf mit einer klaren Meldung ab.
   Später reicht ein Tag: `git tag v1.0.1 && git push origin v1.0.1`.
   Versionsschema: `vX.Y.Z` → versionCode `X*10000 + Y*100 + Z` (Y und Z unter 100). Jede Play-Version braucht
   einen höheren versionCode, also für jeden Upload eine neue Version.
7. [ ] Optional, automatischer Upload aus GitHub (Releases und Store-Eintrag): Dienstkonto einrichten, siehe
   [Zugang für Uploads](#zugang-für-uploads-play_service_account_json). Erst nutzbar, nachdem das erste AAB von Hand
   hochgeladen wurde. Bis die App einmal veröffentlicht ist, den Workflow mit Status `draft` laufen lassen.

### Zugang für Uploads (`PLAY_SERVICE_ACCOUNT_JSON`)

Ein Dienstkonto ist ein technischer Google-Nutzer mit eigenem Schlüssel. Die Workflows „Play release“ und
„Store assets“ melden sich damit bei der Google Play Developer API an.

1. [ ] [Google Cloud Console](https://console.cloud.google.com) mit demselben Google-Konto wie die Play Console
   öffnen, oben **Projekt auswählen → Neues Projekt**, z. B. `snaplet-play`. Kostenlos, kein Rechnungskonto nötig.
2. [ ] **APIs & Dienste → Bibliothek** → „Google Play Android Developer API“ → **Aktivieren**.
3. [ ] **IAM & Verwaltung → Dienstkonten → Dienstkonto erstellen**: Name z. B. `github-play-upload`. Rollen im
   Cloud-Projekt sind nicht nötig, Schritte 2 und 3 des Assistenten überspringen.
4. [ ] Dienstkonto öffnen → **Schlüssel → Schlüssel hinzufügen → Neuen Schlüssel erstellen → JSON**. Die Datei
   wird heruntergeladen. Sie ist ein Passwort: nicht ins Repo, nicht weitergeben.
   (Meldet Google „Erstellung von Dienstkontoschlüsseln ist deaktiviert“, verbietet eine Organisationsrichtlinie
   das; bei privaten Konten ohne Organisation kommt das nicht vor.)
5. [ ] E-Mail-Adresse des Dienstkontos kopieren (`github-play-upload@snaplet-play.iam.gserviceaccount.com`).
6. [ ] [Play Console](https://play.google.com/console) → **Nutzer und Berechtigungen → Neue Nutzer einladen** →
   diese E-Mail-Adresse. Unter **App-Berechtigungen** die App Snaplet hinzufügen und mindestens erlauben:
   - App-Informationen ansehen (nur Lesezugriff)
   - Store-Präsenz verwalten (für „Store assets“)
   - Releases für Test-Tracks erstellen, bearbeiten und einführen (für „Play release“)
   - später zusätzlich: Releases in der Produktion veröffentlichen

   **Nutzer einladen** → bestätigen. Eine Einladungs-Mail muss das Dienstkonto nicht annehmen.
7. [ ] Auf GitHub: **Settings → Secrets and variables → Actions → New repository secret**, Name
   `PLAY_SERVICE_ACCOUNT_JSON`, Wert = kompletter Inhalt der JSON-Datei (mit `{` und `}`).
8. [ ] JSON-Datei im Passwort-Manager ablegen und vom Rechner löschen. Bei Verlust in der Cloud Console einen
   neuen Schlüssel erzeugen, den alten löschen und das Secret ersetzen.
9. [ ] Test: **Actions → Store assets → Run workflow** mit „Play store listing“ = `validate`. Grün = Zugang passt,
   nichts wurde gespeichert. Meldet Google „The caller does not have permission“, sind die Berechtigungen aus
   Schritt 6 noch nicht aktiv (kann bis zu 24 h dauern) oder das erste AAB wurde noch nicht von Hand hochgeladen.

## 3. Datenschutzerklärung veröffentlichen (GitHub Pages)

- [x] Repository → **Settings → Pages → Build and deployment**: Source „Deploy from a branch“, Branch `main`,
      Ordner `/docs`. Pages braucht ein öffentliches Repository (oder einen kostenpflichtigen GitHub-Plan).
- [x] Repository umbenennen: **Settings → General → Repository name** → `Snaplet`. GitHub leitet alte Git- und
      Web-URLs weiter, GitHub Pages aber nicht. Lokale Klone danach umstellen:
      `git remote set-url origin https://github.com/Sarrdai/Snaplet.git`
- [x] Prüfen: `https://sarrdai.github.io/Snaplet/privacy.html` (Pfad = Repository-Name).
- [ ] Impressum in `docs/privacy.html` prüfen: Anschrift ist jetzt 20359 Hamburg (auf
      rainbow.kleinphilipp.de steht noch die falsche PLZ 55758 ohne Ort). Kontakt-Adresse `mail+snaplet@…` muss zustellbar sein. Verweis jetzt auf
      § 5 DDG (ersetzt seit Mai 2024 § 5 TMG).

## 4. Play Console

- [x] Entwicklerkonto anlegen (25 $ einmalig, Identitätsprüfung).
- [x] App anlegen: Name „Snaplet“, Standardsprache Deutsch oder Englisch, App, kostenlos.
- [ ] **Play App Signing**: beim ersten Release den von Google erzeugten App-Signaturschlüssel akzeptieren.
- [x] **Zielgruppe und Inhalte**: Altersgruppen wählen (z. B. „5 und jünger“, „6–8“, Eltern); damit gilt die
      Families-Richtlinie. Werbung: nein.
- [x] **Datensicherheit**: „Keine Daten erhoben“, „Keine Daten geteilt“. Datenschutz-URL aus Punkt 3.
- [x] **Inhaltseinstufung** (IARC-Fragebogen).
- [x] **App-Zugriff**: Hinweis für Prüfer: „Parent area: hold the lock icon in the bottom corner for 3 s“ (in der Sprache der Prüfer, also Englisch).
- [ ] **Store-Eintrag**: Texte aus `store/listing/<sprache>/`, Bilder aus dem Workflow
      **Actions → Store assets** (Artefakt: Screenshots je Sprache, `feature_graphic.png`, `icon_512.png`).
      Optional eigenes Kamerabild: URL oder Pfad im Repo (z. B. `store/camera.jpg`) als Eingabe.
      Automatisch: „Play store listing“ = `upload` legt alle Sprachen an und ersetzt Texte, Screenshots,
      Feature-Grafik und Icon (braucht `PLAY_SERVICE_ACCOUNT_JSON` und das erste AAB). Kategorie, Kontaktdaten
      und Datenschutz-URL bleiben Handarbeit in der Console.
- [ ] Bei privatem Entwicklerkonto: **geschlossener Test mit mindestens 12 Testern über 14 Tage**, erst danach
      ist „Produktion“ freigeschaltet. Vorher interner Test zum eigenen Prüfen.
- [ ] Release-Build (R8) auf mehreren echten Geräten prüfen: Android 8, aktuelles Android, ein Tablet.

## 5. Weitere Sprachen

52 Sprachen sind übersetzt (App, Store-Texte, „Neuigkeiten“, Werbespruch der Feature-Grafik). Auswahl nach
Verbreitung von Android/Google Play. Übersetzt wurde maschinell (KI) – vor der Veröffentlichung möglichst
von Muttersprachlern gegenlesen lassen, vor allem Store-Titel und Kurzbeschreibung.

- [x] **App übersetzen** (`app/src/main/res/values-<sprache>/strings.xml`), `TranslationsTest` prüft Vollständigkeit.
  Sprachcodes mit Android-Altlasten: Indonesisch `values-in`, Hebräisch `values-iw`, Filipino `values-tl`,
  Norwegisch `values-nb`.
- [x] **Play-Store-Texte** in `store/listing/<play-locale>/` und `store/whatsnew/whatsnew-<play-locale>`.
  Mehrere Play-Locales teilen sich einen Text: `es-ES`/`es-419`/`es-US`, `fr-FR`/`fr-CA`, `zh-TW`/`zh-HK`,
  `ms`/`ms-MY`.
- [ ] Alle Sprachen aus `store/listing/` im Store-Eintrag anlegen: automatisch per **Store assets** mit
  „Play store listing“ = `upload`, oder von Hand unter **Store-Eintrag → Übersetzungen verwalten**. Nötig **vor**
  dem ersten automatischen Release-Upload, sonst kann der Upload der Release-Notes aus `store/whatsnew/` für
  fehlende Sprachen fehlschlagen. Alternativ nicht benötigte `whatsnew-*`-Dateien löschen.
- [ ] Store-Screenshots je Sprache prüfen (Workflow **Store assets**): Schriften ohne Baloo-2-Abdeckung
  (z. B. CJK, Arabisch, Thai, indische Schriften außer Devanagari) fallen auf Systemschriften zurück.
- [x] RTL: Elternbereich und PIN-Eingabe spiegeln sich für Arabisch, Persisch, Urdu und Hebräisch
  (Tastenfeld bleibt 1-2-3). Die Kinderkamera bleibt bewusst links-nach-rechts, weil „Auslöser links/rechts“
  die echte Seite meint. Auf einem Gerät mit arabischer Systemsprache einmal durchklicken.
- [x] Screenreader: alle Tipp- und Halte-Gesten haben Bedienungshilfe-Aktionen (Halten = „doppelt tippen und
  halten“), Schalter/Optionen melden ihren Zustand, Regler sind per Lautstärketaste einstellbar
  (`AccessibilityTest`). Offen: einmal mit TalkBack auf einem echten Gerät prüfen.
- [ ] Datenschutzerklärung (`docs/privacy.html`) ggf. um weitere Sprachen ergänzen.
- Neue Sprache später: `values-<sprache>/strings.xml` aus `values/strings.xml` kopieren (ohne
  `translatable="false"`), Klasse `StoreScreenshot<Sprache>` + `Tagline` in `StoreScreenshotTest.kt`,
  Store-Texte wie oben.
