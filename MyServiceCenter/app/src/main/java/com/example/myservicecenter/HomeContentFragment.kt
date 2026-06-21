package com.example.myservicecenter

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
import android.webkit.JavascriptInterface
import android.webkit.WebView
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
        applyWindowInsets()
        setupWebView()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.ivHomeTopBar) { view, insets ->
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = statusTop)
            insets
        }
    }

    /**
     * 兜底：页面加载 1s / 3s 后再尝试注入一次滚动监听，防止 onPageFinished 不触发。
     */
    private fun scheduleFallbackScrollInjection() {
        mainHandler.postDelayed({
            Log.d("HomeScroll", "fallback injection 1s")
            injectScrollListener()
            testJsBridge()
        }, 1000)
        mainHandler.postDelayed({
            Log.d("HomeScroll", "fallback injection 3s")
            injectScrollListener()
            testJsBridge()
        }, 3000)
    }

    /**
     * 调试：测试 JS 桥接是否可用。
     */
    private fun testJsBridge() {
        Log.d("HomeScroll", "testJsBridge called")
        binding.webViewHome.evaluateJavascript("typeof HomeScrollBridge") { result ->
            Log.d("HomeScroll", "bridge type=$result")
        }
        binding.webViewHome.evaluateJavascript("HomeScrollBridge.onScroll(99999)") { result ->
            Log.d("HomeScroll", "direct call result=$result")
        }
    }

    /**
     * 向 H5 页面注入滚动监听脚本，通过 HomeScrollBridge 把 scrollY 传回 Android。
     * 同时使用事件监听 + 定时轮询，兼容 window、body、documentElement 以及各种内部滚动容器。
     */
    private fun injectScrollListener() {
        val js = """
            (function() {
                if (window.__homeScrollInjected) return;
                window.__homeScrollInjected = true;
                function getScrollY() {
                    var y = window.scrollY;
                    if (y > 0) return y;
                    y = document.documentElement ? document.documentElement.scrollTop : 0;
                    if (y > 0) return y;
                    y = document.body ? document.body.scrollTop : 0;
                    if (y > 0) return y;
                    return 0;
                }
                function reportScroll() {
                    var scrollY = getScrollY();
                    if (window.HomeScrollBridge) {
                        window.HomeScrollBridge.onScroll(scrollY);
                    }
                }
                // 兜底轮询：每 100ms 上报一次，确保不会漏掉任何滚动容器
                setInterval(reportScroll, 100);
                // 事件监听：提升响应速度
                window.addEventListener('scroll', reportScroll, { passive: true });
                document.addEventListener('scroll', reportScroll, { passive: true });
                if (document.body) document.body.addEventListener('scroll', reportScroll, { passive: true });
                if (document.documentElement) document.documentElement.addEventListener('scroll', reportScroll, { passive: true });
                reportScroll();
            })();
        """.trimIndent()
        binding.webViewHome.evaluateJavascript(js, null)
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

    /**
     * JS 桥接类，用于接收页面滚动距离。
     */
    inner class ScrollBridge(private val callback: (scrollY: Int) -> Unit) {
        @JavascriptInterface
        fun onScroll(scrollY: Int) {
            callback(scrollY)
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface", "AddJavascriptInterface")
    private fun setupWebView() {
        binding.progressHome.visibility = View.VISIBLE
        mainHandler.removeCallbacks(hideLoadingRunnable)
        // 某些外链脚本可能长时间不结束，8 秒后强制收起加载态，避免页面一直显示“加载中”。
        mainHandler.postDelayed(hideLoadingRunnable, 8000)
        binding.webViewHome.apply {
            addJavascriptInterface(
                ScrollBridge { scrollY ->
                    Log.d("HomeScroll", "scrollY=$scrollY")
                    binding.root.post { updateTopBarOnScroll(scrollY) }
                },
                "HomeScrollBridge"
            )
            setBackgroundColor(0xFFFFFFFF.toInt())
            setOnScrollChangeListener { _, _, scrollY, _, _ ->
                updateTopBarOnScroll(scrollY)
            }
            CommonWebViewSupport.configure(
                webView = this,
                logTag = "HomeWebView",
                onPageCommitVisible = { _, _ ->
                    binding.progressHome.visibility = View.GONE
                    injectScrollListener()
                    scheduleFallbackScrollInjection()
                },
                onPageFinished = { _, _ ->
                    syncHomeStatsToPageStorage()
                    binding.progressHome.visibility = View.GONE
                    mainHandler.removeCallbacks(hideLoadingRunnable)
                    testJsBridge()
                    injectScrollListener()
                    scheduleFallbackScrollInjection()
                    Log.d("HomeScroll", "onPageFinished end")
                }
            )
            loadDataWithBaseURL(
                "file:///android_asset/",
                buildHomeHtml(),
                "text/html",
                "utf-8",
                null
            )
        }
    }

    private fun buildHomeHtml(): String {
        val context = requireContext()
        val html = context.assets.open("00-original.html").bufferedReader().use(BufferedReader::readText)
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
