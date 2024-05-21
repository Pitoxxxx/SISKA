package com.example.siska

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.webkit.DownloadListener
import android.webkit.URLUtil

class MainActivity : AppCompatActivity() {

    private val FILECHOOSER_RESULTCODE = 1
    private var mUploadMessage: ValueCallback<Array<Uri>>? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private var currentPhotoPath: String? = null

    // Mendefinisikan LocationManager
    private lateinit var locationManager: LocationManager

    // URL halaman utama dan dashboard
    private val HOME_URL = "https://helpdesk.yogalabs.cloud"
    private val DASHBOARD_URL = "https://helpdesk.yogalabs.cloud/dashboard"

    private var doubleBackToExitPressedOnce = false

    companion object {
        private const val MIN_TIME_BW_UPDATES: Long = 1000 * 60 * 1 // 1 minute
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Float = 10F // 10 meters
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Inisialisasi LocationManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        checkLocationPermission()

        // Pastikan aplikasi memiliki izin menulis ke penyimpanan eksternal
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission is granted
            loadWebView()
            getLocation()
        }
    }

    // Handling the result of the file chooser activity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (resultCode == Activity.RESULT_OK) {
                val result = if (data == null) null else data.data
                mUploadMessage?.onReceiveValue(arrayOf(result!!))
                mUploadMessage = null
            } else {
                mUploadMessage?.onReceiveValue(null)
                mUploadMessage = null
            }
        }
    }

    // Tangani respons izin dari pengguna
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadWebView()
                    getLocation()
                } else {
                    Toast.makeText(
                        this,
                        "Aplikasi memerlukan izin lokasi untuk bekerja dengan baik",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun checkStoragePermission(): Boolean {
        val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun requestStoragePermission() {
        val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (!checkStoragePermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadWebView() {
        // Lakukan semua inisialisasi WebView dan muat URL di sini
        val webView: WebView = findViewById(R.id.WV)
        webView.webViewClient = WebViewClient()
        webView.loadUrl(HOME_URL)

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // Aktifkan mixed content mode jika perangkat menjalankan Android Lollipop atau versi yang lebih baru
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (mUploadMessage != null) {
                    mUploadMessage?.onReceiveValue(null)
                    mUploadMessage = null
                }
                mUploadMessage = filePathCallback

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "image/*"

                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Pilih Aksi")

                startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE)
                return true
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val request = DownloadManager.Request(Uri.parse(url))

            // Set MIME type dari informasi yang diterima
            request.setMimeType(mimeType)

            // Ambil cookie untuk unduhan
            val cookies: String = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("cookie", cookies)
            request.addRequestHeader("User-Agent", userAgent)

            // Gunakan URLUtil untuk menebak nama file
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)

            // Menentukan lokasi penyimpanan
            val downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDirectory.exists()) {
                downloadDirectory.mkdirs()
            }

            // Set lokasi penyimpanan berdasarkan nama file
            val destinationUri = Uri.withAppendedPath(Uri.fromFile(downloadDirectory), fileName)
            request.setDestinationUri(destinationUri)

            // Tambahkan detail unduhan
            request.setDescription("Downloading $fileName...")
            request.setTitle(fileName)
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            // Enqueue unduhan
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)

            // Tampilkan pesan kepada pengguna
            Toast.makeText(applicationContext, "Downloading: $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
        currentPhotoPath = imageFile.absolutePath
        return imageFile
    }

    // Mendapatkan lokasi pengguna
    private fun getLocation() {
        // Cek apakah GPS atau jaringan tersedia
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        ) {
            // Request pembaruan lokasi
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                locationListener
            )
        } else {
            // GPS atau jaringan tidak tersedia, tampilkan pesan kepada pengguna
            Toast.makeText(
                this,
                "GPS atau jaringan tidak tersedia, aktifkan untuk mendapatkan lokasi",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Mendefinisikan listener untuk mendengarkan perubahan lokasi
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Lokasi pengguna berubah, Anda bisa menggunakan data lokasi di sini
            val latitude = location.latitude
            val longitude = location.longitude

            // Misalnya, Anda dapat menampilkan lokasi pengguna dalam Logcat
            Log.d("Location", "Latitude: $latitude, Longitude: $longitude")
        }
    }

    override fun onBackPressed() {
        val webView: WebView = findViewById(R.id.WV)
        if (webView.url == DASHBOARD_URL) {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed()
                return
            }

            this.doubleBackToExitPressedOnce = true
            Toast.makeText(this, "Tekan sekali lagi untuk keluar aplikasi", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                doubleBackToExitPressedOnce = false
            }, 2000) // 2 detik
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
