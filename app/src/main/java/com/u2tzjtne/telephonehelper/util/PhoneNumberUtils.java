package com.u2tzjtne.telephonehelper.util;

import android.text.TextUtils;

import me.ihxq.projects.pna.ISP;
import me.ihxq.projects.pna.PhoneNumberInfo;
import me.ihxq.projects.pna.PhoneNumberLookup;

/**
 * @author luke
 */
public class PhoneNumberUtils {
    public static String getProvince(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return "未知";
        }
        phoneNumber = phoneNumber.replaceAll(" ", "");
        PhoneNumberLookup phoneNumberLookup = new PhoneNumberLookup();
        String province = phoneNumberLookup.lookup(phoneNumber)
                .map(PhoneNumberInfo::getAttribution)
                .map(attribution -> attribution.getProvince() + attribution.getCity())
                .orElse("未知");
        LogUtils.d("归属地: " + province);
        return province;
    }

    public static String getOperator(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return "未知";
        }
        phoneNumber = phoneNumber.replaceAll(" ", "");
        PhoneNumberLookup phoneNumberLookup = new PhoneNumberLookup();
        String province = phoneNumberLookup.lookup(phoneNumber)
                .map(PhoneNumberInfo::getIsp)
                .map(ISP::getCnName)
                .orElse("未知");
        LogUtils.d("运营商: " + province);
        return province.replace("中国", "");
    }
}
