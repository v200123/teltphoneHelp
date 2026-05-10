package com.u2tzjtne.telephonehelper.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils;

import java.util.List;

/**
 * 特殊提示音号码访问对象
 */
@Dao
public interface CallPromptPhoneDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertInternal(CallPromptPhone phone);

    default long insert(CallPromptPhone phone) {
        if (phone != null) {
            phone.phoneNumber = PhoneNumberUtils.normalizePhoneNumber(phone.phoneNumber);
        }
        return insertInternal(phone);
    }

    @Delete
    void delete(CallPromptPhone phone);

    @Query("DELETE FROM CallPromptPhone WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM CallPromptPhone WHERE promptType = :promptType")
    void deleteAllByType(int promptType);

    @Query("SELECT * FROM CallPromptPhone WHERE promptType = :promptType ORDER BY addTime DESC")
    List<CallPromptPhone> getAllByType(int promptType);

    @Query("SELECT * FROM CallPromptPhone WHERE REPLACE(phoneNumber, ' ', '') = :phoneNumber LIMIT 1")
    CallPromptPhone getByPhoneNumberInternal(String phoneNumber);

    default CallPromptPhone getByPhoneNumber(String phoneNumber) {
        return getByPhoneNumberInternal(PhoneNumberUtils.normalizePhoneNumber(phoneNumber));
    }

    @Query("SELECT * FROM CallPromptPhone WHERE REPLACE(phoneNumber, ' ', '') = :phoneNumber AND promptType = :promptType LIMIT 1")
    CallPromptPhone getByPhoneNumberAndTypeInternal(String phoneNumber, int promptType);

    default CallPromptPhone getByPhoneNumberAndType(String phoneNumber, int promptType) {
        return getByPhoneNumberAndTypeInternal(PhoneNumberUtils.normalizePhoneNumber(phoneNumber), promptType);
    }

    @Query("SELECT COUNT(*) FROM CallPromptPhone WHERE promptType = :promptType")
    int getCountByType(int promptType);
}
