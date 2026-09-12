package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkForestGreen

@Composable
fun AppSplashScreen(
    appName: String = "Digital Tool",
    subtitle: String = "Smart Digital Utility Tools"
) {
    val context = LocalContext.current
    val logoBitmap = remember(context) {
        try {
            BitmapFactory.decodeResource(context.resources, com.example.R.drawable.app_logo_foreground)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val logoAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        logoAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(700, easing = LinearEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.primary,
                        Color(0xFF090D16)
                    )
                )
            )
            .testTag("app_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon Badge with glowing ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(logoAnim.value * scale)
                    .size(110.dp)
                    .background(Color(0x33FFFFFF), CircleShape)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(90.dp)
                        .background(Color(0xFFFFF8EE), CircleShape)
                ) {
                    if (logoBitmap != null) {
                        Image(
                            bitmap = logoBitmap,
                            contentDescription = "App Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = "App Logo",
                            tint = DarkForestGreen,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Title
            Text(
                text = appName,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFF8EE),
                modifier = Modifier
                    .scale(logoAnim.value)
                    .alpha(logoAnim.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = subtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFEAD8C8),
                modifier = Modifier
                    .scale(logoAnim.value)
                    .alpha(logoAnim.value * 0.9f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Animated Loader
            CircularProgressIndicator(
                color = Color(0xFFFFF8EE),
                strokeWidth = 3.5.dp,
                modifier = Modifier
                    .size(36.dp)
                    .alpha(alpha)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "অপেক্ষা করুন, লোড হচ্ছে...",
                fontSize = 13.sp,
                color = Color(0xFFD0C0B0),
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}
