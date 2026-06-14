package com.example.myservicecenter

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.myservicecenter.databinding.FragmentHomeContentBinding
import java.io.BufferedReader

class HomeContentFragment : Fragment(R.layout.fragment_home_content) {
    private var _binding: FragmentHomeContentBinding? = null
    private val binding get() = _binding!!
    private val mainHandler = Handler(Looper.getMainLooper())
    private val hideLoadingRunnable = Runnable {
        _binding?.progressHome?.visibility = View.GONE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeContentBinding.bind(view)
        applyWindowInsets()
        setupWebView()
    }

    private fun applyWindowInsets() {
        val start = binding.root.paddingStart
        val top = binding.root.paddingTop
        val end = binding.root.paddingEnd
        val bottom = binding.root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.root.updatePadding(
                left = start,
                top = top + statusTop,
                right = end,
                bottom = bottom
            )
            insets
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.progressHome.visibility = View.VISIBLE
        mainHandler.removeCallbacks(hideLoadingRunnable)
        // 某些外链脚本可能长时间不结束，8 秒后强制收起加载态，避免页面一直显示“加载中”。
        mainHandler.postDelayed(hideLoadingRunnable, 8000)
        binding.webViewHome.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mediaPlaybackRequiresUserGesture = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(0xFFFFFFFF.toInt())
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    binding.progressHome.visibility = View.GONE
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    syncHomeStatsToPageStorage()
                    binding.progressHome.visibility = View.GONE
                    mainHandler.removeCallbacks(hideLoadingRunnable)
                }
            }
            loadDataWithBaseURL(
                "file:///android_asset/home/",
                buildHomeHtml(),
                "text/html",
                "utf-8",
                null
            )
        }
    }

    private fun buildHomeHtml(): String {
        val context = requireContext()
        val html = context.assets.open("home/10086.html").bufferedReader().use(BufferedReader::readText)
        return html
            .replace(">19.19<", ">${escapeHtmlText(AppPreferences.getWebViewHomeData(context))}<")
            .replace(">547.58<", ">${escapeHtmlText(AppPreferences.getWebViewHomeBalance(context))}<")
            .replace(">200<", ">${escapeHtmlText(AppPreferences.getWebViewHomeCallMinutes(context))}<")
            .replace(">0<", ">${escapeHtmlText(AppPreferences.getWebViewHomePendingRights(context))}<")
            .replace("通用流量剩余19.19GB", "通用流量剩余${escapeHtmlText(AppPreferences.getWebViewHomeData(context))}GB")
            .replace("话费余额547.58元", "话费余额${escapeHtmlText(AppPreferences.getWebViewHomeBalance(context))}元")
            .replace("通用通话剩余200分钟", "通用通话剩余${escapeHtmlText(AppPreferences.getWebViewHomeCallMinutes(context))}分钟")
            .replace("待领取权益0个", "待领取权益${escapeHtmlText(AppPreferences.getWebViewHomePendingRights(context))}个")
    }

    private fun syncHomeStatsToPageStorage() {
        val context = context ?: return
        val data = escapeJsString(AppPreferences.getWebViewHomeData(context))
        val balance = escapeJsString(AppPreferences.getWebViewHomeBalance(context))
        val callMinutes = escapeJsString(AppPreferences.getWebViewHomeCallMinutes(context))
        val pendingRights = escapeJsString(AppPreferences.getWebViewHomePendingRights(context))

        val script = """
            (function() {
              localStorage.setItem('codex_home_data', '$data');
              localStorage.setItem('codex_home_balance', '$balance');
              localStorage.setItem('codex_home_call_minutes', '$callMinutes');
              localStorage.setItem('codex_home_pending_rights', '$pendingRights');
              if (typeof window.applyCodexHomeStats === 'function') {
                window.applyCodexHomeStats();
              }
            })();
        """.trimIndent()

        binding.webViewHome.evaluateJavascript(script, null)
    }

    private fun escapeJsString(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    private fun escapeHtmlText(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    override fun onPause() {
        super.onPause()
        _binding?.webViewHome?.onPause()
    }

    override fun onResume() {
        super.onResume()
        _binding?.webViewHome?.onResume()
        _binding?.webViewHome?.post { syncHomeStatsToPageStorage() }
    }

    override fun onDestroyView() {
        mainHandler.removeCallbacks(hideLoadingRunnable)
        _binding?.webViewHome?.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroyView()
        _binding = null
    }
}
