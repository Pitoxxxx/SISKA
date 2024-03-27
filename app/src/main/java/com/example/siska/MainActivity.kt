package com.example.siska

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : AppCompatActivity() {

    // Deklarasi WebView
    private lateinit var webView: WebView

    // Deklarasi variabel untuk menangani upload file
    private var uploadMessage: ValueCallback<Array<Uri>>? = null
    private var uploadMessageAboveL: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_RESULT_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.WV)
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://siska.kimiafarma.co.id/")

        // Web Setting
        val webSettings = webView.settings

        // Mengaktifkan Javascript
        webSettings.javaScriptEnabled = true

        // Mengaktifkan Tools Lain
        webSettings.domStorageEnabled = true

        // Mendaftarkan WebChromeClient untuk menangani upload file
        webView.webChromeClient = object : WebChromeClient() {
            // For Android < 3.0
            fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
                uploadMessage = ValueCallback<Array<Uri>> { value -> uploadMsg.onReceiveValue((value.firstOrNull()?.let { arrayOf(it) } ?: emptyArray()) as Uri?) }
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "image/*"
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FILE_CHOOSER_RESULT_CODE)
            }

            // For Android >= 3.0
            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String?) {
                openFileChooser(uploadMsg)
            }

//            // For Android >= 4.1
//            override fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String?, capture: String?) {
//                openFileChooser(uploadMsg)
//            }

            // For Android >= 5.0
            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                if (uploadMessageAboveL != null) {
                    uploadMessageAboveL!!.onReceiveValue(null)
                    uploadMessageAboveL = null
                }
                uploadMessageAboveL = filePathCallback
                val intent = fileChooserParams!!.createIntent()
                try {
                    startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE)
                } catch (e: Exception) {
                    uploadMessageAboveL = null
                    return false
                }
                return true
            }
        }
    }

    // Override onActivityResult untuk menangani hasil dari aktivitas lain (misalnya, pemilihan file)
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (uploadMessage == null && uploadMessageAboveL == null) return
            val result = if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
            if (uploadMessageAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, intent)
            } else if (uploadMessage != null) {
                uploadMessage!!.onReceiveValue(arrayOf(result!!))
                uploadMessage = null
            }
        }
    }

    private fun onActivityResultAboveL(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessageAboveL == null) {
            return
        }
        val results = WebChromeClient.FileChooserParams.parseResult(resultCode, intent)
        uploadMessageAboveL!!.onReceiveValue(results)
        uploadMessageAboveL = null
    }
}
