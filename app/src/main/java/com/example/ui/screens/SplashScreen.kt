package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.compose.ui.platform.LocalContext

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val context = LocalContext.current
    val logoScale = remember { Animatable(0.85f) }
    val logoAlpha = remember { Animatable(0f) }

    val permissionsToRequest = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Proceed regardless of permissions
    }

    LaunchedEffect(key1 = true) {
        try {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Soft scale-in and fade-in animation for the logo
        launch {
            logoScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 1200,
                    easing = FastOutSlowInEasing
                )
            )
        }
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = LinearOutSlowInEasing
                )
            )
        }
        
        delay(2200)
        onTimeout()
    }

    // Infinite breathing/pulsing animation for the dot (similar to ChatGPT / AI indicators)
    val infiniteTransition = rememberInfiniteTransition(label = "dot_breathe")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // High-fidelity clean dark background
        contentAlignment = Alignment.Center
    ) {
        // Center-aligned branding logo
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(logoScale.value)
                .alpha(logoAlpha.value),
            contentAlignment = Alignment.Center
        ) {
            com.example.ui.components.AppDynamicLogo(
                size = 110.dp,
                elevation = 8.dp,
                showGlow = true
            )
        }

        // ChatGPT-style pulsing dot at the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 64.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(dotScale)
                    .alpha(dotAlpha)
                    .background(
                        color = MaterialTheme.colorScheme.primary, // Premium theme primary color for branding consistency
                        shape = CircleShape
                    )
            )
        }
    }
}
