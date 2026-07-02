package com.example.myservicecenter.ui.home

import android.annotation.SuppressLint
import android.content.Intent
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
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.myservicecenter.AppPreferences
import com.example.myservicecenter.CommonWebViewSupport
import com.example.myservicecenter.R
import com.example.myservicecenter.SettingsActivity
import com.example.myservicecenter.databinding.FragmentHomeContentBinding

class HomeContentFragment : Fragment(R.layout.fragment_home_content) {
    companion object {
        private const val HOME_PAGE_URL = "https://www.lastcoffee.top:8200/home.html"
    }

    private var _binding: FragmentHomeContentBinding? = null
    private val binding get() = _binding!!
    private var homeWebView: WebView? = null
    private var isHomeWebViewConfigured = false
    private var lastLoadedHomeUrl: String? = null
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
            addJavascriptInterface(HomeBridge(), "HomeBridge")
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
                    applyHomeStats()
                    bindHomeCardClick()
                    binding.progressHome.visibility = View.GONE
                    mainHandler.removeCallbacks(hideLoadingRunnable)
                },null, onProgressChanged = { webView, progress ->
                    Log.d("HomeScroll", "onProgressChanged:"+progress)
                }
            )
            loadHomePage(this)
        }
    }

    private fun loadHomePage(webView: WebView) {
        lastLoadedHomeUrl = HOME_PAGE_URL
        webView.loadUrl(HOME_PAGE_URL)
    }

    private fun applyHomeStats() {
        val context = context ?: return
        val data = AppPreferences.getMineStatData(context)
        val balance = AppPreferences.getMineStatBalance(context)
        val callMinutes = AppPreferences.getWebViewHomeCallMinutes(context)
        val points = AppPreferences.getWebViewHomePoints(context)
        val pendingRights = AppPreferences.getWebViewHomePendingRights(context)
        val script = """
            (function() {
              window.__codexHomeStats = {
                data: ${jsString(data)},
                balance: ${jsString(balance)},
                callMinutes: ${jsString(callMinutes)},
                points: ${jsString(points)},
                pendingRights: ${jsString(pendingRights)}
              };

              function setStopwatchItem(id, value, unit, title) {
                if (value === null || value === undefined || value === '') return;
                var root = document.getElementById(id);
                if (!root) return;
                var numberNode = root.querySelector('.remaining .num span') ||
                  root.querySelector('.remaining .num');
                var unitNode = root.querySelector('.remaining .unit');
                if (numberNode) numberNode.textContent = value;
                if (unitNode) unitNode.textContent = unit;
                root.setAttribute('aria-label', title + value + unit);
              }

              window.__applyCodexHomeStatsDirect = function() {
                var stats = window.__codexHomeStats || {};
                setStopwatchItem('card-stopwatch-item-1', stats.data, 'GB', '通用流量剩余');
                setStopwatchItem('card-stopwatch-item-2', stats.balance, '元', '话费余额');
                setStopwatchItem('card-stopwatch-item-3', stats.callMinutes, '分钟', '通用通话剩余');
                setStopwatchItem('cardstopwatchitem5_0', stats.points, '分', '积分');
                setStopwatchItem('cardstopwatchitem5_1', stats.pendingRights, '个', '待领取权益');
              };

              window.__applyCodexHomeStatsDirect();
              setTimeout(window.__applyCodexHomeStatsDirect, 300);
              setTimeout(window.__applyCodexHomeStatsDirect, 1000);

            })();
        """.trimIndent()

        homeWebView?.evaluateJavascript(script, null)
    }

    private fun bindHomeCardClick() {
        val script = """
            (function() {
              if (window.__homeCardBoxBridgeBound) return;
              window.__homeCardBoxBridgeBound = true;
              document.addEventListener('click', function(event) {
                var target = event.target;
                if (!target || !target.closest) return;
                var cardBox = target.closest('#card-box');
                if (!cardBox) return;
                if (window.HomeBridge && typeof window.HomeBridge.openSettings === 'function') {
                  window.HomeBridge.openSettings();
                }
              }, true);
            })();
        """.trimIndent()
        homeWebView?.evaluateJavascript(script, null)
    }

    /** 将字符串安全编码为 JS 字符串字面量（含两侧双引号）。 */
    private fun jsString(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    override fun onPause() {
        super.onPause()
        homeWebView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        ensureHomeWebView()
        homeWebView?.onResume()
        val targetUrl = HOME_PAGE_URL
        val webView = homeWebView ?: return
        if (lastLoadedHomeUrl != null && targetUrl != lastLoadedHomeUrl) {
            binding.progressHome.visibility = View.VISIBLE
            mainHandler.removeCallbacks(hideLoadingRunnable)
            mainHandler.postDelayed(hideLoadingRunnable, 8000)
            lastLoadedHomeUrl = targetUrl
            webView.loadUrl(targetUrl)
        } else {
            webView.post { applyHomeStats() }
        }
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

    inner class HomeBridge {
        @JavascriptInterface
        fun openSettings() {
            activity?.runOnUiThread {
                startActivity(Intent(requireContext(), SettingsActivity::class.java))
            }
        }
    }
}
