# NovaTV — Özellik Karşılaştırması

NovaTV, TiviMate'in **özellik setini ve kullanıcı deneyimini** hedefler.
TiviMate'in kodu, logosu, ikonları veya varlıkları **kopyalanmamıştır**;
tüm kod ve görseller NovaTV için sıfırdan yazılmıştır.

## Ana özellikler

| Özellik | NovaTV | Not |
|---|:---:|---|
| M3U / M3U8 playlist | ✅ | `#EXTINF`, `group-title`, `tvg-*` |
| Xtream Codes | ✅ | `player_api.php`, `xmltv.php`, timeshift |
| Stalker Portal (MAG) | ✅ | handshake, `get_ordered_list`, `create_link` |
| XMLTV EPG | ✅ | `XmlPullParser`, streaming |
| Harici EPG URL'i | ✅ | Playlist başına ayrı EPG |
| Kanal arama (isim) | ✅ | Room index ile |
| Program arama (başlık/açıklama) | ✅ | |
| Favoriler | ✅ | |
| İzleme listesi | ✅ | |
| Son izlenenler + konum | ✅ | |
| Catch-up / Geçmiş | ✅ | `catchup-source` şablonları, Xtream `timeshift.php` |
| Yerel DVR | ✅ | MPEG-TS, byte-level tee |
| Zamanlanmış kayıt | ⚠️ | Şema hazır, tetikleme altyapısı eksik |
| Multiview (2–4) | ✅ | Cihaz donanım sınırına bağlı |
| Program detay kartı | ✅ | |
| Ebeveyn kilidi (PIN) | ✅ | Sanal rakam ızgarası (uzaktan kumanda) |
| 10 vurgu rengi | ✅ | |
| Arayüz yoğunluğu + punto | ✅ | 48–56sp taban (10-feot) |
| Yedekleme / geri yükleme | ⚠️ | Android Auto Backup etkin; manuel export/geri yükleme eksik |
| PiP | ⚠️ | Activity bazlı; sistem PiP entegrasyonu bekliyor |

## Oynatma

| Özellik | NovaTV |
|---|:---:|
| ExoPlayer (HLS/DASH/progressive) | ✅ |
| ExoPlayer (RTSP/SmoothStreaming) | ✅ |
| libVLC yedek motoru (TS/RTMP/RTSP) | ✅ |
| Otomatik motor seçimi + hata geri düşüşü | ✅ |
| İki kademeli ilerleme çubuğu | ✅ |
| Alt kanal karuseli + chevron | ✅ |
| Ses parçası / altyazı seçimi | ⚠️ |
| Zaman kaydırma (timeshift) | ⚠️ |
| Donanım çözücü doğrulaması | ✅ `MediaCodecList` |
| Ön plan servisi (`mediaPlayback`) | ✅ |
| Dolby Vision / HDR | ⏸️ Bilinçli olarak pasif |

## Bilinçli kararlar

- **Hilt kullanılmadı.** Tek modüllü projede DI katmanı için
  AGP 9 + KSP2 + Kotlin 2.4.20 üçlüsünde uyum riski alınmadı;
  `AppContainer` ile elle DI kuruldu.
- **Yerel kayıt MPEG-TS olarak yazılır**, MP4 olarak değil.
  Media3'ün genel API'si canlı yayın → MP4 kaydını desteklemiyor
  (`media3-transformer` yalnızca hazır dosyaları işler; `media3-muxer`
  MP4/WebM/Ogg/WAV üretir, TS yok). Ham `.ts` doğrudan oynatılabilir.
- **`androidx.tv.material3` içinde `TextField` ve `Dialog` yoktur.**
  Metin girişi `BasicTextField` üzerine özel bir TV bileşeniyle
  (`TvTextField`), seçici ise tam ekran modal yüzeyle çözüldü.
- **minSdk 24, targetSdk 34.** Cihaz Android 11/12; Play Store dağıtımı
  hedeflenmediği için targetSdk yükseltilmedi.

## Yapı zinciri notları

| Bileşen | Sürüm | Gerekçe |
|---|---|---|
| Gradle | 9.6.0 | AGP 9.4 minimum |
| AGP | 9.4.1 | built-in Kotlin varsayılan |
| compileSdk | **37** | Compose 1.12.1 zorunlu kılıyor |
| Kotlin | 2.4.20 | `kotlin.android` **uygulanmaz** |
| KSP | 2.3.12 | KSP2; KSP1 AGP 9 ile uyumsuz |
| Compose BOM | 2026.09.00 | → Compose 1.12.1 |
| Media3 | 1.11.1 | minCompileSdk 36 |
