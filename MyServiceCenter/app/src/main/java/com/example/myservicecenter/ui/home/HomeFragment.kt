package com.example.myservicecenter.ui.home

import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder
import com.example.myservicecenter.AppPreferences
import com.example.myservicecenter.R
import com.example.myservicecenter.SettingsActivity
import com.example.myservicecenter.databinding.FragmentHomeBinding
import java.util.Calendar

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val selectCodeTableBottomImage = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val context = context ?: return@registerForActivityResult
        uri ?: return@registerForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)
            if (context.contentResolver.getType(uri).orEmpty().startsWith("video/")) {
                AppPreferences.setCodeTableBottomVideoUri(context, uri)
            } else {
                AppPreferences.setCodeTableBottomImageMedia(context, uri)
            }
            renderCodeTableBottomImage()
        } catch (_: SecurityException) {
            Toast.makeText(context, "无法保存所选图片的访问权限，请重新选择", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        Glide.with(this).load("https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/7efac0c5395a41fc811856473284b7c0.png?fmt=webp").into(_binding!!.ivShowTopGif);
        Glide.with(this).load("https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/a752f551f6f34cb0a55d15bce93e5fc7.png?fmt=webp").into(_binding!!.searchViewScan);
        initTopBar()
        _binding!!.codeTableBottomContainer.visibility = View.VISIBLE
        applyCodeTableConfigs()
        bindCodeTableButtons()
        bindCodeTableBottomImage()
        bindSearchText()
        positionCodeTableMediaOverlay()

    }
    private fun initTopBar(){
        Glide.with(this).load("https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/8c74e600bbc440148a6b6a56f2fb68e5.gif").into(_binding!!.rlContainerTopInfoRightOtherBtn.iconView);
        _binding!!.rlContainerTopInfoRightOtherBtn.apply { this.text = "签到有礼" }
        _binding!!.rlContainerTopInfoRightOtherBtn01.apply { this.text = "消息"
        this.setUnreadCount("80")
        this.iconView.visibility = View.GONE
        }



    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            applyCodeTableConfigs()
            renderCodeTableBottomImage()
            applySearchText()
            positionCodeTableMediaOverlay()
        }
    }

    private fun applyCodeTableConfigs() {
        val items = listOf(
            binding.newCodeTablePoint0, binding.newCodeTablePoint1,
            binding.newCodeTablePoint2, binding.newCodeTablePoint3,
            binding.newCodeTablePoint4, binding.newCodeTablePoint5,
            binding.newCodeTablePoint6, binding.newCodeTablePoint7
        )
        val buttons = listOf(
            binding.newCodeTableBtnText0, binding.newCodeTableBtnText1,
            binding.newCodeTableBtnText2, binding.newCodeTableBtnText3
        )
        items.forEachIndexed { index, item ->
            val config = AppPreferences.getCodeTableItemConfig(requireContext(), index)
            item.setTipText(config.tip)
            item.setTipVisible(config.tipVisible)
            item.setValueText(config.value)
            item.setUnitText(config.unit)
            item.setTitleText(config.title)
            if (index < buttons.size) buttons[index].text = config.buttonText
        }
    }

    private fun bindCodeTableButtons() {
        val codeTableItems = listOf(
            binding.newCodeTablePoint0,
            binding.newCodeTablePoint1,
            binding.newCodeTablePoint2,
            binding.newCodeTablePoint3,
            binding.newCodeTablePoint4,
            binding.newCodeTablePoint5,
            binding.newCodeTablePoint6,
            binding.newCodeTablePoint7
        )
        codeTableItems.forEachIndexed { index, item ->
            item.setOnClickListener { showCodeTableEditDialog(index) }
        }
        listOf(
            binding.newCodeTableBtnText0,
            binding.newCodeTableBtnText1,
            binding.newCodeTableBtnText2,
            binding.newCodeTableBtnText3
        ).forEachIndexed { index, button ->
            button.setOnClickListener { showCodeTableEditDialog(index) }
        }
    }

    private fun bindSearchText() {
        applySearchText()
        binding.searchViewVlt.setOnClickListener {
            val context = requireContext()
            val input = EditText(context).apply {
                hint = "请输入搜索栏文案"
                setText(AppPreferences.getHomeSearchText(context))
                setSelectAllOnFocus(true)
                maxLines = 1
                setPadding(dp(16), 0, dp(16), 0)
                setBackgroundResource(R.drawable.bg_search_input)
            }
            AlertDialog.Builder(context)
                .setTitle("设置搜索栏文案")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存") { _, _ ->
                    AppPreferences.setHomeSearchText(context, input.text.toString())
                    applySearchText()
                }
                .show()
        }
    }

    private fun applySearchText() {
        binding.searchViewVlt.text = AppPreferences.getHomeSearchText(requireContext())
    }

    private fun bindCodeTableBottomImage() {
        binding.codeTableBottomContainer.setOnClickListener {
            selectCodeTableBottomImage.launch(arrayOf("image/*", "video/*"))
        }
        binding.ivCodeTableBottomImage.setOnClickListener {
            selectCodeTableBottomImage.launch(arrayOf("image/*", "video/*"))
        }
        renderCodeTableBottomImage()
    }

    /** Places the media area over the fixed CodeTable card, starting at the divider position. */
    private fun positionCodeTableMediaOverlay() {
        binding.root.doOnLayout {
            binding.codetable.doOnLayout {
                val rootLocation = IntArray(2)
                val tableLocation = IntArray(2)
                binding.root.getLocationOnScreen(rootLocation)
                binding.codetable.getLocationOnScreen(tableLocation)
                val dividerTop = tableLocation[1] - rootLocation[1] + binding.codetable.height
                (binding.codeTableBottomContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    topMargin = dividerTop
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.x120) +
                        (ViewCompat.getRootWindowInsets(binding.root)
                            ?.getInsets(WindowInsetsCompat.Type.systemBars())
                            ?.bottom ?: 0)
                    binding.codeTableBottomContainer.layoutParams = this
                }
                binding.codeTableBottomContainer.bringToFront()
                binding.ivCodeViewLine.visibility = View.VISIBLE
                binding.ivCodeViewLine.bringToFront()
            }
        }
    }

    private fun renderCodeTableBottomImage() {
        val context = context ?: return
        if (AppPreferences.getCodeTableBottomMediaType(context) == AppPreferences.CODE_TABLE_BOTTOM_MEDIA_TYPE_VIDEO) {
            AppPreferences.getCodeTableBottomVideoUri(context)?.let { videoUri ->
                Glide.with(this).clear(binding.ivCodeTableBottomImage)
                binding.ivCodeTableBottomImage.visibility = View.GONE
                binding.playerCodeTableBottom.visibility = View.VISIBLE
                binding.playerCodeTableBottom.onVideoReset()
                GSYVideoOptionBuilder()
                    .setUrl(videoUri.toString())
                    .setVideoTitle("")
                    .setLooping(true)
                    .setIsTouchWiget(false)
                    .setAutoFullWithSize(false)
                    .setShowFullAnimation(false)
                    .build(binding.playerCodeTableBottom)
                binding.playerCodeTableBottom.startPlayLogic()
                return
            }
        }
        val imageUri = AppPreferences.getCodeTableBottomImageUri(context)
        if (imageUri == null) {
            Glide.with(this).clear(binding.ivCodeTableBottomImage)
            binding.ivCodeTableBottomImage.visibility = View.GONE
            binding.playerCodeTableBottom.onVideoReset()
            binding.playerCodeTableBottom.visibility = View.GONE
            return
        }
        binding.playerCodeTableBottom.onVideoReset()
        binding.playerCodeTableBottom.visibility = View.GONE
        binding.ivCodeTableBottomImage.visibility = View.VISIBLE
        Glide.with(this)
            .load(imageUri)
            .override(Target.SIZE_ORIGINAL)
            .dontTransform()
            .into(binding.ivCodeTableBottomImage)
    }

    override fun onPause() {
        binding.playerCodeTableBottom.onVideoPause()
        super.onPause()
    }

    override fun onDestroyView() {
        _binding?.playerCodeTableBottom?.onVideoReset()
        _binding = null
        super.onDestroyView()
    }

    private fun showCodeTableEditDialog(index: Int) {
        val context = requireContext()
        val config = AppPreferences.getCodeTableItemConfig(context, index)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), 0)
        }
        fun input(hint: String, value: String, inputType: Int = InputType.TYPE_CLASS_TEXT) = EditText(context).apply {
            this.hint = hint
            setText(value)
            this.inputType = inputType
            maxLines = 1
            setTextColor(resources.getColor(R.color.text_primary, null))
            setHintTextColor(resources.getColor(R.color.text_hint, null))
            setBackgroundResource(R.drawable.bg_search_input)
            setPadding(dp(12), 0, dp(12), 0)
        }
        fun addInput(
            hint: String,
            value: String,
            inputType: Int = InputType.TYPE_CLASS_TEXT
        ): EditText = input(hint, value, inputType).also {
            container.addView(it, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
            ).apply { topMargin = dp(10) })
        }
        val tip = addInput("提示文字（可留空）", config.tip)
        val tipVisible = Switch(context).apply {
            text = "显示提示标签"
            isChecked = config.tipVisible
            setTextColor(resources.getColor(R.color.text_primary, null))
        }
        container.addView(tipVisible, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(4) })
        val value = addInput(
            "数值（仅数字）",
            config.value,
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        val unit = addInput("单位", config.unit)
        val title = addInput("标题", config.title)
        val buttonText = if (index < 4) addInput("按钮文字", config.buttonText) else null

        AlertDialog.Builder(context)
            .setTitle("编辑${config.buttonText.ifBlank { "指标 ${index + 1}" }}")
            .setView(container)
            .setNeutralButton("恢复默认") { _, _ ->
                AppPreferences.resetCodeTableItemConfig(context, index)
                applyCodeTableConfigs()
                Toast.makeText(context, "已恢复默认", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                AppPreferences.setCodeTableItemConfig(context, index, AppPreferences.CodeTableItemConfig(
                    tip = tip.text.toString(),
                    tipVisible = tipVisible.isChecked,
                    value = value.text.toString(),
                    unit = unit.text.toString(),
                    title = title.text.toString(),
                    buttonText = buttonText?.text?.toString().orEmpty()
                ))
                applyCodeTableConfigs()
                Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

}
