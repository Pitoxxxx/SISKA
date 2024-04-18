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
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val FILECHOOSER_RESULTCODE = 1
    private var mUploadMessage: ValueCallback<Array<Uri>>? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // Mendefinisikan LocationManager
    private lateinit var locationManager: LocationManager

    companion object {
        // Inisialisasi waktu minimum antara pembaruan lokasi (dalam milidetik)
        private const val MIN_TIME_BW_UPDATES: Long = 1000 * 60 * 1 // 1 minute

        // Inisialisasi jarak minimum antara pembaruan lokasi (dalam meter)
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

        // Cek apakah izin lokasi sudah diberikan
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Jika belum, minta izin
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Izin lokasi sudah diberikan, lanjutkan aktivitas selanjutnya
            // contoh: muat WebView
            loadWebView()
            // Dapatkan lokasi pengguna
            getLocation()
        }
    }

    // Handling the result of the file chooser activity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (resultCode == Activity.RESULT_OK) {
                val result = data?.data?.let { arrayOf(it) }
                mUploadMessage?.onReceiveValue(result)
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
                    // Izin diberikan, lanjutkan aktivitas selanjutnya
                    // contoh: muat WebView
                    loadWebView()
                    // Dapatkan lokasi pengguna
                    getLocation()
                } else {
                    // Izin ditolak, beri tahu pengguna tentang keterbatasan aplikasi
                    Toast.makeText(
                        this,
                        "Aplikasi memerlukan izin lokasi untuk bekerja dengan baik",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadWebView() {
        // Lakukan semua inisialisasi WebView dan muat URL di sini
        val webView: WebView = findViewById(R.id.WV)
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://siska.kimiafarma.co.id/")

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
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"
                startActivityForResult(
                    Intent.createChooser(intent, "Select Picture"),
                    FILECHOOSER_RESULTCODE
                )
                return true
            }
        }

        // Tambahkan atau ganti baris berikut pada bagian setDownloadListener
//        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
//            val request = DownloadManager.Request(Uri.parse(url))
//            request.setMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") // Tentukan tipe MIME sebagai XLS
//            request.addRequestHeader("User-Agent", userAgent)
//
//            // Tentukan lokasi penyimpanan untuk file yang diunduh (misalnya di folder Downloads)
//            val filename = "report.xls"  // Nama file menjadi report.xlsx
//            val destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
//
//            // Dapatkan DownloadManager dari sistem
//            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//
//            // Mulai unduhan
//            downloadManager.enqueue(request)
//
//            // Tampilkan pesan kepada pengguna bahwa file sedang diunduh
//            Toast.makeText(applicationContext, "Downloading Excel file...", Toast.LENGTH_SHORT).show()
//        }
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, length ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            val cookies: String = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("cookie", cookies)
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Download File...")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, userAgent))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            // Menambahkan penanganan khusus untuk file XLSX
            if (mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") {
                // Tentukan direktori penyimpanan untuk unduhan
                val downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadDirectory.exists()) {
                    downloadDirectory.mkdirs()
                }
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val destinationUri = Uri.withAppendedPath(Uri.fromFile(downloadDirectory), fileName)
                request.setDestinationUri(destinationUri)
            } else {
                // Untuk format file selain XLSX, gunakan metode setDestinationInExternalPublicDir
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(
                        url, contentDisposition, mimeType
                    ))
            }

            val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(applicationContext, "Sedang mendownload file", Toast.LENGTH_SHORT).show()
        }
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
                "GPS atau jaringan tidak tersedia, aktivkan untuk mendapatkan lokasi",
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

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }
}
