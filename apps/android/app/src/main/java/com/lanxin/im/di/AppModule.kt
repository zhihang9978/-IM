package com.lanxin.im.di

import android.content.Context
import com.lanxin.im.data.local.AppDatabase
import com.lanxin.im.data.local.ConversationDao
import com.lanxin.im.data.local.MessageDao
import com.lanxin.im.data.remote.ApiService
import com.lanxin.im.data.remote.RetrofitClient
import com.lanxin.im.data.repository.ChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return RetrofitClient.apiService
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }
    
    @Provides
    @Singleton
    fun provideConversationDao(database: AppDatabase): ConversationDao {
        return database.conversationDao()
    }
    
    @Provides
    @Singleton
    fun provideChatRepository(
        apiService: ApiService,
        messageDao: MessageDao,
        conversationDao: ConversationDao
    ): ChatRepository {
        return ChatRepository(apiService, messageDao, conversationDao)
    }
}
