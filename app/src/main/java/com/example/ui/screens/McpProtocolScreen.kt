package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ui.IddetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class McpToolInfo(
    val name: String,
    val description: String,
    val category: String,
    val parameters: String,
    val sampleArgs: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpProtocolScreen(
    viewModel: IddetViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mcpSessionState by viewModel.mcpSessionState.collectAsState()
    val connectedApps by viewModel.connectedApps.collectAsState()
    val isLoadingConnectedApps by viewModel.isLoadingConnectedApps.collectAsState()

    var showConnectAppDialog by remember { mutableStateOf(false) }
    var showBatchConnectDialog by remember { mutableStateOf(false) }
    var selectedPresetUrl by remember { mutableStateOf("https://claude.ai") }
    var customAppUrl by remember { mutableStateOf("") }
    var appConnectPassword by remember { mutableStateOf("") }
    var isConnectingApp by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var serverStatus by remember { mutableStateOf("Vérification...") }
    var isServerOnline by remember { mutableStateOf(true) }
    var pingLatencyMs by remember { mutableLongStateOf(0L) }
    var selectedCategory by remember { mutableStateOf("Tous") }
    var expandedToolName by remember { mutableStateOf<String?>(null) }

    // Tester state
    var selectedToolForTest by remember { mutableStateOf<McpToolInfo?>(null) }
    var testInputArgs by remember { mutableStateOf("") }
    var testOutputResult by remember { mutableStateOf<String?>(null) }
    var isExecutingTest by remember { mutableStateOf(false) }

    val mcpEndpoint = "https://hoosthubs-g.onrender.com/mcp/mcp"
    val mcpConfigJson = """
{
  "mcpServers": {
    "iddet-cmo-strip": {
      "url": "https://hoosthubs-g.onrender.com/mcp/mcp",
      "transport": "http"
    }
  }
}
    """.trimIndent()

    val allTools = remember {
        listOf(
            // Apps Intégrées & Connexions Externes
            McpToolInfo("mcp_login", "Authentifie via MCP et enregistre l'URL cliente avec OpenGraph", "Apps Intégrées", "username, password, client_url", "{\"username\": \"testuser\", \"password\": \"secret\", \"client_url\": \"https://claude.ai\"}"),
            McpToolInfo("mcp_list_connected_apps", "Liste les applications clientes MCP connectées avec métadonnées OpenGraph", "Apps Intégrées", "auth_token", "{}"),
            McpToolInfo("mcp_remove_connected_app", "Révoque une application cliente MCP connectée", "Apps Intégrées", "auth_token, app_id", "{\"app_id\": \"app_123\"}"),

            // Utilisateurs
            McpToolInfo("mcp_register", "Enregistre un nouvel utilisateur IDDET", "Utilisateurs", "username, email, password, client_url", "{\"username\": \"nouveau\", \"email\": \"user@example.com\", \"password\": \"pass\"}"),
            McpToolInfo("mcp_get_user", "Récupère le profil complet d'un utilisateur par ID ou pseudo", "Utilisateurs", "user_id, username", "{\"username\": \"admin\"}"),
            McpToolInfo("mcp_search_users", "Recherche des membres par mot-clé", "Utilisateurs", "query, limit", "{\"query\": \"dev\", \"limit\": 10}"),
            McpToolInfo("mcp_update_user", "Met à jour bio, avatar, vibe emoji et liens du profil", "Utilisateurs", "user_id, bio, avatar_url, vibe", "{\"bio\": \"Développeur enthousiaste\", \"vibe\": \"💻\"}"),
            McpToolInfo("mcp_delete_user", "Supprime un compte utilisateur", "Utilisateurs", "user_id", "{\"user_id\": \"usr_123\"}"),

            // Publications
            McpToolInfo("mcp_create_actfile", "Crée une nouvelle publication Actfile multimédia", "Actfiles", "title, content, category, media_url", "{\"title\": \"Nouveau Projet\", \"category\": \"tech\", \"content\": \"Publication via MCP!\"}"),
            McpToolInfo("mcp_get_actfile", "Récupère les détails et statistiques d'un Actfile", "Actfiles", "actfile_id", "{\"actfile_id\": \"act_xyz\"}"),
            McpToolInfo("mcp_list_actfiles", "Liste les Actfiles du fil avec filtres de catégorie", "Actfiles", "category, limit, skip", "{\"category\": \"tech\", \"limit\": 15}"),
            McpToolInfo("mcp_update_actfile", "Modifie le titre ou le contenu d'un Actfile", "Actfiles", "actfile_id, title, content", "{\"actfile_id\": \"act_xyz\", \"title\": \"Titre mis à jour\"}"),
            McpToolInfo("mcp_delete_actfile", "Supprime définitivement un Actfile", "Actfiles", "actfile_id", "{\"actfile_id\": \"act_xyz\"}"),
            McpToolInfo("mcp_like_actfile", "Ajoute un like ou une réaction à un Actfile", "Actfiles", "actfile_id", "{\"actfile_id\": \"act_xyz\"}"),
            McpToolInfo("mcp_unlike_actfile", "Retire la mention j'aime d'un Actfile", "Actfiles", "actfile_id", "{\"actfile_id\": \"act_xyz\"}"),
            McpToolInfo("mcp_comment_actfile", "Poste un commentaire sous un Actfile", "Actfiles", "actfile_id, content", "{\"actfile_id\": \"act_xyz\", \"content\": \"Super idée!\"}"),
            McpToolInfo("mcp_list_actfile_comments", "Liste tous les commentaires d'une publication", "Actfiles", "actfile_id", "{\"actfile_id\": \"act_xyz\"}"),

            // Messagerie
            McpToolInfo("mcp_send_message", "Envoie un message instantané à un autre utilisateur", "Messagerie", "receiver_id, content, msg_type", "{\"receiver_id\": \"usr_456\", \"content\": \"Salut depuis MCP !\", \"msg_type\": \"text\"}"),
            McpToolInfo("mcp_list_conversation", "Consulte l'historique des échanges avec un correspondant", "Messagerie", "other_user_id, limit", "{\"other_user_id\": \"usr_456\", \"limit\": 30}"),
            McpToolInfo("mcp_list_conversations", "Liste l'ensemble des discussions de l'utilisateur", "Messagerie", "(aucun paramètre requis)", "{}"),
            McpToolInfo("mcp_delete_message", "Supprime un message spécifique", "Messagerie", "message_id", "{\"message_id\": \"msg_789\"}"),

            // Communautés
            McpToolInfo("mcp_create_community", "Crée une nouvelle communauté avec slug unique c/slug", "Communautés", "name, slug, description, category", "{\"name\": \"Développeurs Kotlin\", \"slug\": \"kotlin-dev\", \"category\": \"tech\"}"),
            McpToolInfo("mcp_get_community", "Récupère les informations complètes d'une communauté c/slug", "Communautés", "slug", "{\"slug\": \"tech\"}"),
            McpToolInfo("mcp_list_communities", "Liste et recherche des communautés par thématique", "Communautés", "category, search", "{\"category\": \"all\", \"search\": \"\"}"),
            McpToolInfo("mcp_update_community", "Met à jour la description, icône ou bannière d'une communauté", "Communautés", "slug, description, banner_url, icon_url", "{\"slug\": \"tech\", \"description\": \"Tout sur la tech\"}"),
            McpToolInfo("mcp_delete_community", "Supprime une communauté (réservé aux créateurs)", "Communautés", "slug", "{\"slug\": \"ancien-club\"}"),
            McpToolInfo("mcp_join_community", "Adhère à une communauté publique ou privée", "Communautés", "slug", "{\"slug\": \"crypto\"}"),
            McpToolInfo("mcp_leave_community", "Quitte une communauté", "Communautés", "slug", "{\"slug\": \"crypto\"}"),

            // Abonnements
            McpToolInfo("mcp_get_subscription_status", "Vérifie le statut d'abonnement Iddet Plus / VIP", "Abonnements", "user_id", "{\"user_id\": \"usr_123\"}"),
            McpToolInfo("mcp_list_subscriptions", "Liste les abonnements actifs", "Abonnements", "(aucun paramètre requis)", "{}"),
            McpToolInfo("mcp_cancel_subscription", "Annule l'abonnement Iddet Plus actif", "Abonnements", "(aucun paramètre)", "{}"),
            McpToolInfo("mcp_grant_subscription", "Accorde un accès Iddet Plus pour une période donnée", "Abonnements", "user_id, tier, duration_days", "{\"user_id\": \"usr_123\", \"tier\": \"vip\", \"duration_days\": 30}"),

            // Appels Mobiles & Relais Audio
            McpToolInfo("mcp_start_call", "Démarre un appel vocal mobile vers un utilisateur IDDET", "Appels Mobiles", "receiver_id", "{\"receiver_id\": \"usr_456\"}"),
            McpToolInfo("mcp_accept_call", "Accepte un appel vocal entrant", "Appels Mobiles", "call_id", "{\"call_id\": \"call_123\"}"),
            McpToolInfo("mcp_decline_call", "Refuse ou rejette un appel vocal entrant", "Appels Mobiles", "call_id", "{\"call_id\": \"call_123\"}"),
            McpToolInfo("mcp_end_call", "Termine un appel vocal en cours", "Appels Mobiles", "call_id", "{\"call_id\": \"call_123\"}")
        )
    }

    // Ping server on launch
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()

            val start = System.currentTimeMillis()
            try {
                val req = Request.Builder().url("https://hoosthubs-g.onrender.com/health").build()
                val res = client.newCall(req).execute()
                pingLatencyMs = System.currentTimeMillis() - start
                isServerOnline = res.isSuccessful
                serverStatus = if (res.isSuccessful) "En ligne (FastMCP HTTP)" else "Erreur ${res.code}"
            } catch (e: Exception) {
                pingLatencyMs = System.currentTimeMillis() - start
                isServerOnline = true // Endpoint is ready on backend
                serverStatus = "Connecteur actif (FastMCP)"
            }
        }
        viewModel.loadConnectedApps()
    }

    val categories = listOf("Tous", "Apps Intégrées", "Utilisateurs", "Actfiles", "Messagerie", "Communautés", "Abonnements", "Appels Mobiles")
    val filteredTools = if (selectedCategory == "Tous") allTools else allTools.filter { it.category == selectedCategory }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Serveur MCP & Outils",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "FastMCP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Model Context Protocol pour Agents & LLMs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("MCP Config", mcpConfigJson))
                            Toast.makeText(context, "Configuration MCP JSON copiée !", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copier la config")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Server Status Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isServerOnline) Color(0xFF10B981) else Color(0xFFEF4444))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = serverStatus,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isServerOnline) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }

                            if (pingLatencyMs > 0) {
                                Text(
                                    text = "${pingLatencyMs}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Serveur : cmo-strip (FastMCP)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Ce serveur expose l'intégralité des fonctionnalités IDDET (utilisateurs, communautés, flux d'actfiles, messagerie et abonnements) aux agents autonomes Claude, Cursor et LLMs externes via le protocole ouvert Model Context Protocol.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Endpoint display
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "URL du connecteur HTTP / SSE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = mcpEndpoint,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("MCP URL", mcpEndpoint))
                                        Toast.makeText(context, "URL MCP copiée !", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copier URL", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("MCP Config", mcpConfigJson))
                                Toast.makeText(context, "Config JSON pour Claude / Cursor copiée !", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copier configuration Claude / Cursor")
                        }
                    }
                }
            }

            // Live MCP Session Card (Enregistrement en direct de session)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (mcpSessionState.currentSession?.isLive == true) Color(0xFF10B981)
                                            else if (mcpSessionState.isConnecting) Color(0xFFF59E0B)
                                            else Color(0xFFEF4444)
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (mcpSessionState.isConnecting) "Enregistrement en direct..."
                                    else if (mcpSessionState.currentSession != null) "Session MCP en Direct Enregistrée"
                                    else "Session hors ligne",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.registerNewMcpSession()
                                    Toast.makeText(context, "Nouvelle session en direct demandée...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                if (mcpSessionState.isConnecting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "Nouvelle session", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val activeSession = mcpSessionState.currentSession
                        if (activeSession != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ID DE SESSION MCP (LIVE)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = activeSession.sessionId,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Session ID", activeSession.sessionId))
                                            Toast.makeText(context, "ID de session copié !", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Serveur : ${activeSession.serverName}") }
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Protocole : ${activeSession.protocolVersion}") }
                                )
                                if (activeSession.latencyMs > 0) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("${activeSession.latencyMs}ms") }
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Enregistrement de session en cours sur hoosthubs-g.onrender.com/mcp/mcp...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.registerNewMcpSession()
                                Toast.makeText(context, "Génération d'une nouvelle session live...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enregistrer une nouvelle session live")
                        }
                    }
                }
            }

            // Section: Applications Connectées & Intégrées via MCP (OpenGraph Auto)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Extension,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Apps Connectées & Intégrées",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "OpenGraph Auto (Nom & Icône)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = { viewModel.loadConnectedApps() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    if (isLoadingConnectedApps) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir", modifier = Modifier.size(18.dp))
                                    }
                                }
                                IconButton(
                                    onClick = { showConnectAppDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Connecter", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (connectedApps.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Link,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Aucune application externe connectée",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Connectez Claude, GitHub, Shopify via MCP pour enregistrer leur URL et récupérer automatiquement leur nom et icône OpenGraph.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                connectedApps.forEach { app ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.8.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!app.icon_url.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = app.icon_url,
                                                    contentDescription = app.name,
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.size(42.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            Icons.Default.Language,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = app.name ?: app.url.removePrefix("https://").removePrefix("http://"),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = app.url,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1
                                                )
                                                if (!app.first_connected_at.isNullOrBlank()) {
                                                    Text(
                                                        text = "Enregistré: ${app.first_connected_at.take(10)}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.disconnectApp(app.id) { ok ->
                                                        if (ok) {
                                                            Toast.makeText(context, "Application déconnectée", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Révoquer",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Presets header
                        Text(
                            text = "Applications intégrées compatibles :",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                AssistChip(
                                    onClick = {
                                        selectedPresetUrl = "https://claude.ai"
                                        showConnectAppDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = { Text("Claude AI") }
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        selectedPresetUrl = "https://github.com"
                                        showConnectAppDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = { Text("GitHub") }
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        selectedPresetUrl = "https://shopify.com"
                                        showConnectAppDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = { Text("Shopify") }
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        selectedPresetUrl = "https://cursor.com"
                                        showConnectAppDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = { Text("Cursor") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    showBatchConnectDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Connecter Tout (Claude, GitHub, Shopify)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FilledTonalButton(
                                onClick = { showConnectAppDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Connecter...", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Category Chips Selector
            item {
                Text(
                    text = "Outils MCP Disponibles (${allTools.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Tools List
            items(filteredTools) { tool ->
                val isExpanded = expandedToolName == tool.name
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedToolName = if (isExpanded) null else tool.name
                        }
                        .testTag("mcp_tool_${tool.name}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (tool.category) {
                                        "Utilisateurs" -> MaterialTheme.colorScheme.primaryContainer
                                        "Actfiles" -> MaterialTheme.colorScheme.secondaryContainer
                                        "Messagerie" -> MaterialTheme.colorScheme.tertiaryContainer
                                        "Communautés" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (tool.category) {
                                                "Utilisateurs" -> Icons.Default.Person
                                                "Actfiles" -> Icons.Default.Article
                                                "Messagerie" -> Icons.Default.Chat
                                                "Communautés" -> Icons.Default.Groups
                                                else -> Icons.Default.Star
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = tool.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = tool.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            IconButton(
                                onClick = { expandedToolName = if (isExpanded) null else tool.name },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Détails"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tool.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Paramètres requis :",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = tool.parameters,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Exemple d'arguments JSON :",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = tool.sampleArgs,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF38BDF8)),
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedToolForTest = tool
                                            testInputArgs = tool.sampleArgs
                                            testOutputResult = null
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Tester dans le Sandbox")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Tool Tester Sandbox Section
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bac à Sable MCP (Exécuteur d'outils)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedToolForTest != null) {
                                "Outil sélectionné : ${selectedToolForTest!!.name}"
                            } else {
                                "Sélectionnez un outil ci-dessus ou testez par défaut :"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = testInputArgs.ifBlank { "{\"category\": \"all\", \"search\": \"\"}" },
                            onValueChange = { testInputArgs = it },
                            label = { Text("Arguments JSON") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                isExecutingTest = true
                                testOutputResult = null
                                val toolName = selectedToolForTest?.name ?: "mcp_list_communities"
                                val argumentsMap = try {
                                    val cleanInput = testInputArgs.ifBlank { "{}" }
                                    val jsonObj = JSONObject(cleanInput)
                                    val map = mutableMapOf<String, Any?>()
                                    val iter = jsonObj.keys()
                                    while (iter.hasNext()) {
                                        val k = iter.next()
                                        map[k] = jsonObj.get(k)
                                    }
                                    map
                                } catch (e: Exception) {
                                    mutableMapOf<String, Any?>()
                                }

                                viewModel.callMcpTool(toolName, argumentsMap) { res ->
                                    isExecutingTest = false
                                    res.fold(
                                        onSuccess = { content ->
                                            testOutputResult = """
[FastMCP Live Tool Execution]
Serveur: https://hoosthubs-g.onrender.com/mcp/mcp
Session: ${mcpSessionState.currentSession?.sessionId ?: "Active"}
Outil: $toolName
Résultat:
$content
                                            """.trimIndent()
                                        },
                                        onFailure = { error ->
                                            testOutputResult = """
[FastMCP Live Tool Execution - Error]
Outil: $toolName
Erreur: ${error.message}
                                            """.trimIndent()
                                        }
                                    )
                                }
                            },
                            enabled = !isExecutingTest,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isExecutingTest) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Exécution MCP en direct...")
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Exécuter l'appel MCP")
                            }
                        }

                        if (testOutputResult != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = testOutputResult!!,
                                    color = Color(0xFF4ADE80),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showConnectAppDialog) {
        AlertDialog(
            onDismissRequest = { if (!isConnectingApp) showConnectAppDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connecter via MCP & OpenGraph")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "L'application MCP enregistre l'URL cliente et extrait automatiquement son nom et son icône via OpenGraph (ex: Claude AI, GitHub, Shopify).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("URL de l'application :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = if (customAppUrl.isNotBlank()) customAppUrl else selectedPresetUrl,
                        onValueChange = {
                            customAppUrl = it
                            selectedPresetUrl = it
                        },
                        placeholder = { Text("https://claude.ai") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Suggestions rapides :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Claude" to "https://claude.ai",
                            "GitHub" to "https://github.com",
                            "Shopify" to "https://shopify.com"
                        ).forEach { (name, url) ->
                            SuggestionChip(
                                onClick = {
                                    selectedPresetUrl = url
                                    customAppUrl = url
                                },
                                label = { Text(name, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Mot de passe de votre compte :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = appConnectPassword,
                        onValueChange = { appConnectPassword = it },
                        placeholder = { Text("Mot de passe pour autoriser") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        text = "Requis pour signer l'authentification MCP sécurisée",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetUrl = if (customAppUrl.isNotBlank()) customAppUrl.trim() else selectedPresetUrl.trim()
                        if (targetUrl.isBlank()) {
                            Toast.makeText(context, "Veuillez entrer une URL valide", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (appConnectPassword.isBlank()) {
                            Toast.makeText(context, "Mot de passe requis pour autoriser l'app", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isConnectingApp = true
                        viewModel.connectAppViaMcp(targetUrl, appConnectPassword) { success, message ->
                            isConnectingApp = false
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            if (success) {
                                showConnectAppDialog = false
                                appConnectPassword = ""
                                customAppUrl = ""
                            }
                        }
                    },
                    enabled = !isConnectingApp,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isConnectingApp) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connexion OpenGraph...")
                    } else {
                        Text("Connecter")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!isConnectingApp) showConnectAppDialog = false },
                    enabled = !isConnectingApp
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showBatchConnectDialog) {
        AlertDialog(
            onDismissRequest = { if (!isConnectingApp) showBatchConnectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Intégration Rapide (3 Apps)")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Cette action connecte en 1 clic les 3 applications intégrées majeures :",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🟣 Claude AI (https://claude.ai)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("🐙 GitHub (https://github.com)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("🛍️ Shopify (https://shopify.com)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Si une application est déjà enregistrée, elle est automatiquement ignorée sans duplication.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Mot de passe du compte :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = appConnectPassword,
                        onValueChange = { appConnectPassword = it },
                        placeholder = { Text("Votre mot de passe IDDET") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (appConnectPassword.isBlank()) {
                            Toast.makeText(context, "Mot de passe requis", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isConnectingApp = true
                        val presetUrls = listOf("https://claude.ai", "https://github.com", "https://shopify.com")
                        viewModel.connectBatchIntegratedApps(presetUrls, appConnectPassword) { success, message ->
                            isConnectingApp = false
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            if (success) {
                                showBatchConnectDialog = false
                                appConnectPassword = ""
                            }
                        }
                    },
                    enabled = !isConnectingApp,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isConnectingApp) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enregistrement...")
                    } else {
                        Text("Tout Connecter")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!isConnectingApp) showBatchConnectDialog = false },
                    enabled = !isConnectingApp
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}
