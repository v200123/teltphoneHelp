package com.u2tzjtne.telephonehelper.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 自定义号码归属地实体
 * 用于存储用户自定义的特殊号码归属地信息
 * 
 * @author u2tzjtne
 */
@Entity(tableName = "custom_phone_location", indices = {@Index(value = {"phone"}, unique = true)})
public class CustomPhoneLocation {
    
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    /**
     * 手机号码（去掉后4位的前缀，如 1380000）
     */
    @ColumnInfo(name = "phone")
    public String phone;

    /**
     * 省份
     */
    @ColumnInfo(name = "province")
    public String province;

    /**
     * 城市
     */
    @ColumnInfo(name = "city")
    public String city;

    /**
     * 运营商
     */
    @ColumnInfo(name = "carrier")
    public String carrier;

    /**
     * 创建时间
     */
    @ColumnInfo(name = "create_time")
    public long createTime;

    public CustomPhoneLocation() {
    }
@Ignore
    public CustomPhoneLocation(String phone, String province, String city, String carrier) {
        this.phone = phone;
        this.province = province;
        this.city = city;
        this.carrier = carrier;
        this.createTime = System.currentTimeMillis();
    }
}
