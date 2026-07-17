package com.example.myservicecenter.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.example.myservicecenter.R
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer

/**
 * 无控制 UI 的视频播放器，只保留视频画面。
 */
class NoControlVideoPlayer : StandardGSYVideoPlayer {

    constructor(context: Context, fullFlag: Boolean) : super(context, fullFlag)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun getLayoutId(): Int = R.layout.player_no_control

    override fun touchSurfaceMoveFullLogic(absDeltaX: Float, absDeltaY: Float) {
        mChangePosition = false
        mChangeVolume = false
        mBrightness = false
    }

    override fun touchDoubleUp(e: MotionEvent) {
    }
}
