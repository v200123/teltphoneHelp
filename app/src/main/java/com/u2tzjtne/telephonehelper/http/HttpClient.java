package com.u2tzjtne.telephonehelper.http;


import com.u2tzjtne.telephonehelper.http.bean.LoginBean;
import com.u2tzjtne.telephonehelper.http.bean.PhoneLocationBean;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author u2tzjtne@gmail.com
 */
public class HttpClient {

    /**
     * 超时时间 单位S
     */
    private static final int DEFAULT_TIMEOUT = 5;

    private HttpServer mHttpServer;

    public static String BASE_URL = "http://114.116.40.8:18116/";


    private HttpClient() {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        httpClientBuilder.addInterceptor(chain -> {
            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder();
            Request request = requestBuilder.build();
            return chain.proceed(request);
        });
        Retrofit retrofit = new Retrofit.Builder()
                .client(httpClientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(BASE_URL)
                .build();
        mHttpServer = retrofit.create(HttpServer.class);
    }

    /**
     * 用来统一处理Http的resultCode,并将HttpResult的Data部分剥离出来
     *
     * @param <T> 真正需要的数据类型，也就是Data部分的数据类型
     */
    private class HttpResultFunc<T> implements Function<HttpResult<T>, T> {

        @Override
        public T apply(HttpResult<T> httpResult) {
            if (httpResult.getCode() != 200) {
                throw new ApiException(httpResult.getMessage());
            }
            return httpResult.getData();
        }
    }

    /**
     * 在访问HttpMethods时创建单例
     */
    private static class SingletonHolder {
        private static final HttpClient INSTANCE = new HttpClient();
    }

    /**
     * 获取单例
     *
     * @return
     */
    public static HttpClient getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private RequestBody createBody(JSONObject jsonObject) {
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());
    }

    /**
     * 添加线程管理并订阅
     *
     * @param observable
     * @param observer
     */
    private void toSubscribe(Observable observable, Observer observer) {
        observable.subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }


    /***********************************************************************************************
     *                                             接口实现部分
     * ********************************************************************************************/


    public void getCode(String phone, Observer<Object> observer) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("phone", phone);
            Observable observable = mHttpServer.getCode(createBody(jsonObject)).map(new HttpResultFunc<>());;
            toSubscribe(observable, observer);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void login(String phone, String code, Observer<LoginBean> observer) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("phone", phone);
            jsonObject.put("code", code);
            Observable observable = mHttpServer.login(createBody(jsonObject)).map(new HttpResultFunc<>());;
            toSubscribe(observable, observer);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void getPhoneLocation(String phone, Observer<PhoneLocationBean> observer) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("phone", phone);
            Observable observable = mHttpServer.getPhoneLocation(createBody(jsonObject)).map(new HttpResultFunc<>());;
            toSubscribe(observable, observer);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
