package com.example.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Gestionnaire universel de partage (Share Portfolio & Entity Share) pour IDDET.
 * Aligné strictement avec le serveur public FastAPI :
 * - Base URL : https://hoosthubs-g.onrender.com
 * - Protocole : iddet://
 * - Entités supportées : Profil/User (portfolio), Actfile (post), Son (audio),
 *   Vidéo (wing/reel), Communauté (c/slug), Application IDDET.
 */
object ShareHelper {

    const val BASE_WEB_URL = "https://hoosthubs-g.onrender.com"
    const val SCHEME = "iddet"
    const val APP_NAME = "IDDET"

    // ==========================================
    // 1. URLs & DEEP LINKS GÉNÉRATION
    // ==========================================

    fun getUserWebUrl(username: String): String = "$BASE_WEB_URL/u/$username"
    fun getUserDeepLink(username: String): String = "$SCHEME://profile/$username"

    fun getCommunityWebUrl(slug: String): String = "$BASE_WEB_URL/c/$slug"
    fun getCommunityDeepLink(slug: String): String = "$SCHEME://community/$slug"

    fun getActfileWebUrl(actfileId: String): String = "$BASE_WEB_URL/actfile/$actfileId"
    fun getActfileDeepLink(actfileId: String): String = "$SCHEME://actfile/$actfileId"

    fun getSoundWebUrl(soundId: String): String = "$BASE_WEB_URL/sounds/$soundId"
    fun getSoundDeepLink(soundId: String): String = "$SCHEME://sounds/$soundId"

    fun getVideoWebUrl(videoId: String): String = "$BASE_WEB_URL/video/$videoId"
    fun getVideoDeepLink(videoId: String): String = "$SCHEME://video/$videoId"

    fun getAppWebUrl(): String = "$BASE_WEB_URL/"
    fun getAppDeepLink(): String = "$SCHEME://home"

    // ==========================================
    // 2. PARTAGE UTILISATEUR & PORTFOLIO
    // ==========================================

    fun shareUserProfile(
        context: Context,
        username: String,
        displayName: String? = null,
        bio: String? = null,
        isVerified: Boolean = false,
        followersCount: Int? = null,
        videosCount: Int? = null,
        soundsCount: Int? = null
    ) {
        val webUrl = getUserWebUrl(username)
        val deepLink = getUserDeepLink(username)
        val verifiedTag = if (isVerified) " ✅ (Vérifié)" else ""
        val nameLabel = if (!displayName.isNullOrBlank() && displayName != username) "$displayName (@$username)" else "@$username"

        val statsList = mutableListOf<String>()
        if (followersCount != null && followersCount > 0) statsList.add("$followersCount abonnés")
        if (videosCount != null && videosCount > 0) statsList.add("$videosCount vidéos")
        if (soundsCount != null && soundsCount > 0) statsList.add("$soundsCount sons")
        val statsLine = if (statsList.isNotEmpty()) "\n📊 Portfolio : " + statsList.joinToString(" · ") else ""

        val bioText = if (!bio.isNullOrBlank()) "\n💬 « $bio »\n" else "\n"

        val text = buildString {
            append("Découvrez le portfolio de $nameLabel$verifiedTag sur $APP_NAME !")
            append(bioText)
            append(statsLine)
            append("\n🌐 Lien web : $webUrl")
            append("\n📱 Ouvrir dans l'application : $deepLink")
        }

        launchShareIntent(context, text, "Partager le profil de @$username")
    }

    // ==========================================
    // 3. PARTAGE ACTFILE (PUBLICATION MARKDOWN)
    // ==========================================

    fun shareActfile(
        context: Context,
        actfileId: String,
        authorUsername: String,
        content: String? = null,
        category: String? = null
    ) {
        val webUrl = getActfileWebUrl(actfileId)
        val deepLink = getActfileDeepLink(actfileId)
        val catTag = if (!category.isNullOrBlank() && category != "Autres") " [$category]" else ""

        val snippet = if (!content.isNullOrBlank()) {
            val clean = content.replace(Regex("[#*`_~]"), " ").trim()
            val truncated = if (clean.length > 140) clean.take(140) + "..." else clean
            "\n« $truncated »\n"
        } else "\n"

        val text = buildString {
            append("Découvrez cette publication de @$authorUsername$catTag sur $APP_NAME :")
            append(snippet)
            append("\n🌐 Lire sur le web : $webUrl")
            append("\n📱 Ouvrir dans l'application : $deepLink")
        }

        launchShareIntent(context, text, "Partager la publication de @$authorUsername")
    }

    // ==========================================
    // 4. PARTAGE SON & AUDIO (STRIP SOUNDS)
    // ==========================================

    fun shareSound(
        context: Context,
        soundId: String,
        title: String,
        authorUsername: String,
        category: String? = null
    ) {
        val webUrl = getSoundWebUrl(soundId)
        val deepLink = getSoundDeepLink(soundId)
        val catText = if (!category.isNullOrBlank()) " ($category)" else ""

        val text = buildString {
            append("🎵 Écoutez le son « $title »$catText par @$authorUsername sur $APP_NAME Sons !")
            append("\n🌐 Écouter sur le web : $webUrl")
            append("\n📱 Ouvrir dans l'application : $deepLink")
        }

        launchShareIntent(context, text, "Partager le son « $title »")
    }

    // ==========================================
    // 5. PARTAGE VIDÉO / REEL / CLIP
    // ==========================================

    fun shareVideo(
        context: Context,
        videoId: String,
        authorUsername: String? = null,
        description: String? = null
    ) {
        val webUrl = getVideoWebUrl(videoId)
        val deepLink = getVideoDeepLink(videoId)
        val authorText = if (!authorUsername.isNullOrBlank()) " de @$authorUsername" else ""
        val descText = if (!description.isNullOrBlank()) "\n« ${description.take(120)} »\n" else "\n"

        val text = buildString {
            append("🎬 Regardez cette vidéo$authorText sur $APP_NAME :")
            append(descText)
            append("\n🌐 Regarder sur le web : $webUrl")
            append("\n📱 Ouvrir dans l'application : $deepLink")
        }

        launchShareIntent(context, text, "Partager la vidéo")
    }

    // ==========================================
    // 6. PARTAGE COMMUNAUTÉ (c/slug)
    // ==========================================

    fun shareCommunity(
        context: Context,
        slug: String,
        name: String,
        description: String? = null,
        membersCount: Int? = null,
        isVerified: Boolean = false
    ) {
        val webUrl = getCommunityWebUrl(slug)
        val deepLink = getCommunityDeepLink(slug)
        val verifiedTag = if (isVerified) " 🛡️" else ""
        val countText = if (membersCount != null && membersCount > 0) " ($membersCount membres)" else ""
        val descText = if (!description.isNullOrBlank()) "\n« ${description.take(140)} »\n" else "\n"

        val text = buildString {
            append("Rejoignez la communauté c/$slug ($name)$verifiedTag$countText sur $APP_NAME !")
            append(descText)
            append("\n🌐 Voir sur le web : $webUrl")
            append("\n📱 Ouvrir dans l'application : $deepLink")
        }

        launchShareIntent(context, text, "Partager la communauté c/$slug")
    }

    // ==========================================
    // 7. PARTAGE GLOBAL APPLICATION IDDET
    // ==========================================

    fun shareApp(context: Context) {
        val webUrl = getAppWebUrl()
        val deepLink = getAppDeepLink()

        val text = buildString {
            append("🚀 Rejoins-moi sur $APP_NAME !")
            append("\nLa plateforme tout-en-un de streaming vidéo, publications Markdown, musique & sons originaux, communautés avec salons et messagerie instantanée en temps réel.")
            append("\n\n🌐 Découvrir sur le web : $webUrl")
            append("\n📱 Ouvrir l'application : $deepLink")
        }

        launchShareIntent(context, text, "Inviter sur $APP_NAME")
    }

    // ==========================================
    // 8. UTILITAIRES COPIE DANS LE PRESSE-PAPIER
    // ==========================================

    fun copyToClipboard(context: Context, text: String, label: String = "Lien copié !") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("IDDET Link", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur de copie", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchShareIntent(context: Context, text: String, chooserTitle: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, chooserTitle)
            }
            val chooser = Intent.createChooser(intent, chooserTitle)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Impossible d'ouvrir le sélecteur de partage", Toast.LENGTH_SHORT).show()
        }
    }
}
