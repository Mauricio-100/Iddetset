package com.example.data


import retrofit2.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Json
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import okhttp3.MultipartBody
import okhttp3.RequestBody

data class RegisterRequest(val username: String, val password: String? = null)
data class UserResponse(val id: String?, val username: String, val email: String?, val avatar_url: String?, val created_at: String?)
data class IddetPlusStatusResponse(
    val is_iddet_plus: Boolean,
    val expires_at: String?,
    val credits: Int,
    val card_style: String,
    val monthly_price_usd: Double,
    val monthly_price_cdf: Double? = 2800.0,
    val monthly_prices: Map<String, Double>? = null,
    val monthly_credits: Int,
    val history: List<IddetPlusHistoryItem>
)

data class IddetPlusHistoryItem(
    val status: String,
    val started_at: String?,
    val expires_at: String?,
    val price_amount: Double?,
    val price_currency: String?,
    val payment_ref: String?
)

data class UserCardResponse(
    val user_id: String,
    val username: String,
    val avatar_url: String?,
    val bio: String?,
    val is_verified: Boolean,
    val card_style: String,
    val is_iddet_plus: Boolean,
    val credits: Int? = null,
    val level: LevelInfo? = null
)

data class LevelTableItem(
    val name: String = "",
    val min_score: Int = 0
)

data class LevelsTableResponse(
    val levels: List<LevelTableItem> = emptyList()
)

data class LevelInfo(
    val score: Int = 0,
    val level_index: Int = 0,
    val level_name: String = "Débutant",
    val next_level_name: String? = null,
    val next_level_score: Int? = null,
    val points_to_next: Int = 0,
    val progress: Double = 0.0,
    val is_max_level: Boolean = false
)



data class CardStyleUpdateRequest(val style: String)
data class CardStyleUpdateResponse(val card_style: String)

data class CreditsResponse(
    val credits: Int,
    val transactions: List<CreditTransaction>
)

data class CreditTransaction(
    val amount: Int,
    val reason: String,
    val reference: String?,
    val balance_after: Int,
    val created_at: String
)

data class PhoneRequest(
    val number: String,
    val country_code: String
)

data class ChariowWidgetConfig(
    val product_id: String = "prd_zs6iyq84",
    val store_domain: String = "xnycggrc.mychariow.market",
    val customer_email: String? = null
)

data class IddetPlusCheckoutRequest(
    val currency: String = "CDF"
)

data class IddetPlusCheckoutResponse(
    val reference: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val widget: ChariowWidgetConfig? = null,
    val checkout_url: String? = null,
    val url: String? = null,
    val payment_url: String? = null
)

data class IddetPlusCheckoutStatusResponse(
    val reference: String,
    val status: String, // "pending", "success", "failed"
    val amount: Double,
    val currency: String,
    val created_at: String,
    val confirmed_at: String? = null
)
data class TokenResponse(
    val access_token: String,
    val token_type: String = "bearer",
    val user_id: String? = null,
    val username: String? = null,
    val avatar_url: String? = null,
    val is_verified: Boolean = false
)

data class CategoriesResponse(val categories: List<String>)
data class PreferredCategoriesResponse(val preferred_categories: List<String>)
data class UpdateCategoriesRequest(val categories: List<String>)
data class UpdateCategoriesResponse(val status: String, val preferred_categories: List<String>)

data class ActfileNetwork(
    val id: String = "",
    val content: String = "",
    val likes_count: Int = 0,
    val views_count: Int = 0,
    val comments_count: Int? = 0,
    val created_at: String = "",
    val user_id: String = "",
    val username: String = "",
    val avatar_url: String? = null,
    val is_verified: Boolean? = false,
    val liked: Boolean? = false,
    val category: String? = null,
    val community_id: String? = null,
    val community_slug: String? = null,
    val community_name: String? = null,
    val community_icon_url: String? = null,
    val community_is_verified: Boolean? = false,
    val channel_id: String? = null,
    val channel_slug: String? = null,
    val channel_name: String? = null,
    val is_sponsored: Boolean? = false,
    val ad_campaign_id: String? = null,
    val ad_status: String? = null,
    val ad_budget: Double? = null,
    val ad_currency: String? = null
)

data class AdCampaignRequest(
    val budget_amount: Double,
    val currency: String = "USD",
    val daily: Boolean = true,
    val target_country: String? = null
)

data class AdCampaignResponse(
    val campaign_id: String? = null,
    val checkout_url: String? = null,
    val status: String? = null
)

data class AdStatusResponse(
    val campaign_id: String? = null,
    val ad_status: String? = null,
    val is_sponsored: Boolean = false,
    val ad_expires_at: String? = null
)

data class PublishActfileRequest(
    @field:Json(name = "content") val content: String,
    @field:Json(name = "category") val category: String = "Autres",
    @field:Json(name = "community_slug") val community_slug: String? = null,
    @field:Json(name = "channel_slug") val channel_slug: String? = null
)

data class UserProfileNetwork(
    val id: String? = null,
    val username: String,
    val avatar_url: String?,
    val bio: String?,
    val is_verified: Boolean = false,
    val followers_count: Int = 0,
    val following_count: Int = 0,
    val videos_count: Int = 0,
    val is_online: Boolean = false,
    val email: String? = null,
    val phone_number: String? = null,
    val phone: String? = null,
    val birth_date: String? = null,
    val birthdate: String? = null,
    val zodiac_sign: String? = null,
    val country: String? = null,
    val country_source: String? = null,
    val created_at: String? = null,
    val last_seen: String? = null,
    val is_iddet_plus: Boolean = false,
    val card_style: String? = null,
    val is_following: Boolean? = null,
    val likes_received: Int? = 0
)

val UserProfileNetwork.actualPhone: String? get() = phone_number ?: phone
val UserProfileNetwork.actualBirthDate: String? get() = birth_date ?: birthdate

data class FollowResponse(
    val following: Boolean = false,
    val message: String? = null,
    val followers_count: Int? = null
)

data class VerificationRequest(
    val birth_date: String
)

data class VerificationResponse(
    val verified: Boolean = false,
    val badge: Boolean = false,
    val reason: String? = null,
    val zodiac_sign: String? = null
)

data class SearchResult(
    val results: List<SearchResultItem>
)

data class SearchResultItem(
    val id: String,
    val username: String?,
    val avatar_url: String?,
    val bio: String?,
    val is_verified: Boolean?
)

data class MessageReactionGroup(
    val emoji: String,
    val count: Int = 1,
    @Json(name = "user_ids") val user_ids: List<String> = emptyList(),
    val users: List<String> = emptyList(),
    @Json(name = "has_reacted") val has_reacted: Boolean = false
)

data class MessageReactionsResponse(
    val message_id: String? = null,
    val reactions: List<MessageReactionGroup> = emptyList()
)

data class ConversationNetwork(
    val id: String? = null,
    val user_id: String = "",
    val username: String = "",
    val avatar_url: String? = null,
    val last_message: String? = null,
    val last_message_time: String? = null,
    val unread_count: Int? = 0,
    val is_online: Boolean = false,
    val is_verified: Boolean = false,
    @Json(name = "is_iddet_plus") val is_iddet_plus: Boolean = false,
    @Json(name = "card_style") val card_style: String? = null
)

data class MessageNetwork(
    val id: String = "",
    val content: String = "",
    val type: String = "text",
    val sender_id: String = "",
    val receiver_id: String = "",
    val read: Boolean = false,
    val created_at: String = "",
    val sender_username: String? = null,
    val sender_avatar: String? = null,
    val reaction: String? = null,
    val reactions: List<MessageReactionGroup>? = null
)

data class SendMessageRequest(
    val content: String,
    val receiver_id: String,
    val type: String = "text"
)

data class UploadResponse(
    val url: String,
    val public_id: String? = null,
    val secure_url: String? = null
)

data class ActfileCommentCreate(val content: String)

data class ActfileCommentResponse(
    val id: String,
    val content: String,
    val created_at: String,
    val user: CommentUser?
)

data class CommentUser(
    val id: String,
    val username: String,
    val avatar_url: String?
)

data class NotificationNetwork(
    val id: String,
    val type: String,
    val from_user_id: String,
    val from_username: String?,
    val from_avatar: String?,
    val message: String,
    val target_id: String?,
    val read: Boolean,
    val created_at: String
)

data class ActfileCommentListResponse(
    val id: String,
    val content: String,
    val created_at: String,
    val user_id: String,
    val username: String,
    val avatar_url: String?,
    val is_verified: Boolean
)

data class UpdateProfileRequest(
    val bio: String? = null,
    val avatar_url: String? = null,
    val username: String? = null,
    val preferred_category: String? = null,
    val phone_number: String? = null,
    val phone: String? = null,
    val privacy_setting: String? = null,
    val email: String? = null,
    val birth_date: String? = null,
    val birthdate: String? = null,
    val zodiac_sign: String? = null,
    val country: String? = null
)

data class MessageReactionRequest(
    val emoji: String
)

data class UserLevelResponse(
    val user_id: String? = null,
    val score: Int = 0,
    val level_index: Int = 0,
    val level_name: String = "Débutant",
    val next_level_name: String? = "Bronze",
    val next_level_score: Int? = 100,
    val points_to_next: Int = 100,
    val progress: Float = 0f,
    val is_max_level: Boolean = false
)



data class StoryResponse(
    val id: String,
    val media_url: String,
    val media_type: String = "image",
    val effect: String? = null,
    val created_at: String = "",
    val user: StoryUserResponse,
    val views: Int? = null,
    val view_count: Int? = null,
    val reactions: Map<String, Int>? = null
)

data class StoryUserResponse(
    val id: String,
    val username: String,
    val avatar_url: String? = null,
    val is_verified: Boolean? = false
)

data class CreateStoryResponse(
    val status: String? = null,
    val story_id: String? = null,
    val media_url: String? = null,
    val media_type: String? = null,
    val effect_applied: String? = null
)

interface ApiService {

    @retrofit2.http.GET("/api/users/me/iddet-plus")
    suspend fun getMyIddetPlusStatus(
        @retrofit2.http.Header("Authorization") token: String
    ): IddetPlusStatusResponse

    @retrofit2.http.GET("/api/users/me/card")
    suspend fun getMyCard(
        @retrofit2.http.Header("Authorization") token: String
    ): UserCardResponse

    @retrofit2.http.GET("/api/users/{user_id}/card")
    suspend fun getUserCard(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("user_id") userId: String
    ): UserCardResponse

    @retrofit2.http.PUT("/api/users/me/card/style")
    suspend fun updateCardStyle(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: CardStyleUpdateRequest
    ): CardStyleUpdateResponse

    @retrofit2.http.GET("/api/credits/me")
    suspend fun getMyCredits(
        @retrofit2.http.Header("Authorization") token: String
    ): CreditsResponse

    @retrofit2.http.POST("/api/iddet-plus/checkout")
    suspend fun createIddetPlusCheckout(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: IddetPlusCheckoutRequest
    ): IddetPlusCheckoutResponse

    @retrofit2.http.GET("/api/iddet-plus/checkout/{reference}/status")
    suspend fun getCheckoutStatus(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("reference") reference: String
    ): IddetPlusCheckoutStatusResponse
    @POST("/api/users/profile")
    suspend fun updateProfile(
        @retrofit2.http.Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): UserProfileNetwork

    @FormUrlEncoded
    @POST("/api/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String?
    ): TokenResponse

    @POST("/api/users/register")
    suspend fun signup(@Body request: RegisterRequest): UserResponse

    @retrofit2.http.GET("/api/actfile")
    suspend fun getActfiles(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Query("limit") limit: Int? = 900,
        @retrofit2.http.Query("cursor") cursor: String? = null,
        @retrofit2.http.Query("user_id") userId: String? = null
    ): List<ActfileNetwork>

    @retrofit2.http.GET("/api/actfiles")
    suspend fun getActfilesPlural(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Query("limit") limit: Int? = 900,
        @retrofit2.http.Query("cursor") cursor: String? = null,
        @retrofit2.http.Query("user_id") userId: String? = null
    ): List<ActfileNetwork>

    @POST("/api/actfile")
    suspend fun publishActfile(
        @retrofit2.http.Header("Authorization") token: String?,
        @Body request: PublishActfileRequest
    ): ActfileNetwork

    @retrofit2.http.DELETE("/api/actfile/{id}")
    suspend fun deleteActfile(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, Any>

    @POST("/api/actfile/{id}/ads")
    suspend fun createActfileAd(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String,
        @Body request: AdCampaignRequest
    ): AdCampaignResponse

    @retrofit2.http.GET("/api/actfile/{id}/ads")
    suspend fun getActfileAdStatus(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): AdStatusResponse

    @retrofit2.http.DELETE("/api/actfile/{id}/ads")
    suspend fun cancelActfileAd(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, Any>

    @POST("/api/actfile/{id}/like")
    suspend fun likeActfile(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, Boolean>

    @POST("/api/actfile/{id}/view")
    suspend fun viewActfile(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, String>

    @POST("/api/actfile/{id}/comment")
    suspend fun commentActfile(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String,
        @Body request: ActfileCommentCreate
    ): ActfileCommentResponse

    @retrofit2.http.GET("/api/actfile/{id}/comments")
    suspend fun getActfileComments(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): List<ActfileCommentListResponse>

    @retrofit2.http.GET("/api/users/me")
    suspend fun getMyProfile(
        @retrofit2.http.Header("Authorization") token: String?
    ): UserProfileNetwork

    @Multipart
    @PUT("/api/users/me")
    suspend fun updateProfileMultipart(
        @retrofit2.http.Header("Authorization") token: String,
        @Part avatar: MultipartBody.Part? = null,
        @Part("bio") bio: RequestBody? = null,
        @Part("phone_number") phoneNumber: RequestBody? = null
    ): UserProfileNetwork

    @retrofit2.http.GET("/api/users/{id}")
    suspend fun getUserProfile(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): UserProfileNetwork

    @FormUrlEncoded
    @PUT("/api/users/me")
    suspend fun updateProfileForm(
        @retrofit2.http.Header("Authorization") token: String,
        @Field("bio") bio: String? = null,
        @Field("phone_number") phoneNumber: String? = null
    ): UserProfileNetwork

    @POST("/api/users/me/verify")
    suspend fun requestVerification(
        @retrofit2.http.Header("Authorization") token: String,
        @Body request: VerificationRequest
    ): VerificationResponse

    @POST("/api/users/{id}/follow")
    suspend fun followUser(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): FollowResponse

    @POST("/api/users/{id}/unfollow")
    suspend fun unfollowUser(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): FollowResponse

    @DELETE("/api/users/{id}/follow")
    suspend fun unfollowUserDelete(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, @JvmSuppressWildcards Any>

    @PUT("/api/users/me")
    suspend fun updateMyProfilePut(
        @retrofit2.http.Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): UserProfileNetwork

    @retrofit2.http.GET("/api/search")
    suspend fun search(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Query("q") query: String,
        @retrofit2.http.Query("type") type: String = "users"
    ): SearchResult

    @retrofit2.http.GET("/api/messages/conversations")
    suspend fun getConversations(
        @retrofit2.http.Header("Authorization") token: String?
    ): List<ConversationNetwork>

    @retrofit2.http.GET("/api/messages/{id}")
    suspend fun getMessages(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") userId: String
    ): List<MessageNetwork>

    @POST("/api/messages/send")
    suspend fun sendMessage(
        @retrofit2.http.Header("Authorization") token: String?,
        @Body request: SendMessageRequest
    ): MessageNetwork

    @POST("/api/messages/read/{userId}")
    suspend fun markMessagesRead(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("userId") userId: String
    ): Map<String, Any>

    @retrofit2.http.DELETE("/api/messages/{id}")
    suspend fun deleteMessage(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, Any>

    @retrofit2.http.DELETE("/api/messages/conversation/{userId}")
    suspend fun deleteConversation(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("userId") userId: String
    ): Map<String, Any>

    @FormUrlEncoded
    @POST("/api/calls/start")
    suspend fun startCall(
        @retrofit2.http.Header("Authorization") token: String?,
        @Field("receiver_id") receiverId: String
    ): CallStartResponse

    @POST("/api/calls/{callId}/accept")
    suspend fun acceptCall(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("callId") callId: String
    ): CallStatusResponse

    @POST("/api/calls/{callId}/decline")
    suspend fun declineCall(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("callId") callId: String
    ): CallStatusResponse

    @POST("/api/calls/{callId}/end")
    suspend fun endCall(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("callId") callId: String
    ): CallEndResponse

    @GET("/api/calls/{callId}/status")
    suspend fun getCallStatus(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("callId") callId: String
    ): CallStatusDetailsResponse

    @GET("/api/calls/missed")
    suspend fun getMissedCalls(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Query("limit") limit: Int = 50
    ): MissedCallsResponse

    @POST("/api/messages/{id}/react")
    suspend fun reactToMessage(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String,
        @Body body: MessageReactionRequest
    ): Map<String, Any>

    @retrofit2.http.GET("/api/messages/{id}/reactions")
    suspend fun getMessageReactions(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): MessageReactionsResponse

    @retrofit2.http.DELETE("/api/messages/{id}/react")
    suspend fun removeMessageReaction(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, Any>

    @Multipart
    @POST("/api/upload")
    suspend fun uploadAudio(
        @retrofit2.http.Header("Authorization") token: String?,
        @Part file: okhttp3.MultipartBody.Part
    ): UploadResponse

    @retrofit2.http.GET("/api/notifications")
    suspend fun getNotifications(
        @retrofit2.http.Header("Authorization") token: String?
    ): List<NotificationNetwork>

    @retrofit2.http.GET("/api/categories")
    suspend fun getCategories(): CategoriesResponse

    @retrofit2.http.GET("/api/users/me/categories")
    suspend fun getMyPreferredCategories(
        @retrofit2.http.Header("Authorization") token: String
    ): PreferredCategoriesResponse

    @retrofit2.http.PUT("/api/users/me/categories")
    suspend fun updatePreferredCategories(
        @retrofit2.http.Header("Authorization") token: String,
        @Body body: UpdateCategoriesRequest
    ): UpdateCategoriesResponse

    @POST("/api/communities")
    suspend fun createCommunity(
        @retrofit2.http.Header("Authorization") token: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @retrofit2.http.GET("/api/communities")
    suspend fun searchCommunities(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Query("q") query: String?,
        @retrofit2.http.Query("category") category: String?,
        @retrofit2.http.Query("sort") sort: String = "popular",
        @retrofit2.http.Query("limit") limit: Int = 20
    ): List<Community>

    @retrofit2.http.GET("/api/communities/{slug}")
    suspend fun getCommunity(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("slug") slug: String
    ): Community

    @POST("/api/communities/{slug}/join")
    suspend fun joinCommunity(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String
    ): Map<String, @JvmSuppressWildcards Any>

    @retrofit2.http.GET("/api/communities/{slug}/posts")
    suspend fun getCommunityPosts(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Query("limit") limit: Int = 20,
        @retrofit2.http.Query("cursor") cursor: String? = null
    ): List<ActfileNetwork>

    @retrofit2.http.GET("/api/communities/{slug}/channels")
    suspend fun getCommunityChannels(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("slug") slug: String
    ): List<Channel>

    @retrofit2.http.POST("/api/communities/{slug}/channels")
    suspend fun createChannel(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Channel
    
    @retrofit2.http.GET("/api/users/me/communities")
    suspend fun getMyCommunities(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Query("role") role: String? = null
    ): List<Community>

    @retrofit2.http.PUT("/api/communities/{slug}")
    suspend fun updateCommunity(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Community

    @DELETE("/api/communities/{slug}")
    suspend fun deleteCommunity(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String
    ): Map<String, @JvmSuppressWildcards Any>

    @PUT("/api/communities/{slug}/members/{user_id}/role")
    suspend fun updateMemberRole(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Path("user_id") userId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Map<String, @JvmSuppressWildcards Any>

    @POST("/api/communities/{slug}/ban")
    suspend fun banCommunityMember(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Map<String, @JvmSuppressWildcards Any>

    @retrofit2.http.Multipart
    @retrofit2.http.PUT("/api/communities/{slug}/icon")
    suspend fun updateCommunityIcon(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Part icon: okhttp3.MultipartBody.Part
    ): Map<String, @JvmSuppressWildcards Any>

    @retrofit2.http.Multipart
    @retrofit2.http.POST("/api/communities/{slug}/icon")
    suspend fun updateCommunityIconPost(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Part icon: okhttp3.MultipartBody.Part
    ): Map<String, @JvmSuppressWildcards Any>

    @retrofit2.http.Multipart
    @retrofit2.http.PUT("/api/community/{slug}/icon")
    suspend fun updateCommunitySlugIconPut(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Part icon: okhttp3.MultipartBody.Part
    ): Map<String, @JvmSuppressWildcards Any>

    @retrofit2.http.Multipart
    @retrofit2.http.POST("/api/community/{slug}/icon")
    suspend fun updateCommunitySlugIconPost(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Part icon: okhttp3.MultipartBody.Part
    ): Map<String, @JvmSuppressWildcards Any>

    @retrofit2.http.Multipart
    @retrofit2.http.POST("/api/community/icon")
    suspend fun uploadCommunityIconDirectPost(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Part("slug") slug: okhttp3.RequestBody,
        @retrofit2.http.Part icon: okhttp3.MultipartBody.Part
    ): Map<String, @JvmSuppressWildcards Any>

    @retrofit2.http.Multipart
    @retrofit2.http.PUT("/api/community/icon")
    suspend fun uploadCommunityIconDirectPut(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Part("slug") slug: okhttp3.RequestBody,
        @retrofit2.http.Part icon: okhttp3.MultipartBody.Part
    ): Map<String, @JvmSuppressWildcards Any>

    @retrofit2.http.GET("/api/connected-apps")
    suspend fun getConnectedApps(
        @retrofit2.http.Header("Authorization") token: String
    ): List<ConnectedAppItem>

    @retrofit2.http.DELETE("/api/connected-apps/{app_id}")
    suspend fun removeConnectedApp(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("app_id") appId: String
    ): Map<String, @JvmSuppressWildcards Any>

    @retrofit2.http.GET("/api/communities/{slug}/bots")
    suspend fun getCommunityBots(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String
    ): Response<ResponseBody>

    @retrofit2.http.POST("/api/communities/{slug}/bots")
    suspend fun createCommunityBot(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @retrofit2.http.PUT("/api/communities/{slug}/bots/{bot_id}")
    suspend fun updateCommunityBot(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Path("bot_id") botId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @DELETE("/api/communities/{slug}/bots/{bot_id}")
    suspend fun deleteCommunityBot(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Path("bot_id") botId: String
    ): Response<ResponseBody>

    @retrofit2.http.POST("/api/communities/{slug}/bots/{bot_id}/actions")
    suspend fun performBotModAction(
        @retrofit2.http.Header("X-Bot-Token") botToken: String?,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Path("bot_id") botId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @retrofit2.http.GET("/api/communities/{slug}/mod-actions")
    suspend fun getCommunityModActions(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("slug") slug: String,
        @retrofit2.http.Query("limit") limit: Int = 50
    ): Response<ResponseBody>

    @retrofit2.http.GET("/api/stories")
    suspend fun getStories(
        @retrofit2.http.Header("Authorization") token: String?
    ): List<StoryResponse>

    @Multipart
    @POST("/api/stories")
    suspend fun createStory(
        @retrofit2.http.Header("Authorization") token: String?,
        @Part file: okhttp3.MultipartBody.Part,
        @Part("effect") effect: okhttp3.RequestBody? = null
    ): CreateStoryResponse

    @retrofit2.http.DELETE("/api/stories/{id}")
    suspend fun deleteStory(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): Map<String, Any>

    @retrofit2.http.GET("/api/users/me/level")
    suspend fun getMyLevel(
        @retrofit2.http.Header("Authorization") token: String?
    ): UserLevelResponse

    @retrofit2.http.GET("/api/users/{user_id}/level")
    suspend fun getUserLevel(
        @retrofit2.http.Path("user_id") userId: String
    ): UserLevelResponse

    @retrofit2.http.GET("/api/levels")
    suspend fun getLevelsTable(): LevelsTableResponse

    // ── STRIP SOUNDS & MUSIQUE ──
    @retrofit2.http.GET("/api/sounds/recommendations")
    suspend fun getRecommendedSounds(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Query("limit") limit: Int = 20
    ): List<SoundNetwork>

    @retrofit2.http.GET("/api/sounds/{id}")
    suspend fun getSoundDetails(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): SoundDetailsResponse

    @retrofit2.http.GET("/api/sounds/{id}/short")
    suspend fun getSoundShort(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): SoundShortResponse

    @retrofit2.http.POST("/api/sounds/{id}/play")
    suspend fun recordSoundPlay(
        @retrofit2.http.Path("id") id: String
    ): Map<String, Any>

    @retrofit2.http.POST("/api/sounds/{id}/like")
    suspend fun likeSound(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("id") id: String
    ): Map<String, Any>

    @retrofit2.http.GET("/api/sounds/{id}/likes/count")
    suspend fun getSoundLikesCount(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("id") id: String
    ): SoundLikesCountResponse

    @retrofit2.http.GET("/api/sounds/category/{category}")
    suspend fun getSoundsByCategory(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Path("category") category: String,
        @retrofit2.http.Query("limit") limit: Int = 20
    ): List<SoundNetwork>

    @retrofit2.http.GET("/api/users/{user_id}/sounds")
    suspend fun getUserSounds(
        @retrofit2.http.Path("user_id") userId: String
    ): List<SoundNetwork>

    @retrofit2.http.Multipart
    @retrofit2.http.POST("/api/sounds/upload")
    suspend fun uploadSound(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Part audio_file: MultipartBody.Part,
        @retrofit2.http.Part cover_file: MultipartBody.Part?,
        @retrofit2.http.Part("title") title: RequestBody,
        @retrofit2.http.Part("description") description: RequestBody,
        @retrofit2.http.Part("category") category: RequestBody
    ): SoundUploadResponse

    @retrofit2.http.POST("/api/sounds/{id}/comment")
    suspend fun commentSound(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: Map<String, String>
    ): Map<String, Any>

    @retrofit2.http.GET("/api/sounds/{id}/comments")
    suspend fun getSoundComments(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Query("limit") limit: Int = 50
    ): List<Map<String, Any>>

    // ── VIDÉOS & FEED RÉELS DU SERVEUR ──
    @retrofit2.http.GET("/api/feed")
    suspend fun getVideoFeed(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Query("cursor") cursor: String? = null,
        @retrofit2.http.Query("limit") limit: Int = 20
    ): List<VideoFeedItemNetwork>

    @retrofit2.http.GET("/api/feed/random")
    suspend fun getRandomVideos(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Query("limit") limit: Int = 20
    ): List<VideoFeedItemNetwork>

    @retrofit2.http.GET("/api/feed/next")
    suspend fun getNextVideos(
        @retrofit2.http.Header("Authorization") token: String?,
        @retrofit2.http.Query("cursor") cursor: String? = null,
        @retrofit2.http.Query("limit") limit: Int = 10
    ): List<VideoFeedItemNetwork>

    @retrofit2.http.POST("/api/videos/{id}/like")
    suspend fun likeVideo(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("id") id: String
    ): Map<String, Any>

    @retrofit2.http.POST("/api/videos/{id}/view")
    suspend fun recordVideoView(
        @retrofit2.http.Path("id") id: String
    ): Map<String, Any>

    @retrofit2.http.Multipart
    @retrofit2.http.POST("/api/videos/upload")
    suspend fun uploadVideo(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Part video: MultipartBody.Part,
        @retrofit2.http.Part("description") description: RequestBody,
        @retrofit2.http.Part("is_public") isPublic: RequestBody,
        @retrofit2.http.Part("has_original_sound") hasOriginalSound: RequestBody,
        @retrofit2.http.Part("sound_id") soundId: RequestBody? = null
    ): VideoUploadResponse

    // ── CLOUDFLARE LIVE STREAMING ──
    @retrofit2.http.POST("/api/live/create")
    suspend fun createCloudflareLive(
        @retrofit2.http.Header("Authorization") token: String
    ): LiveCreateResponse

    @retrofit2.http.GET("/api/live/{input_id}/status")
    suspend fun getCloudflareLiveStatus(
        @retrofit2.http.Path("input_id") inputId: String
    ): LiveStatusResponse

    @retrofit2.http.GET("/api/live/{input_id}/playback")
    suspend fun getCloudflareLivePlayback(
        @retrofit2.http.Path("input_id") inputId: String
    ): LivePlaybackResponse

    // ── GÉOLOCALISATION & SNAP MAP & EMBOUTEILLAGES ──
    @retrofit2.http.POST("/api/location")
    suspend fun updateLocation(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: LocationUpdateRequest
    ): LocationUpdateResponse

    @retrofit2.http.DELETE("/api/location")
    suspend fun stopLocationSharing(
        @retrofit2.http.Header("Authorization") token: String
    ): Map<String, @JvmSuppressWildcards Any>

    @retrofit2.http.GET("/api/friends/nearby")
    suspend fun getNearbyFriends(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Query("limit") limit: Int = 50
    ): List<NearbyFriendNetwork>

    @retrofit2.http.PUT("/api/users/me/country")
    suspend fun setMyCountry(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: CountryUpdateRequest
    ): Map<String, @JvmSuppressWildcards Any>

    @retrofit2.http.POST("/api/users/me/country/auto")
    suspend fun redetectMyCountryAuto(
        @retrofit2.http.Header("Authorization") token: String
    ): Map<String, @JvmSuppressWildcards Any>
}

data class CountryUpdateRequest(
    val country: String
)

data class LocationUpdateRequest(
    val latitude: Double,
    val longitude: Double,
    val is_sharing: Boolean? = true,
    val detect_country: Boolean = true
)

data class LocationUpdateResponse(
    val latitude: Double,
    val longitude: Double,
    val speed_kmh: Double = 0.0,
    val is_sharing: Boolean = true,
    val updated_at: String = ""
)

data class NearbyFriendNetwork(
    val user_id: String,
    val username: String,
    val avatar_url: String? = null,
    val latitude: Double,
    val longitude: Double,
    val distance_km: Double = 0.0,
    val last_update: String = ""
)

data class SoundNetwork(
    val id: String = "",
    val title: String = "",
    val description: String? = "",
    val audio_url: String? = null,
    val cover_url: String? = null,
    val category: String = "Autres",
    val duration: Double? = 0.0,
    val plays_count: Int = 0,
    val likes_count: Int = 0,
    val uses_count: Int = 0,
    val created_at: String? = null,
    val author_id: String? = null,
    val author_username: String? = null,
    val is_verified: Boolean? = false,
    val author_is_verified: Boolean? = false,
    val algo_score: Double? = null,
    val liked: Boolean = false
)

data class SoundDetailsResponse(
    val sound: SoundNetwork,
    val attribution_label: String? = null,
    val videos: List<Map<String, Any>> = emptyList()
)

data class SoundShortResponse(
    val sound_id: String,
    val name: String,
    val covers: String? = null,
    val ondes_des_sound: String? = null,
    val creator_is_verified: Boolean = false,
    val tags: String? = null,
    val mini_description: String? = null,
    val status: String? = null,
    val sound_ecoute_voila: String? = null
)

data class SoundLikesCountResponse(
    val sound_id: String,
    val likes_count: Int,
    val has_liked: Boolean
)

data class SoundUploadResponse(
    val status: String = "",
    val sound_id: String = ""
)

data class VideoFeedItemNetwork(
    val id: String = "",
    val video_url: String = "",
    val thumbnail_url: String? = null,
    val description: String? = "",
    val likes: Int = 0,
    val views: Int = 0,
    val duration: Double? = 0.0,
    val created_at: String? = null,
    val user_id: String = "",
    val username: String = "",
    val avatar_url: String? = null,
    val is_verified: Boolean = false,
    val liked: Boolean = false
)

data class VideoUploadResponse(
    val id: String = "",
    val video_url: String = "",
    val thumbnail_url: String? = null,
    val description: String? = "",
    val created_at: String? = null
)

data class LiveCreateResponse(
    val status: String,
    val live_id: String,
    val stream_url: String?,
    val stream_key: String?,
    val message: String?
)

data class LiveStatusResponse(
    val live_id: String,
    val is_live: Boolean,
    val details: Map<String, Any>? = null
)

data class LivePlaybackResponse(
    val live_id: String,
    val hls_url: String?,
    val dash_url: String?
)

data class ConnectedAppItem(
    val id: String,
    val user_id: String? = null,
    val url: String,
    val name: String? = null,
    val icon_url: String? = null,
    val first_connected_at: String? = null,
    val last_connected_at: String? = null
)

data class CallStartResponse(
    val call_id: String,
    val status: String,
    val receiver_username: String? = null,
    val receiver_avatar: String? = null
)

data class CallStatusResponse(
    val call_id: String,
    val status: String
)

data class CallEndResponse(
    val call_id: String,
    val status: String,
    val duration_seconds: Int = 0
)

data class CallStatusDetailsResponse(
    val call_id: String,
    val status: String,
    val accepted_at: String? = null,
    val ended_at: String? = null,
    val duration_seconds: Int = 0
)

data class MissedCallItem(
    val call_id: String,
    val caller_id: String,
    val caller_username: String,
    val caller_avatar: String? = null,
    val started_at: String? = null,
    val missed_at: String? = null
)

data class MissedCallsResponse(
    val missed_calls: List<MissedCallItem> = emptyList()
)

object RetrofitClient {
    private const val BASE_URL = "https://hoosthubs-g.onrender.com"

    val apiService: ApiService by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}

