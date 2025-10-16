package com.lanxin.im.data.local

import androidx.room.*
import com.lanxin.im.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * 用户数据访问对象
 */
@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Long): User?
    
    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUserById(userId: Long): Flow<User?>
    
    @Query("SELECT * FROM users WHERE lanxinId = :lanxinId")
    suspend fun getUserByLanxinId(lanxinId: String): User?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Delete
    suspend fun deleteUser(user: User)
    
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Long)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}

