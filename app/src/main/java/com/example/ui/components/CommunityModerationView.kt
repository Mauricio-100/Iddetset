package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Community
import com.example.data.CommunityBot
import com.example.data.CommunityModAction
import com.example.ui.IddetViewModel

@Composable
fun CommunityModerationTabContent(
    community: Community,
    viewModel: IddetViewModel,
    onShowSnackbar: (String) -> Unit
) {
    val bots by viewModel.communityBots.collectAsStateWithLifecycle()
    val modActions by viewModel.communityModActions.collectAsStateWithLifecycle()
    val isBotsLoading by viewModel.isBotsLoading.collectAsStateWithLifecycle()
    val isModActionsLoading by viewModel.isModActionsLoading.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableStateOf(0) } // 0: Bots, 1: Audit Log, 2: Membres & Sanctions

    var showCreateBotDialog by remember { mutableStateOf(false) }
    var createdBotTokenToDisplay by remember { mutableStateOf<Pair<String, String>?>(null) } // Name, Token
    var botToEdit by remember { mutableStateOf<CommunityBot?>(null) }
    var botToAction by remember { mutableStateOf<CommunityBot?>(null) }
    var showBanUserDialog by remember { mutableStateOf(false) }
    var showUpdateRoleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(community.slug) {
        viewModel.loadCommunityBots(community.slug)
        viewModel.loadCommunityModActions(community.slug)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Moderation Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Centre de Modération & Bots",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "c/${community.slug} • Gestion automatisée et sanctions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ModerationStatItem(
                        label = "Bots Actifs",
                        count = "${bots.count { it.isActive }}/${bots.size}",
                        icon = Icons.Default.SmartToy
                    )
                    ModerationStatItem(
                        label = "Actions Journal",
                        count = "${modActions.size}",
                        icon = Icons.Default.History
                    )
                    ModerationStatItem(
                        label = "Membres",
                        count = "${community.membersCount}",
                        icon = Icons.Default.Group
                    )
                }
            }
        }

        // Sub-navigation bar
        SingleChoiceSegmentedRow(
            selectedIndex = activeSubTab,
            options = listOf("Bots (${bots.size})", "Audit (${modActions.size})", "Sanctions"),
            onSelect = { activeSubTab = it }
        )

        // Sub-tab content
        when (activeSubTab) {
            0 -> {
                // BOTS VIEW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BOTS DE MODÉRATION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FilledTonalButton(
                        onClick = { showCreateBotDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nouveau Bot", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }

                if (isBotsLoading && bots.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else if (bots.isEmpty()) {
                    EmptyModerationState(
                        icon = Icons.Default.SmartToy,
                        title = "Aucun bot configuré",
                        description = "Créez un bot de modération pour filtrer automatiquement les insultes, le spam et accueillir les membres.",
                        actionLabel = "Créer un premier bot",
                        onAction = { showCreateBotDialog = true }
                    )
                } else {
                    bots.forEach { bot ->
                        BotCardItem(
                            bot = bot,
                            onToggleActive = { isActive ->
                                viewModel.updateCommunityBot(
                                    slug = community.slug,
                                    botId = bot.id,
                                    isActive = isActive,
                                    onSuccess = {
                                        onShowSnackbar(if (isActive) "Bot activé" else "Bot désactivé")
                                    },
                                    onError = { err -> onShowSnackbar(err) }
                                )
                            },
                            onEdit = { botToEdit = bot },
                            onTestAction = { botToAction = bot },
                            onDelete = {
                                viewModel.deleteCommunityBot(
                                    slug = community.slug,
                                    botId = bot.id,
                                    onSuccess = { onShowSnackbar("Bot ${bot.name} supprimé") },
                                    onError = { err -> onShowSnackbar(err) }
                                )
                            }
                        )
                    }
                }
            }

            1 -> {
                // AUDIT LOG VIEW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JOURNAL D'AUDIT DE MODÉRATION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { viewModel.loadCommunityModActions(community.slug) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser le journal", modifier = Modifier.size(18.dp))
                    }
                }

                if (isModActionsLoading && modActions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else if (modActions.isEmpty()) {
                    EmptyModerationState(
                        icon = Icons.Default.VerifiedUser,
                        title = "Journal vierge",
                        description = "Aucune action de modération n'a encore été enregistrée pour cette communauté.",
                        actionLabel = null,
                        onAction = null
                    )
                } else {
                    modActions.forEach { action ->
                        ModActionLogCard(action = action)
                    }
                }
            }

            2 -> {
                // SANCTIONS & ROLES VIEW
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "GESTION DES MEMBRES & SANCTIONS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Card 1: Ban user
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Column {
                                    Text("Bannir un utilisateur", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text("Empêche un membre de publier ou d'interagir", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Button(
                                onClick = { showBanUserDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bannir un membre")
                            }
                        }
                    }

                    // Card 2: Manage Roles
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Attribuer un rôle", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text("Nommer un modérateur ou un administrateur", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            OutlinedButton(
                                onClick = { showUpdateRoleDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Modifier le rôle d'un membre")
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOGS

    // 1. Create Bot Dialog
    if (showCreateBotDialog) {
        CreateBotDialog(
            onDismiss = { showCreateBotDialog = false },
            onConfirm = { name, words, autoBan, welcome ->
                showCreateBotDialog = false
                viewModel.createCommunityBot(
                    slug = community.slug,
                    name = name,
                    bannedWords = words,
                    autoBanThreshold = autoBan,
                    welcomeMessage = welcome,
                    onSuccess = { bot ->
                        onShowSnackbar("Bot ${bot.name} créé avec succès !")
                        if (!bot.token.isNullOrBlank()) {
                            createdBotTokenToDisplay = Pair(bot.name, bot.token)
                        }
                    },
                    onError = { err -> onShowSnackbar(err) }
                )
            }
        )
    }

    // 2. Token Reveal Dialog
    createdBotTokenToDisplay?.let { (botName, token) ->
        val clipboardManager = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { createdBotTokenToDisplay = null },
            icon = { Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Token du bot : $botName", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Ce token d'API est affiché une seule fois. Copiez-le et conservez-le en lieu sûr pour vos intégrations de modération automatique ou webhooks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = token,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(token))
                        onShowSnackbar("Token copié dans le presse-papier !")
                        createdBotTokenToDisplay = null
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copier le token")
                }
            },
            dismissButton = {
                TextButton(onClick = { createdBotTokenToDisplay = null }) {
                    Text("Fermer")
                }
            }
        )
    }

    // 3. Edit Bot Dialog
    botToEdit?.let { bot ->
        EditBotDialog(
            bot = bot,
            onDismiss = { botToEdit = null },
            onConfirm = { name, words, autoBan, welcome ->
                botToEdit = null
                viewModel.updateCommunityBot(
                    slug = community.slug,
                    botId = bot.id,
                    name = name,
                    bannedWords = words,
                    autoBanThreshold = autoBan,
                    welcomeMessage = welcome,
                    onSuccess = { onShowSnackbar("Configuration du bot mise à jour !") },
                    onError = { err -> onShowSnackbar(err) }
                )
            }
        )
    }

    // 4. Test Bot Action Dialog
    botToAction?.let { bot ->
        TestBotActionDialog(
            bot = bot,
            onDismiss = { botToAction = null },
            onConfirm = { actionType, targetUserId, targetActfileId, reason ->
                botToAction = null
                viewModel.performBotModAction(
                    slug = community.slug,
                    botId = bot.id,
                    botToken = bot.token,
                    actionType = actionType,
                    targetUserId = targetUserId,
                    targetActfileId = targetActfileId,
                    reason = reason,
                    onSuccess = { onShowSnackbar("Action '$actionType' exécutée avec succès !") },
                    onError = { err -> onShowSnackbar(err) }
                )
            }
        )
    }

    // 5. Ban User Dialog
    if (showBanUserDialog) {
        BanUserDialog(
            onDismiss = { showBanUserDialog = false },
            onConfirm = { userId, reason ->
                showBanUserDialog = false
                viewModel.banCommunityMember(
                    slug = community.slug,
                    userId = userId,
                    reason = reason
                ) { success ->
                    if (success) {
                        onShowSnackbar("L'utilisateur a été banni de la communauté.")
                    } else {
                        onShowSnackbar("Échec du bannissement.")
                    }
                }
            }
        )
    }

    // 6. Update Role Dialog
    if (showUpdateRoleDialog) {
        UpdateRoleDialog(
            onDismiss = { showUpdateRoleDialog = false },
            onConfirm = { userId, role ->
                showUpdateRoleDialog = false
                viewModel.updateMemberRole(
                    slug = community.slug,
                    userId = userId,
                    role = role
                ) { success ->
                    if (success) {
                        onShowSnackbar("Rôle de l'utilisateur mis à jour : $role")
                    } else {
                        onShowSnackbar("Échec de la mise à jour du rôle.")
                    }
                }
            }
        )
    }
}

@Composable
fun ModerationStatItem(label: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = count, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SingleChoiceSegmentedRow(
    selectedIndex: Int,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, title ->
            val selected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.surface
                        else Color.Transparent
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BotCardItem(
    bot: CommunityBot,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onTestAction: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (bot.isActive) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = if (bot.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = bot.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (bot.isActive) Color(0xFF10B981).copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (bot.isActive) "ACTIF" else "INACTIF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (bot.isActive) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        CopyableUserId(
                            id = bot.id,
                            isBot = true,
                            fontSize = 10.sp,
                            iconSize = 11.dp
                        )
                        if (bot.autoBanThreshold > 0) {
                            Text(
                                text = "Auto-ban après ${bot.autoBanThreshold} infractions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = "Pas de ban automatique",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Switch(
                    checked = bot.isActive,
                    onCheckedChange = onToggleActive,
                    modifier = Modifier.testTag("bot_toggle_${bot.id}")
                )
            }

            // Banned words chips
            if (bot.bannedWords.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Mots interdits (${bot.bannedWords.size}) :",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(bot.bannedWords) { word ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = word,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Welcome message preview
            if (!bot.welcomeMessage.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.WavingHand,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = bot.welcomeMessage,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action buttons
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onTestAction,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Actionner", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifier le bot", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Supprimer le bot",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModActionLogCard(action: CommunityModAction) {
    val (badgeColor, textColor, icon) = when (action.actionType.lowercase()) {
        "ban" -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, Icons.Default.Block)
        "delete_post" -> Triple(Color(0xFFFFE0B2), Color(0xFFE65100), Icons.Default.Delete)
        "mute" -> Triple(Color(0xFFFFF9C4), Color(0xFFF57F17), Icons.AutoMirrored.Filled.VolumeMute)
        "warn" -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, Icons.Default.Warning)
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Gavel)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(12.dp))
                            Text(
                                text = action.actionType.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }

                    if (!action.actorType.isNullOrBlank()) {
                        Text(
                            text = "par ${if (action.actorType == "bot") "Bot 🤖" else "Modérateur 🛡️"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!action.createdAt.isNullOrBlank()) {
                    Text(
                        text = action.createdAt.take(16).replace("T", " "),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (!action.targetUserId.isNullOrBlank()) {
                Text(
                    text = "Cible : ${action.targetUserId}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            if (!action.targetActfileId.isNullOrBlank()) {
                Text(
                    text = "Publication : #${action.targetActfileId.take(12)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!action.reason.isNullOrBlank()) {
                Text(
                    text = "Raison : \"${action.reason}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyModerationState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onAction, shape = RoundedCornerShape(10.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOGS
// -------------------------------------------------------------

@Composable
fun CreateBotDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, bannedWords: List<String>, autoBanThreshold: Int, welcomeMessage: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var currentWord by remember { mutableStateOf("") }
    val bannedWords = remember { mutableStateListOf<String>() }
    var autoBanThreshold by remember { mutableFloatStateOf(0f) }
    var welcomeMessage by remember { mutableStateOf("") }

    val quickWords = listOf("spam", "arnaque", "insulte", "crypto", "pub")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer un Bot de Modération", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du Bot *") },
                    placeholder = { Text("ex: GuardianBot") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Banned words input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Mots interdits à filtrer :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = currentWord,
                            onValueChange = { currentWord = it },
                            placeholder = { Text("Ajouter un mot") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                val trimmed = currentWord.trim().lowercase()
                                if (trimmed.isNotEmpty() && !bannedWords.contains(trimmed)) {
                                    bannedWords.add(trimmed)
                                    currentWord = ""
                                }
                            })
                        )
                        IconButton(
                            onClick = {
                                val trimmed = currentWord.trim().lowercase()
                                if (trimmed.isNotEmpty() && !bannedWords.contains(trimmed)) {
                                    bannedWords.add(trimmed)
                                    currentWord = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Ajouter", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Quick suggestions
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(quickWords) { w ->
                            SuggestionChip(
                                onClick = { if (!bannedWords.contains(w)) bannedWords.add(w) },
                                label = { Text("+$w", fontSize = 11.sp) }
                            )
                        }
                    }

                    // Added words chips
                    if (bannedWords.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(bannedWords) { word ->
                                InputChip(
                                    selected = false,
                                    onClick = { bannedWords.remove(word) },
                                    label = { Text(word) },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }
                    }
                }

                // Auto ban threshold slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Seuil d'auto-bannissement :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (autoBanThreshold.toInt() == 0) "Désactivé" else "${autoBanThreshold.toInt()} avertissements",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (autoBanThreshold.toInt() == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = autoBanThreshold,
                        onValueChange = { autoBanThreshold = it },
                        valueRange = 0f..10f,
                        steps = 9
                    )
                }

                // Welcome message
                OutlinedTextField(
                    value = welcomeMessage,
                    onValueChange = { welcomeMessage = it },
                    label = { Text("Message d'accueil (optionnel)") },
                    placeholder = { Text("Bienvenue dans la communauté !") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            bannedWords.toList(),
                            autoBanThreshold.toInt(),
                            welcomeMessage.ifBlank { null }
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun EditBotDialog(
    bot: CommunityBot,
    onDismiss: () -> Unit,
    onConfirm: (name: String, bannedWords: List<String>, autoBanThreshold: Int, welcomeMessage: String?) -> Unit
) {
    var name by remember { mutableStateOf(bot.name) }
    var currentWord by remember { mutableStateOf("") }
    val bannedWords = remember { mutableStateListOf<String>().apply { addAll(bot.bannedWords) } }
    var autoBanThreshold by remember { mutableFloatStateOf(bot.autoBanThreshold.toFloat()) }
    var welcomeMessage by remember { mutableStateOf(bot.welcomeMessage ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier ${bot.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du Bot *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Banned words
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mots interdits :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = currentWord,
                            onValueChange = { currentWord = it },
                            placeholder = { Text("Ajouter un mot") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val trimmed = currentWord.trim().lowercase()
                            if (trimmed.isNotEmpty() && !bannedWords.contains(trimmed)) {
                                bannedWords.add(trimmed)
                                currentWord = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Ajouter", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (bannedWords.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(bannedWords) { word ->
                                InputChip(
                                    selected = false,
                                    onClick = { bannedWords.remove(word) },
                                    label = { Text(word) },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }
                    }
                }

                // Threshold
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Seuil d'auto-bannissement :", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (autoBanThreshold.toInt() == 0) "Désactivé" else "${autoBanThreshold.toInt()} infractions",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = autoBanThreshold,
                        onValueChange = { autoBanThreshold = it },
                        valueRange = 0f..10f,
                        steps = 9
                    )
                }

                OutlinedTextField(
                    value = welcomeMessage,
                    onValueChange = { welcomeMessage = it },
                    label = { Text("Message d'accueil") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), bannedWords.toList(), autoBanThreshold.toInt(), welcomeMessage.ifBlank { null })
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun TestBotActionDialog(
    bot: CommunityBot,
    onDismiss: () -> Unit,
    onConfirm: (actionType: String, targetUserId: String?, targetActfileId: String?, reason: String?) -> Unit
) {
    val actionTypes = listOf("warn", "mute", "delete_post", "ban")
    var selectedAction by remember { mutableStateOf("warn") }
    var targetUserId by remember { mutableStateOf("") }
    var targetActfileId by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Actionner ${bot.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Exécutez une action de modération manuelle ou simulez le déclenchement du bot :",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Segmented actions
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    actionTypes.forEach { type ->
                        val isSelected = selectedAction == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAction = type },
                            label = { Text(type.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                OutlinedTextField(
                    value = targetUserId,
                    onValueChange = { targetUserId = it },
                    label = { Text("ID Utilisateur cible (optionnel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedAction == "delete_post") {
                    OutlinedTextField(
                        value = targetActfileId,
                        onValueChange = { targetActfileId = it },
                        label = { Text("ID Publication cible *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Raison de l'action") },
                    placeholder = { Text("ex: Langage inapproprié") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedAction,
                        targetUserId.ifBlank { null },
                        targetActfileId.ifBlank { null },
                        reason.ifBlank { null }
                    )
                }
            ) {
                Text("Exécuter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun BanUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (userId: String, reason: String?) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Bannir un Membre", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "L'utilisateur banni sera exclu de la communauté et ne pourra plus y interagir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("ID de l'utilisateur *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Raison du bannissement (optionnel)") },
                    placeholder = { Text("ex: Non respect des règles de la communauté") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (userId.isNotBlank()) onConfirm(userId.trim(), reason.ifBlank { null }) },
                enabled = userId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirmer le ban")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun UpdateRoleDialog(
    onDismiss: () -> Unit,
    onConfirm: (userId: String, role: String) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("moderator") }
    val roles = listOf("member" to "Membre ⭐", "moderator" to "Modérateur 🛡️", "admin" to "Administrateur 👑")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le rôle d'un membre", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("ID de l'utilisateur *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Sélectionner le rôle :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                roles.forEach { (roleKey, roleLabel) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedRole == roleKey) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { selectedRole = roleKey }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRole == roleKey,
                            onClick = { selectedRole = roleKey }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = roleLabel,
                            fontWeight = if (selectedRole == roleKey) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (userId.isNotBlank()) onConfirm(userId.trim(), selectedRole) },
                enabled = userId.isNotBlank()
            ) {
                Text("Mettre à jour")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
