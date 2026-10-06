# Kur'an Mealleri — Android projesi

Yenilenmiş arayüzlü uygulamanın Android (WebView) sarmalayıcısı.
9 meal, 48 makale ve yönetici paneli; internet izni yok, tamamen çevrimdışı çalışır.

## APK'yı üretmenin iki yolu

**1) Bilgisayarsız: GitHub Actions**
1. Yeni bir GitHub deposu aç, bu klasörün içindekileri yükle (`.github` klasörü dahil).
2. Depoda *Actions → APK derle → Run workflow*.
3. İş bitince sayfanın altındaki **kuran-mealleri-apk** dosyasını indir, zip'in içindeki `.apk`'yı telefona kur.

**2) Android Studio**
1. *File → Open* ile bu klasörü aç, Gradle eşitlemesini bekle.
2. *Build → Build APK(s)* ya da telefonu bağlayıp ▶ Run.

## Yönetici paneli
Ayarlar (sağ üstteki simge) → **Yönetici paneli**. İlk açılışta PIN yoktur; *Güvenlik* bölümünden PIN belirle.
Ana sayfa metin/resmi, makaleler (ekle, düzenle, sil), meal ayetleri (düzenle, eksik ayet ekle) ve kendi mealin buradan yönetilir.
Veriler yalnızca telefonda saklanır; *Yedekle / geri yükle* ile kopyalayıp başka yere kaydedebilirsin.
Uygulama verisi silinirse (Ayarlar → Uygulamalar → Depolamayı temizle) yönetici verilerin de gider; önce yedek al.

## Bilmen gerekenler
- Paket adı orijinalle aynı: `com.mb.Kuranix` (sürüm 2.0, kod 9). Debug derlemesi `com.mb.Kuranix.yeni` olarak yan yana kurulur; eski uygulamayı silmene gerek yok.
- Mevcut uygulamanın üzerine güncelleme olarak kurmak için release derlemesini **orijinal anahtar dosyanla (keystore)** imzalaman gerekir. `keystore.properties` dosyası oluştur:
  ```
  storeFile=anahtar.jks
  storePassword=...
  keyAlias=...
  keyPassword=...
  ```
  sonra `gradle assembleRelease`. Anahtar yoksa yeni bir uygulama olarak yayınlanır.
- Eski uygulamadaki yer imleri (SQLite) bu sürüme otomatik aktarılmaz; yenisi kendi kayıtlarını tutar.
- Sistem yazı tipleri kullanılır. Google Fonts (Fraunces/Literata) istersen `AndroidManifest.xml`'e INTERNET izni ekleyip `www/index.html` içine fonts.googleapis.com bağlantısını geri koyabilirsin.
- Gerekli: güncel Android System WebView (Android 8.0+).

## Mimari not: kalıcı tercih ve geçici gezinme
- `activeSortOrder` (Mushaf/Nüzul) ve `activeAuthorId` (varsayılan meal) yalnızca `Prefs` nesnesi üzerinden, kullanıcı ayarlardan/meal seçicilerden seçtiğinde değişir (localStorage, tek doğruluk kaynağı).
- Yer imi, "Kaldığın yer", arama sonucu gibi geçici atlamalar `navigateToVerse({targetSurahId, targetVerseNumber, overrideMealId})` ile açılır; `overrideMealId` yalnızca okuma ekranının yerel durumuna (`R.tr`/`R.ov`) uygulanır ve ekran kapanınca silinir.
- Yer imi kaydı: `surahId:verseNumber` (sabit Mushaf no 1–114) + o anki meal (`overrideMealId` olarak kullanılır). Sıralama modu artık yer iminde saklanmaz.
- Varsayılan stiller: Konu Başlıkları Serif/17, Ana Sayfa metinleri Serif/16 (`DEF` sabiti); yönetici panelinden değiştirilebilir.

## Yazar Biyografileri (yönetici paneli)
- Panel → "Yazar Biyografileri": tüm yazarlar (meal yazarları + ek yazarlar) ve biyografi durumu (Var/Yok) listelenir; ekle/düzenle formunda yazar adı ve çok satırlı biyografi (## ara başlık, **kalın**, *italik*, canlı önizleme) bulunur.
- Veri `ADM.bios` altında saklanır ve yönetici yedeğine dahildir. Biyografi, meal bilgi kartında (openTrInfo) gösterilir.
- Sunucu olmadığından "API" yerel bir katmandır (`BioAPI`): `list` (GET /authors), `get` (GET /authors/:id), `create` (POST /authors), `update` (PUT /authors/:id). Oluşturma/güncelleme yalnızca yönetici paneli açıkken çalışır; silme tanımlı değildir.

## Yedekleme (dosya tabanlı)
- **Yedekle (Dosya Kaydet):** Android'in dosya kaydetme penceresi (`ACTION_CREATE_DOCUMENT`) açılır; önerilen ad `kuran_uygulamasi_yedek_GGAAYYYY.json`.
- **Yedek Dosyası Yükle:** Dosya seçici (`ACTION_OPEN_DOCUMENT`) açılır; dosya doğrulanır, onay alınır ve veriler yüklenip uygulama yenilenir.
- Kapsam: tüm ayarlar (tema, yazı, seçili meal, sıralama, arama kapsamı…), yer imleri, kaldığın yer, notlar, vurgular, kayıtlı makaleler, son aramalar ve yönetici içeriği (ana sayfa, makaleler, ek mealler, biyografiler, yazı tipleri). Yönetici PIN'i yedeğe girmez.
- İzin gerekmez (SAF). Kotlin tarafı: `MainActivity` → `saveBackup` / `pickBackup`; JS tarafı: `buildBackup`, `parseBackup`, `applyBackup`.

## Yazar silme (yönetici paneli → Yazar Biyografileri)
- Her yazar satırında "Yazarı ve Tüm Meallerini Sil" butonu; onay penceresi: "Bu yazarı ve yazara ait tüm mealleri silmek istediğinize emin misiniz? Bu işlem geri alınamaz."
- `AuthorAPI.remove(id)` (DELETE /authors/:id eşdeğeri): önce yazarın meal kayıtları (girilen ayetler), sonra meal/yazar kaydı (ek meal silinir; yerleşik meal `ADM.hidden` ile kaldırılır), sonra biyografi, sonra yer imlerindeki meal referansları temizlenir. Hata olursa tüm değişiklik geri alınır.
- Son kalan meal silinemez; yalnızca yönetici paneli açıkken çalışır. Silinen yerleşik yazarlar yedek dosyasıyla da taşınır (`hidden`).

## Yerleşik ve dinamik mealler
- **Yerleşik (silinemez):** Diyanet İşleri, E. Hamdi Yazır, Edip Yüksel, Y. Nuri Öztürk.
- **Dinamik (yönetici panelinden silinebilir):** diğer 10 meal (`"dyn":true`) ve sonradan eklenen mealler. Yazar listesinde "Dinamik" / "Eklenen" etiketi ve "Yazarı ve Tüm Meallerini Sil" butonu vardır; yerleşik mealde buton yoktur.
- Not: dinamik meallerin verisi APK içinde durmaya devam eder; silinen meal uygulamadan tamamen kalkar (liste, okuma, arama), ancak APK boyutu küçülmez. Geri almak için yedek dosyası ya da uygulama verisini sıfırlamak gerekir.

## Makale silme
- Yönetici → Makaleler: her satırda "Makaleyi Sil"; onay: "Bu makaleyi silmek istediğinize emin misiniz?". `ArticleAPI.remove(id)` (DELETE /articles/:id eşdeğeri): düzenlemeler, makale kaydı ve kullanıcının kaydedilenler listesindeki kayıt temizlenir; hata olursa geri alınır.

## Kalıcı silme (hard delete) ve paketten arındırma
- Admin panelinden silinen **eklenen** meal/makaleler veritabanından tamamen silinir.
- Pakete gömülü (dinamik) meal ve makaleler çalışırken listeden ve okumadan kalıcı olarak kalkar. "Silinenler / Geri getir" bölümü kaldırıldı; geri getirme yolu yoktur (yalnızca yedek geri yükleme).
- **Uygulama verisi sıfırlanırsa:** APK'nın içindeki kopya yeniden okunacağından, silinen paket içeriği geri görünür. Bunu kökten çözmek için içeriğin APK'dan çıkarılması gerekir:
  1. Uygulamada Kayıtlar → Yedekle (Dosya Kaydet) ile yedek alın.
  2. `python tools/prune_deleted.py yedek.json` (ön izleme) ve `--apply` ile `index.html` içindeki meta, meal ve makale verilerinden silinenleri gerçekten çıkarın (`index.html.bak` yedeği alınır).
  3. APK'yı yeniden derleyin. Silinen içerik artık pakette yoktur; sıfırlama sonrası yalnızca kalan (aktif) varsayılan veriler yüklenir.
