package com.u2tzjtne.telephonehelper.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;

import static androidx.room.OnConflictStrategy.IGNORE;

/**
 * @author u2tzjtne@gmail.com
 */
@Dao
public interface CallRecordDao {
    /**
     * 分页加载 默认30条
     *
     * @param pos 末尾下标
     */
    @Query("select * from CallRecord order by id desc limit :pos,:pos+30")
    List<CallRecord> getAllForPage(int pos);

    /**
     * 获取全部数据
     */
    @Query("select * from CallRecord order by id desc")
    Maybe<List<CallRecord>> getAll();

    /**
     * 按组获取全部数据
     */
    @Query("select * from CallRecord where endTime in(select max(endTime) from CallRecord group by phoneNumber) order by startTime desc")
    Maybe<List<CallRecord>> getAllByGroup();

    /**
     * 根据ID获取
     */
    @Query("select * from CallRecord where id = :id")
    CallRecord getById(int id);

    /**
     * 根据号码查询
     */
    @Query("select * from CallRecord where phoneNumber = :number order by startTime desc limit 1")
    Maybe<CallRecord> getByNumber(String number);

    @Query("select * from CallRecord where phoneNumber = :number order by startTime desc")
    Maybe<List<CallRecord>> getByNumberMuti(String number);

    /**
     * 插入数据
     */
    @Insert(onConflict = IGNORE)
    Completable insert(CallRecord record);

    /**
     * 删除指定数据
     */
    @Delete
    Completable delete(CallRecord record);

    /**
     * 根据Id删除数据
     */
    @Query("delete from CallRecord where id like :id")
    void deleteById(int id);

    /**
     * 根据number删除数据
     */
    @Query("delete from CallRecord where phoneNumber like :number")
    Completable deleteByNumber(String number);

    /**
     * 删除所有数据
     */
    @Query("delete from CallRecord")
    void deleteAll();
}