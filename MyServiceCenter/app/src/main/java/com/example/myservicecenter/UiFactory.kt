package com.example.myservicecenter

import android.content.res.Resources
import android.graphics.drawable.GradientDrawable

object UiFactory {
    private val density: Float
        get() = Resources.getSystem().displayMetrics.density

    private fun px(valueDp: Float): Float = valueDp * density

    fun gradient(
        colors: IntArray,
        radiusDp: Float = 18f,
        topRadiusDp: Float = radiusDp,
        bottomRadiusDp: Float = radiusDp
    ): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors).apply {
            cornerRadii = floatArrayOf(
                px(topRadiusDp), px(topRadiusDp),
                px(topRadiusDp), px(topRadiusDp),
                px(bottomRadiusDp), px(bottomRadiusDp),
                px(bottomRadiusDp), px(bottomRadiusDp)
            )
        }
    }

    fun roundedFill(fillColor: Int, radiusDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = px(radiusDp)
            setColor(fillColor)
        }
    }

    fun pill(fillColor: Int, strokeColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = px(999f)
            setColor(fillColor)
            setStroke(px(1f).toInt().coerceAtLeast(1), strokeColor)
        }
    }

    fun outlinedCard(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = px(16f)
            setColor(0xFFFFFFFF.toInt())
            setStroke(px(1f).toInt().coerceAtLeast(1), 0xFFE9EEF5.toInt())
        }
    }
}
