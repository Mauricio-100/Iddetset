package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class ZodiacSignInfo(
    val sign: String,
    val latinName: String,
    val element: String,
    val elementColor: Color,
    val dateRange: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val rulingPlanet: String,
    val traits: List<String>,
    val chillDescription: String
)

object ZodiacHelper {

    fun getZodiacSign(day: Int, month: Int): String {
        return when (month) {
            1 -> if (day < 20) "Capricorne" else "Verseau"
            2 -> if (day < 19) "Verseau" else "Poissons"
            3 -> if (day < 21) "Poissons" else "Bélier"
            4 -> if (day < 20) "Bélier" else "Taureau"
            5 -> if (day < 21) "Taureau" else "Gémeaux"
            6 -> if (day < 21) "Gémeaux" else "Cancer"
            7 -> if (day < 23) "Cancer" else "Lion"
            8 -> if (day < 23) "Lion" else "Vierge"
            9 -> if (day < 23) "Vierge" else "Balance"
            10 -> if (day < 23) "Balance" else "Scorpion"
            11 -> if (day < 22) "Scorpion" else "Sagittaire"
            12 -> if (day < 22) "Sagittaire" else "Capricorne"
            else -> "Bélier"
        }
    }

    fun parseFromDateString(dateStr: String?): String? {
        if (dateStr.isNullOrBlank()) return null
        try {
            val clean = dateStr.trim()
            if (clean.contains("/")) {
                val parts = clean.split("/")
                if (parts.size >= 2) {
                    val day = parts[0].trim().toIntOrNull()
                    val month = parts[1].trim().toIntOrNull()
                    if (day != null && month != null && day in 1..31 && month in 1..12) {
                        return getZodiacSign(day, month)
                    }
                }
            } else if (clean.contains("-")) {
                val parts = clean.split("-")
                if (parts.size >= 3) {
                    // YYYY-MM-DD or DD-MM-YYYY
                    if (parts[0].length == 4) {
                        val month = parts[1].trim().toIntOrNull()
                        val day = parts[2].trim().toIntOrNull()
                        if (day != null && month != null && day in 1..31 && month in 1..12) {
                            return getZodiacSign(day, month)
                        }
                    } else {
                        val day = parts[0].trim().toIntOrNull()
                        val month = parts[1].trim().toIntOrNull()
                        if (day != null && month != null && day in 1..31 && month in 1..12) {
                            return getZodiacSign(day, month)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getInfo(sign: String?): ZodiacSignInfo {
        val normalized = sign?.trim()?.lowercase() ?: ""
        return when {
            normalized.contains("bélier") || normalized.contains("belier") || normalized.contains("aries") -> ZodiacSignInfo(
                sign = "Bélier",
                latinName = "Aries",
                element = "Feu",
                elementColor = Color(0xFFEF4444),
                dateRange = "21 Mars - 19 Avril",
                primaryColor = Color(0xFFDC2626),
                secondaryColor = Color(0xFFF97316),
                rulingPlanet = "Mars",
                traits = listOf("Audacieux", "Passionné", "Pionnier", "Énergique"),
                chillDescription = "Leader naturel avec une énergie débordante. Tu inspires confiance et ton dynamisme illumine chaque conversation !"
            )
            normalized.contains("taureau") || normalized.contains("taurus") -> ZodiacSignInfo(
                sign = "Taureau",
                latinName = "Taurus",
                element = "Terre",
                elementColor = Color(0xFF10B981),
                dateRange = "20 Avril - 20 Mai",
                primaryColor = Color(0xFF059669),
                secondaryColor = Color(0xFF34D399),
                rulingPlanet = "Vénus",
                traits = listOf("Fidèle", "Persévérant", "Généreux", "Posé"),
                chillDescription = "Force tranquille et amoureux des bonnes choses. Ta loyauté et ta présence apaisante sont un véritable repère pour tes proches."
            )
            normalized.contains("gémeaux") || normalized.contains("gemeaux") || normalized.contains("gemini") -> ZodiacSignInfo(
                sign = "Gémeaux",
                latinName = "Gemini",
                element = "Air",
                elementColor = Color(0xFFF59E0B),
                dateRange = "21 Mai - 20 Juin",
                primaryColor = Color(0xFFD97706),
                secondaryColor = Color(0xFFFBBF24),
                rulingPlanet = "Mercure",
                traits = listOf("Curieux", "Éloquent", "Vif d'esprit", "Sociable"),
                chillDescription = "Esprit brillant et communicateur hors pair. Tu as toujours une histoire captivante ou une bonne vibe à partager."
            )
            normalized.contains("cancer") -> ZodiacSignInfo(
                sign = "Cancer",
                latinName = "Cancer",
                element = "Eau",
                elementColor = Color(0xFF06B6D4),
                dateRange = "21 Juin - 22 Juillet",
                primaryColor = Color(0xFF0891B2),
                secondaryColor = Color(0xFF38BDF8),
                rulingPlanet = "Lune",
                traits = listOf("Intuitif", "Protecteur", "Sensible", "Créatif"),
                chillDescription = "Grand cœur et créativité sans limite. Tu sais écouter et créer un havre de paix où tout le monde se sent bien."
            )
            normalized.contains("lion") || normalized.contains("leo") -> ZodiacSignInfo(
                sign = "Lion",
                latinName = "Leo",
                element = "Feu",
                elementColor = Color(0xFFF97316),
                dateRange = "23 Juillet - 22 Août",
                primaryColor = Color(0xFFEA580C),
                secondaryColor = Color(0xFFFBBF24),
                rulingPlanet = "Soleil",
                traits = listOf("Charismatique", "Généreux", "Rayonnant", "Fier"),
                chillDescription = "Aura royale et générosité d'or. Tu attires la lumière et réchauffes tous ceux qui croisent ton chemin."
            )
            normalized.contains("vierge") || normalized.contains("virgo") -> ZodiacSignInfo(
                sign = "Vierge",
                latinName = "Virgo",
                element = "Terre",
                elementColor = Color(0xFF14B8A6),
                dateRange = "23 Août - 22 Septembre",
                primaryColor = Color(0xFF0D9488),
                secondaryColor = Color(0xFF2DD4BF),
                rulingPlanet = "Mercure",
                traits = listOf("Méthodique", "Bienveillant", "Précis", "Dévoué"),
                chillDescription = "Œil de lynx et bienveillance discrète. Tu trouves toujours les solutions parfaites pour rendre la vie plus belle."
            )
            normalized.contains("balance") || normalized.contains("libra") -> ZodiacSignInfo(
                sign = "Balance",
                latinName = "Libra",
                element = "Air",
                elementColor = Color(0xFF6366F1),
                dateRange = "23 Septembre - 22 Octobre",
                primaryColor = Color(0xFF4F46E5),
                secondaryColor = Color(0xFF818CF8),
                rulingPlanet = "Vénus",
                traits = listOf("Harmonieux", "Diplomate", "Élégant", "Juste"),
                chillDescription = "Recherche d'harmonie et sens inné du style. Tu crées des ponts entre les gens et diffuses la paix partout."
            )
            normalized.contains("scorpion") || normalized.contains("scorpio") -> ZodiacSignInfo(
                sign = "Scorpion",
                latinName = "Scorpio",
                element = "Eau",
                elementColor = Color(0xFFE11D48),
                dateRange = "23 Octobre - 21 Novembre",
                primaryColor = Color(0xFFBE123C),
                secondaryColor = Color(0xFFFB7185),
                rulingPlanet = "Pluton",
                traits = listOf("Magnétique", "Intense", "Perspicace", "Loyal"),
                chillDescription = "Magnétisme intense et intuition perçante. Rien ne t'échappe et ta loyauté est inconditionnelle."
            )
            normalized.contains("sagittaire") || normalized.contains("sagittarius") -> ZodiacSignInfo(
                sign = "Sagittaire",
                latinName = "Sagittarius",
                element = "Feu",
                elementColor = Color(0xFF8B5CF6),
                dateRange = "22 Novembre - 21 Décembre",
                primaryColor = Color(0xFF7C3AED),
                secondaryColor = Color(0xFFA78BFA),
                rulingPlanet = "Jupiter",
                traits = listOf("Optimiste", "Aventurier", "Libre", "Enthousiaste"),
                chillDescription = "Aventurier dans l'âme et éternel optimiste. Ta soif de découvertes et ta joie de vivre sont contagieuses."
            )
            normalized.contains("capricorne") || normalized.contains("capricorn") -> ZodiacSignInfo(
                sign = "Capricorne",
                latinName = "Capricorn",
                element = "Terre",
                elementColor = Color(0xFF64748B),
                dateRange = "22 Décembre - 19 Janvier",
                primaryColor = Color(0xFF475569),
                secondaryColor = Color(0xFF94A3B8),
                rulingPlanet = "Saturne",
                traits = listOf("Ambitieux", "Sage", "Résolu", "Fiable"),
                chillDescription = "Pilier inébranlable et vision à long terme. Ta détermination t'amène au sommet pas à pas en toute sérénité."
            )
            normalized.contains("verseau") || normalized.contains("aquarius") -> ZodiacSignInfo(
                sign = "Verseau",
                latinName = "Aquarius",
                element = "Air",
                elementColor = Color(0xFF3B82F6),
                dateRange = "20 Janvier - 18 Février",
                primaryColor = Color(0xFF2563EB),
                secondaryColor = Color(0xFF60A5FA),
                rulingPlanet = "Uranus",
                traits = listOf("Visionnaire", "Original", "Humaniste", "Indépendant"),
                chillDescription = "Visionnaire avant-gardiste et esprit libre. Tu penses toujours un coup d'avance avec un grand cœur pour le monde."
            )
            normalized.contains("poissons") || normalized.contains("pisces") -> ZodiacSignInfo(
                sign = "Poissons",
                latinName = "Pisces",
                element = "Eau",
                elementColor = Color(0xFFEC4899),
                dateRange = "19 Février - 20 Mars",
                primaryColor = Color(0xFFDB2777),
                secondaryColor = Color(0xFFF472B6),
                rulingPlanet = "Neptune",
                traits = listOf("Empathique", "Rêveur", "Poétique", "Dévoué"),
                chillDescription = "Âme poétique et profonde empathie. Ta sensibilité artistique et ton écoute bienveillante réconfortent les cœurs."
            )
            else -> ZodiacSignInfo(
                sign = "Mystique",
                latinName = "Cosmos",
                element = "Éther",
                elementColor = Color(0xFF8B5CF6),
                dateRange = "Cosmique",
                primaryColor = Color(0xFF6366F1),
                secondaryColor = Color(0xFF06B6D4),
                rulingPlanet = "Galaxie",
                traits = listOf("Unique", "Inspirant", "Curieux"),
                chillDescription = "Une énergie stellaire unique qui apporte une touche d'originalité à la communauté IDDET !"
            )
        }
    }
}

/**
 * Authentic Jetpack Compose Vector Graphic renderer for Astrological Zodiac Glyphs.
 * Pure custom vector canvas rendering without emoji shortcuts.
 */
@Composable
fun ZodiacVectorIcon(
    sign: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    strokeWidth: Dp = 2.5.dp
) {
    val normalized = remember(sign) { sign.trim().lowercase() }

    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val path = Path()

        when {
            // Bélier / Aries (Ram Horns)
            normalized.contains("bélier") || normalized.contains("belier") || normalized.contains("aries") -> {
                path.moveTo(cx, h * 0.85f)
                path.lineTo(cx, h * 0.42f)
                // Left horn
                path.cubicTo(cx, h * 0.15f, w * 0.12f, h * 0.15f, w * 0.15f, h * 0.45f)
                // Right horn
                path.moveTo(cx, h * 0.42f)
                path.cubicTo(cx, h * 0.15f, w * 0.88f, h * 0.15f, w * 0.85f, h * 0.45f)
                drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            // Taureau / Taurus (Circle + Horns)
            normalized.contains("taureau") || normalized.contains("taurus") -> {
                val circleRadius = w * 0.26f
                drawCircle(
                    color = tint,
                    radius = circleRadius,
                    center = Offset(cx, cy + h * 0.12f),
                    style = Stroke(width = stroke)
                )
                path.moveTo(w * 0.20f, h * 0.18f)
                path.cubicTo(w * 0.28f, h * 0.38f, w * 0.72f, h * 0.38f, w * 0.80f, h * 0.18f)
                drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
            // Gémeaux / Gemini (Roman II with top/bottom arcs)
            normalized.contains("gémeaux") || normalized.contains("gemeaux") || normalized.contains("gemini") -> {
                // Top arc
                path.moveTo(w * 0.18f, h * 0.22f)
                path.cubicTo(w * 0.38f, h * 0.16f, w * 0.62f, h * 0.16f, w * 0.82f, h * 0.22f)
                // Bottom arc
                path.moveTo(w * 0.18f, h * 0.78f)
                path.cubicTo(w * 0.38f, h * 0.84f, w * 0.62f, h * 0.84f, w * 0.82f, h * 0.78f)
                // Left pillar
                path.moveTo(w * 0.36f, h * 0.21f)
                path.lineTo(w * 0.36f, h * 0.79f)
                // Right pillar
                path.moveTo(w * 0.64f, h * 0.21f)
                path.lineTo(w * 0.64f, h * 0.79f)
                drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
            // Cancer (Crab Claws / 69 Glyph)
            normalized.contains("cancer") -> {
                // Top loop
                drawCircle(color = tint, radius = w * 0.12f, center = Offset(w * 0.36f, h * 0.32f), style = Stroke(width = stroke))
                path.moveTo(w * 0.48f, h * 0.32f)
                path.cubicTo(w * 0.75f, h * 0.22f, w * 0.82f, h * 0.40f, w * 0.70f, h * 0.46f)
                // Bottom loop
                drawCircle(color = tint, radius = w * 0.12f, center = Offset(w * 0.64f, h * 0.68f), style = Stroke(width = stroke))
                path.moveTo(w * 0.52f, h * 0.68f)
                path.cubicTo(w * 0.25f, h * 0.78f, w * 0.18f, h * 0.60f, w * 0.30f, h * 0.54f)
                drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
            // Lion / Leo (Sun Head + Lion Tail)
            normalized.contains("lion") || normalized.contains("leo") -> {
                drawCircle(color = tint, radius = w * 0.13f, center = Offset(w * 0.28f, h * 0.65f), style = Stroke(width = stroke))
                path.moveTo(w * 0.34f, h * 0.54f)
                path.cubicTo(w * 0.35f, h * 0.22f, w * 0.68f, h * 0.18f, w * 0.68f, h * 0.48f)
                path.cubicTo(w * 0.68f, h * 0.78f, w * 0.82f, h * 0.78f, w * 0.86f, h * 0.65f)
                drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
            // Vierge / Virgo (M with loop)
            normalized.contains("vierge") || normalized.contains("virgo") -> {
                path.moveTo(w * 0.16f, h * 0.76f)
                path.lineTo(w * 0.16f, h * 0.30f)
                path.cubicTo(w * 0.16f, h * 0.18f, w * 0.36f, h * 0.18f, w * 0.36f, h * 0.32f)
                path.lineTo(w * 0.36f, h * 0.76f)
                path.moveTo(w * 0.36f, h * 0.32f)
                path.cubicTo(w * 0.36f, h * 0.18f, w * 0.56f, h * 0.18f, w * 0.56f, h * 0.32f)
                path.lineTo(w * 0.56f, h * 0.76f)
                // Loop
                path.moveTo(w * 0.56f, h * 0.45f)
                path.cubicTo(w * 0.78f, h * 0.45f, w * 0.84f, h * 0.72f, w * 0.70f, h * 0.86f)
                path.cubicTo(w * 0.58f, h * 0.98f, w * 0.64f, h * 0.62f, w * 0.84f, h * 0.84f)
                drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            // Balance / Libra (Scales)
            normalized.contains("balance") || normalized.contains("libra") -> {
                // Bottom line
                drawLine(color = tint, start = Offset(w * 0.15f, h * 0.78f), end = Offset(w * 0.85f, h * 0.78f), strokeWidth = stroke, cap = StrokeCap.Round)
                // Middle bar with upper arch
                path.moveTo(w * 0.15f, h * 0.52f)
                path.lineTo(w * 0.35f, h * 0.52f)
                path.cubicTo(w * 0.35f, h * 0.20f, w * 0.65f, h * 0.20f, w * 0.65f, h * 0.52f)
                path.lineTo(w * 0.85f, h * 0.52f)
                drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
            // Scorpion / Scorpio (M with Arrow)
            normalized.contains("scorpion") || normalized.contains("scorpio") -> {
                path.moveTo(w * 0.16f, h * 0.76f)
                path.lineTo(w * 0.16f, h * 0.30f)
                path.cubicTo(w * 0.16f, h * 0.18f, w * 0.36f, h * 0.18f, w * 0.36f, h * 0.32f)
                path.lineTo(w * 0.36f, h * 0.76f)
                path.moveTo(w * 0.36f, h * 0.32f)
                path.cubicTo(w * 0.36f, h * 0.18f, w * 0.56f, h * 0.18f, w * 0.56f, h * 0.32f)
                path.lineTo(w * 0.56f, h * 0.76f)
                // Arrow tail
                path.moveTo(w * 0.56f, h * 0.50f)
                path.cubicTo(w * 0.76f, h * 0.50f, w * 0.80f, h * 0.68f, w * 0.80f, h * 0.78f)
                path.lineTo(w * 0.86f, h * 0.78f)
                // Arrow head
                path.moveTo(w * 0.80f, h * 0.70f)
                path.lineTo(w * 0.88f, h * 0.78f)
                path.lineTo(w * 0.80f, h * 0.86f)
                drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            // Sagittaire / Sagittarius (Archer Arrow)
            normalized.contains("sagittaire") || normalized.contains("sagittarius") -> {
                // Main diagonal shaft
                drawLine(color = tint, start = Offset(w * 0.20f, h * 0.80f), end = Offset(w * 0.80f, h * 0.20f), strokeWidth = stroke, cap = StrokeCap.Round)
                // Arrowhead
                path.moveTo(w * 0.55f, h * 0.20f)
                path.lineTo(w * 0.80f, h * 0.20f)
                path.lineTo(w * 0.80f, h * 0.45f)
                // Crossbar
                drawLine(color = tint, start = Offset(w * 0.35f, h * 0.50f), end = Offset(w * 0.50f, h * 0.65f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            // Capricorne / Capricorn (Goat Horn & Tail)
            normalized.contains("capricorne") || normalized.contains("capricorn") -> {
                path.moveTo(w * 0.18f, h * 0.28f)
                path.lineTo(w * 0.36f, h * 0.76f)
                path.lineTo(w * 0.54f, h * 0.28f)
                path.cubicTo(w * 0.72f, h * 0.28f, w * 0.78f, h * 0.56f, w * 0.64f, h * 0.74f)
                path.cubicTo(w * 0.52f, h * 0.90f, w * 0.60f, h * 0.94f, w * 0.74f, h * 0.82f)
                drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            // Verseau / Aquarius (Undulating Water Waves)
            normalized.contains("verseau") || normalized.contains("aquarius") -> {
                // Top wave
                path.moveTo(w * 0.15f, h * 0.38f)
                path.lineTo(w * 0.32f, h * 0.28f)
                path.lineTo(w * 0.50f, h * 0.38f)
                path.lineTo(w * 0.68f, h * 0.28f)
                path.lineTo(w * 0.85f, h * 0.38f)
                // Bottom wave
                path.moveTo(w * 0.15f, h * 0.68f)
                path.lineTo(w * 0.32f, h * 0.58f)
                path.lineTo(w * 0.50f, h * 0.68f)
                path.lineTo(w * 0.68f, h * 0.58f)
                path.lineTo(w * 0.85f, h * 0.68f)
                drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            // Poissons / Pisces (Two Arcs with Crossbar)
            normalized.contains("poissons") || normalized.contains("pisces") -> {
                // Left arc
                path.moveTo(w * 0.32f, h * 0.20f)
                path.cubicTo(w * 0.16f, h * 0.40f, w * 0.16f, h * 0.60f, w * 0.32f, h * 0.80f)
                // Right arc
                path.moveTo(w * 0.68f, h * 0.20f)
                path.cubicTo(w * 0.84f, h * 0.40f, w * 0.84f, h * 0.60f, w * 0.68f, h * 0.80f)
                // Joining crossbar
                drawLine(color = tint, start = Offset(w * 0.18f, cy), end = Offset(w * 0.82f, cy), strokeWidth = stroke, cap = StrokeCap.Round)
                drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
            // Cosmic / Default Star
            else -> {
                drawCircle(color = tint, radius = w * 0.32f, center = Offset(cx, cy), style = Stroke(width = stroke))
                drawLine(color = tint, start = Offset(cx, h * 0.12f), end = Offset(cx, h * 0.88f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.12f, cy), end = Offset(w * 0.88f, cy), strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }
    }
}

/**
 * Prominent Zodiac & Origin Highlight Badge for user profiles.
 * Features glowing vector icon, sign name, element, and optional country.
 */
@Composable
fun ZodiacProfileHighlightCard(
    zodiacSign: String?,
    country: String? = null,
    birthDate: String? = null,
    modifier: Modifier = Modifier,
    onChillClick: (() -> Unit)? = null
) {
    if (zodiacSign.isNullOrBlank() && country.isNullOrBlank() && birthDate.isNullOrBlank()) {
        return
    }

    val calculatedSign = remember(zodiacSign, birthDate) {
        if (!zodiacSign.isNullOrBlank()) zodiacSign else ZodiacHelper.parseFromDateString(birthDate)
    }

    val info = remember(calculatedSign) {
        calculatedSign?.let { ZodiacHelper.getInfo(it) }
    }

    var showChillModal by remember { mutableStateOf(false) }

    Surface(
        onClick = {
            if (onChillClick != null) onChillClick() else showChillModal = true
        },
        shape = RoundedCornerShape(16.dp),
        color = (info?.primaryColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.09f),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    (info?.primaryColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.5f),
                    (info?.secondaryColor ?: MaterialTheme.colorScheme.tertiary).copy(alpha = 0.3f)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("zodiac_profile_highlight_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Jetpack Vector Icon inside Glowing Badge
            if (info != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(info.primaryColor, info.secondaryColor)
                            )
                        )
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ZodiacVectorIcon(
                        sign = info.sign,
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.White,
                        strokeWidth = 2.2.dp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (info != null) {
                        Text(
                            text = info.sign,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Element pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = info.elementColor.copy(alpha = 0.18f),
                            border = BorderStroke(0.8.dp, info.elementColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = info.element,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = info.elementColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (!country.isNullOrBlank()) {
                        if (info != null) Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = country,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Subtitle / Date range or Chill motto
                Text(
                    text = if (info != null) "${info.dateRange} • Astrovibe IDDET" else country ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }

            // Chill Button Icon
            Surface(
                shape = CircleShape,
                color = (info?.primaryColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "Zodiac Chill",
                        tint = info?.primaryColor ?: MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }

    if (showChillModal && info != null) {
        ZodiacChillDialog(
            info = info,
            country = country,
            birthDate = birthDate,
            onDismiss = { showChillModal = false }
        )
    }
}

@Composable
fun ZodiacChillDialog(
    info: ZodiacSignInfo,
    country: String?,
    birthDate: String?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, info.primaryColor.copy(alpha = 0.3f)),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("zodiac_chill_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Vector Glyph Box
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(info.primaryColor, info.secondaryColor, info.primaryColor)
                            )
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ZodiacVectorIcon(
                        sign = info.sign,
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.White,
                        strokeWidth = 3.dp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sign Name & Latin name
                Text(
                    text = info.sign,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${info.latinName} • ${info.dateRange}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Element & Planet Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = info.elementColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, info.elementColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "Élément : ${info.element}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = info.elementColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "Astre : ${info.rulingPlanet}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    if (!country.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = country,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Personality traits tags
                Text(
                    text = "Traits & Vibe",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    info.traits.forEach { trait ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 3.dp)
                        ) {
                            Text(
                                text = "✨ $trait",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Chill Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = info.primaryColor.copy(alpha = 0.08f),
                    border = BorderStroke(0.8.dp, info.primaryColor.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Spa,
                                contentDescription = null,
                                tint = info.primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Chill & Astro Vibe",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = info.primaryColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = info.chillDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = info.primaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("C'est chill !", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
