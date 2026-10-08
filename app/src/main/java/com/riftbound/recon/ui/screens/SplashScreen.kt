package com.riftbound.recon.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riftbound.recon.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Animation States
    val animProgress = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textSlide = remember { Animatable(20f) }

    // Colors according to theme
    val bgColor = if (isDark) DarkBackground else LightBackground
    val runeRingColor = if (isDark) PrimaryPurple else LightPrimary
    val laserColor = if (isDark) SecondaryTeal else LightSecondary
    val bladeColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val bladeBorderColor = if (isDark) PrimaryPurple.copy(alpha = 0.7f) else LightPrimary.copy(alpha = 0.7f)
    val textColor = if (isDark) TextPrimary else LightOnBackground
    val subtitleColor = if (isDark) TextSecondary else LightOnSurfaceVariant

    // Continuous subtle pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        // Animate emblem in 0.0s -> 0.8s
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )

        // Animate text slide & fade
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350, easing = LinearEasing)
        )
        textSlide.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )

        // Hold briefly for branding perception
        delay(400)

        // Transition to main screen
        onAnimationComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Arcane Aperture Animated Canvas Emblem
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(0.85f + (animProgress.value * 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArcaneAperture(
                        progress = animProgress.value,
                        pulseGlow = pulseGlow,
                        runeColor = runeRingColor,
                        laserColor = laserColor,
                        bladeFill = bladeColor,
                        bladeBorder = bladeBorderColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Animated Typography Lockup
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = textSlide.value.dp)
                    .alpha(textAlpha.value)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = textColor)) {
                            append("RIFTBOUND ")
                        }
                        withStyle(SpanStyle(color = laserColor)) {
                            append("RECON")
                        }
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Reconhecimento Óptico & Indexação Posicional",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = subtitleColor,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

private fun DrawScope.drawArcaneAperture(
    progress: Float,
    pulseGlow: Float,
    runeColor: Color,
    laserColor: Color,
    bladeFill: Color,
    bladeBorder: Color
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxRadius = size.minDimension / 2f * 0.88f
    val currentRadius = maxRadius * progress

    if (currentRadius <= 0f) return

    // 1. Outer Arcane Ring with Rotation
    val rotation = progress * 60f
    rotate(degrees = rotation, pivot = center) {
        // Outer Glow/Stroke Ring
        drawCircle(
            color = runeColor.copy(alpha = 0.85f),
            radius = currentRadius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )

        // Subtle Secondary Inner Ring
        drawCircle(
            color = runeColor.copy(alpha = 0.35f),
            radius = currentRadius * 0.82f,
            center = center,
            style = Stroke(width = 1.2.dp.toPx())
        )

        // 8 Arcane Cardinal & Intercardinal Notches / Runes
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45.0))
            val startDist = currentRadius * 0.82f
            val endDist = currentRadius * 1.06f
            val startX = center.x + (startDist * cos(angle)).toFloat()
            val startY = center.y + (startDist * sin(angle)).toFloat()
            val endX = center.x + (endDist * cos(angle)).toFloat()
            val endY = center.y + (endDist * sin(angle)).toFloat()

            drawLine(
                color = if (i % 2 == 0) laserColor else runeColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = if (i % 2 == 0) 2.5.dp.toPx() else 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }

    // 2. Optical Camera Aperture Shutter Blades (6 Blades)
    val apertureRadius = currentRadius * 0.65f
    val bladeAngleOffset = (1f - progress) * 30f

    rotate(degrees = -rotation * 0.5f + bladeAngleOffset, pivot = center) {
        for (b in 0 until 6) {
            val a1 = Math.toRadians((b * 60.0))
            val a2 = Math.toRadians(((b + 1) * 60.0))
            val innerA = Math.toRadians((b * 60.0 + 30.0))

            val p1 = Offset(
                center.x + (apertureRadius * cos(a1)).toFloat(),
                center.y + (apertureRadius * sin(a1)).toFloat()
            )
            val p2 = Offset(
                center.x + (apertureRadius * cos(a2)).toFloat(),
                center.y + (apertureRadius * sin(a2)).toFloat()
            )
            val innerR = apertureRadius * (0.28f + (1f - progress) * 0.3f)
            val pCenter = Offset(
                center.x + (innerR * cos(innerA)).toFloat(),
                center.y + (innerR * sin(innerA)).toFloat()
            )

            val path = Path().apply {
                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(pCenter.x, pCenter.y)
                close()
            }

            drawPath(path = path, color = bladeFill)
            drawPath(path = path, color = bladeBorder, style = Stroke(width = 1.5.dp.toPx()))
        }
    }

    // 3. Central Laser Scanning Beam Ray
    val laserAngle = Math.toRadians((progress * 360.0 - 45.0))
    val laserLen = apertureRadius * 0.85f
    val lx = center.x + (laserLen * cos(laserAngle)).toFloat()
    val ly = center.y + (laserLen * sin(laserAngle)).toFloat()

    drawLine(
        color = laserColor.copy(alpha = 0.9f),
        start = center,
        end = Offset(lx, ly),
        strokeWidth = 2.5.dp.toPx(),
        cap = StrokeCap.Round
    )

    // 4. Central Optic Core Lens (Glowing Center)
    val coreRadius = 8.dp.toPx() * (progress * 0.9f + (pulseGlow - 0.85f) * 0.5f)
    drawCircle(
        color = laserColor,
        radius = coreRadius,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = coreRadius * 0.45f,
        center = center
    )
}
