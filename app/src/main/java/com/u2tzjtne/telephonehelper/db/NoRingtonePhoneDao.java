package com.u2tzjtne.telephonehelper.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils;

import java.util.List;



/**
 * 不显示彩铃号码数据访问对象
 */
@Dao
public interface NoRingtonePhoneDao {

    /**
     * 添加不显示彩铃的号码
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertInternal(NoRingtonePhone phone);

    default long insert(NoRingtonePhone phone) {
        if (phone != null) {
            phone.phoneNumber = PhoneNumberUtils.normalizePhoneNumber(phone.phoneNumber);
        }
        return insertInternal(phone);
    }

    /**
     * 删除记录
     */
    @Delete
    void delete(NoRingtonePhone phone);

    /**
     * 根据ID删除
     */
    @Query("DELETE FROM NoRingtonePhone WHERE id = :id")
    void deleteById(int id);

    /**
     * 根据号码删除
     */
    @Query("DELETE FROM NoRingtonePhone WHERE REPLACE(phoneNumber, ' ', '') = :phoneNumber")
    void deleteByPhoneNumberInternal(String phoneNumber);

    default void deleteByPhoneNumber(String phoneNumber) {
        deleteByPhoneNumberInternal(PhoneNumberUtils.normalizePhoneNumber(phoneNumber));
    }

    /**
     * 查询所有记录
     */
    @Query("SELECT * FROM NoRingtonePhone ORDER BY addTime DESC")
    List<NoRingtonePhone> getAll();

    /**
     * 根据号码查询
     */
    @Query("SELECT * FROM NoRingtonePhone WHERE REPLACE(phoneNumber, ' ', '') = :phoneNumber LIMIT 1")
    NoRingtonePhone getByPhoneNumberInternal(String phoneNumber);

    default NoRingtonePhone getByPhoneNumber(String phoneNumber) {
        return getByPhoneNumberInternal(PhoneNumberUtils.normalizePhoneNumber(phoneNumber));
    }

    /**
     * 检查号码是否在不显示彩铃列表中
     */
    @Query("SELECT COUNT(*) FROM NoRingtonePhone WHERE REPLACE(phoneNumber, ' ', '') = :phoneNumber")
    int isNoRingtonePhoneInternal(String phoneNumber);

    default boolean isNoRingtonePhone(String phoneNumber) {
        return isNoRingtonePhoneInternal(PhoneNumberUtils.normalizePhoneNumber(phoneNumber)) > 0;
    }

    /**
     * 清空所有记录
     */
    @Query("DELETE FROM NoRingtonePhone")
    void deleteAll();

    /**
     * 获取记录数量
     */
    @Query("SELECT COUNT(*) FROM NoRingtonePhone")
    int getCount();
}
