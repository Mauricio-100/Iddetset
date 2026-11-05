package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ActfileWithUser

// Official IDDET Ads gold color
val IddetAdsGold = Color(0xFFFFCC00)
val IddetAdsGoldDark = Color(0xFF1A1A1A)
val IddetAdsWarmBg = Color(0xFFFEF3C7)
val IddetAdsTextDark = Color(0xFF92400E)
val IddetAdsBorder = Color(0xFFD97706)

/**
 * Badge "Sponsorisé" officiel IDDET Ads.
 * Design épuré, ultra-compact ("plus petit" et "plus propre") placé en bas à gauche de la publication.
 */
@Composable
fun SponsoredBadge(
    modifier: Modifier = Modifier,
    isPending: Boolean = false,
    moneyText: String = "$5.00",
    onClick: (() -> Unit)? = null
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.let { it.red < 0.5f }

    val bgColor = if (isPending) {
        if (isDark) Color(0xFF261E0A) else Color(0xFFFFFBEB)
    } else {
        if (isDark) Color(0xFF2A2007) else Color(0xFFFEF3C7)
    }
    val borderColor = if (isPending) {
        IddetAdsGold.copy(alpha = 0.35f)
    } else {
        IddetAdsBorder.copy(alpha = 0.45f)
    }
    val contentColor = if (isPending) {
        if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
    } else {
        if (isDark) Color(0xFFFACC15) else Color(0xFF92400E)
    }

    Surface(
        shape = RoundedCornerShape(5.dp),
        color = bgColor,
        border = BorderStroke(0.6.dp, borderColor),
        modifier = modifier
            .testTag("sponsored_badge")
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onClick()
                        }
                    )
                } else Modifier
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Campaign,
                contentDescription = "Sponsorisé",
                tint = contentColor,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (isPending) "En attente" else "Sponsorisé",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
                color = contentColor
            )
            Spacer(modifier = Modifier.width(2.5.dp))
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.65f),
                modifier = Modifier.size(8.dp)
            )
        }
    }
}

/**
 * Dialogue complet et soigné pour créer ou gérer une campagne IDDET Ads sur un Actfile.
 */
@Composable
fun PromoteActfileDialog(
    actfile: ActfileWithUser,
    onDismiss: () -> Unit,
    onPromote: (budget: Double, currency: String, daily: Boolean, targetCountry: String?) -> Unit,
    onCancelAd: () -> Unit,
    isLoading: Boolean = false
) {
    var currency by remember { mutableStateOf("USD") }
    var selectedBudget by remember { mutableStateOf(5.0) }
    var customBudgetInput by remember { mutableStateOf("") }
    var isCustom by remember { mutableStateOf(false) }
    var isDaily by remember { mutableStateOf(true) }
    var targetCountry by remember { mutableStateOf("Tous") }
    var showExplanation by remember { mutableStateOf(false) }

    val usdPresets = listOf(2.0, 5.0, 10.0, 25.0)
    val cdfPresets = listOf(5000.0, 15000.0, 30000.0, 60000.0)

    val currentPresets = if (currency == "USD") usdPresets else cdfPresets

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, IddetAdsGold.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with Gold Ads icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(IddetAdsGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = IddetAdsGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "IDDET Ads",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (actfile.isSponsored) "Campagne publicitaire active" else "Sponsoriser votre Actfile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // If already sponsored or pending
                if (actfile.isSponsored || actfile.adStatus == "pending" || actfile.adStatus == "active") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = IddetAdsGold.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, IddetAdsGold.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = IddetAdsGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (actfile.isSponsored) "Statut : Actif dans le flux" else "Statut : En attente de diffusion",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IddetAdsGold
                                )
                            }
                            if (!actfile.adCampaignId.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Identifiant campagne : ${actfile.adCampaignId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Cette publication bénéficie actuellement d'une diffusion prioritaire avec le badge Sponsorisé.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onCancelAd,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Arrêter la sponsorisation",
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Campaign Creation Form
                    Text(
                        text = "Diffusez votre publication à un public élargi avec le badge officiel Sponsorisé dans le fil d'actualité.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Currency Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Devise du budget",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            FilterChip(
                                selected = currency == "USD",
                                onClick = {
                                    currency = "USD"
                                    isCustom = false
                                    selectedBudget = 5.0
                                },
                                label = { Text("USD ($)") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            FilterChip(
                                selected = currency == "CDF",
                                onClick = {
                                    currency = "CDF"
                                    isCustom = false
                                    selectedBudget = 15000.0
                                },
                                label = { Text("CDF (FC)") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset budget chips
                    Text(
                        text = "Montant du budget",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentPresets.forEach { amount ->
                            val label = if (currency == "USD") "$${amount.toInt()}" else "${(amount / 1000).toInt()}k FC"
                            val isSelected = !isCustom && selectedBudget == amount
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    isCustom = false
                                    selectedBudget = amount
                                },
                                label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IddetAdsGold,
                                    selectedLabelColor = IddetAdsGoldDark
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Custom budget field
                    OutlinedTextField(
                        value = if (isCustom) customBudgetInput else "",
                        onValueChange = {
                            customBudgetInput = it
                            isCustom = true
                        },
                        placeholder = { Text("Ou montant personnalisé (${if (currency == "USD") "ex: 15.00" else "ex: 25000"})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Budget Mode: Daily vs Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Type de budget",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            FilterChip(
                                selected = isDaily,
                                onClick = { isDaily = true },
                                label = { Text("Quotidien", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            FilterChip(
                                selected = !isDaily,
                                onClick = { isDaily = false },
                                label = { Text("Budget total", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Target country selection
                    Text(
                        text = "Ciblage géographique",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Tous", "RDC", "France", "Belgique").forEach { country ->
                            FilterChip(
                                selected = targetCountry == country,
                                onClick = { targetCountry = country },
                                label = { Text(country, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            val finalBudget = if (isCustom) {
                                customBudgetInput.toDoubleOrNull() ?: selectedBudget
                            } else {
                                selectedBudget
                            }
                            val resolvedTarget = if (targetCountry == "Tous") null else targetCountry
                            onPromote(finalBudget, currency, isDaily, resolvedTarget)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IddetAdsGold,
                            contentColor = IddetAdsGoldDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = IddetAdsGoldDark,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.ElectricBolt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lancer la campagne publicitaire",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
