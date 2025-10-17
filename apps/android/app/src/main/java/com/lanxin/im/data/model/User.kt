package com.lanxin.im.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: Long,
    val username: String,
    val displayName: String? = null, // 显示名称（备注优先，否则用username）
    val phone: String?,
    val email: String?,
    val avatar: String?,
    val lanxinId: String,
    val role: String, // user, admin
    val status: String, // active, banned, deleted
    val lastLoginAt: Long?,
    val createdAt: Long
)

