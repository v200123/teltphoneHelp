package com.example.myservicecenter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myservicecenter.databinding.FragmentPackageDetailWebviewBinding
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            setBackgroundColor(Color.WHITE)
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
                    syncBasicInfoToPage()
                    bindBasicInfoEditorClick()
                    bindEditorClick()
                }
            )
            loadUrl("https://www.lastcoffee.top:8200/merged_order_tabs.html")
        }
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

    private fun syncBasicInfoToPage() {
        val context = context ?: return
        val customPhone = AppPreferences.getCustomPhoneNumber(context).trim()
        val displayPhone = if (customPhone.isNotBlank()) customPhone else ""
        val displayName = AppPreferences.getCustomDisplayName(context).trim()
        val badgeLevel = AppPreferences.getCustomStarLevel(context)
        val openedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val script = """
            (function() {
              var config = {
                phoneNumber: '${escapeJsString(displayPhone)}',
                name: '${escapeJsString(displayName)}',
                badgeLevel: '${escapeJsString(badgeLevel.toString())}',
                openedAt: '${escapeJsString(openedAt)}'
              };
              if (typeof window.applyPackageDetailConfig === 'function') {
                window.applyPackageDetailConfig(config);
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

    private fun bindBasicInfoEditorClick() {
        val script = """
            (function() {
              if (window.__basicInfoEditorBound) return;
              window.__basicInfoEditorBound = true;
              document.addEventListener('click', function(event) {
                var target = event.target;
                if (!target) return;
                var userInfo = target.closest ? target.closest('.userInfo') : null;
                var noticeView = target.closest ? target.closest('.notice-view') : null;
                if (!userInfo && !noticeView) return;
                if (window.PackageDetailBridge && typeof window.PackageDetailBridge.openBasicInfoEditor === 'function') {
                  window.PackageDetailBridge.openBasicInfoEditor();
                }
              }, true);
            })();
        """.trimIndent()
        binding.webViewPackageDetail.evaluateJavascript(script, null)
    }

    private fun showBasicInfoEditorDialog() {
        val context = context ?: return
        val phoneInput = EditText(context).apply {
            hint = "请输入手机号"
            inputType = InputType.TYPE_CLASS_PHONE
            setText(AppPreferences.getCustomPhoneNumber(context))
            maxLines = 1
        }
        val nameInput = EditText(context).apply {
            hint = "请输入姓名"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            setText(AppPreferences.getCustomDisplayName(context))
            maxLines = 1
        }
        val starInput = EditText(context).apply {
            hint = "请输入等级（1-5）"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(AppPreferences.getCustomStarLevel(context).toString())
            maxLines = 1
        }

        listOf(phoneInput, nameInput, starInput).forEachIndexed { index, editText ->
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            editText.setTextColor(Color.parseColor("#333333"))
            editText.setPadding(dpToPx(12), 0, dpToPx(12), 0)
            editText.background = context.getDrawable(R.drawable.bg_search_input)
            editText.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(44)
            ).apply {
                if (index > 0) {
                    topMargin = dpToPx(10)
                }
            }
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(12), dpToPx(20), 0)
            addView(phoneInput)
            addView(nameInput)
            addView(starInput)
        }

        AlertDialog.Builder(context)
            .setTitle("设置详单基本信息")
            .setMessage("点击顶部信息区或温馨提示即可再次打开。手机号会自动脱敏，温馨提示号码与手机号保持一致。")
            .setView(container)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val phone = phoneInput.text?.toString().orEmpty().trim()
                val name = nameInput.text?.toString().orEmpty().trim()
                val starLevel = starInput.text?.toString()?.toIntOrNull()?.coerceIn(1, 5) ?: 3
                AppPreferences.setCustomPhoneNumber(context, phone)
                AppPreferences.setCustomDisplayName(context, name)
                AppPreferences.setCustomStarLevel(context, starLevel)
                syncBasicInfoToPage()
                Toast.makeText(context, "详单基本信息已更新", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showEditorDialog() {
        val context = context ?: return
        val editor = EditText(context).apply {
            setText(AppPreferences.getWebViewFixedFeeListJson(context))
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            minLines = 10
            gravity = Gravity.TOP or Gravity.START
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
        _binding?.webViewPackageDetail?.post {
            syncPackageListToPage()
            syncBasicInfoToPage()
        }
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

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    inner class EditorBridge {
        @JavascriptInterface
        fun openEditor() {
            activity?.runOnUiThread { showEditorDialog() }
        }

        @JavascriptInterface
        fun openBasicInfoEditor() {
            activity?.runOnUiThread { showBasicInfoEditorDialog() }
        }
    }
}
