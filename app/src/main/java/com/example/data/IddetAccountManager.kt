package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Dedicated System Singleton Manager for the Official IDDET Account.
 *
 * Responsibilities:
 * - Initializes and persists the official "IDDET" account with a verified yellow checkmark.
 * - Enforces read-only permissions for regular public users.
 * - Provides authorization checks allowing only designated admins (C.M.O, Crislem, Offranel, etc.)
 *   to post official messages and announcements on behalf of IDDET.
 * - Serves as the platform's official information hub.
 */
object IddetAccountManager {

    const val IDDET_USER_ID = "iddet-official-hub-id"
    const val IDDET_USERNAME = "IDDET"
    const val IDDET_DISPLAY_NAME = "IDDET"
    const val IDDET_AVATAR_URL = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=80"
    const val IDDET_BIO = "🌟 Compte Officiel IDDET • Hub central d'informations, d'annonces et d'actualités certifiées de la plateforme STRIP."

    // Designated administrators authorized to manage and publish as IDDET
    val DESIGNATED_ADMINS = listOf(
        "C.M.O",
        "Crislem",
        "Offranel",
        "Doffranel",
        "doffranel",
        "Mauricio-100",
        "admin"
    )

    /**
     * Constructs the official IDDET User model with genuine metadata.
     */
    fun getOfficialIddetUser(): User {
        return User(
            id = IDDET_USER_ID,
            username = IDDET_USERNAME,
            passwordHash = "iddet_system_secure_hash",
            avatarUrl = IDDET_AVATAR_URL,
            bio = IDDET_BIO,
            privacySetting = "Public",
            isVerified = true,
            followingCount = 0,
            followersCount = 0,
            isGiant = true,
            level = 10,
            xp = 0,
            badges = "IDDET,Official,Verified,Hub",
            email = "contact@iddet.system",
            preferredCategory = "Actu"
        )
    }

    /**
     * Checks if a username corresponds to the official IDDET account.
     */
    fun isOfficialIddetAccount(username: String?): Boolean {
        if (username.isNullOrBlank()) return false
        val clean = username.trim()
        return clean.equals("IDDET", ignoreCase = true) ||
               clean.equals("Iddet", ignoreCase = true) ||
               clean.equals("IDDET Official", ignoreCase = true)
    }

    /**
     * Checks if a user ID corresponds to the official IDDET account.
     */
    fun isOfficialIddetUserId(userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        return userId == IDDET_USER_ID ||
               userId.equals("iddet-official-id", ignoreCase = true) ||
               userId.equals("IDDET", ignoreCase = true)
    }

    /**
     * Checks whether the given user is a designated administrator with official IDDET posting privileges.
     */
    fun isUserDesignatedAdmin(username: String?): Boolean {
        if (username.isNullOrBlank()) return false
        val clean = username.trim()
        return DESIGNATED_ADMINS.any { it.equals(clean, ignoreCase = true) }
    }

    /**
     * Determines whether the given user is authorized to publish or modify content under IDDET.
     * With simulated IDDET account removed, always returns false.
     */
    fun canPostAsIddet(user: User?): Boolean {
        return false
    }

    /**
     * Checks if the IDDET account is in read-only mode for the current user.
     */
    fun isReadOnlyForUser(currentUser: User?, targetUsername: String?): Boolean {
        if (!isOfficialIddetAccount(targetUsername)) return false
        return !canPostAsIddet(currentUser)
    }

    /**
     * Purges and removes the simulated IDDET official user from Room local database.
     */
    suspend fun purgeSimulatedIddetAccount(userDao: UserDao, actfileDao: ActfileDao) = withContext(Dispatchers.IO) {
        try {
            userDao.deleteUser(IDDET_USER_ID, IDDET_USERNAME)
            userDao.deleteUser("iddet-official-id", "Iddet")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Seeds initial beautiful actfiles and verified users if database is empty to ensure a lively feed experience.
     */
    suspend fun seedInitialActfilesIfEmpty(userDao: UserDao, actfileDao: ActfileDao) = withContext(Dispatchers.IO) {
        // Empty to ensure only real server responses are used
    }

    @Deprecated("Simulated IDDET account has been removed. Use purgeSimulatedIddetAccount.")
    suspend fun ensureIddetAccountExists(userDao: UserDao, actfileDao: ActfileDao) = withContext(Dispatchers.IO) {
        purgeSimulatedIddetAccount(userDao, actfileDao)
    }
}
