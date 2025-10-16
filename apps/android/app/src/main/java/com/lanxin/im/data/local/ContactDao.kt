package com.lanxin.im.data.local

import androidx.room.*
import com.lanxin.im.data.model.Contact
import kotlinx.coroutines.flow.Flow

/**
 * 联系人数据访问对象
 */
@Dao
interface ContactDao {
    
    @Query("SELECT * FROM contacts WHERE status = 'normal' ORDER BY createdAt DESC")
    fun observeAllContacts(): Flow<List<Contact>>
    
    @Query("SELECT * FROM contacts WHERE status = 'normal' ORDER BY createdAt DESC")
    suspend fun getAllContacts(): List<Contact>
    
    @Query("SELECT * FROM contacts WHERE id = :contactId")
    suspend fun getContactById(contactId: Long): Contact?
    
    @Query("SELECT * FROM contacts WHERE contactId = :userId AND status = 'normal'")
    suspend fun getContactByUserId(userId: Long): Contact?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)
    
    @Update
    suspend fun updateContact(contact: Contact)
    
    @Delete
    suspend fun deleteContact(contact: Contact)
    
    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteContactById(contactId: Long)
    
    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()
}

