package com.u2tzjtne.telephonehelper.http.bean;

public class PhoneLocalBean {
   private String province;
    private  String City;
    private String carrier;

    public PhoneLocalBean(String province, String city, String carrier) {
        this.province = province;
        City = city;

        this.carrier = carrier;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return City;
    }

    public void setCity(String city) {
        City = city;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }
}
