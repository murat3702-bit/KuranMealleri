package com.mb.kuranmealleri

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import java.io.IOException
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature

class MainActivity : AppCompatActivity() {

    companion object {
        private const val MAX_BACKUP_BYTES = 25 * 1024 * 1024
    }

    private lateinit var web: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    // Yönetici panelindeki "resim seç" için dosya seçici sonucu
    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val uris = WebChromeClient.FileChooserParams.parseResult(r.resultCode, r.data)
        fileCallback?.onReceiveValue(uris)
        fileCallback = null
    }

    // ---- Yedekleme: Storage Access Framework (izin gerektirmez) ----
    private var pendingBackup: String? = null

    /** "Yedekle (Dosya Kaydet)": sistemin dosya kaydetme penceresi (ACTION_CREATE_DOCUMENT). */
    private val createBackupDoc =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            val text = pendingBackup
            pendingBackup = null
            if (uri == null || text == null) {
                callJs("window.__onBackupSaved&&window.__onBackupSaved(false,'cancel')")
                return@registerForActivityResult
            }
            Thread {
                try {
                    val out = contentResolver.openOutputStream(uri, "wt")
                        ?: throw IOException("Dosya açılamadı")
                    out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    callJs("window.__onBackupSaved&&window.__onBackupSaved(true,'')")
                } catch (e: Exception) {
                    callJs("window.__onBackupSaved&&window.__onBackupSaved(false," + JSONObject.quote(e.message ?: "bilinmeyen hata") + ")")
                }
            }.start()
        }

    /** "Yedek Dosyası Yükle": sistemin dosya seçicisi (ACTION_OPEN_DOCUMENT). */
    private val openBackupDoc =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                callJs("window.__onBackupPicked&&window.__onBackupPicked(null,'cancel')")
                return@registerForActivityResult
            }
            Thread {
                try {
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IOException("Dosya açılamadı")
                    if (bytes.size > MAX_BACKUP_BYTES) throw IOException("Dosya çok büyük")
                    val text = String(bytes, Charsets.UTF_8)
                    callJs("window.__onBackupPicked&&window.__onBackupPicked(" + JSONObject.quote(text) + ")")
                } catch (e: Exception) {
                    callJs("window.__onBackupPicked&&window.__onBackupPicked(null," + JSONObject.quote(e.message ?: "bilinmeyen hata") + ")")
                }
            }.start()
        }

    private fun callJs(script: String) {
        runOnUiThread { if (::web.isInitialized) web.evaluateJavascript(script, null) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        web = WebView(this)
        setContentView(web)

        // İçerik https://appassets.androidplatform.net/assets/... adresinden, cihaz içinden sunulur.
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true      // yer imleri, notlar ve ayarlar (localStorage) burada saklanır
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(false)
        }
        applyDark()

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.url.host == WebViewAssetLoader.DEFAULT_DOMAIN) return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                } catch (_: Exception) {
                }
                return true
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return try {
                    picker.launch(params.createIntent())
                    true
                } catch (e: Exception) {
                    fileCallback = null
                    false
                }
            }
        }
        web.addJavascriptInterface(Bridge(), "AndroidBridge")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("(window.__androidBack && window.__androidBack()) ? 1 : 0") { r ->
                    if (r != "1") {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })

        web.loadUrl("https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/www/index.html")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyDark()
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
    }

    override fun onPause() {
        web.onPause()
        super.onPause()
    }

    /** Sistem koyu temadaysa sayfadaki prefers-color-scheme: dark devreye girsin. */
    @Suppress("DEPRECATION")
    private fun applyDark() {
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(web.settings, false)
        } else {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(
                    web.settings,
                    if (night) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                )
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                WebSettingsCompat.setForceDarkStrategy(
                    web.settings,
                    WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY
                )
            }
        }
    }

    /** Sayfadan çağrılan yerel işlevler (paylaş, kopyala, durum/gezinme çubuğu rengi). */
    inner class Bridge {
        @JavascriptInterface
        fun share(text: String) {
            runOnUiThread {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                startActivity(Intent.createChooser(send, null))
            }
        }

        @JavascriptInterface
        fun copy(text: String): Boolean {
            runOnUiThread {
                val cm = this@MainActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("ayet", text))
            }
            return true
        }

        /** Yedek metnini kullanıcının seçtiği klasöre .json dosyası olarak kaydettirir. */
        @JavascriptInterface
        fun saveBackup(json: String, fileName: String) {
            val safe = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "kuran_uygulamasi_yedek.json" }
            runOnUiThread {
                pendingBackup = json
                try {
                    createBackupDoc.launch(safe)
                } catch (e: Exception) {
                    pendingBackup = null
                    callJs("window.__onBackupSaved&&window.__onBackupSaved(false," + JSONObject.quote(e.message ?: "dosya penceresi açılamadı") + ")")
                }
            }
        }

        /** Dosya seçiciyi açar; seçilen yedeğin metnini sayfaya iletir (doğrulama sayfada yapılır). */
        @JavascriptInterface
        fun pickBackup() {
            runOnUiThread {
                try {
                    openBackupDoc.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
                } catch (e: Exception) {
                    callJs("window.__onBackupPicked&&window.__onBackupPicked(null," + JSONObject.quote(e.message ?: "dosya seçici açılamadı") + ")")
                }
            }
        }

        @Suppress("DEPRECATION")
        @JavascriptInterface
        fun setBars(cssColor: String) {
            val m = Regex("""rgba?\(\s*(\d+)[,\s]+(\d+)[,\s]+(\d+)""").find(cssColor) ?: return
            val (r, g, b) = m.destructured
            val color = Color.rgb(r.toInt(), g.toInt(), b.toInt())
            runOnUiThread {
                window.statusBarColor = color
                window.navigationBarColor = color
                web.setBackgroundColor(color)
                val light = ColorUtils.calculateLuminance(color) > 0.5
                val ctl = WindowInsetsControllerCompat(window, window.decorView)
                ctl.isAppearanceLightStatusBars = light
                ctl.isAppearanceLightNavigationBars = light
            }
        }
    }
}
