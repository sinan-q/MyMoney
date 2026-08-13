package com.sinxn.mymoney.core.data.repository

import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaceRepository @Inject constructor(
    private val moneyDao: MoneyDao
) {
    fun getPlaces(): Flow<List<PlaceEntity>> {
        return moneyDao.getPlaces()
    }

    suspend fun getPlaceById(id: String): PlaceEntity? {
        return moneyDao.getPlaceById(id)
    }

    fun getTransactionsForPlace(placeId: String): Flow<List<TransactionWithCategory>> {
        return moneyDao.getTransactionsForPlace(placeId)
    }

    suspend fun savePlace(
        id: String?,
        name: String,
        icon: String = "ic_place",
        address: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        isArchived: Boolean = false,
        tag: String? = null
    ): String {
        val now = System.currentTimeMillis()
        val placeId = id ?: UUID.randomUUID().toString()

        val place = PlaceEntity(
            id = placeId,
            name = name,
            icon = icon,
            address = address,
            latitude = latitude,
            longitude = longitude,
            isArchived = isArchived,
            isDeleted = false,
            lastEdit = now,
            tag = tag
        )

        moneyDao.insertPlace(place)
        return placeId
    }

    suspend fun updatePlaceArchived(placeId: String, isArchived: Boolean) {
        val now = System.currentTimeMillis()
        moneyDao.updatePlaceArchived(placeId, isArchived, now)
    }

    suspend fun deletePlace(placeId: String) {
        val now = System.currentTimeMillis()
        moneyDao.softDeletePlace(placeId, now)
    }
}
