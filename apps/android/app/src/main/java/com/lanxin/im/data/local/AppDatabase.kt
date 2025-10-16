package com.lanxin.im.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lanxin.im.data.model.User
import com.lanxin.im.data.model.Message
import com.lanxin.im.data.model.Conversation
import com.lanxin.im.data.model.Contact

/**
 * Room数据库
 * 用于本地数据缓存和离线存储
 */
@Database(
    entities = [
        User::class,
        Message::class,
        Conversation::class,
        Contact::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun contactDao(): ContactDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lanxin_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

