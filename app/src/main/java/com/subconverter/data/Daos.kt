package com.subconverter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionSourceDao {
    @Query("SELECT * FROM subscription_sources ORDER BY id DESC")
    fun observeAll(): Flow<List<SubscriptionSourceEntity>>

    @Query("SELECT * FROM subscription_sources WHERE id = :id")
    suspend fun getById(id: Long): SubscriptionSourceEntity?

    @Query("SELECT * FROM subscription_sources WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<SubscriptionSourceEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(source: SubscriptionSourceEntity): Long

    @Update
    suspend fun update(source: SubscriptionSourceEntity)

    @Delete
    suspend fun delete(source: SubscriptionSourceEntity)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY isDefault DESC, id DESC")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates ORDER BY isDefault DESC, id DESC")
    suspend fun getAll(): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getById(id: Long): TemplateEntity?

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(template: TemplateEntity): Long

    @Update
    suspend fun update(template: TemplateEntity)

    @Delete
    suspend fun delete(template: TemplateEntity)
}

@Dao
interface OutputProfileDao {
    @Query("SELECT * FROM output_profiles ORDER BY id DESC")
    fun observeAll(): Flow<List<OutputProfileEntity>>

    @Query("SELECT * FROM output_profiles WHERE id = :id")
    suspend fun getById(id: Long): OutputProfileEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: OutputProfileEntity): Long

    @Update
    suspend fun update(profile: OutputProfileEntity)

    @Delete
    suspend fun delete(profile: OutputProfileEntity)

    @Query("UPDATE output_profiles SET fetchCount = fetchCount + 1 WHERE id = :id")
    suspend fun incrementFetchCount(id: Long)
}
