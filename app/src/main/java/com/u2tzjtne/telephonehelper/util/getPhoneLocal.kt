package com.u2tzjtne.telephonehelper.util

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

 fun getPhoneNumberLocal(lifecycleScope:LifecycleCoroutineScope,context:Context,phone: String):String? {
    val pattern = "^(13[0-9]|14[579]|15[0-35-9]|166|17[0135678]|18[0-9]|19[0-9])\\d{8}\$"
    val isPhone = phone.trim().matches(pattern.toRegex())
    if (isPhone) {
        var result = ""
        lifecycleScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()

            val request = Request.Builder()
                .url("https://api.songzixian.com/api/phone-location?dataSource=PHONE_NUMBER_LOCATION&phoneNumber=${phone}")
                .get()
                .build()
            try {
                val response = client.newCall(request).execute()
                val gson = Gson()
                val fromJson = gson.fromJson(response.body!!.string(), JsonObject::class.java)
                withContext(Dispatchers.Main) {
                    if (fromJson.get("message").asString == "正常响应") {
                        val jsonObject = fromJson.get("data").asJsonObject
                        val province = jsonObject.get("province").asString
                        val city = jsonObject.get("city").asString
                        val carrier = jsonObject.get("carrier").asString
                        val message = "$province $city"
                        result = "${province}-$city-$carrier"
                        return@withContext result;
                    }
                }
            }catch (e:Exception){
                Toast.makeText(context,"获取失败",Toast.LENGTH_SHORT).show()
            }

        }
    }
    return null;
}