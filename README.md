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
