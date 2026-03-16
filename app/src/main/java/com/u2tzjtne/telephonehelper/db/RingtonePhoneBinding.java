package com.u2tzjtne.telephonehelper.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 彩铃与手机号码绑定关系表
 * 
 * 功能说明：
 * - 一个彩铃可以绑定多个手机号码
 * - 一个手机号码只能绑定一个彩铃（或没有）
 * - 拨打电话时根据号码查找绑定的彩铃
 */
@Entity(
    tableName = "RingtonePhoneBinding",
    foreignKeys = {
        @ForeignKey(
            entity = RingVideo.class,
            parentColumns = "id",
            childColumns = "ringtoneId",
            onDelete = ForeignKey.CASCADE // 彩铃删除时自动解绑
        )
    },
    indices = {
        @Index(value = {"phoneNumber"}, unique = true), // 号码唯一
        @Index(value = {"ringtoneId"}) // 加速按彩铃查询
    }
)
public class RingtonePhoneBinding {
    
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    // 手机号码（唯一）
    public String phoneNumber;
    
    // 绑定的彩铃ID
    public int ringtoneId;
    
    // 绑定时间
    public long bindTime;
    
    public RingtonePhoneBinding() {
    }
    
    @Ignore
    public RingtonePhoneBinding(String phoneNumber, int ringtoneId) {
        this.phoneNumber = phoneNumber;
        this.ringtoneId = ringtoneId;
        this.bindTime = System.currentTimeMillis();
    }
}