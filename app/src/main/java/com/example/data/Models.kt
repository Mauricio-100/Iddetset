package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import com.squareup.moshi.Json

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val username: String,
    val passwordHash: String, // We'll just mock this for now
    val avatarUrl: String? = null,
    val bio: String = "Welcome to my profile!",
    val privacySetting: String = "Public", // "Public" or "Private"
    val isVerified: Boolean = false,
    val followingCount: Int = 0,
    val followersCount: Int = 0,
    val isGiant: Boolean = false,
    val level: Int = 1,
    val xp: Int = 0,
    val badges: String = "", // Comma-separated list of badge names
    val email: String? = null,
    val phoneNumber: String? = null,
    val birthDate: String? = null,
    val zodiacSign: String? = null,
    val country: String? = null,
    val preferredCategory: String? = "@(fun)"
)

@Entity(tableName = "actfiles")
data class Actfile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val content: String,
    val tags: String = "",
    val likesCount: Int = 0,
    val viewsCount: Int = 0,
    val commentsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isLikedByMe: Boolean = false,
    val category: String? = null,
    val communityId: String? = null,
    val channelId: String? = null,
    val channelSlug: String? = null,
    val channelName: String? = null,
    // Attached Sound
    val soundId: String? = null,
    val soundTitle: String? = null,
    val soundAuthor: String? = null,
    val soundAudioUrl: String? = null,
    val soundCoverUrl: String? = null,
    // Attached Community info
    val communityName: String? = null,
    val communityIconUrl: String? = null,
    val communityIsVerified: Boolean = false,
    // IDDET Ads (Sponsored post)
    val isSponsored: Boolean = false,
    val adCampaignId: String? = null,
    val adStatus: String? = "none",
    val adBudget: Double? = null,
    val adCurrency: String? = null
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val receiverId: String,
    val content: String,
    val type: String = "text",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val reaction: String? = null,
    val replyToId: String? = null,
    val replyToContent: String? = null
)

@Entity(tableName = "follows")
data class Follow(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val followerId: String,
    val followingId: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ActfileWithUser(
    val id: String,
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val isVerified: Boolean,
    val content: String,
    val tags: String,
    val likesCount: Int,
    val viewsCount: Int,
    val commentsCount: Int,
    val createdAt: Long,
    val isLikedByMe: Boolean = false,
    val category: String? = null,
    val communityId: String? = null,
    val channelId: String? = null,
    val channelSlug: String? = null,
    val channelName: String? = null,
    // Attached Sound
    val soundId: String? = null,
    val soundTitle: String? = null,
    val soundAuthor: String? = null,
    val soundAudioUrl: String? = null,
    val soundCoverUrl: String? = null,
    // Attached Community info
    val communityName: String? = null,
    val communityIconUrl: String? = null,
    val communityIsVerified: Boolean = false,
    // IDDET Ads (Sponsored post)
    val isSponsored: Boolean = false,
    val adCampaignId: String? = null,
    val adStatus: String? = "none",
    val adBudget: Double? = null,
    val adCurrency: String? = null
)

data class AttachedSound(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val audioUrl: String = "",
    val coverUrl: String = ""
)

object ActfileMetadataHelper {
    private val SOUND_REGEX = Regex("<!--sound:\\{(.*?)\\}-->")
    private val COMM_REGEX = Regex("<!--community:\\{(.*?)\\}-->")

    private fun extractJsonValue(body: String, key: String): String {
        val r = Regex("\"$key\"\\s*:\\s*\"(.*?)\"")
        return r.find(body)?.groupValues?.get(1)?.replace("\\\"", "\"") ?: ""
    }

    fun parseSound(content: String): AttachedSound? {
        try {
            val match = SOUND_REGEX.find(content) ?: return null
            val body = match.groupValues[1]
            val title = extractJsonValue(body, "title")
            if (title.isBlank()) return null
            return AttachedSound(
                id = extractJsonValue(body, "id"),
                title = title,
                author = extractJsonValue(body, "author"),
                audioUrl = extractJsonValue(body, "url"),
                coverUrl = extractJsonValue(body, "cover")
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun parseCommunity(content: String): Pair<String, String>? {
        try {
            val match = COMM_REGEX.find(content) ?: return null
            val body = match.groupValues[1]
            val name = extractJsonValue(body, "name")
            if (name.isBlank()) return null
            return Pair(name, extractJsonValue(body, "icon"))
        } catch (e: Exception) {
            return null
        }
    }

    fun cleanContent(content: String): String {
        return content
            .replace(SOUND_REGEX, "")
            .replace(COMM_REGEX, "")
            .trimEnd()
    }
}

@Entity(tableName = "actfile_comments")
data class ActfileComment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val actfileId: String,
    val content: String,
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val isVerified: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val type: String, // "like", "comment", "follow", "message", "mention"
    val fromUserId: String,
    val fromUsername: String,
    val fromAvatar: String? = null,
    val message: String,
    val targetId: String? = null, // e.g., actfileId or messageId
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)




@Entity(tableName = "saved_accounts")
data class SavedAccount(
    @PrimaryKey val username: String,
    val avatarUrl: String?,
    val token: String,
    val isVerified: Boolean = false,
    val savedAt: Long = System.currentTimeMillis()
)

data class Community(
    val id: String = "",
    val slug: String = "",
    val name: String = "",
    val description: String? = null,
    @Json(name = "icon_url") val iconUrl: String? = null,
    @Json(name = "banner_url") val bannerUrl: String? = null,
    val category: String = "Général",
    @Json(name = "creator_id") val creatorId: String? = null,
    @Json(name = "is_private") val isPrivate: Boolean = false,
    @Json(name = "is_verified") val isVerified: Boolean = false,
    @Json(name = "members_count") val membersCount: Int = 1,
    @Json(name = "online_count") val onlineCount: Int? = null,
    @Json(name = "posts_count") val postsCount: Int = 0,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "is_member") val isMember: Boolean = true,
    @Json(name = "my_role") val myRole: String? = "admin"
)

/**
 * Calcule de manière cohérente, dynamique et réaliste le nombre de personnes en ligne.
 * Ne dépasse JAMAIS membersCount. Minimum 1 si la communauté a au moins 1 membre.
 */
fun Community.getComputedOnlineCount(nowMillis: Long = System.currentTimeMillis()): Int {
    if (membersCount <= 0) return 0
    if (membersCount == 1) return 1
    if (onlineCount != null && onlineCount > 0) {
        return onlineCount.coerceIn(1, membersCount)
    }
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val timeRatio = when (hour) {
        in 18..23 -> 0.18 // Soirée: forte affluence
        in 12..14 -> 0.14 // Pause déjeuner
        in 8..11, in 15..17 -> 0.10 // Heures actives de journée
        else -> 0.05 // Nuit
    }
    val window = nowMillis / (5 * 60 * 1000) // Fenêtre de 5 minutes
    val jitter = (Math.abs((slug.hashCode() xor window.hashCode())) % 5) - 2
    val rawCount = (membersCount * timeRatio).toInt() + jitter
    return rawCount.coerceIn(1, membersCount)
}

data class Channel(
    val id: String = "",
    @Json(name = "community_id") val communityId: String = "",
    val slug: String = "",
    val name: String = "",
    val description: String? = null,
    @Json(name = "is_default") val isDefault: Boolean = false,
    @Json(name = "created_at") val createdAt: String = ""
)

@Entity(tableName = "channel_messages")
data class ChannelMessage(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val channelId: String,
    val communitySlug: String,
    val senderId: String,
    val senderUsername: String,
    val senderAvatarUrl: String? = null,
    val isVerified: Boolean = false,
    val content: String,
    val type: String = "text", // "text", "voice", "image"
    val createdAt: Long = System.currentTimeMillis()
)

data class CommunityBot(
    val id: String = "",
    @Json(name = "community_id") val communityId: String? = null,
    val name: String = "",
    @Json(name = "banned_words") val bannedWords: List<String> = emptyList(),
    @Json(name = "auto_ban_threshold") val autoBanThreshold: Int = 0,
    @Json(name = "welcome_message") val welcomeMessage: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "created_at") val createdAt: String? = null,
    val token: String? = null
)

data class CommunityModAction(
    val id: String = "",
    @Json(name = "community_id") val communityId: String? = null,
    @Json(name = "action_type") val actionType: String = "", // delete_post, ban, mute, warn
    @Json(name = "actor_type") val actorType: String? = null, // bot, moderator, admin
    @Json(name = "actor_id") val actorId: String? = null,
    @Json(name = "target_user_id") val targetUserId: String? = null,
    @Json(name = "target_actfile_id") val targetActfileId: String? = null,
    val reason: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

data class Story(
    val id: String,
    @Json(name = "media_url") val mediaUrl: String,
    @Json(name = "media_type") val mediaType: String = "image", // "image", "video", "audio"
    val effect: String? = null,
    @Json(name = "created_at") val createdAt: String = "",
    val user: StoryUser,
    val isViewed: Boolean = false,
    val views: Int = 0,
    val reactions: Map<String, Int> = emptyMap()
)

data class StoryUser(
    val id: String,
    val username: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "is_verified") val isVerified: Boolean = false,
    val profession: String? = null
)

data class Tag(val name: String, val usesCount: Int)

sealed class MentionType {
    data class UserMention(val username: String) : MentionType()
    data class CommunityMention(val slug: String) : MentionType()
    data class ChannelMention(val slug: String) : MentionType()
}

fun getCategoryDefaultIcon(category: String): String {
    return when (category) {
        "Fun" -> "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=200&q=80"
        "Amour" -> "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=200&q=80"
        "Motivation" -> "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=200&q=80"
        "Tech" -> "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=200&q=80"
        "Sport" -> "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=200&q=80"
        "Musique" -> "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=200&q=80"
        "Actu" -> "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=200&q=80"
        "Business" -> "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=200&q=80"
        "Spiritualité" -> "https://images.unsplash.com/photo-1474552226712-ac0f0961a954?w=200&q=80"
        else -> "https://images.unsplash.com/photo-1511556532299-8f662fc26c06?w=200&q=80"
    }
}

fun getCategoryDefaultBanner(category: String): String {
    return when (category) {
        "Fun" -> "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=600&q=80"
        "Amour" -> "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=600&q=80"
        "Motivation" -> "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=600&q=80"
        "Tech" -> "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&q=80"
        "Sport" -> "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=600&q=80"
        "Musique" -> "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&q=80"
        "Actu" -> "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=600&q=80"
        "Business" -> "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=600&q=80"
        "Spiritualité" -> "https://images.unsplash.com/photo-1474552226712-ac0f0961a954?w=600&q=80"
        else -> "https://images.unsplash.com/photo-1511556532299-8f662fc26c06?w=600&q=80"
    }
}
