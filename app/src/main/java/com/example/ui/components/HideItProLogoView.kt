package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.HideItColorPreset
import com.example.utils.HideItModel
import com.example.utils.HideItProManager

@Composable
fun AppDynamicLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    elevation: Dp = 0.dp,
    showGlow: Boolean = false
) {
    val currentModel by HideItProManager.currentModel.collectAsState()
    val currentColor by HideItProManager.currentColor.collectAsState()

    HideItProLogoView(
        model = currentModel,
        colorPreset = currentColor,
        modifier = modifier,
        size = size,
        elevation = elevation,
        showGlow = showGlow
    )
}

@Composable
fun HideItProLogoView(
    model: HideItModel,
    colorPreset: HideItColorPreset,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    elevation: Dp = 10.dp,
    showGlow: Boolean = true
) {
    val animatedPrimary by animateColorAsState(
        targetValue = colorPreset.primaryColor,
        animationSpec = tween(durationMillis = 400),
        label = "primary_color"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = colorPreset.secondaryColor,
        animationSpec = tween(durationMillis = 400),
        label = "secondary_color"
    )
    val animatedAccent by animateColorAsState(
        targetValue = colorPreset.accentColor,
        animationSpec = tween(durationMillis = 400),
        label = "accent_color"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Soft outer ambient glow for high-end feel
        if (showGlow) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.92f)
                    .clip(RoundedCornerShape(size * 0.28f))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                animatedPrimary.copy(alpha = 0.45f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Main app icon container
        Surface(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .shadow(elevation, RoundedCornerShape(size * 0.24f), spotColor = animatedPrimary),
            shape = RoundedCornerShape(size * 0.24f),
            color = Color(0xFF0B0F19),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        animatedAccent.copy(alpha = 0.8f),
                        animatedPrimary.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.15f)
                    )
                )
            )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = this.size.width
                val canvasHeight = this.size.height

                // Draw rich metallic / gradient background
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedSecondary.copy(alpha = 0.7f),
                            Color(0xFF0F172A),
                            Color(0xFF020617)
                        ),
                        center = Offset(canvasWidth * 0.35f, canvasHeight * 0.3f),
                        radius = canvasWidth * 0.9f
                    )
                )

                // Sub-model graphics rendering
                when (model) {
                    HideItModel.ORIGINAL_NEO -> {
                        drawOriginalNeoLogo(animatedPrimary, animatedAccent, canvasWidth, canvasHeight)
                    }
                    HideItModel.CYBER_SHIELD -> {
                        drawCyberShieldLogo(animatedPrimary, animatedAccent, canvasWidth, canvasHeight)
                    }
                    HideItModel.QUANTUM_EYE -> {
                        drawQuantumEyeLogo(animatedPrimary, animatedAccent, canvasWidth, canvasHeight)
                    }
                    HideItModel.GOLD_VIP -> {
                        drawGoldVipLogo(animatedPrimary, animatedAccent, canvasWidth, canvasHeight)
                    }
                    HideItModel.STEALTH_CALCULATOR -> {
                        drawStealthCalculatorLogo(animatedPrimary, animatedAccent, canvasWidth, canvasHeight)
                    }
                    HideItModel.STEALTH_NOTES -> {
                        drawStealthNotesLogo(animatedPrimary, animatedAccent, canvasWidth, canvasHeight)
                    }
                    HideItModel.STEALTH_WEATHER -> {
                        drawStealthWeatherLogo(animatedPrimary, animatedAccent, canvasWidth, canvasHeight)
                    }
                    HideItModel.STEALTH_AUDIO -> {
                        drawStealthAudioLogo(animatedPrimary, animatedAccent, canvasWidth, canvasHeight)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawOriginalNeoLogo(primary: Color, accent: Color, w: Float, h: Float) {
    // Red central disc with neon stroke
    drawCircle(
        brush = Brush.linearGradient(
            listOf(primary, primary.copy(alpha = 0.85f))
        ),
        radius = w * 0.38f,
        center = Offset(w * 0.5f, h * 0.5f)
    )

    // Inner Cat silhouette (White)
    val catPath = Path().apply {
        moveTo(w * 0.30f, h * 0.28f) // Left ear top
        lineTo(w * 0.42f, h * 0.42f)
        lineTo(w * 0.58f, h * 0.42f)
        lineTo(w * 0.70f, h * 0.28f) // Right ear top
        lineTo(w * 0.75f, h * 0.54f) // Cheek right
        cubicTo(w * 0.75f, h * 0.72f, w * 0.62f, h * 0.78f, w * 0.50f, h * 0.78f) // Chin
        cubicTo(w * 0.38f, h * 0.78f, w * 0.25f, h * 0.72f, w * 0.25f, h * 0.54f)
        close()
    }
    drawPath(catPath, color = Color.White)

    // Sleek Cat Eyes in Primary Color
    drawOval(
        color = primary,
        topLeft = Offset(w * 0.36f, h * 0.50f),
        size = Size(w * 0.09f, h * 0.05f)
    )
    drawOval(
        color = primary,
        topLeft = Offset(w * 0.55f, h * 0.50f),
        size = Size(w * 0.09f, h * 0.05f)
    )

    // Small nose
    val nose = Path().apply {
        moveTo(w * 0.50f, h * 0.59f)
        lineTo(w * 0.47f, h * 0.63f)
        lineTo(w * 0.53f, h * 0.63f)
        close()
    }
    drawPath(nose, color = primary)
}

private fun DrawScope.drawCyberShieldLogo(primary: Color, accent: Color, w: Float, h: Float) {
    // Shield polygon
    val shield = Path().apply {
        moveTo(w * 0.5f, h * 0.18f)
        lineTo(w * 0.78f, h * 0.30f)
        lineTo(w * 0.78f, h * 0.56f)
        cubicTo(w * 0.78f, h * 0.74f, w * 0.5f, h * 0.85f, w * 0.5f, h * 0.85f)
        cubicTo(w * 0.5f, h * 0.85f, w * 0.22f, h * 0.74f, w * 0.22f, h * 0.56f)
        lineTo(w * 0.22f, h * 0.30f)
        close()
    }
    drawPath(
        shield,
        brush = Brush.linearGradient(
            listOf(primary.copy(alpha = 0.3f), primary.copy(alpha = 0.7f)),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
    )
    drawPath(
        shield,
        color = accent,
        style = Stroke(width = w * 0.04f, cap = StrokeCap.Round)
    )

    // Inner lock / core keyhole
    drawCircle(
        color = Color.White,
        radius = w * 0.10f,
        center = Offset(w * 0.5f, h * 0.48f)
    )
    val keyhole = Path().apply {
        moveTo(w * 0.45f, h * 0.50f)
        lineTo(w * 0.55f, h * 0.50f)
        lineTo(w * 0.58f, h * 0.66f)
        lineTo(w * 0.42f, h * 0.66f)
        close()
    }
    drawPath(keyhole, color = Color.White)
}

private fun DrawScope.drawQuantumEyeLogo(primary: Color, accent: Color, w: Float, h: Float) {
    // Outer eye outline
    val eyePath = Path().apply {
        moveTo(w * 0.16f, h * 0.50f)
        cubicTo(w * 0.32f, h * 0.25f, w * 0.68f, h * 0.25f, w * 0.84f, h * 0.50f)
        cubicTo(w * 0.68f, h * 0.75f, w * 0.32f, h * 0.75f, w * 0.16f, h * 0.50f)
        close()
    }
    drawPath(
        eyePath,
        brush = Brush.linearGradient(listOf(primary, accent)),
        style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
    )

    // Iris circle
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, accent, primary)
        ),
        radius = w * 0.18f,
        center = Offset(w * 0.5f, h * 0.5f)
    )

    // Pupil
    drawCircle(
        color = Color(0xFF0F172A),
        radius = w * 0.08f,
        center = Offset(w * 0.5f, h * 0.5f)
    )
    // Quantum glint
    drawCircle(
        color = Color.White,
        radius = w * 0.025f,
        center = Offset(w * 0.46f, h * 0.46f)
    )
}

private fun DrawScope.drawGoldVipLogo(primary: Color, accent: Color, w: Float, h: Float) {
    // Crown VIP polygon
    val crown = Path().apply {
        moveTo(w * 0.20f, h * 0.70f)
        lineTo(w * 0.80f, h * 0.70f)
        lineTo(w * 0.85f, h * 0.40f)
        lineTo(w * 0.66f, h * 0.52f)
        lineTo(w * 0.50f, h * 0.30f)
        lineTo(w * 0.34f, h * 0.52f)
        lineTo(w * 0.15f, h * 0.40f)
        close()
    }
    drawPath(
        crown,
        brush = Brush.linearGradient(
            listOf(Color(0xFFFFFBEB), Color(0xFFFFD700), Color(0xFFB45309))
        )
    )
    // Crown base jewel bar
    drawRoundRect(
        color = Color(0xFFFFFBEB),
        topLeft = Offset(w * 0.20f, h * 0.72f),
        size = Size(w * 0.60f, h * 0.06f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
    )
    // Three jewels on points
    drawCircle(color = Color.White, radius = w * 0.03f, center = Offset(w * 0.15f, h * 0.38f))
    drawCircle(color = Color.White, radius = w * 0.04f, center = Offset(w * 0.50f, h * 0.28f))
    drawCircle(color = Color.White, radius = w * 0.03f, center = Offset(w * 0.85f, h * 0.38f))
}

private fun DrawScope.drawStealthCalculatorLogo(primary: Color, accent: Color, w: Float, h: Float) {
    // Body of calculator
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(w * 0.18f, h * 0.14f),
        size = Size(w * 0.64f, h * 0.72f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    // Display screen
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(w * 0.24f, h * 0.20f),
        size = Size(w * 0.52f, h * 0.16f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
    )
    // Display numbers (Green accent digits)
    drawRect(
        color = accent,
        topLeft = Offset(w * 0.60f, h * 0.26f),
        size = Size(w * 0.12f, h * 0.04f)
    )
    // Buttons grid
    val btnCols = 3
    val btnRows = 3
    val startX = w * 0.25f
    val startY = h * 0.42f
    val btnW = w * 0.14f
    val btnH = h * 0.09f
    val gap = w * 0.04f

    for (r in 0 until btnRows) {
        for (c in 0 until btnCols) {
            val isEquals = (r == 2 && c == 2)
            val btnColor = if (isEquals) accent else Color(0xFF334155)
            drawRoundRect(
                color = btnColor,
                topLeft = Offset(startX + c * (btnW + gap), startY + r * (btnH + gap)),
                size = Size(btnW, btnH),
                cornerRadius = CornerRadius(w * 0.025f, w * 0.025f)
            )
        }
    }
}

private fun DrawScope.drawStealthNotesLogo(primary: Color, accent: Color, w: Float, h: Float) {
    // Pad background
    drawRoundRect(
        color = primary,
        topLeft = Offset(w * 0.18f, h * 0.14f),
        size = Size(w * 0.64f, h * 0.72f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    // Inner page
    drawRoundRect(
        color = Color(0xFFFEF3C7),
        topLeft = Offset(w * 0.24f, h * 0.20f),
        size = Size(w * 0.52f, h * 0.60f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
    )
    // Ruled lines
    val lines = 4
    val lineStartY = h * 0.30f
    val lineSpacing = h * 0.10f
    for (i in 0 until lines) {
        drawRect(
            color = Color(0xFFD97706),
            topLeft = Offset(w * 0.30f, lineStartY + i * lineSpacing),
            size = Size(w * 0.40f, h * 0.025f)
        )
    }
}

private fun DrawScope.drawStealthWeatherLogo(primary: Color, accent: Color, w: Float, h: Float) {
    // Sky background
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1))),
        topLeft = Offset(w * 0.18f, h * 0.14f),
        size = Size(w * 0.64f, h * 0.72f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    // Sun
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFFFDE047), Color(0xFFF59E0B))),
        radius = w * 0.14f,
        center = Offset(w * 0.56f, h * 0.40f)
    )
    // Cloud
    val cloud = Path().apply {
        moveTo(w * 0.30f, h * 0.66f)
        lineTo(w * 0.68f, h * 0.66f)
        cubicTo(w * 0.76f, h * 0.66f, w * 0.76f, h * 0.52f, w * 0.68f, h * 0.52f)
        cubicTo(w * 0.68f, h * 0.42f, w * 0.52f, h * 0.40f, w * 0.46f, h * 0.48f)
        cubicTo(w * 0.40f, h * 0.46f, w * 0.32f, h * 0.50f, w * 0.32f, h * 0.58f)
        cubicTo(w * 0.24f, h * 0.58f, w * 0.24f, h * 0.66f, w * 0.30f, h * 0.66f)
        close()
    }
    drawPath(cloud, color = Color.White)
}

private fun DrawScope.drawStealthAudioLogo(primary: Color, accent: Color, w: Float, h: Float) {
    // Dark player background
    drawRoundRect(
        color = Color(0xFF18181B),
        topLeft = Offset(w * 0.18f, h * 0.14f),
        size = Size(w * 0.64f, h * 0.72f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    // Neon wave circle
    drawCircle(
        brush = Brush.radialGradient(listOf(accent, primary)),
        radius = w * 0.22f,
        center = Offset(w * 0.5f, h * 0.5f)
    )
    // Play / equalizer symbol
    val play = Path().apply {
        moveTo(w * 0.44f, h * 0.40f)
        lineTo(w * 0.62f, h * 0.50f)
        lineTo(w * 0.44f, h * 0.60f)
        close()
    }
    drawPath(play, color = Color.White)
}
