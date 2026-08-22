package com.example.data.repository

import com.example.data.dao.GlucoseDao
import com.example.data.dao.InsulinDao
import com.example.data.dao.ProfileDao
import com.example.data.dao.ReminderDao
import com.example.data.dao.CartridgeRefillLogDao
import com.example.data.dao.BloodPressureDao
import com.example.data.model.GlucoseReading
import com.example.data.model.InsulinRecord
import com.example.data.model.Reminder
import com.example.data.model.UserProfile
import com.example.data.model.CartridgeRefillLog
import com.example.data.model.BloodPressureRecord
import com.example.data.model.StepCountRecord
import com.example.data.dao.StepDao
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val insulinDao: InsulinDao,
    private val glucoseDao: GlucoseDao,
    private val reminderDao: ReminderDao,
    private val profileDao: ProfileDao,
    private val cartridgeRefillLogDao: CartridgeRefillLogDao,
    private val bloodPressureDao: BloodPressureDao,
    private val stepDao: StepDao
) {
    // Insulin Doses
    fun allInsulinRecords(profileId: Int): Flow<List<InsulinRecord>> = insulinDao.getAllInsulinRecords(profileId)

    suspend fun insertInsulinRecord(record: InsulinRecord) {
        insulinDao.insertInsulinRecord(record)
    }

    suspend fun deleteInsulinRecord(record: InsulinRecord) {
        insulinDao.deleteInsulinRecord(record)
    }

    suspend fun deleteInsulinRecordById(id: Long) {
        insulinDao.deleteInsulinRecordById(id)
    }

    suspend fun clearAllInsulinRecords(profileId: Int) {
        insulinDao.clearAllInsulinRecords(profileId)
    }

    // Glucose Readings
    fun allGlucoseReadings(profileId: Int): Flow<List<GlucoseReading>> = glucoseDao.getAllGlucoseReadings(profileId)

    suspend fun insertGlucoseReading(reading: GlucoseReading) {
        glucoseDao.insertGlucoseReading(reading)
    }

    suspend fun deleteGlucoseReading(reading: GlucoseReading) {
        glucoseDao.deleteGlucoseReading(reading)
    }

    suspend fun deleteGlucoseReadingById(id: Long) {
        glucoseDao.deleteGlucoseReadingById(id)
    }

    suspend fun clearAllGlucoseReadings(profileId: Int) {
        glucoseDao.clearAllGlucoseReadings(profileId)
    }

    // Reminders
    fun allReminders(profileId: Int): Flow<List<Reminder>> = reminderDao.getAllReminders(profileId)

    suspend fun insertReminder(reminder: Reminder) {
        reminderDao.insertReminder(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        reminderDao.deleteReminder(reminder)
    }

    suspend fun deleteReminderById(id: Long) {
        reminderDao.deleteReminderById(id)
    }

    suspend fun updateReminderStatus(id: Long, isEnabled: Boolean) {
        reminderDao.updateReminderStatus(id, isEnabled)
    }

    // Profile Settings
    val userProfile: Flow<UserProfile?> = profileDao.getProfile()
    val allProfiles: Flow<List<UserProfile>> = profileDao.getAllProfiles()

    suspend fun getProfileSync(): UserProfile? {
        return profileDao.getProfileSync()
    }

    suspend fun getAnyProfileSync(): UserProfile? {
        return profileDao.getAnyProfileSync()
    }

    suspend fun getProfileByUsername(userName: String): UserProfile? {
        return profileDao.getProfileByUsername(userName)
    }

    suspend fun insertOrUpdateProfile(profile: UserProfile) {
        profileDao.insertOrUpdateProfile(profile)
    }

    suspend fun deleteProfile(profile: UserProfile) {
        profileDao.deleteProfile(profile)
    }

    suspend fun selectProfile(id: Int) {
        profileDao.deactivateAll()
        profileDao.activateProfile(id)
    }

    // Cartridge Refill Logs
    fun allRefillLogs(profileId: Int): Flow<List<CartridgeRefillLog>> = cartridgeRefillLogDao.getAllRefillLogs(profileId)

    suspend fun insertRefillLog(log: CartridgeRefillLog) {
        cartridgeRefillLogDao.insertRefillLog(log)
    }

    suspend fun deleteRefillLog(log: CartridgeRefillLog) {
        cartridgeRefillLogDao.deleteRefillLog(log)
    }

    suspend fun deleteRefillLogById(id: Long) {
        cartridgeRefillLogDao.deleteRefillLogById(id)
    }

    suspend fun clearAllRefillLogs(profileId: Int) {
        cartridgeRefillLogDao.clearAllRefillLogs(profileId)
    }

    // Blood Pressure Records
    fun allBloodPressureRecords(profileId: Int): Flow<List<BloodPressureRecord>> = bloodPressureDao.getAllBloodPressureRecords(profileId)

    suspend fun insertBloodPressureRecord(record: BloodPressureRecord) {
        bloodPressureDao.insertBloodPressureRecord(record)
    }

    suspend fun deleteBloodPressureRecord(record: BloodPressureRecord) {
        bloodPressureDao.deleteBloodPressureRecord(record)
    }

    suspend fun deleteBloodPressureRecordById(id: Long) {
        bloodPressureDao.deleteBloodPressureRecordById(id)
    }

    suspend fun clearAllBloodPressureRecords(profileId: Int) {
        bloodPressureDao.clearAllBloodPressureRecords(profileId)
    }

    suspend fun clearAllReminders(profileId: Int) {
        reminderDao.clearAllReminders(profileId)
    }

    suspend fun clearAllProfiles() {
        profileDao.clearAllProfiles()
    }

    suspend fun getAnyReminderSync(profileId: Int): Reminder? {
        return reminderDao.getAnyReminderSync(profileId)
    }

    // Step Counts
    fun allStepRecords(profileId: Int): Flow<List<StepCountRecord>> = stepDao.getAllStepRecords(profileId)

    suspend fun insertStepRecord(record: StepCountRecord) {
        stepDao.insertStepRecord(record)
    }

    suspend fun deleteStepRecord(record: StepCountRecord) {
        stepDao.deleteStepRecord(record)
    }

    suspend fun deleteStepRecordById(id: Long) {
        stepDao.deleteStepRecordById(id)
    }

    suspend fun clearAllStepRecords(profileId: Int) {
        stepDao.clearAllStepRecords(profileId)
    }

    suspend fun getAnyStepRecordSync(profileId: Int): StepCountRecord? {
        return stepDao.getAnyStepRecordSync(profileId)
    }
}
