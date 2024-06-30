package com.u2tzjtne.telephonehelper.ui.dialog


import android.content.Context
import android.graphics.Color
import android.widget.TextView
import com.lxj.xpopup.core.BubbleAttachPopupView
import com.lxj.xpopup.util.XPopupUtils
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.ui.activity.AddCallRecordActivity.Companion.formatWithSpaces


class CopyPhoneNumberDialog(context: Context,val phone:String,val block:()->Unit) : BubbleAttachPopupView(context) {


    override fun getImplLayoutId(): Int {
        return R.layout.xpop_phone_number_show
    }

    override fun onCreate() {
        super.onCreate()
        setBubbleBgColor(Color.WHITE)
        setBubbleShadowSize(XPopupUtils.dp2px(context, 6f))
        setBubbleShadowColor(Color.BLACK)
        setArrowWidth(XPopupUtils.dp2px(context, 8f))
        setArrowHeight(XPopupUtils.dp2px(context, 9f))
        //                                .setBubbleRadius(100)
        setArrowRadius(XPopupUtils.dp2px(context, 2f))
        val textView = findViewById<TextView>(R.id.tv_copy_phone_number)
        textView.text = phone.formatWithSpaces()
        textView.setOnClickListener {
            dismiss()
            block()
            block
        }
    }
}