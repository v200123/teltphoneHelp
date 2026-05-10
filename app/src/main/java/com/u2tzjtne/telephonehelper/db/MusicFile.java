package com.u2tzjtne.telephonehelper.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 音乐库文件记录
 * 用于保存拨打电话时可播放的音乐文件
 */
@Entity(
    indices = {
        @Index(value = {"id"}, unique = true),
        @Index(value = {"audioUri"}, unique = true),
        @Index(value = {"isSelected"})
    }
)
public class MusicFile {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String audioName;

    public String audioUri;

    public String mimeType;

    public long fileSize;

    public long duration;

    public long createdAt;

    public boolean isSelected;

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
