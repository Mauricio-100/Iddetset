package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.IddetViewModel

enum class AuthMode {
    INITIAL, LOGIN, SIGNUP_CREDENTIALS, SIGNUP_PERSONAL, SIGNUP_IDENTITY, SIGNUP_AVATAR
}

val PRESET_AVATARS = listOf(
    "https://images.unsplash.com/photo-1543852786-1cf6624b9987?w=400&q=80",
    "https://images.unsplash.com/photo-1517849845537-4d257902454a?w=400&q=80",
    "https://images.unsplash.com/photo-1474511320723-9a56873867b5?w=400&q=80",
    "https://images.unsplash.com/photo-1534361960057-19889db9621e?w=400&q=80",
    "https://images.unsplash.com/photo-1507146426996-ef05306b995a?w=400&q=80",
    "https://images.unsplash.com/photo-1555685812-4b943f1cb0eb?w=400&q=80"
)

val ZODIAC_SIGNS = listOf(
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(viewModel: IddetViewModel) {
    var mode by remember { mutableStateOf(AuthMode.INITIAL) }
    val currentTargetLang by viewModel.targetLanguage.collectAsState()
    var languageMenuExpanded by remember { mutableStateOf(false) }
    
    // Login / Credentials
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // Additional Signup Fields
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var zodiac by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    
    val authError by viewModel.authError.collectAsState()

    LaunchedEffect(mode, username, password) {
        viewModel.clearAuthError()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .imePadding()
    ) {
        // Top Bar with Back Button & Language Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (mode != AuthMode.INITIAL) {
                IconButton(
                    onClick = {
                        mode = when (mode) {
                            AuthMode.LOGIN, AuthMode.SIGNUP_CREDENTIALS -> AuthMode.INITIAL
                            AuthMode.SIGNUP_PERSONAL -> AuthMode.SIGNUP_CREDENTIALS
                            AuthMode.SIGNUP_IDENTITY -> AuthMode.SIGNUP_PERSONAL
                            AuthMode.SIGNUP_AVATAR -> AuthMode.SIGNUP_IDENTITY
                            else -> AuthMode.INITIAL
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Language Selector Button & Dropdown
            Box {
                AssistChip(
                    onClick = { languageMenuExpanded = true },
                    label = { Text(currentTargetLang, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                DropdownMenu(
                    expanded = languageMenuExpanded,
                    onDismissRequest = { languageMenuExpanded = false },
                    modifier = Modifier.heightIn(max = 280.dp)
                ) {
                    com.example.utils.TranslationHelper.supportedLanguages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang, fontWeight = if (lang == currentTargetLang) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                viewModel.setTargetLanguage(lang)
                                languageMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = if (mode == AuthMode.INITIAL) Arrangement.Center else Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (mode == AuthMode.INITIAL) {
                com.example.ui.components.AppDynamicLogo(
                    size = 96.dp,
                    elevation = 6.dp,
                    showGlow = true
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "IDDET",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Welcome to the Giant Network",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { mode = AuthMode.SIGNUP_CREDENTIALS },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Get Started", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { mode = AuthMode.LOGIN },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Log In")
                }

                val savedAccounts by viewModel.savedAccounts.collectAsState()
                if (savedAccounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Saved Accounts",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.height(240.dp)
                    ) {
                        items(savedAccounts) { acc ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.loginWithSavedAccount(acc.token) }
                                    .padding(8.dp)
                            ) {
                                AsyncImage(
                                    model = acc.avatarUrl?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) } ?: "https://images.unsplash.com/photo-1511367461989-f85a21fda167?w=400&q=80",
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = acc.username,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                
                if (authError != null) {
                    Text(
                        text = authError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                AnimatedContent(targetState = mode, label = "Auth Mode") { currentMode ->
                    when (currentMode) {
                        AuthMode.LOGIN -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Welcome Back",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                OutlinedTextField(
                                    value = username, onValueChange = { username = it },
                                    label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = password, onValueChange = { password = it },
                                    label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                                    visualTransformation = PasswordVisualTransformation()
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Button(
                                    onClick = { if (username.isNotBlank()) viewModel.login(username, password) },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Log In", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        AuthMode.SIGNUP_CREDENTIALS -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Create Account",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Step 1 of 4",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                OutlinedTextField(
                                    value = username, onValueChange = { username = it },
                                    label = { Text("Choose a Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = password, onValueChange = { password = it },
                                    label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                                    visualTransformation = PasswordVisualTransformation()
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Button(
                                    onClick = { if (username.isNotBlank() && password.isNotBlank()) mode = AuthMode.SIGNUP_PERSONAL },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Next", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        AuthMode.SIGNUP_PERSONAL -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Personal Info",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Step 2 of 4",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                OutlinedTextField(
                                    value = email, onValueChange = { email = it },
                                    label = { Text("Email (Optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = phone, onValueChange = { phone = it },
                                    label = { Text("Phone Number (Optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = birthDate, onValueChange = { birthDate = it },
                                    label = { Text("Date of Birth (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Button(
                                    onClick = { mode = AuthMode.SIGNUP_IDENTITY },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Next", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        AuthMode.SIGNUP_IDENTITY -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Your Identity",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Step 3 of 4",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                OutlinedTextField(
                                    value = bio, onValueChange = { bio = it },
                                    label = { Text("Bio (Tell us about yourself)") }, modifier = Modifier.fillMaxWidth().height(120.dp),
                                    maxLines = 4
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(
                                    text = "Zodiac Sign (Optional)",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // Simple dropdown or chips. Let's do chips.
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ZODIAC_SIGNS.forEach { sign ->
                                        FilterChip(
                                            selected = zodiac == sign,
                                            onClick = { zodiac = if (zodiac == sign) "" else sign },
                                            label = { Text(sign) }
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                Button(
                                    onClick = { mode = AuthMode.SIGNUP_AVATAR },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Next", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        AuthMode.SIGNUP_AVATAR -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Pick an Avatar",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Final Step",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.height(240.dp)
                                ) {
                                    items(PRESET_AVATARS) { url ->
                                        val isSelected = avatarUrl == url
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(CircleShape)
                                                .clickable { avatarUrl = url }
                                                .border(
                                                    width = if (isSelected) 4.dp else 0.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                        ) {
                                            AsyncImage(
                                                model = url,
                                                contentDescription = "Avatar",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.4f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                Button(
                                    onClick = {
                                        viewModel.signupFull(
                                            username = username,
                                            password = password,
                                            avatarUrl = avatarUrl,
                                            bio = bio,
                                            email = email,
                                            phoneNumber = phone,
                                            birthDate = birthDate,
                                            zodiacSign = zodiac
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Complete Sign Up", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
