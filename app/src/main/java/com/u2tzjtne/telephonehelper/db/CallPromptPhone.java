package com.u2tzjtne.telephonehelper.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 特殊提示音号码表
 * 用于记录拨号后命中关机/空号提示音的号码
 */
@Entity(
    tableName = "CallPromptPhone",
    indices = {
        @Index(value = {"phoneNumber"}, unique = true)
    }
)
public class CallPromptPhone {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String phoneNumber;

    /**
     * 1 = 关机提示音
     * 2 = 空号提示音
     */
    public int promptType;

    public long addTime;

    public CallPromptPhone() {
    }

    @Ignore
    public CallPromptPhone(String phoneNumber, int promptType) {
        this.phoneNumber = phoneNumber;
        this.promptType = promptType;
        this.addTime = System.currentTimeMillis();
    }
}
