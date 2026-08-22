package com.suisui.reminder.data

class EventRepository(private val dao: EventDao) {
    fun observeAll() = dao.observeAll()
    suspend fun getAll(): List<EventEntity> = dao.getAll()
    suspend fun getById(id: Long): EventEntity? = dao.getById(id)
    suspend fun insert(event: EventEntity): Long = dao.insert(event)
    suspend fun update(event: EventEntity) = dao.update(event)
    suspend fun delete(event: EventEntity) = dao.delete(event)
}
