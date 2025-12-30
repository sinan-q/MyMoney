package com.sinxn.mymoney.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val file: String,
    val name: String,
    val type: String?,
    val size: Long,
    val tag: String?,
    val lastEdit: Long,
    val isDeleted: Boolean
)

@Entity(
    tableName = "transaction_attachment",
    primaryKeys = ["transactionId", "attachmentId"],
    foreignKeys = [
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = AttachmentEntity::class, parentColumns = ["id"], childColumns = ["attachmentId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("transactionId"), Index("attachmentId")]
)
data class TransactionAttachmentEntity(
    val transactionId: String,
    val attachmentId: String,
    val isDeleted: Boolean,
    val lastEdit: Long,
    val id: String // uuid
)

@Entity(
    tableName = "transfer_attachment",
    primaryKeys = ["transferId", "attachmentId"],
    foreignKeys = [
        ForeignKey(entity = TransferEntity::class, parentColumns = ["id"], childColumns = ["transferId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = AttachmentEntity::class, parentColumns = ["id"], childColumns = ["attachmentId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("transferId"), Index("attachmentId")]
)
data class TransferAttachmentEntity(
    val transferId: String,
    val attachmentId: String,
    val isDeleted: Boolean,
    val lastEdit: Long,
    val id: String // uuid
)
