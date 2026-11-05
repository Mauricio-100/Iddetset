package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import com.example.ui.components.VideoUrlHelper
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Community
import com.example.ui.IddetViewModel
import com.example.ui.components.APP_CATEGORIES
import com.example.ui.components.CategoryInfo
import com.example.ui.components.IddetAdsGold
import com.example.ui.components.SponsoredBadge
import com.example.ui.components.MarkdownActfile
import com.example.utils.LocalAiManager
import com.example.utils.MusicPlayerManager
import com.example.utils.MusicTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ActfileTemplate(
    val title: String,
    val emoji: String,
    val description: String,
    val categoryId: String,
    val suggestedTags: String,
    val content: String
)

val PRESET_TEMPLATES = listOf(
    ActfileTemplate(
        title = "Snippet & Astuce Tech",
        emoji = "💻",
        description = "Partagez du code propre avec explication et astuce.",
        categoryId = "@(coding)",
        suggestedTags = "coding, tech, android, kotlin",
        content = """# 🚀 Astuce Code : Titre de l'astuce

Voici un exemple pratique en **Kotlin** :

```kotlin
// Votre snippet ici
fun optimizeFlow() {
    println("Code optimisé et réactif !")
}
```

> [!TIP]
> **Pourquoi c'est utile :** Cela réduit la latence et améliore la lisibilité.

### Points clés :
- [x] Simple à mettre en place
- [x] Zéro dépendance externe
- [ ] À tester dans vos projets !"""
    ),
    ActfileTemplate(
        title = "Showcase & Projet",
        emoji = "🚀",
        description = "Présentez une création, application ou outil.",
        categoryId = "@(tech)",
        suggestedTags = "showcase, dev, open_source, build",
        content = """# 🌟 Présentation : Nom du Projet

**Une application moderne construite avec passion.**

### 🎯 Objectif
Résoudre le problème de manière élégante et intuitive.

### ✨ Fonctionnalités majeures
- ⚡ Rendu ultra rapide
- 🔒 Confidentialité locale
- 🎨 Design Material 3 soigné

---
🔗 **Lien / Démo :** [Voir sur GitHub](https://github.com)
> Donnez-moi vos retours en commentaire ! 🙌"""
    ),
    ActfileTemplate(
        title = "Débat & Sondage",
        emoji = "💡",
        description = "Posez une question et lancez une discussion.",
        categoryId = "@(philosophy)",
        suggestedTags = "debat, avis, communaute, discussion",
        content = """# 🤔 Débat : Votre avis sur la question ?

Dans le développement moderne, il existe deux grandes approches...

### Vous êtes plutôt :
- [ ] Option A : Approche minimaliste
- [ ] Option B : Approche tout-en-un

> 💬 **Partagez votre point de vue et vos arguments ci-dessous !**"""
    ),
    ActfileTemplate(
        title = "Journal & Pensée du Jour",
        emoji = "☕",
        description = "Humeur, réflexion, citation ou bilan de journée.",
        categoryId = "@(lifestyle)",
        suggestedTags = "journal, chill, inspiration, mood",
        content = """# ☕ Pensée du jour

*Aujourd'hui, j'ai pris le temps de réfléchir à l'importance de faire des pauses constructives.*

> « La simplicité est la sophistication suprême. » — *Léonard de Vinci*

Prenez soin de vous et belle journée à tous ! ✨"""
    ),
    ActfileTemplate(
        title = "Tutoriel Pas-à-Pas",
        emoji = "📚",
        description = "Guide étape par étape clair et didactique.",
        categoryId = "@(education)",
        suggestedTags = "tuto, guide, apprentissage, howto",
        content = """# 📘 Guide Pas-à-Pas : Titre du Tutoriel

Dans ce guide rapide, nous allons voir comment accomplir cet objectif en 3 étapes.

### 1️⃣ Étape 1 : Préparation
Installez les outils nécessaires et initialisez votre environnement.

### 2️⃣ Étape 2 : Configuration
Modifiez les paramètres selon vos besoins.

### 3️⃣ Étape 3 : Validation
Testez le résultat et profitez !

> [!NOTE]
> En cas de question, posez-la directement dans les réponses."""
    )
)

val POPULAR_TAGS_SUGGESTIONS = listOf(
    "tech", "dev", "android", "ai", "coding", "gaming", "chill", "astuce", "markdown", "design", "actu", "musique"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActfileComposerScreen(
    viewModel: IddetViewModel,
    onDismiss: () -> Unit,
    onPublish: (
        content: String,
        tags: String,
        category: String?,
        postAsIddet: Boolean,
        attachedSound: MusicTrack?,
        selectedCommunity: Community?
    ) -> Unit,
    modifier: Modifier = Modifier,
    initialCommunity: Community? = null,
    initialCategory: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val initialTextFromVm by viewModel.composerInitialContent.collectAsState()
    var contentValue by remember(initialTextFromVm) { 
        mutableStateOf(TextFieldValue(initialTextFromVm ?: "")) 
    }
    var postAsIddet by remember { mutableStateOf(false) }
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var tags by remember { mutableStateOf("") }

    // IDDET Ads integration
    var promoteWithAds by remember { mutableStateOf(false) }
    var adBudget by remember { mutableDoubleStateOf(5.0) }
    var adCurrency by remember { mutableStateOf("USD") }
    var adDaily by remember { mutableStateOf(true) }
    var adTargetCountry by remember { mutableStateOf<String?>("CD") }

    // Attached Sound integration
    val attachedSoundFromVm by viewModel.attachedComposerSound.collectAsStateWithLifecycle()
    var attachedSound by remember(attachedSoundFromVm) { mutableStateOf<MusicTrack?>(attachedSoundFromVm) }
    var showSoundPickerDialog by remember { mutableStateOf(false) }
    val availableSounds by viewModel.sounds.collectAsStateWithLifecycle()
    val isSoundsLoading by viewModel.isSoundsLoading.collectAsStateWithLifecycle()

    LaunchedEffect(showSoundPickerDialog) {
        if (showSoundPickerDialog && availableSounds.isEmpty()) {
            viewModel.loadSounds()
        }
    }

    LaunchedEffect(initialTextFromVm) {
        if (!initialTextFromVm.isNullOrBlank()) {
            viewModel.setComposerInitialContent(null)
        }
    }

    // Categories
    val categories = APP_CATEGORIES
    var selectedCategory by remember(initialCategory) { 
        mutableStateOf(initialCategory ?: categories.firstOrNull()?.id ?: "@(fun)") 
    }

    // Modes & States
    var isPreviewMode by remember { mutableStateOf(false) }
    val characterLimit = 1500
    val currentLength = contentValue.text.length
    val wordCount = remember(contentValue.text) {
        contentValue.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
    val readingTimeSeconds = remember(wordCount) {
        maxOf(5, (wordCount / 3))
    }

    // Communities & destination selectors
    val myCommunities by produceState<List<Community>>(initialValue = emptyList()) {
        viewModel.getMyCommunitiesFlow().collect { value = it }
    }
    var selectedCommunity by remember(initialCommunity) { mutableStateOf<Community?>(initialCommunity) }
    var showDestinationMenu by remember { mutableStateOf(false) }

    // Dialogs
    var showTemplatesDialog by remember { mutableStateOf(false) }
    var showAiAssistantDialog by remember { mutableStateOf(false) }
    var showInsertLinkDialog by remember { mutableStateOf(false) }
    var showInsertImageDialog by remember { mutableStateOf(false) }
    var showDiscardDraftConfirm by remember { mutableStateOf(false) }

    // Real Location integration
    val realLocation by viewModel.realLocation.collectAsStateWithLifecycle()
    val currentUserVibe by viewModel.currentUserVibe.collectAsStateWithLifecycle()

    // AI suggestion & moderation
    var isAiSuggesting by remember { mutableStateOf(false) }
    var isCheckingSafety by remember { mutableStateOf(false) }
    var safetyError by remember { mutableStateOf<String?>(null) }

    // Inline insertion helper
    val insertFormatting: (String, String) -> Unit = { prefix, suffix ->
        val currentValue = contentValue
        val selection = currentValue.selection
        val text = currentValue.text

        val selectedText = if (selection.start < selection.end) {
            text.substring(selection.start, selection.end)
        } else ""

        val newText = StringBuilder(text)
            .replace(selection.start, selection.end, "$prefix$selectedText$suffix")
            .toString()

        val newCursor = if (selectedText.isEmpty()) {
            selection.start + prefix.length
        } else {
            selection.start + prefix.length + selectedText.length + suffix.length
        }

        contentValue = currentValue.copy(
            text = newText,
            selection = TextRange(newCursor, newCursor)
        )
    }

    // Append text at cursor
    val insertSnippet: (String) -> Unit = { snippet ->
        val currentValue = contentValue
        val selection = currentValue.selection
        val text = currentValue.text

        val newText = StringBuilder(text)
            .replace(selection.start, selection.end, snippet)
            .toString()

        val newCursor = selection.start + snippet.length
        contentValue = currentValue.copy(
            text = newText,
            selection = TextRange(newCursor, newCursor)
        )
    }

    var isUploadingMedia by remember { mutableStateOf(false) }
    
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingMedia = true
            scope.launch {
                try {
                    val url = uri.toString()
                    val isVideo = url.contains("video") || url.endsWith(".mp4")
                    val snippet = if (isVideo) {
                        "\n\n![]($url)\n\n"
                    } else {
                        "\n\n![]($url)\n\n"
                    }
                    insertSnippet(snippet)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isUploadingMedia = false
                }
            }
        }
    }

    // Auto-categorize heuristic and AI
    LaunchedEffect(contentValue.text) {
        val text = contentValue.text
        if (text.length > 40 && LocalAiManager.state.value == com.example.utils.AiModelState.READY) {
            delay(1200)
            isAiSuggesting = true
            val suggestedName = LocalAiManager.suggestCategory(text)
            if (suggestedName != null) {
                val matched = categories.find { it.name.equals(suggestedName, ignoreCase = true) }
                if (matched != null) {
                    selectedCategory = matched.id
                }
            }
            isAiSuggesting = false
        }
    }

    Dialog(
        onDismissRequest = {
            if (contentValue.text.isNotBlank()) {
                showDiscardDraftConfirm = true
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize().testTag("actfile_composer_screen"),
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (contentValue.text.isNotBlank()) {
                                    showDiscardDraftConfirm = true
                                } else {
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.testTag("composer_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    },
                    title = {
                        // Contextual Destination & Feed Selector
                        Surface(
                            onClick = { showDestinationMenu = true },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("composer_destination_selector")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (selectedCommunity != null) Icons.Default.Group else Icons.Default.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedCommunity?.let { "c/${it.slug}" } ?: "Mon Journal (Public)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    },
                    actions = {
                        // Templates Catalog Button
                        IconButton(onClick = { showTemplatesDialog = true }) {
                            Icon(
                                Icons.Outlined.AutoStories,
                                contentDescription = "Modèles",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // AI Studio Button
                        IconButton(onClick = { showAiAssistantDialog = true }) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Assistant IA",
                                tint = Color(0xFF8B5CF6)
                            )
                        }

                        // Publish Button
                        val isContentValid = currentLength in 1..characterLimit
                        Button(
                            onClick = {
                                val textContent = contentValue.text
                                if (textContent.isNotBlank() && isContentValid) {
                                    isCheckingSafety = true
                                    safetyError = null
                                    scope.launch {
                                        val isSafe = LocalAiManager.checkAppropriate(textContent)
                                        if (isSafe) {
                                            val finalContent = if (selectedCommunity != null) {
                                                "$textContent\n\n@c/${selectedCommunity!!.slug}"
                                            } else textContent
                                            viewModel.setAttachedComposerSound(null)
                                            if (promoteWithAds) {
                                                viewModel.publishAndPromoteActfile(
                                                    content = finalContent,
                                                    tags = tags,
                                                    category = selectedCategory,
                                                    communityId = selectedCommunity?.id,
                                                    channelId = null,
                                                    postAsIddet = postAsIddet,
                                                    soundId = attachedSound?.id,
                                                    soundTitle = attachedSound?.title,
                                                    soundAuthor = attachedSound?.artist,
                                                    soundAudioUrl = attachedSound?.audioUrl,
                                                    soundCoverUrl = attachedSound?.albumArt,
                                                    communityName = selectedCommunity?.name,
                                                    communityIconUrl = selectedCommunity?.iconUrl,
                                                    budget = adBudget,
                                                    currency = adCurrency,
                                                    daily = adDaily,
                                                    targetCountry = adTargetCountry,
                                                    onSuccess = { checkoutUrl ->
                                                        Toast.makeText(context, "Actfile publié et sponsorisé avec succès !", Toast.LENGTH_SHORT).show()
                                                        onDismiss()
                                                    },
                                                    onError = { err ->
                                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                        onDismiss()
                                                    }
                                                )
                                            } else {
                                                onPublish(finalContent, tags, selectedCategory, postAsIddet, attachedSound, selectedCommunity)
                                            }
                                        } else {
                                            safetyError = "⚠️ Ce contenu enfreint les règles de la communauté Iddet."
                                        }
                                        isCheckingSafety = false
                                    }
                                }
                            },
                            enabled = isContentValid && !isCheckingSafety,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("composer_publish_button")
                        ) {
                            if (isCheckingSafety) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Publier",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Category Selection Ribbon
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Catégorie de l'Actfile",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isAiSuggesting) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "IA suggère...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories, key = { it.id }) { cat ->
                                val isSelected = cat.id == selectedCategory
                                Surface(
                                    onClick = { selectedCategory = cat.id },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) cat.color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color.Transparent else cat.color.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.testTag("composer_category_${cat.name}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(text = cat.emoji, fontSize = 14.sp)
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Attached Sound banner in composer
                    if (attachedSound != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val playerState by MusicPlayerManager.state.collectAsState()
                                val isPlayingThis = playerState.isPlaying && (
                                    playerState.currentTrack?.id == attachedSound?.id ||
                                    playerState.currentTrack?.title == attachedSound?.title
                                )

                                FilledIconButton(
                                    onClick = {
                                        val sound = attachedSound ?: return@FilledIconButton
                                        if (isPlayingThis) {
                                            MusicPlayerManager.togglePlayPause()
                                        } else {
                                            MusicPlayerManager.playTrack(sound)
                                        }
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlayingThis) "Pause" else "Lancer le son",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = attachedSound?.title ?: "Son",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "par @${attachedSound?.artist ?: "créateur"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        attachedSound = null
                                        viewModel.setAttachedComposerSound(null)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Retirer le son",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Mode Switcher (Éditeur vs Rendu Markdown Riche) + Live Metrics Ribbon
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Tab Pills
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    onClick = { isPreviewMode = false },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (!isPreviewMode) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    shadowElevation = if (!isPreviewMode) 2.dp else 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text("Éditer", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Surface(
                                    onClick = { isPreviewMode = true },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isPreviewMode) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    shadowElevation = if (isPreviewMode) 2.dp else 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text("Aperçu Markdown", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Dynamic Live Stats Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "⏱️ ~${readingTimeSeconds}s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "$wordCount mots",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Main Body: Editor or Rich Markdown Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (isPreviewMode) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                if (contentValue.text.isBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("📝", fontSize = 36.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Rien à prévisualiser pour l'instant.",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Passez en mode 'Éditer' ou choisissez un modèle d'inspiration !",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Box(modifier = Modifier.padding(14.dp)) {
                                            MarkdownActfile(content = contentValue.text)
                                        }
                                    }
                                }
                            }
                        } else {
                            val isDebateDraft = remember(contentValue.text) {
                                contentValue.text.contains("[!DEBATE]", ignoreCase = true) ||
                                (contentValue.text.contains("sujet à débattre", ignoreCase = true) && contentValue.text.contains("google search", ignoreCase = true))
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (isDebateDraft) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            Color(0xFF4285F4).copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = Color(0xFF4285F4),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "⚡ sujet à débattre • source première google search",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = contentValue,
                                    onValueChange = {
                                        if (it.text.length <= characterLimit + 50) {
                                            contentValue = it
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 250.dp)
                                        .testTag("composer_text_input"),
                                    placeholder = {
                                        Text(
                                            text = "Exprimez-vous ici... Utilisez le Markdown (# Titre, **gras**, `code`, > citation, [liens](url))\n\nCliquez sur les raccourcis de la barre d'outils ci-dessous pour formater rapidement !",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        disabledBorderColor = Color.Transparent,
                                        errorBorderColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }

                    // Safety Error Alert
                    safetyError?.let { err ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Hashtags & Quick Suggestions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                    ) {
                        if (com.example.data.IddetAccountManager.canPostAsIddet(currentUser)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Publier en tant que IDDET",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Switch(checked = postAsIddet, onCheckedChange = { postAsIddet = it })
                            }
                        }
                        OutlinedTextField(
                            value = tags,
                            onValueChange = { tags = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("composer_tags_input"),
                            placeholder = { Text("Tags séparés par virgule (ex: tech, gopu, fun)") },
                            leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (tags.isNotBlank()) {
                                    IconButton(onClick = { tags = "" }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Quick popular tag pills
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(POPULAR_TAGS_SUGGESTIONS) { sug ->
                                Surface(
                                    onClick = {
                                        val currentTags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                        if (!currentTags.contains(sug)) {
                                            tags = if (tags.isBlank()) sug else "$tags, $sug"
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "#$sug",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // IDDET Ads integration section
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (promoteWithAds) IddetAdsGold.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (promoteWithAds) IddetAdsGold else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(IddetAdsGold.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Campaign,
                                                contentDescription = null,
                                                tint = IddetAdsGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Sponsoriser (IDDET Ads)",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                SponsoredBadge()
                                            }
                                            Text(
                                                text = "+50 000 pts boost algo Pour Toi",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = promoteWithAds,
                                        onCheckedChange = { promoteWithAds = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = IddetAdsGold,
                                            checkedTrackColor = IddetAdsGold.copy(alpha = 0.4f)
                                        )
                                    )
                                }

                                if (promoteWithAds) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = IddetAdsGold.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Currency toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Devise",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf("USD", "CDF").forEach { curr ->
                                                FilterChip(
                                                    selected = adCurrency == curr,
                                                    onClick = {
                                                        adCurrency = curr
                                                        adBudget = if (curr == "CDF") 10000.0 else 5.0
                                                    },
                                                    label = { Text(if (curr == "USD") "USD ($)" else "CDF (FC)") }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Budget options
                                    Text(
                                        text = "Budget publicitaire (${if (adDaily) "/ jour" else "total"}) :",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    val budgetOptions = if (adCurrency == "USD") listOf(2.0, 5.0, 10.0, 25.0) else listOf(5000.0, 10000.0, 25000.0, 50000.0)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(budgetOptions) { amount ->
                                            val isSelected = adBudget == amount
                                            SuggestionChip(
                                                onClick = { adBudget = amount },
                                                label = {
                                                    Text(
                                                        text = if (adCurrency == "USD") "$${amount.toInt()}" else "${amount.toInt()} FC",
                                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal
                                                    )
                                                },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = if (isSelected) IddetAdsGold.copy(alpha = 0.25f) else Color.Transparent
                                                ),
                                                border = SuggestionChipDefaults.suggestionChipBorder(
                                                    enabled = true,
                                                    borderColor = if (isSelected) IddetAdsGold else MaterialTheme.colorScheme.outlineVariant
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Country targeting
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Ciblage :",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf("CD" to "🇨🇩 RDC", "FR" to "🇫🇷 FR", null to "🌍 Global").forEach { (code, label) ->
                                                FilterChip(
                                                    selected = adTargetCountry == code,
                                                    onClick = { adTargetCountry = code },
                                                    label = { Text(label, fontSize = 11.sp) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Complete Markdown & Media Toolbar with Progress Gauge
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(bottom = 48.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = 2.dp,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Markdown Toolbar Tools (Scrollable Ribbon)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Bold
                                IconButton(onClick = { insertFormatting("**", "**") }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.FormatBold, contentDescription = "Gras", modifier = Modifier.size(18.dp))
                                }
                                // Italic
                                IconButton(onClick = { insertFormatting("*", "*") }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.FormatItalic, contentDescription = "Italique", modifier = Modifier.size(18.dp))
                                }
                                // Strikethrough
                                IconButton(onClick = { insertFormatting("~~", "~~") }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.FormatStrikethrough, contentDescription = "Barré", modifier = Modifier.size(18.dp))
                                }
                                // Header H1
                                IconButton(onClick = { insertSnippet("\n# ") }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Title, contentDescription = "Titre", modifier = Modifier.size(18.dp))
                                }
                                // Header H2
                                IconButton(onClick = { insertSnippet("\n## ") }, modifier = Modifier.size(36.dp)) {
                                    Text("H2", fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                // Code Block
                                IconButton(onClick = { insertSnippet("\n```kotlin\n// Votre code ici\n```\n") }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Code, contentDescription = "Bloc de code", modifier = Modifier.size(18.dp))
                                }
                                // Quote
                                IconButton(onClick = { insertSnippet("\n> ") }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.FormatQuote, contentDescription = "Citation", modifier = Modifier.size(18.dp))
                                }
                                // Callout Tip Alert
                                IconButton(onClick = { insertSnippet("\n> [!TIP]\n> **Astuce :** ") }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = "Astuce", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                }
                                // Checklist
                                IconButton(onClick = { insertSnippet("\n- [ ] ") }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Checklist, contentDescription = "Checklist", modifier = Modifier.size(18.dp))
                                }
                                // Bullet list
                                IconButton(onClick = { insertSnippet("\n- ") }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.FormatListBulleted, contentDescription = "Liste à puces", modifier = Modifier.size(18.dp))
                                }
                                // Link
                                IconButton(onClick = { showInsertLinkDialog = true }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Link, contentDescription = "Lien", modifier = Modifier.size(18.dp))
                                }
                                // Hide Link Photo
                                IconButton(onClick = { insertSnippet("\n<!--hide_og_photo-->\n") }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.LinkOff, contentDescription = "Cacher photo lien", modifier = Modifier.size(18.dp))
                                }
                                // Image / Video Upload
                                IconButton(
                                    onClick = {
                                        mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                    },
                                    modifier = Modifier.size(36.dp),
                                    enabled = !isUploadingMedia
                                ) {
                                    if (isUploadingMedia) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Image, contentDescription = "Image ou Vidéo", modifier = Modifier.size(18.dp))
                                    }
                                }
                                // Sound Attachment
                                IconButton(
                                    onClick = { showSoundPickerDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "Ajouter un son",
                                        tint = if (attachedSound != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                // Divider
                                IconButton(onClick = { insertSnippet("\n---\n") }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.HorizontalRule, contentDescription = "Séparateur", modifier = Modifier.size(18.dp))
                                }
                                // Real GPS Tag
                                IconButton(
                                    onClick = {
                                        val locText = if (realLocation.isRealGpsAcquired) "📍 ${realLocation.address}" else "📍 Paris, France"
                                        insertSnippet("\n> $locText\n")
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = "Ma localisation", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                }
                                // Vibe mood
                                IconButton(
                                    onClick = {
                                        insertSnippet(" ${currentUserVibe.emoji} ")
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text(currentUserVibe.emoji, fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Circular Character Counter Gauge
                            val ratio = currentLength.toFloat() / characterLimit.toFloat()
                            val gaugeColor = when {
                                ratio > 0.9f -> MaterialTheme.colorScheme.error
                                ratio > 0.6f -> Color(0xFFF59E0B)
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { 1f },
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        strokeWidth = 3.dp
                                    )
                                    CircularProgressIndicator(
                                        progress = { ratio.coerceAtMost(1f) },
                                        color = gaugeColor,
                                        strokeWidth = 3.dp
                                    )
                                }

                                Text(
                                    text = "$currentLength",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (currentLength > characterLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Destination Picker Dialog
                if (showDestinationMenu) {
                    AlertDialog(
                        onDismissRequest = { showDestinationMenu = false },
                        title = {
                            Text("Où publier cet Actfile ?", fontWeight = FontWeight.Black)
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Mon Journal
                                Surface(
                                    onClick = {
                                        selectedCommunity = null
                                        showDestinationMenu = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedCommunity == null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selectedCommunity == null) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Mon Journal Public", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Visible par tous les membres du réseau Iddet", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("Mes Communautés", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                if (myCommunities.isEmpty()) {
                                    Text(
                                        "Vous n'avez rejoint aucune communauté pour l'instant.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    myCommunities.forEach { com ->
                                        val isSelected = selectedCommunity?.id == com.id
                                        Surface(
                                            onClick = {
                                                selectedCommunity = com
                                                showDestinationMenu = false
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val icon = com.iconUrl ?: "https://images.unsplash.com/photo-1511556532299-8f662fc26c06?w=50&q=80"
                                                    AsyncImage(
                                                        model = icon,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(com.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text("c/${com.slug}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDestinationMenu = false }) {
                                Text("Fermer")
                            }
                        }
                    )
                }

                // Templates Dialog
                if (showTemplatesDialog) {
                    AlertDialog(
                        onDismissRequest = { showTemplatesDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Modèles d'Actfiles", fontWeight = FontWeight.Black)
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 380.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PRESET_TEMPLATES.forEach { template ->
                                    Surface(
                                        onClick = {
                                            contentValue = TextFieldValue(template.content, selection = TextRange(template.content.length))
                                            selectedCategory = template.categoryId
                                            if (tags.isBlank()) tags = template.suggestedTags
                                            showTemplatesDialog = false
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(template.emoji, fontSize = 22.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(template.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(template.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showTemplatesDialog = false }) {
                                Text("Fermer")
                            }
                        }
                    )
                }

                // AI Assistant Studio Dialog
                if (showAiAssistantDialog) {
                    AiAssistantModal(
                        currentText = contentValue.text,
                        onDismiss = { showAiAssistantDialog = false },
                        onApplyText = { newText ->
                            contentValue = TextFieldValue(newText, selection = TextRange(newText.length))
                            showAiAssistantDialog = false
                        },
                        onApplyTags = { newTags ->
                            tags = if (tags.isBlank()) newTags else "$tags, $newTags"
                            showAiAssistantDialog = false
                        }
                    )
                }

                // Insert Link Dialog
                if (showInsertLinkDialog) {
                    InsertLinkModal(
                        onDismiss = { showInsertLinkDialog = false },
                        onInsert = { title, url ->
                            val linkMarkdown = "[$title]($url)"
                            insertSnippet(linkMarkdown)
                            showInsertLinkDialog = false
                        }
                    )
                }

                // Insert Image Dialog
                if (showInsertImageDialog) {
                    InsertImageModal(
                        onDismiss = { showInsertImageDialog = false },
                        onInsert = { alt, url ->
                            val imgMarkdown = "\n![$alt]($url)\n"
                            insertSnippet(imgMarkdown)
                            showInsertImageDialog = false
                        }
                    )
                }

                // Sound Picker Dialog
                if (showSoundPickerDialog) {
                    SoundPickerModal(
                        availableSounds = availableSounds,
                        isLoading = isSoundsLoading,
                        onDismiss = { showSoundPickerDialog = false },
                        onSelectSound = { sound ->
                            attachedSound = sound
                            viewModel.setAttachedComposerSound(sound)
                            showSoundPickerDialog = false
                        },
                        onRefresh = { viewModel.loadSounds() }
                    )
                }

                // Discard Draft Confirm
                if (showDiscardDraftConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDiscardDraftConfirm = false },
                        title = { Text("Quitter la rédaction ?") },
                        text = { Text("Votre texte en cours sera perdu si vous ne publiez pas.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDiscardDraftConfirm = false
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Abandonner")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDiscardDraftConfirm = false }) {
                                Text("Continuer la rédaction")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AiAssistantModal(
    currentText: String,
    onDismiss: () -> Unit,
    onApplyText: (String) -> Unit,
    onApplyTags: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var generatedResult by remember { mutableStateOf<String?>(null) }
    var generatedType by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF8B5CF6))
                Spacer(modifier = Modifier.width(8.dp))
                Text("✨ Studio IA d'Écriture", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Utilisez l'IA locale pour sublimer votre Actfile, générer des accroches ou structurer votre texte.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF8B5CF6))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Génération créative en cours...", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else if (generatedResult != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Proposition IA :", fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(generatedResult!!, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    // Action 1: Polish & Style
                    Surface(
                        onClick = {
                            if (currentText.isBlank()) return@Surface
                            isProcessing = true
                            generatedType = "text"
                            scope.launch {
                                delay(600)
                                val polished = "# 🌟 " + currentText.lines().firstOrNull().orEmpty() + "\n\n" +
                                        currentText + "\n\n> [!NOTE]\n> Partagez votre avis en commentaire ! ✨"
                                generatedResult = polished
                                isProcessing = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🪄", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Sublimer la mise en page Markdown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Ajoute titres, citations et callouts stylisés", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Action 2: Generate Catchy Hooks
                    Surface(
                        onClick = {
                            isProcessing = true
                            generatedType = "text"
                            scope.launch {
                                delay(500)
                                generatedResult = """# 🔥 3 Idées de Sujets & Accroches

1. **La méthode méconnue pour doubler sa productivité en code**
2. **Pourquoi le Markdown est le meilleur format d'écriture en 2026**
3. **Le futur des applications décentralisées et interactives**"""
                                isProcessing = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Générer 3 Accroches Captivantes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Trouvez une idée de titre percutante", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Action 3: Auto Tags
                    Surface(
                        onClick = {
                            isProcessing = true
                            generatedType = "tags"
                            scope.launch {
                                delay(400)
                                val extracted = when {
                                    currentText.contains("code", ignoreCase = true) || currentText.contains("kotlin", ignoreCase = true) -> "coding, dev, tech, software"
                                    currentText.contains("gaming", ignoreCase = true) || currentText.contains("jeu", ignoreCase = true) -> "gaming, gameplay, fun, esport"
                                    currentText.contains("tuto", ignoreCase = true) || currentText.contains("guide", ignoreCase = true) -> "tuto, guide, howto, learn"
                                    else -> "inspiration, iddet, markdown, community"
                                }
                                generatedResult = extracted
                                isProcessing = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🏷️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Générer des Tags Intelligents", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Détecte les thèmes clés de votre texte", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (generatedResult != null) {
                Button(
                    onClick = {
                        if (generatedType == "tags") {
                            onApplyTags(generatedResult!!)
                        } else {
                            onApplyText(generatedResult!!)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Appliquer à l'Actfile")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun InsertLinkModal(
    onDismiss: () -> Unit,
    onInsert: (title: String, url: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔗 Insérer un lien", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre du lien (ex: Mon GitHub)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL (ex: https://...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = if (title.isBlank()) url else title
                    onInsert(finalTitle, url)
                },
                enabled = url.isNotBlank()
            ) {
                Text("Insérer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun InsertImageModal(
    onDismiss: () -> Unit,
    onInsert: (alt: String, url: String) -> Unit
) {
    var alt by remember { mutableStateOf("Bannière") }
    var url by remember { mutableStateOf("") }

    val presetImages = listOf(
        "Code & Dev" to "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&q=80",
        "Cyberpunk" to "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&q=80",
        "Minimalist" to "https://images.unsplash.com/photo-1494438639946-1ebd1d20bf85?w=800&q=80",
        "Nature" to "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800&q=80"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🖼️ Insérer une image", fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = alt,
                    onValueChange = { alt = it },
                    label = { Text("Description / Alt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL de l'image") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Ou choisissez une suggestion HD :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetImages.forEach { (label, presetUrl) ->
                        FilterChip(
                            selected = url == presetUrl,
                            onClick = {
                                url = presetUrl
                                alt = label
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onInsert(alt, url) },
                enabled = url.isNotBlank()
            ) {
                Text("Insérer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun SoundPickerModal(
    availableSounds: List<MusicTrack>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSelectSound: (MusicTrack) -> Unit,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSounds = remember(availableSounds, searchQuery) {
        if (searchQuery.isBlank()) availableSounds
        else availableSounds.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.genre.contains(searchQuery, ignoreCase = true)
        }
    }
    val playerState by MusicPlayerManager.state.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp)
            .testTag("sound_picker_modal"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ajouter un son",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher un son ou artiste...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoading && availableSounds.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else if (filteredSounds.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Aucun son trouvé",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onRefresh) {
                                Text("Recharger les sons")
                            }
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSounds, key = { it.id }) { track ->
                            val isPlaying = playerState.isPlaying && (
                                playerState.currentTrack?.id == track.id ||
                                playerState.currentTrack?.title == track.title
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectSound(track) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledIconButton(
                                        onClick = {
                                            if (isPlaying) MusicPlayerManager.togglePlayPause()
                                            else MusicPlayerManager.playTrack(track)
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Lire",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "@${track.artist}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = { onSelectSound(track) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Choisir", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

