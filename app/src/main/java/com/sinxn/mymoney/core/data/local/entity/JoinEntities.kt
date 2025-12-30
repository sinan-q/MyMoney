package com.sinxn.mymoney.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_people",
    primaryKeys = ["transactionId", "personId"],
    foreignKeys = [
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("transactionId"), Index("personId")]
)
data class TransactionPeopleEntity(
    val transactionId: String,
    val personId: String,
    val isDeleted: Boolean,
    val lastEdit: Long,
    val id: String // uuid
)

@Entity(
    tableName = "transfer_people",
    primaryKeys = ["transferId", "personId"],
    foreignKeys = [
        ForeignKey(entity = TransferEntity::class, parentColumns = ["id"], childColumns = ["transferId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("transferId"), Index("personId")]
)
data class TransferPeopleEntity(
    val transferId: String,
    val personId: String,
    val isDeleted: Boolean,
    val lastEdit: Long,
    val id: String // uuid
)

@Entity(
    tableName = "event_people",
    primaryKeys = ["eventId", "personId"],
    foreignKeys = [
        ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("eventId"), Index("personId")]
)
data class EventPeopleEntity(
    val eventId: String,
    val personId: String,
    val isDeleted: Boolean,
    val lastEdit: Long,
    val id: String // uuid
)

@Entity(
    tableName = "debt_people",
    primaryKeys = ["debtId", "personId"],
    foreignKeys = [
        ForeignKey(entity = DebtEntity::class, parentColumns = ["id"], childColumns = ["debtId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("debtId"), Index("personId")]
)
data class DebtPeopleEntity(
    val debtId: String,
    val personId: String,
    val isDeleted: Boolean,
    val lastEdit: Long,
    val id: String // uuid
)
