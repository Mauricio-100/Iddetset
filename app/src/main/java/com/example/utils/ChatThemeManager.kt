package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChatThemePreset(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val backgroundColor: Color,
    val backgroundGradient: Brush?,
    val myBubbleColor: Color,
    val myBubbleTextColor: Color,
    val partnerBubbleColor: Color,
    val partnerBubbleTextColor: Color,
    val accentColor: Color,
    val wallpaperPattern: String // "dots", "waves", "stars", "matrix", "clean"
) {
    val backgroundBrush: Brush
        get() = backgroundGradient ?: Brush.linearGradient(listOf(backgroundColor, backgroundColor))
}

object ChatThemeManager {
    private const val PREFS_NAME = "chat_theme_prefs"
    private const val KEY_THEME = "selected_chat_theme"
    private const val KEY_RINGTONE = "selected_chat_ringtone"

    val allThemes: List<ChatThemePreset> get() = THEMES

    val THEMES = listOf(
        ChatThemePreset(
            id = "emerald_dark",
            name = "iDDET Émeraude Pro",
            description = "Style sombre sophistiqué avec accents vert émeraude et cyan",
            emoji = "💎",
            backgroundColor = Color(0xFF0F172A),
            backgroundGradient = Brush.verticalGradient(
                listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF0F172A))
            ),
            myBubbleColor = Color(0xFF059669),
            myBubbleTextColor = Color.White,
            partnerBubbleColor = Color(0xFF1E293B),
            partnerBubbleTextColor = Color(0xFFF1F5F9),
            accentColor = Color(0xFF10B981),
            wallpaperPattern = "dots"
        ),
        ChatThemePreset(
            id = "cyber_neon",
            name = "Cyberpunk Violet",
            description = "Néons fuchsia et violet électrique façon night-city",
            emoji = "🔮",
            backgroundColor = Color(0xFF120B24),
            backgroundGradient = Brush.verticalGradient(
                listOf(Color(0xFF1A0B2E), Color(0xFF28114B), Color(0xFF110726))
            ),
            myBubbleColor = Color(0xFF9333EA),
            myBubbleTextColor = Color.White,
            partnerBubbleColor = Color(0xFF2E1065),
            partnerBubbleTextColor = Color(0xFFF3E8FF),
            accentColor = Color(0xFFC084FC),
            wallpaperPattern = "matrix"
        ),
        ChatThemePreset(
            id = "sunset_gold",
            name = "Sunset VIP Gold",
            description = "Nuances chaleureuses d'ambre, coucher de soleil et or royal",
            emoji = "🌅",
            backgroundColor = Color(0xFF1C1309),
            backgroundGradient = Brush.verticalGradient(
                listOf(Color(0xFF2A1B0E), Color(0xFF3F2314), Color(0xFF1C1309))
            ),
            myBubbleColor = Color(0xFFD97706),
            myBubbleTextColor = Color.White,
            partnerBubbleColor = Color(0xFF3A2410),
            partnerBubbleTextColor = Color(0xFFFEF3C7),
            accentColor = Color(0xFFF59E0B),
            wallpaperPattern = "stars"
        ),
        ChatThemePreset(
            id = "ocean_blue",
            name = "Lagon Océan",
            description = "Bleu marin profond rafraîchissant avec reflets turquoise",
            emoji = "🌊",
            backgroundColor = Color(0xFF0A192F),
            backgroundGradient = Brush.verticalGradient(
                listOf(Color(0xFF0A192F), Color(0xFF172A45), Color(0xFF0D2538))
            ),
            myBubbleColor = Color(0xFF0284C7),
            myBubbleTextColor = Color.White,
            partnerBubbleColor = Color(0xFF1E3A5F),
            partnerBubbleTextColor = Color(0xFFE0F2FE),
            accentColor = Color(0xFF38BDF8),
            wallpaperPattern = "waves"
        ),
        ChatThemePreset(
            id = "forest_zen",
            name = "Forêt Boréale",
            description = "Teintes vertes naturelles apaisantes et feuilles de menthe",
            emoji = "🌲",
            backgroundColor = Color(0xFF0D1F17),
            backgroundGradient = Brush.verticalGradient(
                listOf(Color(0xFF0D1F17), Color(0xFF163829), Color(0xFF0B1913))
            ),
            myBubbleColor = Color(0xFF15803D),
            myBubbleTextColor = Color.White,
            partnerBubbleColor = Color(0xFF1B3D2B),
            partnerBubbleTextColor = Color(0xFFDCFCE7),
            accentColor = Color(0xFF4ADE80),
            wallpaperPattern = "clean"
        ),
        ChatThemePreset(
            id = "crimson_ruby",
            name = "Rubis Intense",
            description = "Rouge carmin signature IDDET au design ultra affirmé",
            emoji = "🔥",
            backgroundColor = Color(0xFF1F0D0E),
            backgroundGradient = Brush.verticalGradient(
                listOf(Color(0xFF2B0E10), Color(0xFF3F1316), Color(0xFF180A0B))
            ),
            myBubbleColor = Color(0xFFDC2626),
            myBubbleTextColor = Color.White,
            partnerBubbleColor = Color(0xFF3D1418),
            partnerBubbleTextColor = Color(0xFFFFE4E6),
            accentColor = Color(0xFFF87171),
            wallpaperPattern = "stars"
        )
    )

    private val _currentTheme = MutableStateFlow(THEMES.first())
    val currentTheme: StateFlow<ChatThemePreset> = _currentTheme.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(KEY_THEME, THEMES.first().id)
        _currentTheme.value = THEMES.find { it.id == savedId } ?: THEMES.first()
    }

    fun setTheme(context: Context, themeId: String) {
        val theme = THEMES.find { it.id == themeId } ?: return
        _currentTheme.value = theme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, themeId).apply()
    }

    fun setTheme(theme: ChatThemePreset, context: Context) {
        setTheme(context, theme.id)
    }
}
