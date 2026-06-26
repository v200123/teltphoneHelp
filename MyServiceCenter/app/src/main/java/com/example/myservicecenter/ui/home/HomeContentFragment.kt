package com.example.myservicecenter.ui.home

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.myservicecenter.AppPreferences
import com.example.myservicecenter.CommonWebViewSupport
import com.example.myservicecenter.R
import com.example.myservicecenter.databinding.FragmentHomeContentBinding

class HomeContentFragment : Fragment(R.layout.fragment_home_content) {
    private var _binding: FragmentHomeContentBinding? = null
    private val binding get() = _binding!!
    private var homeWebView: WebView? = null
    private var isHomeWebViewConfigured = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val hideLoadingRunnable = Runnable {
        _binding?.progressHome?.visibility = View.GONE
    }

    /**
     * 顶部栏背景从透明过渡到白色的滚动阈值（dp）。
     * 页面滚动距离超过该值后背景变为纯白色。
     */
    private val topBarScrollThreshold by lazy {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            200f,
            resources.displayMetrics
        ).toInt()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeContentBinding.bind(view)
        homeWebView = binding.webViewHome
        applyWindowInsets()
        ensureHomeWebView()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.ivHomeTopBar) { view, insets ->
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = statusTop)
            insets
        }
    }

    /**
     * 根据页面滚动距离更新顶部栏：
     * - 背景色：透明 -> 纯白色
     * - 图片颜色：黑色背景反转为白色背景，白色图标反转为黑色图标
     */
    private fun updateTopBarOnScroll(scrollY: Int) {
        val ratio = (scrollY.toFloat() / topBarScrollThreshold).coerceIn(0f, 1f)
        val alpha = (ratio * 255).toInt()
        binding.ivHomeTopBar.setBackgroundColor(Color.argb(alpha, 255, 255, 255))

        // 颜色矩阵插值：从原图（ratio=0）平滑过渡到完全反转（ratio=1）
        val scale = 1f - 2f * ratio
        val translate = 255f * ratio
        val matrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        binding.ivHomeTopBar.colorFilter = ColorMatrixColorFilter(matrix)
    }

    private fun ensureHomeWebView() {
        val binding = _binding ?: return
        if (homeWebView == null) {
            val webView = WebView(requireContext()).apply {
                id = R.id.webViewHome
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            binding.webViewHomeContainer.addView(webView, 0)
            homeWebView = webView
            isHomeWebViewConfigured = false
        }
        if (!isHomeWebViewConfigured) {
            setupWebView(homeWebView ?: return)
            isHomeWebViewConfigured = true
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface", "AddJavascriptInterface")
    private fun setupWebView(webView: WebView) {
        binding.progressHome.visibility = View.VISIBLE
        mainHandler.removeCallbacks(hideLoadingRunnable)
        // 某些外链脚本可能长时间不结束，8 秒后强制收起加载态，避免页面一直显示“加载中”。
        mainHandler.postDelayed(hideLoadingRunnable, 8000)
        webView.apply {
            setBackgroundColor(0xFFFFFFFF.toInt())
            setOnScrollChangeListener { _, _, scrollY, _, _ ->
                updateTopBarOnScroll(scrollY)
            }
            CommonWebViewSupport.configure(
                webView = this,
                logTag = "HomeWebView",
                onPageCommitVisible = { _, _ ->
                    binding.progressHome.visibility = View.GONE
                },
                onPageFinished = { _, _ ->
                    syncHomeStatsToPageStorage()
                    binding.progressHome.visibility = View.GONE
                    mainHandler.removeCallbacks(hideLoadingRunnable)
                },null, onProgressChanged = { webView, progress ->
                    Log.d("HomeScroll", "onProgressChanged:"+progress)
                }
            )
            loadUrl("https://www.lastcoffee.top:8200/cmcc_home.html")
        }
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

        homeWebView?.evaluateJavascript(script, null)
    }

    private fun escapeJsString(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    override fun onPause() {
        super.onPause()
        homeWebView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        ensureHomeWebView()
        homeWebView?.onResume()
        homeWebView?.post { syncHomeStatsToPageStorage() }
    }

    override fun onStop() {
        mainHandler.removeCallbacks(hideLoadingRunnable)
        super.onStop()
    }



    override fun onDestroyView() {
        mainHandler.removeCallbacks(hideLoadingRunnable)
        super.onDestroyView()
        _binding = null
    }
}
