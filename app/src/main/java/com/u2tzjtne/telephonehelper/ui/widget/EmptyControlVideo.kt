package com.u2tzjtne.telephonehelper.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import com.u2tzjtne.telephonehelper.R

/**
 * 无任何控制UI的视频播放器
 * 用于彩铃视频播放,只显示视频画面,不显示任何控制按钮和进度条
 * 
 * 功能说明:
 * - 移除所有控制UI(播放/暂停、进度条、音量、亮度等)
 * - 禁用所有触摸手势(滑动快进、音量调节、亮度调节)
 * - 禁用双击暂停/播放
 * - 只保留视频渲染视图
 */
class EmptyControlVideo : StandardGSYVideoPlayer {

    constructor(context: Context, fullFlag: Boolean) : super(context, fullFlag)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /**
     * 使用空布局,只包含 SurfaceView,不包含任何控制UI
     */
    override fun getLayoutId(): Int {
        return R.layout.empty_control_video
    }

    /**
     * 禁用全屏模式下的触摸手势
     * - 禁用滑动快进/快退
     * - 禁用滑动调节音量
     * - 禁用滑动调节亮度
     */
    override fun touchSurfaceMoveFullLogic(absDeltaX: Float, absDeltaY: Float) {
        super.touchSurfaceMoveFullLogic(absDeltaX, absDeltaY)
        
        // 禁用触摸快进
        mChangePosition = false
        
        // 禁用触摸音量调节
        mChangeVolume = false
        
        // 禁用触摸亮度调节
        mBrightness = false
    }

    /**
     * 禁用双击暂停/播放
     */
    override fun touchDoubleUp(e: MotionEvent) {
        // 不调用 super.touchDoubleUp(),禁用双击功能
    }

    /**
     * 可选:禁用单击暂停/播放
     * 如果需要禁用单击,取消注释下面的方法
     */
    /*
    override fun touchSurfaceUp() {
        // 不调用 super.touchSurfaceUp(),禁用单击暂停
    }
    */
}
