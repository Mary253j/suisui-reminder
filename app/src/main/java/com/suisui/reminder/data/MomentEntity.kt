package com.suisui.reminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "moments")
data class MomentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val note: String? = null,
    val imagePaths: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
