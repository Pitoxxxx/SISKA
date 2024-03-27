package com.example.siska

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : AppCompatActivity() {

//    Deklarasi WebView
    lateinit var webView: WebView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.WV)
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://siska.kimiafarma.co.id/")

//        Web Setting
        val webSettings = webView.settings

//        Mengaktifkan Javascript
        webSettings.javaScriptEnabled = true

//        Mengaktifkan Tools Lain
        webSettings.domStorageEnabled = true
    }
}