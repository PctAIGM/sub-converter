package com.subconverter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionSourceDao {
    @Query("SELECT * FROM subscription_sources")
    suspend fun getAll(): List<SubscriptionSourceEntity>
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


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sources: List<SubscriptionSourceEntity>)

    @Query("DELETE FROM subscription_sources")
    suspend fun deleteAll()
}

@Dao
interface NodeDnsCacheDao {
    @Query("SELECT * FROM node_dns_cache")
    suspend fun getAll(): List<NodeDnsCacheEntity>
    @Query("SELECT * FROM node_dns_cache WHERE sourceId = :sourceId")
    suspend fun getBySourceId(sourceId: Long): List<NodeDnsCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<NodeDnsCacheEntity>)

    @Query("DELETE FROM node_dns_cache WHERE sourceId = :sourceId")
    suspend fun deleteBySourceId(sourceId: Long)


    @Query("DELETE FROM node_dns_cache")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceForSource(sourceId: Long, entries: List<NodeDnsCacheEntity>) {
        deleteBySourceId(sourceId)
        if (entries.isNotEmpty()) insertAll(entries)
    }
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getById(id: Long): TemplateEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM templates")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(template: TemplateEntity): Long

    @Update
    suspend fun update(template: TemplateEntity)

    @Delete
    suspend fun delete(template: TemplateEntity)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<TemplateEntity>)

    @Query("DELETE FROM templates")
    suspend fun deleteAll()
}

@Dao
interface OutputProfileDao {
    @Query("SELECT * FROM output_profiles ORDER BY id DESC")
    fun observeAll(): Flow<List<OutputProfileEntity>>

    @Query("SELECT * FROM output_profiles ORDER BY id DESC")
    suspend fun getAll(): List<OutputProfileEntity>

    @Query("SELECT * FROM output_profiles WHERE id = :id")
    suspend fun getById(id: Long): OutputProfileEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: OutputProfileEntity): Long

    @Update
    suspend fun update(profile: OutputProfileEntity)

    @Delete
    suspend fun delete(profile: OutputProfileEntity)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<OutputProfileEntity>)

    @Query("DELETE FROM output_profiles")
    suspend fun deleteAll()

    @Query("UPDATE output_profiles SET fetchCount = fetchCount + 1 WHERE id = :id")
    suspend fun incrementFetchCount(id: Long)
}
