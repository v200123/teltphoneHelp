package com.u2tzjtne.telephonehelper.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils;

import java.util.List;

@Dao
public interface AutoHangUpRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertInternal(AutoHangUpRule rule);

    default long insert(AutoHangUpRule rule) {
        if (rule != null) {
            rule.phoneNumber = PhoneNumberUtils.normalizePhoneNumber(rule.phoneNumber);
        }
        return insertInternal(rule);
    }

    @Delete
    void delete(AutoHangUpRule rule);

    @Query("DELETE FROM AutoHangUpRule WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM AutoHangUpRule")
    void deleteAll();

    @Query("SELECT * FROM AutoHangUpRule ORDER BY addTime DESC")
    List<AutoHangUpRule> getAll();

    @Query("SELECT * FROM AutoHangUpRule WHERE REPLACE(phoneNumber, ' ', '') = :phoneNumber LIMIT 1")
    AutoHangUpRule getByPhoneNumberInternal(String phoneNumber);

    default AutoHangUpRule getByPhoneNumber(String phoneNumber) {
        return getByPhoneNumberInternal(PhoneNumberUtils.normalizePhoneNumber(phoneNumber));
    }
}
