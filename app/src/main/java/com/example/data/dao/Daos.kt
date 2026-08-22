package com.example.data.dao

import androidx.room.*
import com.example.data.model.InsulinRecord
import com.example.data.model.GlucoseReading
import com.example.data.model.Reminder
import com.example.data.model.UserProfile
import com.example.data.model.CartridgeRefillLog
import com.example.data.model.BloodPressureRecord
import com.example.data.model.StepCountRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface InsulinDao {
    @Query("SELECT * FROM insulin_records WHERE profileId = :profileId ORDER BY dateTimeMillis DESC")
    fun getAllInsulinRecords(profileId: Int): Flow<List<InsulinRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsulinRecord(record: InsulinRecord)

    @Delete
    suspend fun deleteInsulinRecord(record: InsulinRecord)

    @Query("DELETE FROM insulin_records WHERE id = :id")
    suspend fun deleteInsulinRecordById(id: Long)

    @Query("DELETE FROM insulin_records WHERE profileId = :profileId")
    suspend fun clearAllInsulinRecords(profileId: Int)
}

@Dao
interface GlucoseDao {
    @Query("SELECT * FROM glucose_readings WHERE profileId = :profileId ORDER BY dateTimeMillis DESC")
    fun getAllGlucoseReadings(profileId: Int): Flow<List<GlucoseReading>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlucoseReading(reading: GlucoseReading)

    @Delete
    suspend fun deleteGlucoseReading(reading: GlucoseReading)

    @Query("DELETE FROM glucose_readings WHERE id = :id")
    suspend fun deleteGlucoseReadingById(id: Long)

    @Query("DELETE FROM glucose_readings WHERE profileId = :profileId")
    suspend fun clearAllGlucoseReadings(profileId: Int)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE profileId = :profileId ORDER BY hour, minute ASC")
    fun getAllReminders(profileId: Int): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("UPDATE reminders SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateReminderStatus(id: Long, isEnabled: Boolean)

    @Query("SELECT * FROM reminders WHERE profileId = :profileId LIMIT 1")
    suspend fun getAnyReminderSync(profileId: Int): Reminder?

    @Query("DELETE FROM reminders WHERE profileId = :profileId")
    suspend fun clearAllReminders(profileId: Int)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profiles ORDER BY id ASC")
    fun getAllProfiles(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles WHERE isActive = 1 LIMIT 1")
    fun getProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getProfileSync(): UserProfile?

    @Query("SELECT * FROM user_profiles LIMIT 1")
    suspend fun getAnyProfileSync(): UserProfile?

    @Query("SELECT * FROM user_profiles WHERE userName = :userName LIMIT 1")
    suspend fun getProfileByUsername(userName: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)

    @Delete
    suspend fun deleteProfile(profile: UserProfile)

    @Query("UPDATE user_profiles SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE user_profiles SET isActive = 1 WHERE id = :id")
    suspend fun activateProfile(id: Int)

    @Query("DELETE FROM user_profiles")
    suspend fun clearAllProfiles()
}

@Dao
interface CartridgeRefillLogDao {
    @Query("SELECT * FROM cartridge_refill_logs WHERE profileId = :profileId ORDER BY dateTimeMillis DESC")
    fun getAllRefillLogs(profileId: Int): Flow<List<CartridgeRefillLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefillLog(log: CartridgeRefillLog)

    @Delete
    suspend fun deleteRefillLog(log: CartridgeRefillLog)

    @Query("DELETE FROM cartridge_refill_logs WHERE id = :id")
    suspend fun deleteRefillLogById(id: Long)

    @Query("DELETE FROM cartridge_refill_logs WHERE profileId = :profileId")
    suspend fun clearAllRefillLogs(profileId: Int)
}

@Dao
interface BloodPressureDao {
    @Query("SELECT * FROM blood_pressure_records WHERE profileId = :profileId ORDER BY dateTimeMillis DESC")
    fun getAllBloodPressureRecords(profileId: Int): Flow<List<BloodPressureRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBloodPressureRecord(record: BloodPressureRecord)

    @Delete
    suspend fun deleteBloodPressureRecord(record: BloodPressureRecord)

    @Query("DELETE FROM blood_pressure_records WHERE id = :id")
    suspend fun deleteBloodPressureRecordById(id: Long)

    @Query("DELETE FROM blood_pressure_records WHERE profileId = :profileId")
    suspend fun clearAllBloodPressureRecords(profileId: Int)
}

@Dao
interface StepDao {
    @Query("SELECT * FROM step_count_records WHERE profileId = :profileId ORDER BY dateTimeMillis DESC")
    fun getAllStepRecords(profileId: Int): Flow<List<StepCountRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStepRecord(record: StepCountRecord)

    @Delete
    suspend fun deleteStepRecord(record: StepCountRecord)

    @Query("DELETE FROM step_count_records WHERE id = :id")
    suspend fun deleteStepRecordById(id: Long)

    @Query("DELETE FROM step_count_records WHERE profileId = :profileId")
    suspend fun clearAllStepRecords(profileId: Int)

    @Query("SELECT * FROM step_count_records WHERE profileId = :profileId LIMIT 1")
    suspend fun getAnyStepRecordSync(profileId: Int): StepCountRecord?
}
