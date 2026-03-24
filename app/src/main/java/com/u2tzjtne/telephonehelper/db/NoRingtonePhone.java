package com.u2tzjtne.telephonehelper.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 不显示彩铃的号码表
 * 记录用户设置的不播放彩铃视频的号码
 */
@Entity(
    tableName = "NoRingtonePhone",
    indices = {
        @Index(value = {"phoneNumber"}, unique = true)
    }
)
public class NoRingtonePhone {
    
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    // 手机号码（唯一）
    public String phoneNumber;
    
    // 添加时间
    public long addTime;
    
    public NoRingtonePhone() {
    }

    @Ignore
    public NoRingtonePhone(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.addTime = System.currentTimeMillis();
    }
}
