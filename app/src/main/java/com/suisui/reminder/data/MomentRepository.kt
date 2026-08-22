package com.suisui.reminder.data

class MomentRepository(private val dao: MomentDao) {
    fun observeAll() = dao.observeAll()
    suspend fun insert(moment: MomentEntity): Long = dao.insert(moment)
    suspend fun delete(moment: MomentEntity) = dao.delete(moment)
}
