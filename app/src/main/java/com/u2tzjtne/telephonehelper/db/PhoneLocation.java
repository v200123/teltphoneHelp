package com.u2tzjtne.telephonehelper.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "phone_location", indices = {@Index(value = {"city_code"}, name = "area"), @Index(value = {"phone"}, unique = true)})
public class PhoneLocation {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "pref")
    public String pref;

    @ColumnInfo(name = "phone")
    public String phone;

    @ColumnInfo(name = "province")
    public String province;

    @ColumnInfo(name = "city")
    public String city;

    @ColumnInfo(name = "isp")
    public String isp;

    @ColumnInfo(name = "isp_type")
    public int ispType;

    @ColumnInfo(name = "post_code")
    public String postCode;

    @ColumnInfo(name = "city_code")
    public String cityCode;

    @ColumnInfo(name = "area_code")
    public String areaCode;

    @ColumnInfo(name = "create_time")
    public String createTime;
}