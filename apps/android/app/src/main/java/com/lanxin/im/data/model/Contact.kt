package com.lanxin.im.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val contactId: Long,
    val remark: String?,
    val tags: String?,
    val status: String, // normal, blocked
    val createdAt: Long,
    val updatedAt: Long
)

object ContactStatus {
    const val NORMAL = "normal"
    const val BLOCKED = "blocked"
}

