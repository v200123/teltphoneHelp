package com.u2tzjtne.telephonehelper.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 彩铃视频记录表
 * 用于保存拨打电话时可用的彩铃视频位置
 */
@Entity(
    indices = {
        @Index(value = {"id"}, unique = true),
        @Index(value = {"videoUri"}, unique = true),
        @Index(value = {"isSelected"})
    }
)
public class RingVideo {
    @PrimaryKey(autoGenerate = true)
    public int id;

    // 视频名称
    public String videoName;

    // 视频 Uri / 路径
    public String videoUri;

    // 视频类型
    public String mimeType;

    // 文件大小（字节）
    public long fileSize;

    // 视频时长（毫秒）
    public long duration;

    // 创建时间
    public long createdAt;

    // 是否为当前使用中的彩铃视频
    public boolean isSelected;

    /**
     * 获取格式化后的文件大小
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else if (fileSize < 1024L * 1024L * 1024L) {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 获取格式化后的视频时长
     */
    public String getFormattedDuration() {
        long totalSeconds = duration / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }
}
