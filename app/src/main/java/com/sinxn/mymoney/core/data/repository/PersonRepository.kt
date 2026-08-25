package com.sinxn.mymoney.core.data.repository

import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.PersonEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonRepository @Inject constructor(
    private val moneyDao: MoneyDao
) {
    fun getPeople(): Flow<List<PersonEntity>> {
        return moneyDao.getPeople()
    }

    suspend fun getPersonById(id: String): PersonEntity? {
        return moneyDao.getPersonById(id)
    }

    fun getTransactionsForPerson(personId: String): Flow<List<TransactionWithCategory>> {
        return moneyDao.getTransactionsForPerson(personId)
    }

    suspend fun savePerson(
        id: String?,
        name: String,
        icon: String = "ic_person",
        note: String? = null,
        isArchived: Boolean = false,
        tag: String? = null
    ): String {
        val now = System.currentTimeMillis()
        val personId = id ?: UUID.randomUUID().toString()
        val existing = id?.let { moneyDao.getPersonById(it) }
        val lastUsed = existing?.lastUsed ?: now

        val person = PersonEntity(
            id = personId,
            name = name,
            icon = icon,
            note = note,
            isArchived = isArchived,
            isDeleted = false,
            lastEdit = now,
            lastUsed = lastUsed,
            tag = tag
        )

        moneyDao.insertPerson(person)
        return personId
    }

    suspend fun updatePersonArchived(personId: String, isArchived: Boolean) {
        val now = System.currentTimeMillis()
        moneyDao.updatePersonArchived(personId, isArchived, now)
    }

    suspend fun deletePerson(personId: String) {
        val now = System.currentTimeMillis()
        moneyDao.unlinkTransactionsForPerson(personId, now)
        moneyDao.unlinkTransfersForPerson(personId, now)
        moneyDao.unlinkDebtsForPerson(personId, now)
        moneyDao.unlinkEventsForPerson(personId, now)
        moneyDao.softDeletePerson(personId, now)
    }
}
