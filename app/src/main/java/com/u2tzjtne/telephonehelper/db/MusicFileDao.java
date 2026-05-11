package com.u2tzjtne.telephonehelper.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import static androidx.room.OnConflictStrategy.IGNORE;

/**
 * 音乐库数据访问对象
 */
@Dao
public interface MusicFileDao {

    @Query("SELECT * FROM MusicFile ORDER BY isSelected DESC, createdAt DESC")
    List<MusicFile> getAllSync();

    @Query("SELECT * FROM MusicFile WHERE isSelected = 1 LIMIT 1")
    MusicFile getSelectedSync();

    @Query("SELECT * FROM MusicFile WHERE id = :id LIMIT 1")
    MusicFile getByIdSync(int id);

    @Query("SELECT COUNT(*) FROM MusicFile")
    int getCount();

    @Query("SELECT id FROM MusicFile ORDER BY createdAt DESC LIMIT 1")
    Integer getLatestIdSync();

    @Insert(onConflict = IGNORE)
    long insertAndGetId(MusicFile musicFile);

    @Update
    void updateSync(MusicFile musicFile);

    @Delete
    void deleteSync(MusicFile musicFile);

    @Query("DELETE FROM MusicFile WHERE id = :id")
    void deleteByIdSync(int id);

    @Query("UPDATE MusicFile SET isSelected = 0")
    void clearSelectedSync();

    @Query("UPDATE MusicFile SET isSelected = 1 WHERE id = :id")
    void setSelectedSync(int id);
}
