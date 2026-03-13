package com.u2tzjtne.telephonehelper.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 录音记录表
 * 与 CallRecord 形成一对多关系，一次通话可以有多条录音
 *
 * @author u2tzjtne@gmail.com
 */
@Entity(
    indices = {
        @Index(value = {"id"}, unique = true),
        @Index(value = {"callRecordId"})
    },
    foreignKeys = {
        @ForeignKey(
            entity = CallRecord.class,
            parentColumns = "id",
            childColumns = "callRecordId",
            onDelete = ForeignKey.CASCADE // 通话记录删除时，关联的录音也删除
        )
    }
)
public class Recording {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    // 关联的通话记录ID
    public int callRecordId;
    
    // 录音文件路径
    public String filePath;
    
    // 录音开始时间
    public long startTime;
    
    // 录音结束时间
    public long endTime;
    
    // 录音时长（毫秒）
    public long duration;
    
    // 文件大小（字节）
    public long fileSize;
    
    // 录音格式（如 m4a, mp3, amr）
    public String format;
    
    // 创建时间
    public long createdAt;

    /**
     * 获取格式化的时长字符串
     */
    public String getFormattedDuration() {
        long durationSeconds = duration / 1000;
        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 获取格式化的文件大小
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        }
    }
}
