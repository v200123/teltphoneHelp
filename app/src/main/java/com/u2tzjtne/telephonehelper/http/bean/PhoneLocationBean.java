package com.u2tzjtne.telephonehelper.http.bean;

/**
 * @author u2tzjtne@gmail.com
 * @date 2020/6/23
 */
public class PhoneLocationBean {

    /**
     * phone : 17326125059
     * province : 江苏
     * city : 南京
     * zip_code : 210000
     * area_code : 025
     * phone_type : 电信
     */

    private String phone;
    private String province;
    private String city;
    private String zip_code;
    private String area_code;
    private String phone_type;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getZip_code() {
        return zip_code;
    }

    public void setZip_code(String zip_code) {
        this.zip_code = zip_code;
    }

    public String getArea_code() {
        return area_code;
    }

    public void setArea_code(String area_code) {
        this.area_code = area_code;
    }

    public String getPhone_type() {
        return phone_type;
    }

    public void setPhone_type(String phone_type) {
        this.phone_type = phone_type;
    }
}
