package com.u2tzjtne.telephonehelper.http.bean;

/**
 * @author u2tzjtne@gmail.com
 * @date 2020/6/23
 */
public class LoginBean {

    /**
     * extention_number : 1004
     * password : aegis2020
     * court_code : 甘肃省高院
     * court_name : 甘肃省高院
     */

    private String extention_number;
    private String password;
    private String court_code;
    private String court_name;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    private String host;

    public String getLawyer_name() {
        return lawyer_name;
    }

    public void setLawyer_name(String lawyer_name) {
        this.lawyer_name = lawyer_name;
    }

    private String lawyer_name;

    public String getExtention_number() {
        return extention_number;
    }

    public void setExtention_number(String extention_number) {
        this.extention_number = extention_number;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCourt_code() {
        return court_code;
    }

    public void setCourt_code(String court_code) {
        this.court_code = court_code;
    }

    public String getCourt_name() {
        return court_name;
    }

    public void setCourt_name(String court_name) {
        this.court_name = court_name;
    }
}
