package com.example.myservicecenter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myservicecenter.databinding.FragmentPackageDetailWebviewBinding
import org.json.JSONArray
import java.io.BufferedReader

class PackageDetailWebViewFragment : Fragment(R.layout.fragment_package_detail_webview) {
    companion object {
        private const val TAG = "PackageDetailFragment"
    }

    private var _binding: FragmentPackageDetailWebviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        _binding = FragmentPackageDetailWebviewBinding.bind(view)
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun setupWebView() {
//        binding.progressPackageDetail.visibility = View.VISIBLE
        binding.webViewPackageDetail.apply {
            setBackgroundColor(0xFFF5F5F5.toInt())
            addJavascriptInterface(EditorBridge(), "PackageDetailBridge")
            CommonWebViewSupport.configure(
                webView = this,
                logTag = "PackageDetailWebView",
                onPageCommitVisible = { _, _ ->
//                    binding.progressPackageDetail.visibility = View.GONE
                },
                onPageFinished = { _, _ ->
//                    binding.progressPackageDetail.visibility = View.GONE
                    syncPackageListToPage()
                    bindEditorClick()
                }
            )
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            loadDataWithBaseURL(
                "file:///android_asset/",
                buildPackageDetailHtml(),
                "text/html",
                "utf-8",
                null
            )
        }
    }

    private fun buildPackageDetailHtml(): String {
        val context = requireContext()
        return context.assets
            .open("00-original.html")
            .bufferedReader()
            .use(BufferedReader::readText)
    }

    private fun syncPackageListToPage() {
        val context = context ?: return
        val listJson = escapeJsString(AppPreferences.getWebViewFixedFeeListJson(context))
        val script = """
            (function() {
              var records = [];
              try {
                records = JSON.parse('$listJson');
              } catch (e) {
                records = [];
              }
              if (typeof window.renderFixedFeeList === 'function') {
                window.renderFixedFeeList(records);
              }
            })();
        """.trimIndent()
        binding.webViewPackageDetail.evaluateJavascript(script, null)
    }

    private fun bindEditorClick() {
        val script = """
            (function() {
              if (window.__fixedFeeEditorBound) return;
              window.__fixedFeeEditorBound = true;
              document.addEventListener('click', function(event) {
                var target = event.target;
                if (!target) return;
                var item = target.closest ? target.closest('.list1-item') : null;
                if (!item) return;
                if (window.PackageDetailBridge && typeof window.PackageDetailBridge.openEditor === 'function') {
                  window.PackageDetailBridge.openEditor();
                }
              }, true);
            })();
        """.trimIndent()
        binding.webViewPackageDetail.evaluateJavascript(script, null)
    }

    private fun showEditorDialog() {
        val context = context ?: return
        val editor = EditText(context).apply {
            setText(AppPreferences.getWebViewFixedFeeListJson(context))
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            minLines = 10
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        }

        val container = FrameLayout(context).apply {
            val horizontal = dpToPx(20)
            val vertical = dpToPx(12)
            setPadding(horizontal, vertical, horizontal, 0)
            addView(
                editor,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        AlertDialog.Builder(context)
            .setTitle("设置套餐及固定费用详单数据")
            .setMessage("请输入列表 JSON 数组，字段支持 name、cycle、fee、description。")
            .setView(container)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val input = editor.text?.toString().orEmpty().trim()
                try {
                    JSONArray(input)
                    AppPreferences.setWebViewFixedFeeListJson(context, input)
                    syncPackageListToPage()
                    Toast.makeText(context, "套餐详单数据已更新", Toast.LENGTH_SHORT).show()
                } catch (error: Exception) {
                    Toast.makeText(context, "JSON 格式不正确：${error.message}", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    private fun escapeJsString(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    private fun dpToPx(valueDp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            valueDp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        _binding?.webViewPackageDetail?.onResume()
        _binding?.webViewPackageDetail?.post { syncPackageListToPage() }
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        _binding?.webViewPackageDetail?.onPause()
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        _binding?.webViewPackageDetail?.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    inner class EditorBridge {
        @JavascriptInterface
        fun openEditor() {
            activity?.runOnUiThread { showEditorDialog() }
        }
    }
}
