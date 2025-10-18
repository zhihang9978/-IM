package com.lanxin.im.data.cache

import android.content.Context
import com.lanxin.im.data.model.Message
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * OfflineMessageCache单元测试
 * 测试缓存策略和同步逻辑
 */
@ExperimentalCoroutinesApi
class OfflineMessageCacheTest {
    
    private lateinit var context: Context
    private lateinit var cache: OfflineMessageCache
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        cache = OfflineMessageCache(context)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `test memory cache LRU eviction`() {
        // Given - Create 101 messages (exceeds MAX_MEMORY_CACHE_SIZE of 100)
        val messages = (1..101).map { id ->
            Message(
                id = id.toLong(),
                conversationId = 1L,
                senderId = 1L,
                content = "Message $id",
                type = "text",
                createdAt = System.currentTimeMillis()
            )
        }
        
        // When - Cache all messages
        messages.forEach { cache.cacheToMemory(it) }
        
        // Then - First message should be evicted (LRU)
        assertNull("First message should be evicted", cache.getFromMemory(1L))
        assertNotNull("Last message should still be in cache", cache.getFromMemory(101L))
    }
    
    @Test
    fun `test pending sync queue - no duplicates`() {
        // Given
        val message = Message(
            id = 1L,
            conversationId = 1L,
            senderId = 1L,
            content = "Test",
            type = "text",
            createdAt = System.currentTimeMillis()
        )
        
        // When - Add same message twice
        cache.addToPendingSync(message)
        cache.addToPendingSync(message)
        
        // Then - Should only be added once
        val stats = cache.getCacheStats()
        assertEquals("Pending sync should have 1 message", 1, stats.pendingSyncCount)
    }
    
    @Test
    fun `test cache stats accuracy`() {
        // Given
        val message1 = Message(1L, 1L, 1L, "Test 1", "text", System.currentTimeMillis())
        val message2 = Message(2L, 1L, 1L, "Test 2", "text", System.currentTimeMillis())
        
        // When
        cache.cacheToMemory(message1)
        cache.cacheToMemory(message2)
        cache.addToPendingSync(message1)
        
        val stats = cache.getCacheStats()
        
        // Then
        assertEquals("Memory should have 2 messages", 2, stats.memoryMessageCount)
        assertEquals("Pending sync should have 1 message", 1, stats.pendingSyncCount)
    }
    
    @Test
    fun `test clear memory cache`() {
        // Given
        cache.cacheToMemory(Message(1L, 1L, 1L, "Test", "text", System.currentTimeMillis()))
        cache.cacheToMemory(Message(2L, 1L, 1L, "Test", "text", System.currentTimeMillis()))
        
        // When
        cache.clearMemoryCache()
        
        // Then
        val stats = cache.getCacheStats()
        assertEquals("Memory cache should be empty", 0, stats.memoryMessageCount)
    }
}
