package com.example.myservicecenter.ui.home

import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout

import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.example.myservicecenter.PhoneDisplayManager
import com.example.myservicecenter.R
import com.example.myservicecenter.core.AppPreferences
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder
import com.example.myservicecenter.databinding.FragmentHomeBinding
class HomeFragment : Fragment(R.layout.fragment_home) {
    companion object {
        private const val DEFAULT_HOME_PHONE_DISPLAY = "135***3423"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val selectCodeTableBottomImage = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val context = context ?: return@registerForActivityResult
        uri ?: return@registerForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)
            AppPreferences.setCodeTableBottomImageMedia(context, uri)
            renderSecondTopContentImage()
        } catch (_: SecurityException) {
            Toast.makeText(context, "无法保存所选图片的访问权限，请重新选择", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        Glide.with(this).load("https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/7efac0c5395a41fc811856473284b7c0.png?fmt=webp").into(_binding!!.ivShowTopGif);
        Glide.with(this).load("https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/a752f551f6f34cb0a55d15bce93e5fc7.png?fmt=webp").into(_binding!!.searchViewScan);
        Glide.with(this).load("https://res.app.coc.10086.cn/group2/M00/09/EA/CtFOW2kpCRmADcMlAABghzIox8c942.png?fmt=webp").into(_binding!!.ivAdPointBg);
        Glide.with(this).load("https://res.app.coc.10086.cn/group1/M00/09/EA/CtFOBmkpCReAEmvqAAABTBkx0cQ779.png?fmt=webp").into(_binding!!.ivAdPointIcon);
        initTopBar()
        applyManagedPhone()
        applyCodeTableConfigs()
        bindCodeTableButtons()
        bindCodeTableBottomImage()
        bindSearchText()

    }
    private fun initTopBar() {
        Glide.with(this)
            .load("https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/8c74e600bbc440148a6b6a56f2fb68e5.gif")
            .into(binding.rlContainerTopInfoRightOtherBtn.iconView)
        binding.rlContainerTopInfoRightOtherBtn01.iconView.visibility = View.GONE
        applyTopActionConfigs()
        binding.rlContainerTopInfoRightOtherBtn.setOnButtonClickListener {
            showTopActionEditDialog(0)
        }
        binding.rlContainerTopInfoRightOtherBtn01.setOnButtonClickListener {
            showTopActionEditDialog(1)
        }
    }

    private fun applyTopActionConfigs() {
        val buttons = listOf(
            binding.rlContainerTopInfoRightOtherBtn,
            binding.rlContainerTopInfoRightOtherBtn01
        )
        buttons.forEachIndexed { index, button ->
            val config = AppPreferences.getHomeTopActionConfig(requireContext(), index)
            button.text = config.title
            when (config.badgeStyle) {
                AppPreferences.TOP_ACTION_BADGE_DOT -> button.setUnreadDotVisible(true)
                AppPreferences.TOP_ACTION_BADGE_TEXT -> {
                    val count = config.badgeText.toIntOrNull()
                    if (count != null) {
                        button.updateUnreadView(count)
                    } else {
                        button.setUnreadBadge(config.badgeText)
                    }
                }
                else -> button.updateUnreadView(-1)
            }
        }
    }

    private fun showTopActionEditDialog(index: Int) {
        val context = requireContext()
        val config = AppPreferences.getHomeTopActionConfig(context, index)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), 0)
        }
        val titleInput = EditText(context).apply {
            hint = "按钮标题"
            setText(config.title)
            maxLines = 1
            setSelectAllOnFocus(true)
            setBackgroundResource(R.drawable.bg_search_input)
            setPadding(dp(12), 0, dp(12), 0)
        }
        val badgeInput = EditText(context).apply {
            hint = "数字角标内容，例如 8、99+ 或 VIP"
            setText(config.badgeText)
            maxLines = 1
            setBackgroundResource(R.drawable.bg_search_input)
            setPadding(dp(12), 0, dp(12), 0)
        }
        container.addView(titleInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        ).apply { topMargin = dp(10) })

        val badgeStyleGroup = RadioGroup(context).apply { orientation = RadioGroup.VERTICAL }
        val noBadge = RadioButton(context).apply { text = "不显示角标" }
        val dotBadge = RadioButton(context).apply { text = "显示红点" }
        val textBadge = RadioButton(context).apply { text = "显示数字/文字角标" }
        badgeStyleGroup.addView(noBadge)
        badgeStyleGroup.addView(dotBadge)
        badgeStyleGroup.addView(textBadge)
        when (config.badgeStyle) {
            AppPreferences.TOP_ACTION_BADGE_DOT -> dotBadge.isChecked = true
            AppPreferences.TOP_ACTION_BADGE_TEXT -> textBadge.isChecked = true
            else -> noBadge.isChecked = true
        }
        container.addView(badgeStyleGroup, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })
        container.addView(badgeInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        ).apply { topMargin = dp(6) })

        AlertDialog.Builder(context)
            .setTitle("设置顶部按钮")
            .setView(container)
            .setNeutralButton("恢复默认") { _, _ ->
                AppPreferences.resetHomeTopActionConfig(context, index)
                applyTopActionConfigs()
            }
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val badgeStyle = when (badgeStyleGroup.checkedRadioButtonId) {
                    dotBadge.id -> AppPreferences.TOP_ACTION_BADGE_DOT
                    textBadge.id -> AppPreferences.TOP_ACTION_BADGE_TEXT
                    else -> AppPreferences.TOP_ACTION_BADGE_NONE
                }
                AppPreferences.setHomeTopActionConfig(
                    context,
                    index,
                    AppPreferences.HomeTopActionConfig(
                        title = titleInput.text.toString(),
                        badgeStyle = badgeStyle,
                        badgeText = badgeInput.text.toString()
                    )
                )
                applyTopActionConfigs()
                Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    /** 首页顶部账号与设置页的“号码信息自定义”保持同一数据源。 */
    private fun applyManagedPhone() {
        val context = context ?: return
        val managedPhone = PhoneDisplayManager.managedPhone(context)
        binding.tvInfoAccount.text = if (managedPhone.isBlank()) {
            DEFAULT_HOME_PHONE_DISPLAY
        } else {
            PhoneDisplayManager.display(context)
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            applyCodeTableConfigs()
            applyManagedPhone()
            applySearchText()
            renderSecondTopContentImage()
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
        binding.ivSecondTopContent.setOnClickListener {
            selectCodeTableBottomImage.launch(arrayOf("image/*"))
        }
        renderSecondTopContentImage()
    }

    /** Loads the persisted photo into the scrollable second-top content area. */
    private fun renderSecondTopContentImage() {
        val context = context ?: return
        if (AppPreferences.getCodeTableBottomMediaType(context) !=
            AppPreferences.CODE_TABLE_BOTTOM_MEDIA_TYPE_IMAGE
        ) {
            return
        }
        val imageUri = AppPreferences.getCodeTableBottomImageUri(context) ?: return
        Glide.with(this)
            .load(imageUri)
            .override(Target.SIZE_ORIGINAL)
            .dontTransform()
            .into(binding.ivSecondTopContent)
    }




    override fun onPause() {

        super.onPause()
    }

    override fun onDestroyView() {
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
