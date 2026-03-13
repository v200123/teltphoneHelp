package com.u2tzjtne.telephonehelper.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import static androidx.room.OnConflictStrategy.IGNORE;

/**
 * 彩铃视频数据访问对象
 */
@Dao
public interface RingVideoDao {

    /**
     * 获取全部彩铃视频
     */
    @Query("SELECT * FROM RingVideo ORDER BY isSelected DESC, createdAt DESC")
    List<RingVideo> getAllSync();

    /**
     * 根据 ID 获取彩铃视频
     */
    @Query("SELECT * FROM RingVideo WHERE id = :id LIMIT 1")
    RingVideo getByIdSync(int id);

    /**
     * 获取当前已保存的视频数量
     */
    @Query("SELECT COUNT(*) FROM RingVideo")
    int getCount();

    /**
     * 获取最新一条视频 ID
     */
    @Query("SELECT id FROM RingVideo ORDER BY createdAt DESC LIMIT 1")
    Integer getLatestIdSync();

    /**
     * 插入彩铃视频并返回主键
     */
    @Insert(onConflict = IGNORE)
    long insertAndGetId(RingVideo ringVideo);

    /**
     * 更新彩铃视频
     */
    @Update
    void updateSync(RingVideo ringVideo);

    /**
     * 删除彩铃视频
     */
    @Delete
    void deleteSync(RingVideo ringVideo);

    /**
     * 根据 ID 删除彩铃视频
     */
    @Query("DELETE FROM RingVideo WHERE id = :id")
    void deleteByIdSync(int id);

    /**
     * 清空当前使用中的标记
     */
    @Query("UPDATE RingVideo SET isSelected = 0")
    void clearSelectedSync();

    /**
     * 设置当前使用中的彩铃视频
     */
    @Query("UPDATE RingVideo SET isSelected = 1 WHERE id = :id")
    void setSelectedSync(int id);
}
