package com.u2tzjtne.telephonehelper.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import com.u2tzjtne.telephonehelper.databinding.ActivityBadgeRuleEditorBinding
import com.u2tzjtne.telephonehelper.util.BadgeId
import com.u2tzjtne.telephonehelper.util.BadgeItemState
import com.u2tzjtne.telephonehelper.util.BadgeRule
import com.u2tzjtne.telephonehelper.util.RingtoneBadgeRenderHelper
import com.u2tzjtne.telephonehelper.util.RingtoneBadgeRuleStore
import kotlin.math.max

class BadgeRuleEditorActivity : BaseActivity() {
    private val binding: ActivityBadgeRuleEditorBinding by lazy {
        ActivityBadgeRuleEditorBinding.inflate(layoutInflater)
    }

    private val badgeViews = mutableMapOf<BadgeId, AppCompatImageView>()
    private var selectedBadgeId: BadgeId? = null
    private var activeBadgeView: AppCompatImageView? = null
    private var currentRule: BadgeRule? = null
    private var lastRawX = 0f
    private var lastRawY = 0f

    private val scaleDetector by lazy {
        ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val view = activeBadgeView ?: return false
                val newScale = (view.scaleX * detector.scaleFactor).coerceIn(MIN_BADGE_SCALE, MAX_BADGE_SCALE)
                view.scaleX = newScale
                view.scaleY = newScale
                clampViewPosition(view)
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                super.onScaleEnd(detector)
                persistRule()
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        loadRule()
    }

    private fun initView() {
        binding.ivBack.setOnClickListener { finish() }
        binding.btnDeleteSelected.setOnClickListener { deleteSelectedBadge() }
        binding.btnClearAll.setOnClickListener { clearAllBadges() }

        binding.slotBadge1.setOnClickListener { onResourceClick(BadgeId.BADGE_1) }
        binding.slotBadge2.setOnClickListener { onResourceClick(BadgeId.BADGE_2) }
        binding.slotBadge3.setOnClickListener { onResourceClick(BadgeId.BADGE_3) }
        binding.slotBadge4.setOnClickListener { onResourceClick(BadgeId.BADGE_4) }
        binding.slotBadge5.setOnClickListener { onResourceClick(BadgeId.BADGE_5) }
    }

    private fun loadRule() {
        val ruleId = intent.getStringExtra(EXTRA_RULE_ID).orEmpty()
        if (ruleId.isBlank()) {
            Toast.makeText(this, "规则ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val rule = RingtoneBadgeRuleStore.getRule(this, ruleId)
        if (rule == null) {
            Toast.makeText(this, "规则不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentRule = rule
        binding.tvRuleName.text = rule.name
        renderRule(rule)
    }

    private fun renderRule(rule: BadgeRule) {
        badgeViews.clear()
        binding.badgeCanvas.removeAllViews()
        val sortedItems = rule.items
            .mapNotNull { (key, state) ->
                val badgeId = BadgeId.fromKey(key) ?: return@mapNotNull null
                if (!state.added) return@mapNotNull null
                badgeId to state
            }
            .sortedBy { it.second.zIndex }

        sortedItems.forEach { (badgeId, state) ->
            val view = createInteractiveBadgeView(badgeId)
            badgeViews[badgeId] = view
            binding.badgeCanvas.addView(view)
            view.post { RingtoneBadgeRenderHelper.applyStateToView(binding.badgeCanvas, view, state) }
        }
        selectedBadgeId = null
        updateResourceSlots()
        updateSelectedTip()
    }

    private fun createInteractiveBadgeView(badgeId: BadgeId): AppCompatImageView {
        return RingtoneBadgeRenderHelper.createBadgeView(this, badgeId).apply {
            setOnTouchListener { view, event ->
                handleBadgeTouch(view as AppCompatImageView, event)
            }
        }
    }

    private fun handleBadgeTouch(view: AppCompatImageView, event: MotionEvent): Boolean {
        val badgeId = BadgeId.fromKey(view.tag as? String ?: "") ?: return false
        activeBadgeView = view
        selectBadge(badgeId)
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = event.rawX
                lastRawY = event.rawY
                view.bringToFront()
                binding.badgeCanvas.invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && event.pointerCount == 1) {
                    val dx = event.rawX - lastRawX
                    val dy = event.rawY - lastRawY
                    view.x += dx
                    view.y += dy
                    clampViewPosition(view)
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                persistRule()
            }
        }
        return true
    }

    private fun onResourceClick(badgeId: BadgeId) {
        val existed = badgeViews[badgeId]
        if (existed != null) {
            selectBadge(badgeId)
            existed.bringToFront()
            binding.badgeCanvas.invalidate()
            persistRule()
            return
        }
        val newView = createInteractiveBadgeView(badgeId)
        badgeViews[badgeId] = newView
        binding.badgeCanvas.addView(newView)
        newView.post {
            val centerState = BadgeItemState(
                added = true,
                xPercent = 0.5f,
                yPercent = 0.5f,
                scale = 1f,
                zIndex = binding.badgeCanvas.childCount - 1
            )
            RingtoneBadgeRenderHelper.applyStateToView(binding.badgeCanvas, newView, centerState)
            selectBadge(badgeId)
            persistRule()
        }
        updateResourceSlots()
    }

    private fun deleteSelectedBadge() {
        val badgeId = selectedBadgeId ?: return
        val view = badgeViews.remove(badgeId) ?: return
        binding.badgeCanvas.removeView(view)
        selectedBadgeId = null
        updateResourceSlots()
        updateSelectedTip()
        persistRule()
    }

    private fun clearAllBadges() {
        badgeViews.clear()
        selectedBadgeId = null
        binding.badgeCanvas.removeAllViews()
        updateResourceSlots()
        updateSelectedTip()
        persistRule()
    }

    private fun selectBadge(badgeId: BadgeId) {
        selectedBadgeId = badgeId
        updateResourceSlots()
        updateSelectedTip()
    }

    private fun updateSelectedTip() {
        val selectedName = selectedBadgeId?.displayName ?: "未选中"
        binding.tvSelected.text = "当前选中：$selectedName"
    }

    private fun updateResourceSlots() {
        updateSlot(binding.slotBadge1, binding.tvBadge1State, BadgeId.BADGE_1)
        updateSlot(binding.slotBadge2, binding.tvBadge2State, BadgeId.BADGE_2)
        updateSlot(binding.slotBadge3, binding.tvBadge3State, BadgeId.BADGE_3)
        updateSlot(binding.slotBadge4, binding.tvBadge4State, BadgeId.BADGE_4)
        updateSlot(binding.slotBadge5, binding.tvBadge5State, BadgeId.BADGE_5)
    }

    private fun updateSlot(slot: View, stateView: TextView, badgeId: BadgeId) {
        val added = badgeViews.containsKey(badgeId)
        val selected = selectedBadgeId == badgeId
        slot.alpha = if (added) 0.85f else 1f
        slot.setBackgroundColor(
            when {
                selected -> Color.parseColor("#334AA3FF")
                added -> Color.parseColor("#2222CC88")
                else -> Color.TRANSPARENT
            }
        )
        stateView.text = if (added) "已添加" else "未添加"
    }

    private fun persistRule() {
        val rule = currentRule ?: return
        val canvasWidth = binding.badgeCanvas.width
        val canvasHeight = binding.badgeCanvas.height
        if (canvasWidth <= 0 || canvasHeight <= 0) {
            return
        }

        val newItems = RingtoneBadgeRuleStore.createEmptyItems()
        val orderedKeys = mutableListOf<String>()
        for (index in 0 until binding.badgeCanvas.childCount) {
            val child = binding.badgeCanvas.getChildAt(index) as? AppCompatImageView ?: continue
            val key = child.tag as? String ?: continue
            if (BadgeId.fromKey(key) != null) {
                orderedKeys.add(key)
            }
        }
        val zMap = orderedKeys.withIndex().associate { it.value to it.index }

        BadgeId.entries.forEach { badgeId ->
            val view = badgeViews[badgeId]
            if (view == null) {
                newItems[badgeId.key] = BadgeItemState(
                    added = false,
                    xPercent = 0.5f,
                    yPercent = 0.5f,
                    scale = 1f,
                    zIndex = zMap[badgeId.key] ?: 0
                )
            } else {
                val scale = view.scaleX.coerceIn(MIN_BADGE_SCALE, MAX_BADGE_SCALE)
                val scaledWidth = view.width * scale
                val scaledHeight = view.height * scale
                val centerX = (view.x + scaledWidth / 2f) / canvasWidth
                val centerY = (view.y + scaledHeight / 2f) / canvasHeight
                newItems[badgeId.key] = BadgeItemState(
                    added = true,
                    xPercent = centerX.coerceIn(0f, 1f),
                    yPercent = centerY.coerceIn(0f, 1f),
                    scale = scale,
                    zIndex = zMap[badgeId.key] ?: 0
                )
            }
        }

        val newRule = rule.copy(items = newItems)
        if (RingtoneBadgeRuleStore.saveRuleLayout(this, newRule)) {
            currentRule = RingtoneBadgeRuleStore.getRule(this, rule.id) ?: newRule
        }
    }

    private fun clampViewPosition(view: AppCompatImageView) {
        val canvasWidth = binding.badgeCanvas.width.toFloat()
        val canvasHeight = binding.badgeCanvas.height.toFloat()
        val scaledWidth = view.width * view.scaleX
        val scaledHeight = view.height * view.scaleY
        view.x = view.x.coerceIn(0f, max(0f, canvasWidth - scaledWidth))
        view.y = view.y.coerceIn(0f, max(0f, canvasHeight - scaledHeight))
    }

    companion object {
        const val EXTRA_RULE_ID = "extra_rule_id"
        const val MIN_BADGE_SCALE = 0.2f
        const val MAX_BADGE_SCALE = 2.0f
    }
}
