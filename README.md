# NovaTV

Android TV IPTV istemcisi · **TCL 65Q6C** hedefli.

M3U, Xtream Codes ve Stalker Portal (MAG) desteği; EPG, arama, favori,
catch-up, DVR, multiview, PIN kilidi, tema ve yedekleme.

Türkçe arayüz. Tam klavye/uzaktan kumanda deneyimi (dokunmatik yok).

> TiviMate'in kodu, logosu, ikonları veya varlıkları **kopyalanmamıştır**.
> NovaTV, benzer bir özellik setini kendi kodu ve kendi markasıyla sunar.

---

## Gereksinimler

| | |
|---|---|
| Hedef cihaz | TCL 65Q6C (arm64-v8a, Android 11/12) |
| minSdk | 24 |
| targetSdk | 34 |
| compileSdk | 37 |
| JDK | 17 |
| Gradle | 9.6.0 |

## Derleme

### GitHub Actions (önerilen)

```bash
git push                       # build.yml tetiklenir
```

Actions → **Build TV APK** → *novatv-apk* artifact'ını indir.

Etiketle imzalı sürüm:

```bash
git tag v1.0.0 && git push --tags
```

`release.yml` imzalı APK'yı GitHub Releases'e yükler.

### Yerel

```bash
# JDK 17 ve Android SDK platform 37 + build-tools 36.0.0 gerekli
./gradlew assembleDebug
./gradlew installDebug
```

## İmzalama

Ana anahtar **asla** repoya girmez. GitHub Secrets'a eklenecekler:

| Secret | İçerik |
|---|---|
| `KEYSTORE_B64` | `base64 -w0 release.jks` çıktısı |
| `STORE_PASSWORD` | Keystore parolası |
| `KEY_ALIAS` | Ana takma adı |
| `KEY_PASSWORD` | Ana parolası |

Keystore üretmek:

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 \
        -validity 10000 -alias novatv
```

## TV'ye kurulum

```
Ayarlar → Güvenlik → Bilinmeyen kaynaklardan yükleme → etkinleştir
Dosya yöneticisi → novatv.apk → yükle
```

Ağ üzerinden:

```bash
adb connect <TV_IP>
adb install app-release.apk
```

---

## Mimari

```
app/src/main/kotlin/com/hp/novatv/
├─ core/
│  ├─ model/     Playlist, Channel, Program, Recording, …
│  ├─ util/      zaman, boyut, metin biçimleme
│  ├─ theme/     renk paleti, tipografi, yoğunluk
│  └─ focus/     D-pad odak yönetimi
├─ data/
│  ├─ db/        Room varlıkları, DAO'lar, NovaDatabase
│  ├─ prefs/     DataStore (UiSettings)
│  └─ repo/      ChannelRepository — tek veri merkezi
├─ source/
│  ├─ m3u/       M3uParser, XmltvParser, M3uAdapter
│  ├─ xtream/    XtreamAdapter + modeller
│  └─ stalker/   StalkerApi, StalkerAdapter
├─ player/
│  ├─ ExoPlayerEngine   (havuz, 4 slot)
│  ├─ VlcEngine         (yedek motor)
│  ├─ Recorder          (DataSource tee → .ts)
│  ├─ RecordingDataSource
│  ├─ PlaybackService   (foreground, mediaPlayback)
│  └─ DecoderProbe      (MediaCodecList)
└─ ui/
   ├─ components/  ortak TV bileşenleri (TvTextField, kartlar…)
   ├─ screens/     11 ekran
   ├─ nav/         rota tablosu
   ├─ MainActivity · PlayerActivity · PinActivity
   └─ NovaTvApp    AppContainer (elle DI)
```

## Kaynak adaptörleri

| Tür | Uçlar | Not |
|---|---|---|
| **M3U** | liste URL'i + XMLTV | `catchup-source` şablonları, `timeshift-url` |
| **Xtream** | `player_api.php`, `get.php`, `xmltv.php` | `timeshift.php` ile catch-up |
| **Stalker** | `portal_api.php` | handshake → profil → `create_link` |

## Bilinen sınırlar

- Yerel kayıt **MPEG-TS** yazar (MP4 değil) — medya3 muxer'ı TS üretmiyor
- Zamanlanmış kayıt tetikleme altyapısı eksik (şema hazır)
- Manuel yedekleme/geri yükleme ekranı eksik (Android Auto Backup var)
- Multiview 4K'da donanım sınırına takılır; 2 akış önerilir
- Dolby Vision / HDR bilinçli olarak desteklenmiyor

## Lisans

LGPL-2.1 (libVLC bağımlılığı nedeniyle)
