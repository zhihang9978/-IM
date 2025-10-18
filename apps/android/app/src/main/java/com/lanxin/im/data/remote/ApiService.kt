package com.lanxin.im.data.remote

import com.lanxin.im.data.model.User
import com.lanxin.im.data.model.Message
import com.lanxin.im.data.model.Conversation
import com.lanxin.im.data.model.Contact
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API接口定义
 * 基于API_DOCUMENTATION.md
 */
interface ApiService {
    
    // ==================== 认证模块 ====================
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<UserResponse>
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>
    
    @POST("auth/refresh")
    suspend fun refreshToken(): ApiResponse<TokenResponse>
    
    @POST("auth/logout")
    suspend fun logout(): ApiResponse<Any?>
    
    // ==================== 用户模块 ====================
    
    @GET("users/me")
    suspend fun getCurrentUser(): ApiResponse<User>
    
    @PUT("users/me")
    suspend fun updateUserProfile(@Body request: UpdateUserRequest): ApiResponse<User>
    
    @PUT("users/me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ApiResponse<Any?>
    
    @GET("users/search")
    suspend fun searchUsers(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResponse<UserListResponse>
    
    // ==================== 联系人模块 ====================
    
    @GET("contacts")
    suspend fun getContacts(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): ApiResponse<ContactListResponse>
    
    @POST("contacts")
    suspend fun addContact(@Body request: AddContactRequest): ApiResponse<Contact>
    
    @DELETE("contacts/{id}")
    suspend fun deleteContact(@Path("id") id: Long): ApiResponse<Any?>
    
    @PUT("contacts/{id}/remark")
    suspend fun updateContactRemark(
        @Path("id") id: Long,
        @Body request: Map<String, String>
    ): ApiResponse<Any?>
    
    // ==================== 消息模块 ====================
    
    @GET("conversations")
    suspend fun getConversations(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResponse<ConversationListResponse>
    
    @GET("conversations/{id}/settings")
    suspend fun getConversationSettings(@Path("id") id: Long): ApiResponse<ConversationSettings>
    
    @PUT("conversations/{id}/settings")
    suspend fun updateConversationSettings(
        @Path("id") id: Long,
        @Body settings: Map<String, Boolean>
    ): ApiResponse<Any?>
    
    @GET("conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") conversationId: Long,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): ApiResponse<MessageListResponse>
    
    @GET("conversations/{id}/messages/history")
    suspend fun getHistoryMessages(
        @Path("id") conversationId: Long,
        @Query("before_message_id") beforeMessageId: Long,
        @Query("limit") limit: Int = 20
    ): ApiResponse<MessageListResponse>
    
    @POST("messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): ApiResponse<MessageResponse>
    
    @POST("messages/{id}/recall")
    suspend fun recallMessage(@Path("id") messageId: Long): ApiResponse<Any?>
    
    @POST("conversations/{id}/read")
    suspend fun markAsRead(@Path("id") conversationId: Long): ApiResponse<Any?>
    
    @GET("messages/search")
    suspend fun searchMessages(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResponse<MessageListResponse>
    
    // ==================== 文件上传模块 ====================
    
    @GET("files/upload-token")
    suspend fun getUploadToken(
        @Query("file_type") fileType: String,
        @Query("file_name") fileName: String
    ): ApiResponse<UploadTokenResponse>
    
    @POST("files/upload-callback")
    suspend fun uploadCallback(@Body request: UploadCallbackRequest): ApiResponse<Any?>
    
    // ==================== TRTC音视频模块 ====================
    
    @POST("trtc/user-sig")
    suspend fun getTRTCUserSig(@Body request: TRTCUserSigRequest): ApiResponse<TRTCUserSigResponse>
    
    @POST("trtc/call")
    suspend fun initiateCall(@Body request: InitiateCallRequest): ApiResponse<CallResponse>
    
    // ==================== 收藏模块 ====================
    
    @POST("messages/collect")
    suspend fun collectMessage(@Body request: Map<String, Long>): ApiResponse<Any?>
    
    @GET("favorites")
    suspend fun getFavorites(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResponse<FavoriteListResponse>
    
    @DELETE("favorites/{id}")
    suspend fun deleteFavorite(@Path("id") id: Long): ApiResponse<Any?>
    
    // ==================== 举报模块 ====================
    
    @POST("messages/report")
    suspend fun reportMessage(@Body request: Map<String, Any>): ApiResponse<Any?>
    
    @GET("reports")
    suspend fun getReports(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResponse<ReportListResponse>
    
    // ==================== 群组模块 ====================
    
    @POST("groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): ApiResponse<GroupResponse>
    
    @GET("groups/{id}")
    suspend fun getGroupInfo(@Path("id") groupId: Long): ApiResponse<GroupResponse>
    
    @GET("groups/{id}/members")
    suspend fun getGroupMembers(@Path("id") groupId: Long): ApiResponse<GroupMembersResponse>
    
    @POST("groups/{id}/members")
    suspend fun addGroupMembers(
        @Path("id") groupId: Long,
        @Body request: Map<String, List<Long>>
    ): ApiResponse<Any?>
    
    @DELETE("groups/{id}/members/{user_id}")
    suspend fun removeGroupMember(
        @Path("id") groupId: Long,
        @Path("user_id") userId: Long
    ): ApiResponse<Any?>
    
    @POST("groups/{id}/messages")
    suspend fun sendGroupMessage(
        @Path("id") groupId: Long,
        @Body request: SendMessageRequest
    ): ApiResponse<MessageResponse>
    
    @PUT("groups/{id}")
    suspend fun updateGroup(
        @Path("id") groupId: Long,
        @Body request: UpdateGroupRequest
    ): ApiResponse<Any?>
    
    @DELETE("groups/{id}")
    suspend fun disbandGroup(@Path("id") groupId: Long): ApiResponse<Any?>
    
    // ==================== 离线消息 ====================
    
    @GET("messages/offline")
    suspend fun getOfflineMessages(): ApiResponse<OfflineMessagesResponse>
}

// ==================== 请求数据类 ====================

data class RegisterRequest(
    val username: String,
    val password: String,
    val phone: String? = null,
    val email: String? = null
)

data class LoginRequest(
    val identifier: String,
    val password: String
)

data class UpdateUserRequest(
    val username: String? = null,
    val avatar: String? = null,
    val phone: String? = null,
    val email: String? = null
)

data class ChangePasswordRequest(
    val old_password: String,
    val new_password: String
)

data class AddContactRequest(
    val contact_id: Long,
    val remark: String? = null,
    val tags: String? = null
)

data class SendMessageRequest(
    val receiver_id: Long,
    val content: String,
    val type: String = "text", // text, image, voice, video, file
    val file_url: String? = null,
    val file_size: Long? = null,
    val duration: Int? = null
)

data class UploadCallbackRequest(
    val key: String,
    val url: String,
    val size: Long,
    val content_type: String
)

data class TRTCUserSigRequest(
    val room_id: String,
    val user_id: Long
)

data class InitiateCallRequest(
    val receiver_id: Long,
    val call_type: String // audio, video
)

// ==================== 响应数据类 ====================

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)

data class UserResponse(
    val user: User
)

data class LoginResponse(
    val token: String,
    val user: User
)

data class TokenResponse(
    val token: String
)

data class UserListResponse(
    val total: Int,
    val page: Int,
    val page_size: Int,
    val users: List<User>
)

data class ContactListResponse(
    val total: Int,
    val contacts: List<ContactItem>
)

data class ContactItem(
    val id: Long,
    val contact_id: Long,
    val user_id: Long,
    val user: User?,  // ⚠️ 改为可空，防止后端Preload失败导致崩溃
    val remark: String?,
    val tags: String?,
    val status: String,
    val created_at: Long
)

data class ConversationListResponse(
    val conversations: List<ConversationItem>
)

data class ConversationItem(
    val id: Long,
    val type: String,
    val user: User?,
    val last_message: Message?,
    val unread_count: Int,
    val updated_at: Long
)

data class MessageListResponse(
    val total: Int,
    val messages: List<Message>
)

data class MessageResponse(
    val message: Message
)

data class UploadTokenResponse(
    val token: String,
    val bucket: String,
    val region: String,
    val key: String,
    val expires_at: String
)

data class TRTCUserSigResponse(
    val sdk_app_id: Int,
    val user_sig: String,
    val room_id: String,
    val expires_at: String
)

data class CallResponse(
    val room_id: String,
    val call_type: String
)

data class FavoriteListResponse(
    val total: Int,
    val page: Int,
    val page_size: Int,
    val favorites: List<FavoriteItem>
)

data class FavoriteItem(
    val id: Long,
    val user_id: Long,
    val message_id: Long,
    val content: String,
    val type: String,
    val created_at: Long
)

data class ReportListResponse(
    val total: Int,
    val page: Int,
    val page_size: Int,
    val reports: List<ReportItem>
)

data class ReportItem(
    val id: Long,
    val reporter_id: Long,
    val message_id: Long,
    val reason: String,
    val status: String,
    val created_at: Long
)

data class ConversationSettings(
    val is_muted: Boolean,
    val is_top: Boolean,
    val is_starred: Boolean,
    val is_blocked: Boolean
)

// ==================== 群组请求数据类 ====================

data class CreateGroupRequest(
    val name: String,
    val avatar: String?,
    val member_ids: List<Long>
)

data class UpdateGroupRequest(
    val name: String?,
    val avatar: String?
)

// ==================== 群组响应数据类 ====================

data class GroupResponse(
    val group: Group
)

data class Group(
    val id: Long,
    val name: String,
    val avatar: String?,
    val owner_id: Long,
    val type: String,
    val member_count: Int,
    val status: String,
    val created_at: Long
)

data class GroupMembersResponse(
    val members: List<GroupMemberItem>
)

data class GroupMemberItem(
    val id: Long,
    val group_id: Long,
    val user_id: Long,
    val role: String,
    val nickname: String?,
    val joined_at: Long,
    val user: User?
)

data class OfflineMessagesResponse(
    val messages: List<Message>,
    val count: Int
)

