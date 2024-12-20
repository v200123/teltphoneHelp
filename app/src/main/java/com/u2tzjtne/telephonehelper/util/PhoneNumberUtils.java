package com.u2tzjtne.telephonehelper.util;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.u2tzjtne.telephonehelper.http.bean.PhoneLocalBean;
import com.u2tzjtne.telephonehelper.http.download.getLocalCallback;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import me.ihxq.projects.pna.ISP;
import me.ihxq.projects.pna.PhoneNumberInfo;
import me.ihxq.projects.pna.PhoneNumberLookup;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author luke
 */
public class PhoneNumberUtils {

    private static Map<String,PhoneLocalBean> cache = new HashMap<>();

    public static void getProvince(String phoneNumber, getLocalCallback callback) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return ;
        }
        phoneNumber = phoneNumber.replaceAll(" ", "");
        String finalPhoneNumber = phoneNumber.substring(0, phoneNumber.length() - 4);

        if(phoneNumber.length()>7 && cache.containsKey(finalPhoneNumber))
        {
            Log.d("local", "getProvince: 从缓存读取了");
            //如果有缓存就直接读取缓存的内容
            callback.result(cache.get(finalPhoneNumber));
            return;
        }else{
            Log.d("local", "getProvince: 缓存没有");

        }
        PhoneNumberLookup phoneNumberLookup = new PhoneNumberLookup();
        String province = phoneNumberLookup.lookup(phoneNumber)
                .map(PhoneNumberInfo::getAttribution)
                .map((attribution) ->
                {
                    if (attribution.getProvince().equals(attribution.getCity()))
                        return attribution.getProvince();
                    else
                        return attribution.getProvince() + attribution.getCity();
                })
                .orElse("");
        String operator = getOperator(phoneNumberLookup,phoneNumber);
        if(province.isEmpty()||operator.equals("未知")){
            Log.e("local", "getProvince: 查询了网络" );
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://api.songzixian.com/api/phone-location?dataSource=phone_number_location&phoneNumber="+phoneNumber)
                    .get()
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {

                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    Gson gson = new Gson();
                    JsonObject fromJson = gson.fromJson(response.body().string(), JsonObject.class);
                    if (fromJson.get("message").getAsString() .equals("正常响应")) {
                        JsonObject jsonObject = fromJson.get("data").getAsJsonObject();
                        String province = jsonObject.get("province").getAsString();
                        String city = jsonObject.get("city").getAsString();
                        String carrier = jsonObject.get("carrier").getAsString().replace("中国","");
                        PhoneLocalBean phoneLocalBean = new PhoneLocalBean(province,city,carrier);
                        cache.put(finalPhoneNumber,phoneLocalBean);
                        callback.result(phoneLocalBean);
                    }else{
                        callback.result(new PhoneLocalBean("未知","未知","未知" ));
                    }

                }
            });
        }else{
            PhoneLocalBean phoneLocalBean = new PhoneLocalBean(province, "", operator);
            cache.put(finalPhoneNumber,phoneLocalBean);
            callback.result(phoneLocalBean);
        }
    }

    public static String getOperator(PhoneNumberLookup phoneNumberLookup, String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return "未知";
        }
        phoneNumber = phoneNumber.replaceAll(" ", "");
        String province = phoneNumberLookup.lookup(phoneNumber)
                .map(PhoneNumberInfo::getIsp)
                .map(ISP::getCnName)
                .orElse("");
        LogUtils.d("运营商: " + province);
        return province.replace("中国", "");
    }
}
