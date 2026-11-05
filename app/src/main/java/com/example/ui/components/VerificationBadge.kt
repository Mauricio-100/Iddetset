package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Shape creating a spiked / scalloped starburst seal badge ("épines").
 */
class StarburstBadgeShape(
    private val spikes: Int = 12,
    private val innerRadiusRatio: Float = 0.82f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val outerRadius = minOf(centerX, centerY)
        val innerRadius = outerRadius * innerRadiusRatio
        val totalPoints = spikes * 2
        val angleStep = (2 * PI / totalPoints).toFloat()

        for (i in 0 until totalPoints) {
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val angle = i * angleStep - (PI / 2).toFloat() // Start pointing top
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}

// Verification state representing different levels of verification
enum class VerificationState {
    OFFICIAL, // Admin / Founder / Official (Badge Vert à épines avec coche blanche)
    VERIFIED, // Regular Verified User (Badge Bleu à épines avec coche blanche)
    IDDET, // Iddet Official Account (Badge Jaune à épines avec coche blanche)
    COMMUNITY, // Verified Community (Badge Blanc/Argenté à épines avec coche sombre)
    NONE
}

// Special usernames that get the Official/Admin Green Badge
private val FOUNDER_USERNAMES = listOf("C.M.O", "Doffranel", "doffranel", "Crislem", "Mauricio-100", "admin")

/**
 * Resolves the verification state based on username, verification flag, and community flag.
 */
fun getVerificationState(userName: String?, isVerified: Boolean, isCommunity: Boolean = false): VerificationState {
    val name = userName ?: ""
    return when {
        isCommunity && isVerified -> VerificationState.COMMUNITY
        com.example.data.IddetAccountManager.isOfficialIddetAccount(name) -> VerificationState.IDDET
        FOUNDER_USERNAMES.any { it.equals(name, ignoreCase = true) } -> VerificationState.OFFICIAL
        isVerified -> VerificationState.VERIFIED
        else -> VerificationState.NONE
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationBadge(
    modifier: Modifier = Modifier,
    userName: String? = null,
    isVerified: Boolean = false,
    isCommunity: Boolean = false,
    state: VerificationState? = null, // Can be explicitly passed
    showExplainingOnClick: Boolean = true
) {
    val resolvedState = state ?: getVerificationState(userName, isVerified, isCommunity)
    if (resolvedState == VerificationState.NONE) return

    var showBottomSheet by remember { mutableStateOf(false) }
    val name = userName ?: ""

    // Badge Colors:
    // Official/Admin: Green (0xFF16A34A) with white checkmark
    // Verified User: Blue (0xFF1DA1F2) with white checkmark
    // Iddet Official Account: Yellow (0xFFEAB308) with white checkmark
    // Community Verified: White/Silver (0xFFE2E8F0) with dark navy checkmark
    val badgeColor = when (resolvedState) {
        VerificationState.OFFICIAL -> Color(0xFF16A34A)
        VerificationState.VERIFIED -> Color(0xFF1DA1F2)
        VerificationState.IDDET -> Color(0xFFEAB308)
        VerificationState.COMMUNITY -> Color(0xFFE2E8F0)
        VerificationState.NONE -> Color.Transparent
    }

    val checkColor = when (resolvedState) {
        VerificationState.COMMUNITY -> Color(0xFF0F172A)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(enabled = showExplainingOnClick) { showBottomSheet = true }
            .testTag("verification_badge_click_area_$name"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = modifier
                .background(badgeColor, StarburstBadgeShape(spikes = 12, innerRadiusRatio = 0.82f))
                .testTag("verification_badge_${resolvedState.name.lowercase()}_$name"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (resolvedState == VerificationState.COMMUNITY) "Communauté vérifiée" else "Badge vérifié",
                tint = checkColor,
                modifier = Modifier.size(11.dp)
            )
        }
    }

    if (showBottomSheet) {
        VerificationBottomSheet(
            userName = name,
            verificationState = resolvedState,
            onDismiss = { showBottomSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationBottomSheet(
    userName: String,
    verificationState: VerificationState,
    onDismiss: () -> Unit
) {
    if (verificationState == VerificationState.NONE) return

    val badgeColor = when (verificationState) {
        VerificationState.OFFICIAL -> Color(0xFF16A34A)
        VerificationState.IDDET -> Color(0xFFEAB308)
        VerificationState.COMMUNITY -> Color(0xFFE2E8F0)
        else -> Color(0xFF1DA1F2)
    }

    val checkColor = when (verificationState) {
        VerificationState.COMMUNITY -> Color(0xFF0F172A)
        else -> Color.White
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("verification_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Icon / Badge representation with spikes
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = badgeColor,
                        shape = StarburstBadgeShape(spikes = 14, innerRadiusRatio = 0.82f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = checkColor,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = when (verificationState) {
                    VerificationState.OFFICIAL -> "Compte Officiel / Admin ($userName)"
                    VerificationState.IDDET -> "Compte Officiel Iddet"
                    VerificationState.COMMUNITY -> "Communauté Vérifiée ($userName)"
                    else -> "Compte Vérifié"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Body Description text
            Text(
                text = when (verificationState) {
                    VerificationState.OFFICIAL -> "Ce badge vert à épines avec coche blanche distingue les administrateurs, fondateurs et membres officiels de la plateforme."
                    VerificationState.IDDET -> "Ce badge jaune à épines identifie le compte officiel du réseau Iddet."
                    VerificationState.COMMUNITY -> "Ce badge blanc/argenté à épines atteste qu'il s'agit d'une communauté officielle ou vérifiée sur IDDET."
                    else -> "Ce badge bleu à épines avec coche blanche atteste de l'authenticité de ce profil vérifié."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Close button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("verification_sheet_close_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Compris",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
