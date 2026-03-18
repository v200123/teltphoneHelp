package com.u2tzjtne.telephonehelper.util;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.u2tzjtne.telephonehelper.db.AppDatabase;
import com.u2tzjtne.telephonehelper.db.CustomPhoneLocation;
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
    private static Map<String, PhoneLocalBean> customCache = new HashMap<>(); // 自定义归属地缓存

    /**
     * 判断是否为正常的中国手机号码
     * 中国大陆手机号：1开头，第二位为3-9，共11位
     * @param phoneNumber 纯数字号码
     * @return 是否为正常手机号
     */
    private static boolean isValidChinesePhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() != 11) {
            return false;
        }
        // 1开头，第二位为3-9
        return phoneNumber.matches("^1[3-9]\\d{9}$");
    }

    public static void getProvince(String phoneNumber, getLocalCallback callback) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return;
        }
        final String fullPhoneNumber = phoneNumber.replaceAll(" ", "");
        
        // 1. 先检查自定义归属地缓存（完整号码精确匹配）
        if (customCache.containsKey(fullPhoneNumber)) {
            Log.d("local", "getProvince: 从自定义缓存读取了");
            callback.result(customCache.get(fullPhoneNumber));
            return;
        }

        // 2. 异步查询数据库中的自定义归属地，然后继续后续流程
        AppDatabase.getInstance().customPhoneLocationModel()
            .findByPhone(fullPhoneNumber)
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe(
                customLocation -> {
                    if (customLocation != null) {
                        PhoneLocalBean bean = new PhoneLocalBean(
                            customLocation.province,
                            customLocation.city,
                            customLocation.carrier != null ? customLocation.carrier : ""
                        );
                        customCache.put(fullPhoneNumber, bean);
                        Log.d("local", "getProvince: 从自定义数据库读取了");
                        callback.result(bean);
                    } else {
                        // 没有自定义归属地，继续执行后续查询逻辑
                        continueQueryAfterCustom(fullPhoneNumber, callback);
                    }
                },
                error -> {
                    Log.e("local", "查询自定义归属地失败: " + error.getMessage());
                    // 出错时继续执行后续查询逻辑
                    continueQueryAfterCustom(fullPhoneNumber, callback);
                }
            );
    }
    
    /**
     * 在自定义归属地查询完成后继续执行查询逻辑
     * 在IO线程执行，避免阻塞主线程
     */
    private static void continueQueryAfterCustom(String fullPhoneNumber, getLocalCallback callback) {
        // 切换到IO线程执行后续查询
        io.reactivex.schedulers.Schedulers.io().scheduleDirect(() -> {
            // 如果不是正常的中国手机号，直接返回未知
            if (!isValidChinesePhoneNumber(fullPhoneNumber)) {
                Log.d("local", "getProvince: 非正常手机号码，跳过查询");
                notifyCallbackOnMainThread(callback, new PhoneLocalBean("未知", "未知", "未知"));
                return;
            }

            String finalPhoneNumber = fullPhoneNumber.substring(0, fullPhoneNumber.length() - 4);

            // 检查普通缓存
            if (fullPhoneNumber.length() > 7 && cache.containsKey(finalPhoneNumber)) {
                Log.d("local", "getProvince: 从缓存读取了");
                notifyCallbackOnMainThread(callback, cache.get(finalPhoneNumber));
                return;
            } else {
                Log.d("local", "getProvince: 缓存没有");
            }

            // 本地库查询
            PhoneNumberLookup phoneNumberLookup = new PhoneNumberLookup();
            String province = phoneNumberLookup.lookup(fullPhoneNumber)
                    .map(PhoneNumberInfo::getAttribution)
                    .map((attribution) -> {
                        if (attribution.getProvince().equals(attribution.getCity()))
                            return attribution.getProvince();
                        else
                            return attribution.getProvince() + attribution.getCity();
                    })
                    .orElse("");
            String operator = getOperator(phoneNumberLookup, fullPhoneNumber);

            if (province.isEmpty() || operator.equals("未知")) {
                Log.e("local", "getProvince: 查询了网络");
                // 网络查询
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("https://api.songzixian.com/api/phone-location?dataSource=phone_number_location&phoneNumber=" + fullPhoneNumber)
                        .get()
                        .build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        notifyCallbackOnMainThread(callback, new PhoneLocalBean("未知", "未知", "未知"));
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        try {
                            Gson gson = new Gson();
                            JsonObject fromJson = gson.fromJson(response.body().string(), JsonObject.class);
                            if (fromJson.get("message").getAsString().equals("正常响应")) {
                                JsonObject jsonObject = fromJson.get("data").getAsJsonObject();
                                String p = jsonObject.get("province").getAsString();
                                String city = jsonObject.get("city").getAsString();
                                String carrier = jsonObject.get("carrier").getAsString().replace("中国", "");
                                PhoneLocalBean phoneLocalBean = new PhoneLocalBean(p, city, carrier);
                                cache.put(finalPhoneNumber, phoneLocalBean);
                                notifyCallbackOnMainThread(callback, phoneLocalBean);
                            } else {
                                notifyCallbackOnMainThread(callback, new PhoneLocalBean("未知", "未知", "未知"));
                            }
                        } catch (Exception e) {
                            Log.e("local", "解析网络响应失败: " + e.getMessage());
                            notifyCallbackOnMainThread(callback, new PhoneLocalBean("未知", "未知", "未知"));
                        }
                    }
                });
            } else {
                PhoneLocalBean phoneLocalBean = new PhoneLocalBean(province, "", operator);
                cache.put(finalPhoneNumber, phoneLocalBean);
                notifyCallbackOnMainThread(callback, phoneLocalBean);
            }
        });
    }
    
    /**
     * 在主线程回调结果
     */
    private static void notifyCallbackOnMainThread(getLocalCallback callback, PhoneLocalBean bean) {
        io.reactivex.android.schedulers.AndroidSchedulers.mainThread().scheduleDirect(() -> {
            callback.result(bean);
        });
    }
    
    /**
     * 清除自定义归属地缓存
     */
    public static void clearCustomCache() {
        customCache.clear();
        Log.d("local", "自定义归属地缓存已清除");
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
