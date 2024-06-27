package com.u2tzjtne.telephonehelper.http;

import com.u2tzjtne.telephonehelper.http.bean.LoginBean;
import com.u2tzjtne.telephonehelper.http.bean.PhoneLocationBean;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * @author u2tzjtne@gmail.com
 */
public interface HttpServer {

    /**
     * 获取验证码
     *
     * @param body
     * @return
     */
    @POST("get_code")
    Observable<HttpResult<Object>> getCode(@Body RequestBody body);

    /**
     * 登录
     *
     * @param body
     * @return
     */
    @POST("login")
    Observable<HttpResult<LoginBean>> login(@Body RequestBody body);

    /**
     * 获取手机号码归属地
     *
     * @param body
     * @return
     */
    @POST("phone_location")
    Observable<HttpResult<PhoneLocationBean>> getPhoneLocation(@Body RequestBody body);
}
