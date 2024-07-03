package com.u2tzjtne.telephonehelper.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 通话记录表
 *
 * @author u2tzjtne@gmail.com
 */
@Entity(indices = @Index(value = {"id"}, unique = true))
public class CallRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String phoneNumber;//电话号码
    public String attribution;//归属地
    public String operator;//运营商
    public long startTime;//通话开始时间
    public long connectedTime;//接通时间
    public long endTime;//通话结束时间
    public boolean isConnected;//是否接通

    public int callNumber;//响铃次数

    public int callType = 0; //0为呼出 1为呼入
}
