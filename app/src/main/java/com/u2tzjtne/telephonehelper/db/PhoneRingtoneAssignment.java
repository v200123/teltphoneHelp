package com.u2tzjtne.telephonehelper.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 号码与彩铃的分配关系表
 * 记录每个号码被随机分配的彩铃，第一次拨打时随机分配，后续一直使用同一个
 */
@Entity(
    tableName = "PhoneRingtoneAssignment",
    foreignKeys = {
        @ForeignKey(
            entity = RingVideo.class,
            parentColumns = "id",
            childColumns = "ringtoneId",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = {"phoneNumber"}, unique = true),
        @Index(value = {"ringtoneId"})
    }
)
public class PhoneRingtoneAssignment {
    
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    // 手机号码（唯一）
    public String phoneNumber;
    
    // 分配的彩铃ID
    public int ringtoneId;
    
    // 分配时间（第一次拨打时间）
    public long assignTime;
    
    public PhoneRingtoneAssignment() {
    }

    @Ignore
    public PhoneRingtoneAssignment(String phoneNumber, int ringtoneId) {
        this.phoneNumber = phoneNumber;
        this.ringtoneId = ringtoneId;
        this.assignTime = System.currentTimeMillis();
    }
}
