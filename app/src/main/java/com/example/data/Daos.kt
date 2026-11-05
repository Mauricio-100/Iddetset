package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :id OR username = :id LIMIT 1")
    suspend fun getUserById(id: String): User?
    
    @Query("SELECT * FROM users WHERE id = :id OR username = :id LIMIT 1")
    fun getUserByIdFlow(id: String): Flow<User?>

    @Query("SELECT * FROM users WHERE isGiant = 1 AND id != :currentUserId")
    fun getGiants(currentUserId: String): Flow<List<User>>

    @Query("""
        SELECT u.* FROM users u
        WHERE u.id != :currentUserId 
        AND u.id NOT IN (SELECT followingId FROM follows WHERE followerId = :currentUserId)
        ORDER BY 
            (u.followersCount * 5 + u.level) DESC
        LIMIT 10
    """)
    fun getSuggestedUsers(currentUserId: String): Flow<List<User>>

    @Query("SELECT * FROM users WHERE (username LIKE '%' || :query || '%' OR id LIKE '%' || :query || '%') AND id != :currentUserId")
    fun searchUsers(query: String, currentUserId: String): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Query("""
        SELECT users.* FROM users
        INNER JOIN (
            SELECT 
                CASE WHEN senderId = :userId THEN receiverId ELSE senderId END AS partnerId,
                MAX(createdAt) as lastMessageTime
            FROM messages
            WHERE senderId = :userId OR receiverId = :userId
            GROUP BY CASE WHEN senderId = :userId THEN receiverId ELSE senderId END
        ) AS last_msgs ON users.id = last_msgs.partnerId
        ORDER BY last_msgs.lastMessageTime DESC
    """)
    fun getChatPartners(userId: String): Flow<List<User>>

    @Query("DELETE FROM users WHERE id = :id OR username = :username")
    suspend fun deleteUser(id: String, username: String)
}

@Dao
interface ActfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActfile(actfile: Actfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActfiles(actfiles: List<Actfile>)

    @Query("""
        SELECT a.id, a.userId, COALESCE(u.username, 'Utilisateur') AS username, u.avatarUrl, COALESCE(u.isVerified, 0) AS isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category, a.communityId, a.channelId, a.channelSlug, a.channelName,
        a.soundId, a.soundTitle, a.soundAuthor, a.soundAudioUrl, a.soundCoverUrl, a.communityName, a.communityIconUrl, a.communityIsVerified, a.isSponsored, a.adCampaignId, a.adStatus, a.adBudget, a.adCurrency
        FROM actfiles a 
        LEFT JOIN users u ON (a.userId = u.id OR a.userId = u.username) 
        WHERE (a.content NOT LIKE '%@#%')
        ORDER BY a.createdAt DESC
    """)
    fun getAllActfilesWithUser(): Flow<List<ActfileWithUser>>

    @Query("""
        SELECT a.id, a.userId, COALESCE(u.username, 'Utilisateur') AS username, u.avatarUrl, COALESCE(u.isVerified, 0) AS isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category, a.communityId, a.channelId, a.channelSlug, a.channelName,
        a.soundId, a.soundTitle, a.soundAuthor, a.soundAudioUrl, a.soundCoverUrl, a.communityName, a.communityIconUrl, a.communityIsVerified, a.isSponsored, a.adCampaignId, a.adStatus, a.adBudget, a.adCurrency
        FROM actfiles a 
        LEFT JOIN users u ON (a.userId = u.id OR a.userId = u.username) 
        WHERE (a.userId = :userId OR u.username = :userId OR u.id = :userId) AND (a.content NOT LIKE '%@#%')
        ORDER BY a.createdAt DESC
    """)
    fun getActfilesByUser(userId: String): Flow<List<ActfileWithUser>>

    @Query("""
        SELECT a.id, a.userId, COALESCE(u.username, 'Utilisateur') AS username, u.avatarUrl, COALESCE(u.isVerified, 0) AS isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category, a.communityId, a.channelId, a.channelSlug, a.channelName,
        a.soundId, a.soundTitle, a.soundAuthor, a.soundAudioUrl, a.soundCoverUrl, a.communityName, a.communityIconUrl, a.communityIsVerified, a.isSponsored, a.adCampaignId, a.adStatus, a.adBudget, a.adCurrency
        FROM actfiles a 
        LEFT JOIN users u ON (a.userId = u.id OR a.userId = u.username) 
        WHERE (a.content LIKE '%' || :query || '%' OR a.tags LIKE '%' || :query || '%') AND (a.content NOT LIKE '%@#%')
        ORDER BY a.createdAt DESC
    """)
    fun searchActfiles(query: String): Flow<List<ActfileWithUser>>
    
    @Query("SELECT * FROM actfiles WHERE id = :id LIMIT 1")
    suspend fun getActfileRaw(id: String): Actfile?

    @Query("UPDATE actfiles SET viewsCount = viewsCount + 1 WHERE id = :id")
    suspend fun incrementViewCount(id: String)
    
    @Query("UPDATE actfiles SET likesCount = likesCount + 1 WHERE id = :id")
    suspend fun incrementLikeCount(id: String)

    @Query("UPDATE actfiles SET likesCount = MAX(0, likesCount - 1) WHERE id = :id")
    suspend fun decrementLikeCount(id: String)
    
    @Query("UPDATE actfiles SET commentsCount = commentsCount + 1 WHERE id = :id")
    suspend fun incrementCommentCount(id: String)

    @Query("UPDATE actfiles SET commentsCount = :count WHERE id = :id")
    suspend fun updateCommentCount(id: String, count: Int)

    @Query("""
        SELECT a.id, a.userId, COALESCE(u.username, 'Utilisateur') AS username, u.avatarUrl, COALESCE(u.isVerified, 0) AS isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category, a.communityId, a.channelId, a.channelSlug, a.channelName,
        a.soundId, a.soundTitle, a.soundAuthor, a.soundAudioUrl, a.soundCoverUrl, a.communityName, a.communityIconUrl, a.communityIsVerified, a.isSponsored, a.adCampaignId, a.adStatus, a.adBudget, a.adCurrency
        FROM actfiles a 
        LEFT JOIN users u ON (a.userId = u.id OR a.userId = u.username) 
        WHERE (a.communityId = :communityId OR a.communityId = :slug OR a.channelSlug = :slug OR a.content LIKE '%@c/' || :slug || '%') AND (a.content NOT LIKE '%@#%')
        ORDER BY a.createdAt DESC
    """)
    fun getCommunityActfiles(communityId: String, slug: String): Flow<List<ActfileWithUser>>

    @Query("""
        SELECT a.id, a.userId, COALESCE(u.username, 'Utilisateur') AS username, u.avatarUrl, COALESCE(u.isVerified, 0) AS isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category, a.communityId, a.channelId, a.channelSlug, a.channelName,
        a.soundId, a.soundTitle, a.soundAuthor, a.soundAudioUrl, a.soundCoverUrl, a.communityName, a.communityIconUrl, a.communityIsVerified, a.isSponsored, a.adCampaignId, a.adStatus, a.adBudget, a.adCurrency
        FROM actfiles a 
        LEFT JOIN users u ON (a.userId = u.id OR a.userId = u.username) 
        WHERE (a.communityId = :communityId OR a.communityId = :slug OR a.channelSlug = :slug OR a.content LIKE '%@c/' || :slug || '%') AND (a.content NOT LIKE '%@#%')
        ORDER BY a.createdAt DESC
    """)
    suspend fun getCommunityActfilesList(communityId: String, slug: String): List<ActfileWithUser>

    @Query("""
        SELECT a.id, a.userId, COALESCE(u.username, 'Utilisateur') AS username, u.avatarUrl, COALESCE(u.isVerified, 0) AS isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category, a.communityId, a.channelId, a.channelSlug, a.channelName,
        a.soundId, a.soundTitle, a.soundAuthor, a.soundAudioUrl, a.soundCoverUrl, a.communityName, a.communityIconUrl, a.communityIsVerified, a.isSponsored, a.adCampaignId, a.adStatus, a.adBudget, a.adCurrency
        FROM actfiles a 
        LEFT JOIN users u ON (a.userId = u.id OR a.userId = u.username) 
        WHERE a.id = :actfileId
        LIMIT 1
    """)
    fun getActfileById(actfileId: String): Flow<ActfileWithUser?>

    @Query("""
        SELECT a.id, a.userId, COALESCE(u.username, 'Utilisateur') AS username, u.avatarUrl, COALESCE(u.isVerified, 0) AS isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category, a.communityId, a.channelId, a.channelSlug, a.channelName,
        a.soundId, a.soundTitle, a.soundAuthor, a.soundAudioUrl, a.soundCoverUrl, a.communityName, a.communityIconUrl, a.communityIsVerified, a.isSponsored, a.adCampaignId, a.adStatus, a.adBudget, a.adCurrency
        FROM actfiles a 
        LEFT JOIN users u ON (a.userId = u.id OR a.userId = u.username) 
        WHERE a.isLikedByMe = 1 AND (a.content NOT LIKE '%@#%')
        ORDER BY a.createdAt DESC
    """)
    fun getLikedActfiles(): Flow<List<ActfileWithUser>>

    @Query("""
        SELECT DISTINCT a.id, a.userId, COALESCE(u.username, 'Utilisateur') AS username, u.avatarUrl, COALESCE(u.isVerified, 0) AS isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category, a.communityId, a.channelId, a.channelSlug, a.channelName,
        a.soundId, a.soundTitle, a.soundAuthor, a.soundAudioUrl, a.soundCoverUrl, a.communityName, a.communityIconUrl, a.communityIsVerified, a.isSponsored, a.adCampaignId, a.adStatus, a.adBudget, a.adCurrency
        FROM actfiles a 
        LEFT JOIN users u ON (a.userId = u.id OR a.userId = u.username) 
        INNER JOIN actfile_comments c ON a.id = c.actfileId
        WHERE (c.userId = :userId OR u.username = :userId) AND (a.content NOT LIKE '%@#%')
        ORDER BY a.createdAt DESC
    """)
    fun getCommentedActfiles(userId: String): Flow<List<ActfileWithUser>>

    @Query("DELETE FROM actfiles WHERE id = :id")
    suspend fun deleteActfileLocal(id: String)

    @Query("DELETE FROM actfiles")
    suspend fun deleteAllActfiles()
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): Message?

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM messages WHERE (senderId = :user1 AND receiverId = :user2) OR (senderId = :user2 AND receiverId = :user1)")
    suspend fun deleteConversation(user1: String, user2: String)

    @Query("UPDATE messages SET reaction = :reaction WHERE id = :id")
    suspend fun updateReaction(id: String, reaction: String?)

    @Query("SELECT * FROM messages WHERE (senderId = :user1 AND receiverId = :user2) OR (senderId = :user2 AND receiverId = :user1) ORDER BY createdAt ASC")
    fun getMessagesBetween(user1: String, user2: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE senderId = :partnerId OR receiverId = :partnerId ORDER BY createdAt ASC")
    fun getMessagesWithPartner(partnerId: String): Flow<List<Message>>

    @Query("DELETE FROM messages WHERE id LIKE 'conv_%'")
    suspend fun deletePlaceholderConvMessages()
}

@Dao
interface FollowDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: Follow)

    @Query("""
        DELETE FROM follows 
        WHERE (followerId = :followerId OR followerId IN (SELECT id FROM users WHERE username = :followerId) OR followerId IN (SELECT username FROM users WHERE id = :followerId))
          AND (followingId = :followingId OR followingId IN (SELECT id FROM users WHERE username = :followingId) OR followingId IN (SELECT username FROM users WHERE id = :followingId))
    """)
    suspend fun deleteFollow(followerId: String, followingId: String)

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM follows f
            WHERE (f.followerId = :followerId OR f.followerId IN (SELECT id FROM users WHERE username = :followerId) OR f.followerId IN (SELECT username FROM users WHERE id = :followerId))
              AND (f.followingId = :followingId OR f.followingId IN (SELECT id FROM users WHERE username = :followingId) OR f.followingId IN (SELECT username FROM users WHERE id = :followingId))
        )
    """)
    fun isFollowingFlow(followerId: String, followingId: String): Flow<Boolean>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM follows f
            WHERE (f.followerId = :followerId OR f.followerId IN (SELECT id FROM users WHERE username = :followerId) OR f.followerId IN (SELECT username FROM users WHERE id = :followerId))
              AND (f.followingId = :followingId OR f.followingId IN (SELECT id FROM users WHERE username = :followingId) OR f.followingId IN (SELECT username FROM users WHERE id = :followingId))
        )
    """)
    suspend fun isFollowing(followerId: String, followingId: String): Boolean

    @Query("""
        SELECT followingId FROM follows 
        WHERE followerId = :followerId OR followerId IN (SELECT id FROM users WHERE username = :followerId) OR followerId IN (SELECT username FROM users WHERE id = :followerId)
    """)
    fun getFollowingIdsFlow(followerId: String): Flow<List<String>>

    @Query("""
        SELECT a.id, a.userId, COALESCE(u.username, 'Utilisateur') AS username, u.avatarUrl, COALESCE(u.isVerified, 0) AS isVerified, a.content, a.tags, a.likesCount, a.viewsCount, a.commentsCount, a.createdAt, a.isLikedByMe AS isLikedByMe, a.category, a.communityId, a.channelId, a.channelSlug, a.channelName,
        a.soundId, a.soundTitle, a.soundAuthor, a.soundAudioUrl, a.soundCoverUrl, a.communityName, a.communityIconUrl, a.communityIsVerified, a.isSponsored, a.adCampaignId, a.adStatus, a.adBudget, a.adCurrency
        FROM actfiles a 
        LEFT JOIN users u ON (a.userId = u.id OR a.userId = u.username)
        WHERE (
            a.userId = :followerId 
            OR a.userId IN (SELECT username FROM users WHERE id = :followerId)
            OR a.userId IN (SELECT id FROM users WHERE username = :followerId)
            OR a.userId IN (SELECT followingId FROM follows WHERE followerId = :followerId OR followerId IN (SELECT id FROM users WHERE username = :followerId) OR followerId IN (SELECT username FROM users WHERE id = :followerId))
            OR a.userId IN (
                SELECT u2.id FROM users u2 
                INNER JOIN follows f ON (f.followingId = u2.id OR f.followingId = u2.username) 
                WHERE f.followerId = :followerId OR f.followerId IN (SELECT id FROM users WHERE username = :followerId) OR f.followerId IN (SELECT username FROM users WHERE id = :followerId)
            )
            OR a.userId IN (
                SELECT u3.username FROM users u3 
                INNER JOIN follows f2 ON (f2.followingId = u3.id OR f2.followingId = u3.username) 
                WHERE f2.followerId = :followerId OR f2.followerId IN (SELECT id FROM users WHERE username = :followerId) OR f2.followerId IN (SELECT username FROM users WHERE id = :followerId)
            )
        )
        AND (a.channelId IS NULL OR a.channelId = '')
        AND (a.content NOT LIKE '%@#%')
        ORDER BY a.createdAt DESC
    """)
    fun getFollowedActfiles(followerId: String): Flow<List<ActfileWithUser>>

    @Query("""
        SELECT u.* FROM users u
        INNER JOIN follows f ON (f.followingId = u.id OR f.followingId = u.username)
        WHERE f.followerId = :userId OR f.followerId IN (SELECT id FROM users WHERE username = :userId) OR f.followerId IN (SELECT username FROM users WHERE id = :userId)
        GROUP BY u.id
    """)
    fun getFollowedUsersFlow(userId: String): Flow<List<User>>

    @Query("""
        SELECT u.* FROM users u
        INNER JOIN follows f ON (f.followerId = u.id OR f.followerId = u.username)
        WHERE f.followingId = :userId OR f.followingId IN (SELECT id FROM users WHERE username = :userId) OR f.followingId IN (SELECT username FROM users WHERE id = :userId)
        GROUP BY u.id
    """)
    fun getFollowerUsersFlow(userId: String): Flow<List<User>>
}

@Dao
interface CommentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<ActfileComment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: ActfileComment)

    @Query("SELECT * FROM actfile_comments WHERE actfileId = :actfileId ORDER BY createdAt ASC")
    fun getCommentsForActfile(actfileId: String): Flow<List<ActfileComment>>
    
    @Query("DELETE FROM actfile_comments WHERE actfileId = :actfileId")
    suspend fun deleteCommentsForActfile(actfileId: String)
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<Notification>)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotificationsForUser(userId: String): Flow<List<Notification>>

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)
}

@Dao
interface SavedAccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedAccount(account: SavedAccount)

    @Query("SELECT * FROM saved_accounts WHERE token = :token LIMIT 1")
    suspend fun getAccountByToken(token: String): SavedAccount?

    @Query("SELECT * FROM saved_accounts ORDER BY savedAt DESC")
    fun getAllSavedAccounts(): Flow<List<SavedAccount>>

    @Query("DELETE FROM saved_accounts WHERE username = :username")
    suspend fun deleteSavedAccount(username: String)

    @Query("DELETE FROM saved_accounts")
    suspend fun clearSavedAccounts()
}

@Dao
interface ChannelMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChannelMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChannelMessage>)

    @Query("SELECT * FROM channel_messages WHERE channelId = :channelId ORDER BY createdAt ASC")
    fun getMessagesForChannel(channelId: String): Flow<List<ChannelMessage>>

    @Query("DELETE FROM channel_messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("SELECT COUNT(*) FROM channel_messages WHERE channelId = :channelId")
    suspend fun getMessageCount(channelId: String): Int
}




