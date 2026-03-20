package com.u2tzjtne.telephonehelper.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;

import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils;

import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;


/**
 * 自定义号码归属地数据访问对象
 * 
 * @author u2tzjtne
 */
@Dao
public interface CustomPhoneLocationDao {

    /**
     * 插入自定义归属地，如果已存在则替换
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertInternal(CustomPhoneLocation location);

    default Completable insert(CustomPhoneLocation location) {
        if (location != null) {
            location.phone = PhoneNumberUtils.normalizePhoneNumber(location.phone);
        }
        return insertInternal(location);
    }

    /**
     * 根据号码前缀查询归属地
     * @param phone 号码前缀（去掉后4位）
     */
    @Query("SELECT * FROM custom_phone_location WHERE REPLACE(phone, ' ', '') = :phone LIMIT 1")
    Single<CustomPhoneLocation> findByPhoneInternal(String phone);

    default Single<CustomPhoneLocation> findByPhone(String phone) {
        return findByPhoneInternal(PhoneNumberUtils.normalizePhoneNumber(phone));
    }


    /**
     * 查询所有自定义归属地
     */
    @Query("SELECT * FROM custom_phone_location ORDER BY create_time DESC")
    Single<List<CustomPhoneLocation>> getAll();

    /**
     * 删除自定义归属地
     */
    @Delete
    Completable delete(CustomPhoneLocation location);

    /**
     * 根据ID删除
     */
    @Query("DELETE FROM custom_phone_location WHERE id = :id")
    Completable deleteById(int id);

    /**
     * 根据号码删除
     */
    @Query("DELETE FROM custom_phone_location WHERE REPLACE(phone, ' ', '') = :phone")
    Completable deleteByPhoneInternal(String phone);

    default Completable deleteByPhone(String phone) {
        return deleteByPhoneInternal(PhoneNumberUtils.normalizePhoneNumber(phone));
    }


    /**
     * 清空所有自定义归属地
     */
    @Query("DELETE FROM custom_phone_location")
    Completable deleteAll();
}
