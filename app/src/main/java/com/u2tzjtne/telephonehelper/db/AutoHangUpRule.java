package com.u2tzjtne.telephonehelper.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 自动挂断规则
 * 记录指定号码在接通后多少秒自动挂断
 */
@Entity(
    tableName = "AutoHangUpRule",
    indices = {
        @Index(value = {"phoneNumber"}, unique = true)
    }
)
public class AutoHangUpRule {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String phoneNumber;

    public int hangUpDelaySeconds;

    public long addTime;

    public AutoHangUpRule() {
    }

    @Ignore
    public AutoHangUpRule(String phoneNumber, int hangUpDelaySeconds) {
        this.phoneNumber = phoneNumber;
        this.hangUpDelaySeconds = hangUpDelaySeconds;
        this.addTime = System.currentTimeMillis();
    }
}
