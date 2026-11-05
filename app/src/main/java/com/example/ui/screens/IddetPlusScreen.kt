package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ui.IddetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IddetPlusScreen(viewModel: IddetViewModel, navController: NavController) {
    val context = LocalContext.current
    val status by viewModel.myIddetPlusStatus.collectAsStateWithLifecycle()
    val myCard by viewModel.myCard.collectAsStateWithLifecycle()
    val pendingRef by viewModel.pendingPaymentReference.collectAsStateWithLifecycle()
    val pendingStatus by viewModel.pendingPaymentStatus.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    var isLoading by remember { mutableStateOf(false) }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf("CDF") } // "CDF" ou "USD"
    var activeCheckoutResponse by remember { mutableStateOf<com.example.data.IddetPlusCheckoutResponse?>(null) }
    var showWidgetDialog by remember { mutableStateOf(false) }
    var previewStyle by remember { mutableStateOf(myCard?.card_style ?: "gold") }

    LaunchedEffect(Unit) {
        viewModel.loadIddetPlusData()
    }

    val isPremium = status?.is_iddet_plus == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("IDDET", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFD700),
                            contentColor = Color.Black
                        ) {
                            Text(
                                "PLUS",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.loadIddetPlusData()
                        if (pendingRef != null) {
                            viewModel.checkPaymentStatus(pendingRef!!) { st, isSuccess ->
                                if (isSuccess) {
                                    Toast.makeText(context, "🎉 Paiement confirmé ! Abonnement activé.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Statut : $st", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Statut actualisé", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Suivi du paiement en cours (si une transaction Chariow a été initiée)
            if (pendingRef != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (pendingStatus) {
                            "success" -> Color(0xFFE8F5E9)
                            "failed" -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (pendingStatus == "success") {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                                Text("Paiement Confirmé !", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            } else if (pendingStatus == "failed") {
                                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                                Text("Paiement Non Abouti", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("Paiement en attente de validation", fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "Référence : $pendingRef",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = when (pendingStatus) {
                                "success" -> "Votre abonnement Iddet Plus et vos 500 crédits sont désormais actifs. Profitez de vos styles exclusifs !"
                                "failed" -> "La transaction Chariow a été annulée ou n'a pas pu aboutir. Vous pouvez réessayer."
                                else -> "Validation Mobile Money ou Carte en cours. Si vous avez validé le paiement sur Chariow, le serveur met à jour votre compte automatiquement."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.checkPaymentStatus(pendingRef!!) { st, isSuccess ->
                                        if (isSuccess) {
                                            Toast.makeText(context, "🎉 Félicitations ! Votre abonnement est actif !", Toast.LENGTH_LONG).show()
                                        } else if (st == "failed") {
                                            Toast.makeText(context, "Le paiement n'a pas abouti.", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Paiement toujours en attente ($st)...", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Vérifier", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val widget = activeCheckoutResponse?.widget ?: com.example.data.ChariowWidgetConfig(
                                        product_id = "prd_zs6iyq84",
                                        store_domain = "xnycggrc.mychariow.market",
                                        customer_email = user?.email
                                    )
                                    activeCheckoutResponse = com.example.data.IddetPlusCheckoutResponse(
                                        reference = pendingRef,
                                        amount = if (selectedCurrency == "USD") 1.25 else 2800.0,
                                        currency = selectedCurrency,
                                        widget = widget
                                    )
                                    showWidgetDialog = true
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Widget Chariow", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.clearPendingPayment() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Fermer", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // LUXURY PROMOTIONAL HERO BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp), spotColor = Color(0xFFFF9800))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (isPremium) listOf(
                                Color(0xFF0F172A),
                                Color(0xFF1E1B4B),
                                Color(0xFF312E81)
                            ) else listOf(
                                Color(0xFF0A0A14),
                                Color(0xFF1E112A),
                                Color(0xFF2C103C)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFFFD700), Color(0xFFFF8008), Color(0xFFFFC837))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // VIP Badge Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFFD700),
                            contentColor = Color.Black
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPremium) Icons.Default.Diamond else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isPremium) "MEMBRE VIP ACTIF" else "OFFRE DE LANCEMENT EXCLUSIVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isPremium) "Bienvenue dans l'Élite IDDET" else "Sublimez Votre Profil avec Iddet Plus",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isPremium)
                            "Abonnement actif jusqu'au ${status?.expires_at?.take(10) ?: "prochain renouvellement"}"
                        else
                            "Rejoignez les créateurs d'impact. Des fonctionnalités exclusives, une vitesse d'audience décuplée et un statut remarquable.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Price Tag in Hero
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "2 800 CDF",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp,
                                        color = Color(0xFFFFD700)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "5 600 CDF",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.4f),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        )
                                    )
                                }
                                Text(
                                    text = "ou ~1.25 USD • Sans engagement",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF10B981)
                            ) {
                                Text(
                                    text = "-50%",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (!isPremium) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showCheckoutSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD700),
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Débloquer Iddet Plus Maintenant",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // LIVE CARD STYLE SHOWCASE & PREVIEW
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aperçu des Styles de Profil", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "EXCLUSIVITÉ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Choisissez l'apparence de votre profil parmi des finitions luxueuses conçues pour captiver votre communauté.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Interactive Preview Card Box
                    val cardStyleGradient = when (previewStyle) {
                        "gold" -> Brush.linearGradient(listOf(Color(0xFFB8860B), Color(0xFFFFD700), Color(0xFFFFF8DC)))
                        "diamond" -> Brush.linearGradient(listOf(Color(0xFF00B4D8), Color(0xFF90E0EF), Color(0xFFCAF0F8)))
                        "neon" -> Brush.linearGradient(listOf(Color(0xFFFF007F), Color(0xFF7928CA), Color(0xFF00DFD8)))
                        "obsidian" -> Brush.linearGradient(listOf(Color(0xFF111827), Color(0xFF1F2937), Color(0xFF374151)))
                        else -> Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF3B82F6), Color(0xFF60A5FA)))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardStyleGradient)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.25f))
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user?.username?.firstOrNull()?.toString()?.uppercase() ?: "I",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "@${user?.username ?: "mon_profil"}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "Style : ${previewStyle.replaceFirstChar { it.uppercase() }}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = "PRO+",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Style Selector Chips
                    val styles = listOf("gold", "diamond", "neon", "obsidian", "classic")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        styles.forEach { style ->
                            val isSelected = previewStyle == style
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    previewStyle = style
                                    if (isPremium) {
                                        viewModel.updateCardStyle(
                                            style = style,
                                            onSuccess = { Toast.makeText(context, "Style $style appliqué !", Toast.LENGTH_SHORT).show() },
                                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        style.replaceFirstChar { it.uppercase() },
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (isPremium) {
                        Text(
                            text = "💡 Touchez un style pour l'appliquer immédiatement à votre carte.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // COMPARATIVE MATRIX (Gratuit vs Iddet Plus)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Comparatif des Fonctionnalités",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    FeatureComparisonRow(
                        feature = "Styles de carte exclusifs (Gold, Neon...)",
                        freeVal = "Standard",
                        plusVal = "5 Thèmes VIP",
                        isHighlight = true
                    )
                    FeatureComparisonRow(
                        feature = "Crédits de création offerts",
                        freeVal = "50 / mois",
                        plusVal = "500 / mois",
                        isHighlight = true
                    )
                    FeatureComparisonRow(
                        feature = "Badge de vérification VIP",
                        freeVal = "Non",
                        plusVal = "Oui ⭐",
                        isHighlight = true
                    )
                    FeatureComparisonRow(
                        feature = "Passerelle locale Mobile Money",
                        freeVal = "—",
                        plusVal = "Inclus (CDF/USD)",
                        isHighlight = false
                    )
                    FeatureComparisonRow(
                        feature = "Support prioritaire 24/7",
                        freeVal = "Standard",
                        plusVal = "VIP Dédié",
                        isHighlight = false
                    )
                }
            }

            // TRUST & SECURITY BADGES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrustBadge(
                    icon = Icons.Default.Security,
                    title = "Paiement Sécurisé",
                    subtitle = "Passerelle Chariow cryptée",
                    modifier = Modifier.weight(1f)
                )
                TrustBadge(
                    icon = Icons.Default.Payments,
                    title = "Mobile Money",
                    subtitle = "M-Pesa, Airtel, Orange",
                    modifier = Modifier.weight(1f)
                )
                TrustBadge(
                    icon = Icons.Default.SupportAgent,
                    title = "Support Réactif",
                    subtitle = "Assistance instantanée",
                    modifier = Modifier.weight(1f)
                )
            }

            // MAIN CALL TO ACTION
            if (!isPremium) {
                Button(
                    onClick = { showCheckoutSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black
                    ),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "S'abonner à Iddet Plus (2800 CDF)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
    
    // CHECKOUT BOTTOM SHEET
    if (showCheckoutSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { if (!isLoading) showCheckoutSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Paiement Sécurisé Chariow",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Text(
                    text = "Paiement Sécurisé Chariow",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Abonnement Iddet Plus géré par la passerelle officielle du serveur (Mobile Money & Cartes).",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Choix de la devise
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Devise & Moyen de paiement", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedCurrency == "CDF",
                            onClick = { selectedCurrency = "CDF" },
                            label = { Text("CDF (2800 CDF)") },
                            leadingIcon = { if (selectedCurrency == "CDF") Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedCurrency == "USD",
                            onClick = { selectedCurrency = "USD" },
                            label = { Text("USD ($1.25)") },
                            leadingIcon = { if (selectedCurrency == "USD") Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Abonnement", fontWeight = FontWeight.Medium)
                    Text("Iddet Plus (30 jours)")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total à payer", fontWeight = FontWeight.Bold)
                    Text(if (selectedCurrency == "CDF") "2800 CDF" else "$1.25 USD", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                val parsedInitialPhone = remember(user?.phoneNumber) {
                    com.example.utils.PhoneUtils.parse(user?.phoneNumber)
                }
                var countryCodeInput by remember(user?.phoneNumber) { mutableStateOf(parsedInitialPhone.countryCode) }
                var phoneInput by remember(user?.phoneNumber) { mutableStateOf(parsedInitialPhone.number) }
                var emailInput by remember(user?.email) { mutableStateOf(user?.email ?: "") }
                var phoneError by remember { mutableStateOf<String?>(null) }
                var emailError by remember { mutableStateOf<String?>(null) }

                if (user?.email.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 L'adresse email sert d'identifiant pour la validation automatique de votre paiement Chariow.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Saisie du numéro de téléphone (Optionnel)
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Numéro Mobile Money / Téléphone (Optionnel)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = countryCodeInput,
                            onValueChange = { countryCodeInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Code") },
                            prefix = { Text("+") },
                            modifier = Modifier.width(95.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { 
                                phoneInput = it.filter { ch -> ch.isDigit() }
                                phoneError = null
                            },
                            label = { Text("Numéro") },
                            placeholder = { Text("Ex: 812345678") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = phoneError != null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (phoneError != null) {
                        Text(
                            text = phoneError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            text = "Optionnel (le widget Chariow vous demandera votre numéro si vous choisissez Mobile Money)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Saisie de l'email (exigé par le serveur Chariow)
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Adresse Email (reçu Chariow)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { 
                            emailInput = it
                            emailError = null
                        },
                        label = { Text("Email") },
                        placeholder = { Text("Ex: nom@domaine.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = emailError != null,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (emailError != null) {
                        Text(
                            text = emailError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Button(
                    onClick = {
                        if (emailInput.isBlank() || !emailInput.contains("@")) {
                            emailError = "Un email valide est requis pour payer Iddet Plus."
                            Toast.makeText(context, "Un email est requis pour payer Iddet Plus.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        phoneError = null
                        emailError = null
                        isLoading = true

                        val cleanNum = com.example.utils.PhoneUtils.cleanNumber(phoneInput)

                        viewModel.createCheckoutSession(
                            email = emailInput.trim(),
                            phoneNumber = cleanNum.ifBlank { null },
                            countryCode = countryCodeInput,
                            currency = selectedCurrency,
                            onSuccess = { response ->
                                isLoading = false
                                showCheckoutSheet = false
                                activeCheckoutResponse = response
                                showWidgetDialog = true
                                Toast.makeText(context, "Session Chariow prête ! Réf: ${response.reference ?: "IDPLUS"}", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                isLoading = false
                                if (err.contains("email", ignoreCase = true)) {
                                    emailError = err
                                }
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                if (err.contains("connecter", ignoreCase = true) || err.contains("authenticated", ignoreCase = true)) {
                                    showCheckoutSheet = false
                                    navController.navigate("auth")
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        val label = if (selectedCurrency == "CDF") "Procéder au paiement (2800 CDF)" else "Procéder au paiement ($1.25 USD)"
                        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        isLoading = true
                        viewModel.simulateGooglePlayPurchase {
                            isLoading = false
                            showCheckoutSheet = false
                            Toast.makeText(context, "Félicitations ! Votre abonnement Iddet Plus est désormais actif 🎉", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Activer via Google Play / Test direct", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showWidgetDialog && activeCheckoutResponse?.widget != null) {
        val checkout = activeCheckoutResponse!!
        com.example.ui.components.ChariowWidgetDialog(
            widgetConfig = checkout.widget!!,
            reference = checkout.reference,
            amount = checkout.amount,
            currency = checkout.currency ?: selectedCurrency,
            onDismiss = { showWidgetDialog = false },
            onVerifyStatus = {
                val ref = checkout.reference
                if (ref != null) {
                    viewModel.checkPaymentStatus(ref) { st, isSuccess ->
                        if (isSuccess) {
                            showWidgetDialog = false
                            Toast.makeText(context, "🎉 Paiement confirmé ! Votre statut Iddet Plus est actif.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Statut : $st (en cours de confirmation par le webhook Chariow)", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onOpenExternal = { url ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Impossible d'ouvrir le navigateur", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun BenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(text, fontSize = 14.sp)
    }
}

@Composable
fun FeatureComparisonRow(
    feature: String,
    freeVal: String,
    plusVal: String,
    isHighlight: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            modifier = Modifier.weight(1.3f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = freeVal,
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Text(
            text = plusVal,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun TrustBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
        }
    }
}

    
