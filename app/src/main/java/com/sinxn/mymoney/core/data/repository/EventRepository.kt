package com.sinxn.mymoney.core.data.repository

import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val moneyDao: MoneyDao
) {
    fun getEvents(): Flow<List<EventEntity>> {
        return moneyDao.getEvents()
    }

    suspend fun getEventById(id: String): EventEntity? {
        return moneyDao.getEventById(id)
    }

    fun getTransactionsForEvent(eventId: String): Flow<List<TransactionWithCategory>> {
        return moneyDao.getTransactionsForEvent(eventId)
    }

    fun getAllEventTransactions(): Flow<List<TransactionWithCategory>> {
        return moneyDao.getAllEventTransactions()
    }

    suspend fun saveEvent(
        id: String?,
        name: String,
        icon: String = "ic_event",
        startDate: String,
        endDate: String,
        note: String? = null,
        isArchived: Boolean = false,
        tag: String? = null
    ): String {
        val now = System.currentTimeMillis()
        val eventId = id ?: UUID.randomUUID().toString()

        val event = EventEntity(
            id = eventId,
            name = name,
            icon = icon,
            note = note,
            startDate = startDate,
            endDate = endDate,
            isArchived = isArchived,
            isDeleted = false,
            lastEdit = now,
            tag = tag
        )

        moneyDao.insertEvent(event)
        return eventId
    }

    suspend fun updateEventArchived(eventId: String, isArchived: Boolean) {
        val now = System.currentTimeMillis()
        moneyDao.updateEventArchived(eventId, isArchived, now)
    }

    suspend fun deleteEvent(eventId: String) {
        val now = System.currentTimeMillis()
        moneyDao.softDeleteEvent(eventId, now)
    }
}
