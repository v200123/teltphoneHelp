package com.example.myservicecenter

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.myservicecenter.databinding.ActivityPackageDetailWebviewBinding
import java.io.BufferedReader

class PackageDetailWebViewActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PkgDetailBasicWebView"
    }

    private lateinit var binding: ActivityPackageDetailWebviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPackageDetailWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBackPressed()
        setupBasicWebView()
    }

    private fun setupBasicWebView() {
        binding.webViewPackageDetail.apply {
            CommonWebViewSupport.configure(
                webView = this,
                logTag = TAG,
                onPageFinished = { _, url ->
                    Log.d(TAG, "page finished: $url")
                }
            )
            val html = buildNoLoginHtml()
            Log.d(TAG, "loadDataWithBaseURL size=${html.length}")
            loadUrl("https://www.baidu.com")
        }
    }

    private fun buildNoLoginHtml(): String {
        return assets.open("no_login.html").bufferedReader().use(BufferedReader::readText)
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webViewPackageDetail.canGoBack()) {
                    binding.webViewPackageDetail.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        binding.webViewPackageDetail.onResume()
    }

    override fun onPause() {
        binding.webViewPackageDetail.onPause()
        super.onPause()
    }

}
