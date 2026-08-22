package com.suisui.reminder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MomentDao {
    @Query("SELECT * FROM moments ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MomentEntity>>

    @Query("SELECT * FROM moments ORDER BY createdAt DESC")
    suspend fun getAll(): List<MomentEntity>

    @Query("SELECT * FROM moments WHERE id = :id")
    suspend fun getById(id: Long): MomentEntity?

    @Insert
    suspend fun insert(moment: MomentEntity): Long

    @Delete
    suspend fun delete(moment: MomentEntity)
}
