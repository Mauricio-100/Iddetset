package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.ActfileWithUser
import com.example.ui.IddetViewModel
import com.example.ui.components.IddetAdsGold
import com.example.ui.components.PromoteActfileDialog
import com.example.ui.components.SponsoredBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IddetAdsManagerScreen(
    viewModel: IddetViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allActfiles by viewModel.actfiles.collectAsStateWithLifecycle()

    val currentUserId = currentUser?.id ?: ""
    val myActfiles = remember(allActfiles, currentUserId) {
        allActfiles.filter { it.userId == currentUserId }
    }
    val activeCampaigns = remember(myActfiles) {
        myActfiles.filter { it.isSponsored || it.adStatus in listOf("active", "pending") }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Campagnes Actives", "Mes Publications", "Guide IDDET Ads")

    var actfileToPromote by remember { mutableStateOf<ActfileWithUser?>(null) }
    var isLoadingAdAction by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(IddetAdsGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = IddetAdsGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "IDDET Ads Manager",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Sponsorisation & Publicité",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("ads_manager_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    SponsoredBadge()
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Metrics Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    IddetAdsGold.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Visibilité & Publicité",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Booster vos Actfiles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = IddetAdsGold.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, IddetAdsGold)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = IddetAdsGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+50 000 pts",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = IddetAdsGold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Metric 1: Active Ads
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${activeCampaigns.size}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = if (activeCampaigns.isNotEmpty()) IddetAdsGold else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Campagnes actives",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Metric 2: Total publications
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${myActfiles.size}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Mes Actfiles",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Metric 3: Supported Currencies
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "USD / CDF",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Devises gérées",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Tab bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // Content per tab
            when (selectedTab) {
                0 -> {
                    // Active Campaigns
                    if (activeCampaigns.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(IddetAdsGold.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Campaign,
                                        contentDescription = null,
                                        tint = IddetAdsGold,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Aucune campagne active",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Sponsorisez l'une de vos publications pour qu'elle soit mise en avant dans le flux avec le badge SPONSORISÉ.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { selectedTab = 1 },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = IddetAdsGold,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Choisir une publication à sponsoriser", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(activeCampaigns, key = { it.id }) { actfile ->
                                AdCampaignItemCard(
                                    actfile = actfile,
                                    onManage = { actfileToPromote = actfile },
                                    onCancel = {
                                        isLoadingAdAction = true
                                        viewModel.cancelActfileAd(actfile.id) { success ->
                                            isLoadingAdAction = false
                                            if (success) {
                                                Toast.makeText(context, "Campagne annulée", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Erreur lors de l'annulation", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // All User Actfiles
                    if (myActfiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Article,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Vous n'avez pas encore publié d'Actfile",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Publiez un premier Actfile depuis le menu d'accueil avant de pouvoir lancer une campagne Ads.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(myActfiles, key = { it.id }) { actfile ->
                                ActfileAdPromotionRow(
                                    actfile = actfile,
                                    onPromoteClick = { actfileToPromote = actfile }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Educational Guide
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            AdsGuideCard(
                                icon = Icons.Default.Verified,
                                title = "Badge Doré Officiel",
                                description = "Vos Actfiles sponsorisés arborent le badge distinctif doré « SPONSORISÉ » et un liseré lumineux pour capter immédiatement l'attention des utilisateurs.",
                                color = IddetAdsGold
                            )
                        }
                        item {
                            AdsGuideCard(
                                icon = Icons.Default.RocketLaunch,
                                title = "Boost Algorithmique Prioritaire",
                                description = "Un boost massif de +50 000 points est injecté dans le calcul de pertinence du flux « Pour Toi », garantissant une diffusion en tête de liste.",
                                color = Color(0xFF10B981)
                            )
                        }
                        item {
                            AdsGuideCard(
                                icon = Icons.Default.CurrencyExchange,
                                title = "Paiement en USD ou CDF",
                                description = "IDDET Ads s'adapte à votre marché avec une tarification en Franc Congolais (CDF) ou en Dollars Américains (USD), en mode quotidien ou budget global.",
                                color = Color(0xFF3B82F6)
                            )
                        }
                        item {
                            AdsGuideCard(
                                icon = Icons.Default.Public,
                                title = "Ciblage Géographique Précis",
                                description = "Ciblez votre audience locale en République Démocratique du Congo (RDC), en France, en Belgique ou optez pour une visibilité internationale globale.",
                                color = Color(0xFF8B5CF6)
                            )
                        }
                        item {
                            AdsGuideCard(
                                icon = Icons.Default.CheckCircle,
                                title = "Gestion Libre & Sans Engagement",
                                description = "Arrêtez, modifiez ou relancez vos campagnes à tout moment d'un simple clic sans frais cachés.",
                                color = Color(0xFFEC4899)
                            )
                        }
                    }
                }
            }
        }

        // Promotion / Ad Campaign Dialog
        actfileToPromote?.let { targetActfile ->
            PromoteActfileDialog(
                actfile = targetActfile,
                isLoading = isLoadingAdAction,
                onDismiss = { actfileToPromote = null },
                onPromote = { budget, currency, daily, targetCountry ->
                    isLoadingAdAction = true
                    viewModel.promoteActfile(
                        actfileId = targetActfile.id,
                        budget = budget,
                        currency = currency,
                        daily = daily,
                        targetCountry = targetCountry,
                        onSuccess = { checkoutUrl ->
                            isLoadingAdAction = false
                            actfileToPromote = null
                            Toast.makeText(context, "Campagne IDDET Ads lancée avec succès !", Toast.LENGTH_SHORT).show()
                            if (!checkoutUrl.isNullOrBlank()) {
                                val encoded = java.net.URLEncoder.encode(checkoutUrl, "UTF-8")
                                navController.navigate("browser/$encoded")
                            }
                        },
                        onError = { err ->
                            isLoadingAdAction = false
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onCancelAd = {
                    isLoadingAdAction = true
                    viewModel.cancelActfileAd(targetActfile.id) { success ->
                        isLoadingAdAction = false
                        actfileToPromote = null
                        if (success) {
                            Toast.makeText(context, "Sponsorisation arrêtée avec succès", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Impossible d'annuler la sponsorisation", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AdCampaignItemCard(
    actfile: ActfileWithUser,
    onManage: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, IddetAdsGold.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SponsoredBadge()
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Boost +50k pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = IddetAdsGold,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = actfile.content.take(120),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "❤️ ${actfile.likesCount} likes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "💬 ${actfile.commentsCount} comm.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "👁️ ${actfile.viewsCount} vues",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Arrêter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onManage,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IddetAdsGold,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Gérer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ActfileAdPromotionRow(
    actfile: ActfileWithUser,
    onPromoteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (actfile.isSponsored) IddetAdsGold.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (actfile.isSponsored) {
                    SponsoredBadge()
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = (actfile.category ?: "").ifBlank { "Standard" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Button(
                    onClick = onPromoteClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actfile.isSponsored) MaterialTheme.colorScheme.surfaceVariant else IddetAdsGold,
                        contentColor = if (actfile.isSponsored) MaterialTheme.colorScheme.onSurfaceVariant else Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (actfile.isSponsored) "Gérer l'annonce" else "Sponsoriser",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = actfile.content.take(140),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AdsGuideCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
