package com.example.myservicecenter.ui.home

import com.example.myservicecenter.PhoneDisplayManager
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.myservicecenter.AppPreferences
import com.example.myservicecenter.R
import com.example.myservicecenter.SettingsActivity
import com.example.myservicecenter.databinding.FragmentHomeMineBinding
import com.example.myservicecenter.databinding.ItemHomeMineComboBinding
import com.example.myservicecenter.databinding.ItemHomeMineServiceCenterBinding
import com.example.myservicecenter.ui.main.MainActivity
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder

class HomeMineFragment : Fragment(R.layout.fragment_home_mine) {
    private var _binding: FragmentHomeMineBinding? = null
    private val binding get() = _binding!!

    private val selectCurrentDeviceMedia = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val context = context ?: return@registerForActivityResult
        uri ?: return@registerForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            when {
                mimeType.startsWith("image/") -> {
                    AppPreferences.setHomeCurrentDeviceImageUri(context, uri)
                    AppPreferences.setHomeCurrentDeviceMediaType(context, AppPreferences.HOME_DEVICE_MEDIA_TYPE_IMAGE)
                    renderCurrentDeviceImage()
                }
                mimeType.startsWith("video/") -> {
                    AppPreferences.setHomeCurrentDeviceVideoUri(context, uri)
                    AppPreferences.setHomeCurrentDeviceMediaType(context, AppPreferences.HOME_DEVICE_MEDIA_TYPE_VIDEO)
                    renderCurrentDeviceVideo()
                }
                else -> Toast.makeText(context, "请选择图片或视频文件", Toast.LENGTH_SHORT).show()
            }
        } catch (_: SecurityException) {
            Toast.makeText(context, "无法保存所选文件的访问权限，请重新选择", Toast.LENGTH_SHORT).show()
        }
    }

    private data class MineComboItem(
        val iconUrl: String = "",
        val title: String,
        val fallbackText: String = "",
        val showBadge: Boolean = false
    )

    private data class MineServiceCenterItem(
        val iconUrl: String,
        val name: String
    )

    companion object {
        private const val MINE_COMBO_ITEM_COUNT = 4
        private const val SERVICE_CENTER_VISIBLE_COLUMNS = 4
        private const val SERVICE_CENTER_ROW_COUNT = 2
        private const val SERVICE_CENTER_INDICATOR_MIN_WIDTH_DP = 16
        private const val HOME_DEVICE_VIDEO_DEFAULT_HEIGHT_DP = 220

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeMineBinding.bind(view)
        applyWindowInsets()
        initViews()
    }

    override fun onPause() {
        _binding?.playerHomeDeviceCurrent?.onVideoPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            applyHeaderInfo()
            applyStatsInfo()
            renderHomeDeviceMedia()
        }
    }

    private fun initViews() {
        binding.btnMineCustomSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
//        Glide.with(this)
//            .load("https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/d97e0049625e4cfabfcd6c46a3cd8bb0.png?fmt=webp&width=353&height=115")
//            .into(binding.ivMineChangeVersion)
        //服务大厅
        Glide.with(this)
            .load("https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/c587d3a308314e34aa7885d8f37f8d9a.png?fmt=webp&width=288&height=80")
            .into(binding.ivMineServiceCenter)
        Glide.with(this).load("https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/29a5ceb71fcf48daa2ccf2feb3d8b93d.gif").into(binding.floatWindowImg)
        renderMineComboItems()
        renderMineServiceCenterItems()
        renderHomeDeviceMedia()
        binding.viewHomeDevicePickerOverlay.setOnClickListener {
            val context = requireContext()
            val hasImage = AppPreferences.getHomeCurrentDeviceImageUri(context) != null
            val hasVideo = AppPreferences.getHomeCurrentDeviceVideoUri(context) != null
            if (!hasImage && !hasVideo) {
                Toast.makeText(context, R.string.home_device_select_toast, Toast.LENGTH_SHORT).show()
            }
            selectCurrentDeviceMedia.launch(arrayOf("image/*", "video/*"))
        }
        bindServiceCenterScrollIndicator()
        applyHeaderInfo()
        applyStatsInfo()
    }

    private fun renderHomeDeviceMedia() {
        val context = context ?: return
        if (AppPreferences.getHomeCurrentDeviceMediaType(context) == AppPreferences.HOME_DEVICE_MEDIA_TYPE_VIDEO) {
            renderCurrentDeviceVideo()
        } else {
            renderCurrentDeviceImage()
        }
    }

    private fun renderCurrentDeviceImage() {
        val context = context ?: return
        binding.playerHomeDeviceCurrent.onVideoReset()
        binding.playerHomeDeviceCurrent.visibility = View.GONE
        binding.ivHomeDeviceCurrent.visibility = View.VISIBLE
        binding.flHomeDeviceCurrent.layoutParams = binding.flHomeDeviceCurrent.layoutParams.apply {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        val imageUri = AppPreferences.getHomeCurrentDeviceImageUri(context)
        if (imageUri == null) {
            Glide.with(this).clear(binding.ivHomeDeviceCurrent)
            binding.ivHomeDeviceCurrent.setImageResource(R.drawable.bg_home_device_phone_dark)
            return
        }

        Glide.with(this)
            .load(imageUri)
            .placeholder(R.drawable.bg_home_device_phone_dark)
            .error(R.drawable.bg_home_device_phone_dark)
            .into(binding.ivHomeDeviceCurrent)
    }

    private fun renderCurrentDeviceVideo() {
        val context = context ?: return
        val videoUri = AppPreferences.getHomeCurrentDeviceVideoUri(context)
        if (videoUri == null) {
            renderCurrentDeviceImage()
            return
        }

        Glide.with(this).clear(binding.ivHomeDeviceCurrent)
        binding.ivHomeDeviceCurrent.visibility = View.GONE
        binding.playerHomeDeviceCurrent.visibility = View.VISIBLE
        applyHomeDeviceVideoSize(videoUri)

        try {
            binding.playerHomeDeviceCurrent.onVideoReset()
            GSYVideoOptionBuilder()
                .setUrl(videoUri.toString())
                .setVideoTitle("")
                .setCacheWithPlay(true)
                .setLooping(true)
                .setIsTouchWiget(false)
                .setAutoFullWithSize(false)
                .setShowFullAnimation(false)
                .build(binding.playerHomeDeviceCurrent)
            binding.playerHomeDeviceCurrent.startPlayLogic()
        } catch (_: Exception) {
            Toast.makeText(context, "视频加载失败", Toast.LENGTH_SHORT).show()
            renderCurrentDeviceImage()
        }
    }

    private fun applyHomeDeviceVideoSize(videoUri: Uri) {
        val defaultHeight = dpToPx(HOME_DEVICE_VIDEO_DEFAULT_HEIGHT_DP)
        binding.flHomeDeviceCurrent.layoutParams = binding.flHomeDeviceCurrent.layoutParams.apply {
            height = defaultHeight
        }
        binding.flHomeDeviceCurrent.post {
            val binding = _binding ?: return@post
            val containerWidth = binding.flHomeDeviceCurrent.width.takeIf { it > 0 }
                ?: resources.displayMetrics.widthPixels
            val videoSize = readVideoSize(videoUri)
            val targetHeight = if (videoSize != null) {
                (containerWidth.toFloat() * videoSize.second / videoSize.first).toInt()
                    .coerceAtLeast(defaultHeight / 2)
            } else {
                defaultHeight
            }
            binding.flHomeDeviceCurrent.layoutParams = binding.flHomeDeviceCurrent.layoutParams.apply {
                height = targetHeight
            }
        }
    }

    private fun readVideoSize(videoUri: Uri): Pair<Int, Int>? {
        val context = context ?: return null
        return try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                if (width <= 0 || height <= 0) {
                    null
                } else if (rotation == 90 || rotation == 270) {
                    height to width
                } else {
                    width to height
                }
            } finally {
                retriever.release()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun applyWindowInsets() {
        val originalTopPadding = binding.layoutMine.paddingTop
        val originalStartPadding = binding.layoutMine.paddingStart
        val originalEndPadding = binding.layoutMine.paddingEnd
        val originalBottomPadding = binding.layoutMine.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutMine) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.layoutMine.updatePadding(
                left = originalStartPadding,
                top = originalTopPadding + statusBarTop,
                right = originalEndPadding,
                bottom = originalBottomPadding
            )
            insets
        }
    }

    private fun renderMineComboItems() {
        val inflater = LayoutInflater.from(requireContext())
        binding.llMineComboContent.removeAllViews()

        // 固定展示 4 个入口，不足时补空位，避免卡片布局被撑乱。
        val displayItems = buildMineComboItems()
            .take(MINE_COMBO_ITEM_COUNT)
            .let { items ->
                if (items.size == MINE_COMBO_ITEM_COUNT) {
                    items
                } else {
                    items + List(MINE_COMBO_ITEM_COUNT - items.size) { MineComboItem(title = "") }
                }
            }

        displayItems.forEach { item ->
            val itemBinding = ItemHomeMineComboBinding.inflate(inflater, binding.llMineComboContent, false)
            itemBinding.tvComboTitle.text = item.title
            itemBinding.viewComboBadge.visibility = if (item.showBadge) View.VISIBLE else View.GONE

            if (item.iconUrl.isNotBlank()) {
                itemBinding.ivComboIcon.visibility = View.VISIBLE
                Glide.with(this)
                    .load(item.iconUrl)
                    .into(itemBinding.ivComboIcon)
            } else {
                itemBinding.ivComboIcon.visibility = View.GONE
            }

            binding.llMineComboContent.addView(itemBinding.root)
        }
    }

    private fun renderMineServiceCenterItems() {
        val serviceItems = buildMineServiceCenterItems()
        binding.hsvMineServiceCenter.post {
            if (_binding == null) return@post
            val inflater = LayoutInflater.from(requireContext())
            // 服务大厅按“2 行横向展开”排布：一列放 2 个，先展示 4 列共 8 个，剩余继续向右滚动查看。
            val totalColumns =
                if (serviceItems.isEmpty()) 0 else (serviceItems.size + SERVICE_CENTER_ROW_COUNT - 1) / SERVICE_CENTER_ROW_COUNT
            val containerWidth = binding.hsvMineServiceCenter.width
            val itemWidth =
                if (containerWidth > 0) containerWidth / SERVICE_CENTER_VISIBLE_COLUMNS else resources.displayMetrics.widthPixels / SERVICE_CENTER_VISIBLE_COLUMNS

            binding.glMineServiceCenter.removeAllViews()
            binding.glMineServiceCenter.columnCount = totalColumns
            binding.glMineServiceCenter.rowCount = SERVICE_CENTER_ROW_COUNT
            binding.glMineServiceCenter.layoutParams = binding.glMineServiceCenter.layoutParams.apply {
                width = itemWidth * totalColumns
            }

            serviceItems.forEachIndexed { index, item ->
                val itemBinding = ItemHomeMineServiceCenterBinding.inflate(
                    inflater,
                    binding.glMineServiceCenter,
                    false
                )
                itemBinding.tvServiceCenterName.text = item.name
                Glide.with(this)
                    .load(item.iconUrl)
                    .into(itemBinding.ivServiceCenterIcon)

                // 详单查询入口点击跳转到主界面
                if (item.name == getString(R.string.home_service_detail)) {
                    itemBinding.root.setOnClickListener {
                        startActivity(Intent(requireContext(), MainActivity::class.java))
                    }
                }

                // 下标按“先上后下、再到下一列”的方式映射到 GridLayout。
                val row = index % SERVICE_CENTER_ROW_COUNT
                val column = index / SERVICE_CENTER_ROW_COUNT
                itemBinding.root.layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(row),
                    GridLayout.spec(column)
                ).apply {
                    width = itemWidth
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                }
                binding.glMineServiceCenter.addView(itemBinding.root)
            }
            updateServiceCenterScrollIndicator()
        }
    }

    private fun bindServiceCenterScrollIndicator() {
        binding.hsvMineServiceCenter.setOnScrollChangeListener { _, _, _, _, _ ->
            updateServiceCenterScrollIndicator()
        }
        binding.flMineServiceCenterIndicator.doOnLayout {
            updateServiceCenterScrollIndicator()
        }
    }

    private fun updateServiceCenterScrollIndicator() {
        if (_binding == null) return
        val trackWidth = binding.flMineServiceCenterIndicator.width
        val visibleWidth = binding.hsvMineServiceCenter.width
        val contentWidth = binding.glMineServiceCenter.width
        if (trackWidth <= 0 || visibleWidth <= 0 || contentWidth <= 0) return

        // 蓝色滑块宽度按“可视区域 / 内容总宽度”计算，最小保留一段，避免太短难看清。
        val minThumbWidth = dpToPx(SERVICE_CENTER_INDICATOR_MIN_WIDTH_DP)
        val thumbWidth = if (contentWidth <= visibleWidth) {
            trackWidth
        } else {
            ((visibleWidth.toFloat() / contentWidth) * trackWidth)
                .toInt()
                .coerceAtLeast(minThumbWidth)
                .coerceAtMost(trackWidth)
        }
        binding.viewMineServiceCenterIndicatorThumb.layoutParams =
            binding.viewMineServiceCenterIndicatorThumb.layoutParams.apply {
                width = thumbWidth
            }

        // 根据横向滚动比例同步移动蓝色滑块，形成可视化的翻页/滑动进度。
        val maxScroll = (contentWidth - visibleWidth).coerceAtLeast(0)
        val maxTranslation = (trackWidth - thumbWidth).toFloat().coerceAtLeast(0f)
        val progress = if (maxScroll == 0) 0f else binding.hsvMineServiceCenter.scrollX / maxScroll.toFloat()
        binding.viewMineServiceCenterIndicatorThumb.translationX = maxTranslation * progress
    }

    private fun buildMineComboItems(): List<MineComboItem> {
        return listOf(
            MineComboItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/a11e4fa668fb4f8b85210b044a79b27f.png?fmt=webp&width=123&height=123",
                title = getString(R.string.home_promo_week_pack),
                fallbackText = getString(R.string.home_promo_icon_week),
                showBadge = true
            ),
            MineComboItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/c1d8157d8c344c178b643b547aafe3e1.png?fmt=webp&width=123&height=123",
                title = getString(R.string.home_promo_day_pack),
                fallbackText = getString(R.string.home_promo_icon_day)
            ),
            MineComboItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/62eeed79395f4469a2a0e525007dd83e.png?fmt=webp&width=123&height=123",
                title = getString(R.string.home_promo_emergency_pack),
                fallbackText = getString(R.string.home_promo_icon_emergency)
            ),
            MineComboItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/beee928c3ab84a2d8b6138a5ed6f2c18.png?fmt=webp&width=123&height=123",
                title = getString(R.string.home_promo_card_pack),
                fallbackText = getString(R.string.home_promo_icon_card)
            )
        )
    }

    private fun buildMineServiceCenterItems(): List<MineServiceCenterItem> {
        return listOf(
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/3d0faf06272e4d8c89d8d2307fff568c.png?fmt=webp&width=92&height=92",
                name = getString(R.string.home_service_query_left)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/b892cb89d4bc4e12b85acbe8ed26d8a7.png?fmt=webp&width=92&height=92",
                name = getString(R.string.home_service_ordered)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/6b69fe4c0b2c483d9496b072bf302a97.png?fmt=webp&width=92&height=92",
                name = getString(R.string.home_service_bill)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/f2e4962232794d278e28778cdbd53566.png?fmt=webp&width=92&height=92",
                name = getString(R.string.home_service_pay)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/a49d6c5f248047ba8a7274e9423bc2d5.png?fmt=webp&width=92&height=92",
                name = getString(R.string.home_service_package)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/f2e4962232794d278e28778cdbd53566.png?fmt=webp&width=92&height=92",
                name = getString(R.string.home_service_detail)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/fd55be1ad7804f00bcccf75910108b0d.png?fmt=webp&width=92&height=92",
                name = getString(R.string.home_service_complaint)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/e84453b58d4e4cc5bee705d95ea6f0aa.jpg?fmt=webp&width=92&height=92",
                name = getString(R.string.home_service_invoice)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/2f818c39dca44f32a731ce38e24f0df0.png?fmt=webp&width=123&height=123",
                name = getString(R.string.home_service_rights)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/8f7fe8a5c053473ca8cd78baadf22534.png?fmt=webp&width=123&height=123",
                name = getString(R.string.home_service_online)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/8f7fe8a5c053473ca8cd78baadf22534.png?fmt=webp&width=123&height=123",
                name = getString(R.string.home_service_online)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/8f7fe8a5c053473ca8cd78baadf22534.png?fmt=webp&width=123&height=123",
                name = getString(R.string.home_service_online)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/8f7fe8a5c053473ca8cd78baadf22534.png?fmt=webp&width=123&height=123",
                name = getString(R.string.home_service_online)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/8f7fe8a5c053473ca8cd78baadf22534.png?fmt=webp&width=123&height=123",
                name = getString(R.string.home_service_online)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/8f7fe8a5c053473ca8cd78baadf22534.png?fmt=webp&width=123&height=123",
                name = getString(R.string.home_service_online)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/8f7fe8a5c053473ca8cd78baadf22534.png?fmt=webp&width=123&height=123",
                name = getString(R.string.home_service_online)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/8f7fe8a5c053473ca8cd78baadf22534.png?fmt=webp&width=123&height=123",
                name = getString(R.string.home_service_online)
            ),
            MineServiceCenterItem(
                iconUrl = "https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/8f7fe8a5c053473ca8cd78baadf22534.png?fmt=webp&width=123&height=123",
                name = getString(R.string.home_service_online)
            )
        )
    }

    private fun applyHeaderInfo() {
        val context = requireContext()
        val region = AppPreferences.getCustomSelfRegion(context).trim()
        binding.telNumTxt.text = PhoneDisplayManager.display(
            context,
            getString(R.string.home_phone_default)
        )
        binding.cityNameTxt.text = region.ifEmpty { getString(R.string.home_region_default) }
    }

    private fun applyStatsInfo() {
        val context = requireContext()
        val coupon = AppPreferences.getMineStatCoupon(context)
        val data = AppPreferences.getMineStatData(context)
        val balance = AppPreferences.getMineStatBalance(context)
        val bean = AppPreferences.getMineStatBean(context)
        binding.baseCode01.setData(coupon,"","卡券")
        binding.baseCode02.setData(data,"GB","通用流量剩余")
        binding.baseCode03.setData(balance,"元","话费余额")
        binding.baseCode04.setData(bean,"豆","AI豆")

//        binding.tvMineCoupon.text = coupon
//        binding.tvMineData.setNumberWithUnit(data, "GB", numberSizeSp = 16, unitSizeSp = 12)
//        binding.tvMineBalance.setNumberWithUnit(balance, "元", numberSizeSp = 16, unitSizeSp = 12)
//        binding.tvMineBean.setNumberWithUnit(bean, "豆", numberSizeSp = 16, unitSizeSp = 12)
    }

    /**
     * 设置“数字 + 单位”样式，单位字号比数字小。
     *
     * @param number       数字部分文本
     * @param unit         单位部分文本（如 GB、元、豆）
     * @param numberSizeSp 数字字号，单位：sp
     * @param unitSizeSp   单位字号，单位：sp
     */
    private fun TextView.setNumberWithUnit(
        number: String,
        unit: String,
        numberSizeSp: Int,
        unitSizeSp: Int
    ) {
        val fullText = number + unit
        val spannable = SpannableString(fullText)
        spannable.setSpan(
            AbsoluteSizeSpan(numberSizeSp, true),
            0,
            number.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            number.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            AbsoluteSizeSpan(unitSizeSp, true),
            number.length,
            fullText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        text = spannable
    }

    private fun dpToPx(valueDp: Int): Int {
        return (valueDp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        _binding?.playerHomeDeviceCurrent?.onVideoReset()
        super.onDestroyView()
        _binding = null
    }
}
