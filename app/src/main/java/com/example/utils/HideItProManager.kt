package com.example.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HideItModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val isStealth: Boolean,
    val aliasName: String?,
    val defaultPrimaryHex: Long,
    val iconEmoji: String,
    val notificationIconRes: Int
) {
    ORIGINAL_NEO(
        id = "original_neo",
        title = "Iddet Néo Minimaliste",
        subtitle = "L'icône originale signature IDDET avec le chat stylisé",
        isStealth = false,
        aliasName = null,
        defaultPrimaryHex = 0xFFDC2626,
        iconEmoji = "🐱",
        notificationIconRes = R.drawable.ic_notification
    ),
    CYBER_SHIELD(
        id = "cyber_shield",
        title = "Bouclier Cyber Obsidian",
        subtitle = "Armure furtive & géométrique haute sécurité",
        isStealth = false,
        aliasName = "com.example.MainActivityCyberShield",
        defaultPrimaryHex = 0xFF6366F1,
        iconEmoji = "🛡️",
        notificationIconRes = R.drawable.ic_notif_shield
    ),
    QUANTUM_EYE(
        id = "quantum_eye",
        title = "Œil Quantique Hologramme",
        subtitle = "Vision cryptée ultra-moderne avec iris énergétique",
        isStealth = false,
        aliasName = "com.example.MainActivityQuantumEye",
        defaultPrimaryHex = 0xFF06B6D4,
        iconEmoji = "👁️",
        notificationIconRes = R.drawable.ic_notif_eye
    ),
    GOLD_VIP(
        id = "gold_vip",
        title = "Prestige Gold Hide It",
        subtitle = "Finition or massif royal avec biseautage de luxe",
        isStealth = false,
        aliasName = "com.example.MainActivityGoldVip",
        defaultPrimaryHex = 0xFFFFD700,
        iconEmoji = "👑",
        notificationIconRes = R.drawable.ic_notif_crown
    ),
    STEALTH_CALCULATOR(
        id = "stealth_calculator",
        title = "Calculatrice Furtive",
        subtitle = "Camouflage launcher en véritable outil mathématique",
        isStealth = true,
        aliasName = "com.example.MainActivityCalculator",
        defaultPrimaryHex = 0xFF10B981,
        iconEmoji = "🔢",
        notificationIconRes = R.drawable.ic_notif_calc
    ),
    STEALTH_NOTES(
        id = "stealth_notes",
        title = "Bloc-Notes Furtif",
        subtitle = "Camouflage launcher en carnet de notes classique",
        isStealth = true,
        aliasName = "com.example.MainActivityNotes",
        defaultPrimaryHex = 0xFFF59E0B,
        iconEmoji = "📝",
        notificationIconRes = R.drawable.ic_notif_notes
    ),
    STEALTH_WEATHER(
        id = "stealth_weather",
        title = "Météo Climat Furtif",
        subtitle = "Camouflage launcher en bulletin météo inoffensif",
        isStealth = true,
        aliasName = "com.example.MainActivityWeather",
        defaultPrimaryHex = 0xFF0284C7,
        iconEmoji = "☀️",
        notificationIconRes = R.drawable.ic_notif_weather
    ),
    STEALTH_AUDIO(
        id = "stealth_audio",
        title = "Lecteur Audio Studio",
        subtitle = "Camouflage launcher en lecteur de musique discret",
        isStealth = true,
        aliasName = "com.example.MainActivityAudio",
        defaultPrimaryHex = 0xFF8B5CF6,
        iconEmoji = "🎵",
        notificationIconRes = R.drawable.ic_notif_audio
    )
}

enum class HideItColorPreset(
    val id: String,
    val title: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color
) {
    CRIMSON_RED(
        id = "crimson_red",
        title = "Rouge Iddet",
        primaryColor = Color(0xFFDC2626),
        secondaryColor = Color(0xFF991B1B),
        accentColor = Color(0xFFEF4444)
    ),
    CYBER_PURPLE(
        id = "cyber_purple",
        title = "Néon Violet",
        primaryColor = Color(0xFF8B5CF6),
        secondaryColor = Color(0xFF5B21B6),
        accentColor = Color(0xFFA78BFA)
    ),
    EMERALD_STEALTH(
        id = "emerald_stealth",
        title = "Émeraude Furtive",
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF047857),
        accentColor = Color(0xFF34D399)
    ),
    CYAN_HOLO(
        id = "cyan_holo",
        title = "Cyan Cybernétique",
        primaryColor = Color(0xFF06B6D4),
        secondaryColor = Color(0xFF0E7490),
        accentColor = Color(0xFF22D3EE)
    ),
    ROYAL_GOLD(
        id = "royal_gold",
        title = "Or Impérial",
        primaryColor = Color(0xFFFFD700),
        secondaryColor = Color(0xFFB45309),
        accentColor = Color(0xFFFDE047)
    ),
    MIDNIGHT_OBSIDIAN(
        id = "midnight_obsidian",
        title = "Obsidienne Noirceur",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFF0F172A),
        accentColor = Color(0xFFE2E8F0)
    ),
    SUNSET_FIRE(
        id = "sunset_fire",
        title = "Braise Ardente",
        primaryColor = Color(0xFFF97316),
        secondaryColor = Color(0xFFC2410C),
        accentColor = Color(0xFFFB923C)
    )
}

object HideItProManager {
    private const val PREFS_NAME = "hide_it_pro_prefs"
    private const val KEY_SELECTED_MODEL = "key_hideit_model"
    private const val KEY_SELECTED_COLOR = "key_hideit_color"
    private const val KEY_STEALTH_MODE_ENABLED = "key_stealth_mode_enabled"

    private val _currentModel = MutableStateFlow(HideItModel.ORIGINAL_NEO)
    val currentModel: StateFlow<HideItModel> = _currentModel.asStateFlow()

    private val _currentColor = MutableStateFlow(HideItColorPreset.CRIMSON_RED)
    val currentColor: StateFlow<HideItColorPreset> = _currentColor.asStateFlow()

    private val _isStealthActive = MutableStateFlow(false)
    val isStealthActive: StateFlow<Boolean> = _isStealthActive.asStateFlow()

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modelId = prefs.getString(KEY_SELECTED_MODEL, HideItModel.ORIGINAL_NEO.id)
        val colorId = prefs.getString(KEY_SELECTED_COLOR, HideItColorPreset.CRIMSON_RED.id)
        val stealth = prefs.getBoolean(KEY_STEALTH_MODE_ENABLED, false)

        _currentModel.value = HideItModel.values().firstOrNull { it.id == modelId } ?: HideItModel.ORIGINAL_NEO
        _currentColor.value = HideItColorPreset.values().firstOrNull { it.id == colorId } ?: HideItColorPreset.CRIMSON_RED
        _isStealthActive.value = stealth
    }

    fun setModel(context: Context, model: HideItModel, onComplete: (Boolean) -> Unit = {}) {
        _currentModel.value = model
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_MODEL, model.id).apply()

        // Apply launcher alias changes via PackageManager
        val success = applyLauncherAlias(context, model)
        _isStealthActive.value = model.isStealth
        prefs.edit().putBoolean(KEY_STEALTH_MODE_ENABLED, model.isStealth).apply()
        onComplete(success)
    }

    fun setColorPreset(context: Context, preset: HideItColorPreset) {
        _currentColor.value = preset
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_COLOR, preset.id).apply()
    }

    private fun applyLauncherAlias(context: Context, selectedModel: HideItModel): Boolean {
        return try {
            val pm = context.packageManager
            val pkg = context.packageName

            val mainComponent = ComponentName(pkg, "com.example.MainActivity")
            val aliases = listOf(
                "com.example.MainActivityCyberShield",
                "com.example.MainActivityQuantumEye",
                "com.example.MainActivityGoldVip",
                "com.example.MainActivityCalculator",
                "com.example.MainActivityNotes",
                "com.example.MainActivityWeather",
                "com.example.MainActivityAudio"
            )

            if (selectedModel.aliasName != null) {
                // Enable targeted launcher alias (Signature model or Stealth model)
                for (alias in aliases) {
                    val component = ComponentName(pkg, alias)
                    val newState = if (alias == selectedModel.aliasName) {
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    }
                    pm.setComponentEnabledSetting(
                        component,
                        newState,
                        PackageManager.DONT_KILL_APP
                    )
                }
                // Disable main launcher entry so only the chosen alias appears
                pm.setComponentEnabledSetting(
                    mainComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                // Restore default main component (ORIGINAL_NEO)
                pm.setComponentEnabledSetting(
                    mainComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                // Disable all aliases
                for (alias in aliases) {
                    val component = ComponentName(pkg, alias)
                    pm.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getNotificationSmallIconRes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modelId = prefs.getString(KEY_SELECTED_MODEL, HideItModel.ORIGINAL_NEO.id)
        val model = HideItModel.values().firstOrNull { it.id == modelId } ?: HideItModel.ORIGINAL_NEO
        return model.notificationIconRes
    }

    fun getNotificationColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorId = prefs.getString(KEY_SELECTED_COLOR, HideItColorPreset.CRIMSON_RED.id)
        val preset = HideItColorPreset.values().firstOrNull { it.id == colorId } ?: HideItColorPreset.CRIMSON_RED
        return preset.primaryColor.toArgb()
    }

    fun getNotificationLargeIconBitmap(context: Context): Bitmap? {
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_cat_logo) ?: return null
        val width = drawable.intrinsicWidth.coerceAtLeast(128)
        val height = drawable.intrinsicHeight.coerceAtLeast(128)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}

