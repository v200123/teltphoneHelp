package com.u2tzjtne.telephonehelper.util

import android.content.Context
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max

object RingtoneBadgeRenderHelper {
    private const val MIN_BADGE_SCALE = 0.02f
    private const val MAX_BADGE_SCALE = 2.0f

    fun clear(canvas: FrameLayout) {
        canvas.removeAllViews()
    }

    fun renderRule(context: Context, canvas: FrameLayout, rule: BadgeRule?) {
        clear(canvas)
        if (rule == null) {
            return
        }
        val addedItems = rule.items
            .mapNotNull { (key, state) ->
                val badgeId = BadgeId.fromKey(key) ?: return@mapNotNull null
                if (!state.added) return@mapNotNull null
                badgeId to state
            }
            .sortedBy { it.second.zIndex }

        if (addedItems.isEmpty()) {
            return
        }

        addedItems.forEach { (badgeId, state) ->
            val view = createBadgeView(context, badgeId)
            canvas.addView(view)
            view.post {
                applyStateToView(canvas, view, state)
            }
        }
    }

    fun createBadgeView(context: Context, badgeId: BadgeId): AppCompatImageView {
        return AppCompatImageView(context).apply {
            setImageResource(badgeId.drawableRes)
            adjustViewBounds = true
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            tag = badgeId.key
        }
    }

    fun applyStateToView(canvas: FrameLayout, view: AppCompatImageView, state: BadgeItemState) {
        val safeScale = state.scale.coerceIn(MIN_BADGE_SCALE, MAX_BADGE_SCALE)
        view.scaleX = safeScale
        view.scaleY = safeScale
        val scaledWidth = view.width * safeScale
        val scaledHeight = view.height * safeScale
        val centerX = state.xPercent.coerceIn(0f, 1f) * canvas.width
        val centerY = state.yPercent.coerceIn(0f, 1f) * canvas.height
        var left = centerX - scaledWidth / 2f
        var top = centerY - scaledHeight / 2f
        left = left.coerceIn(0f, max(0f, canvas.width - scaledWidth))
        top = top.coerceIn(0f, max(0f, canvas.height - scaledHeight))
        view.x = left
        view.y = top
    }
}
