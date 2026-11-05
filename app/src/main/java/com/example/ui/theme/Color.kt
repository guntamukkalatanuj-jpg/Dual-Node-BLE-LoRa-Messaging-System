package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Minimalist High-Contrast Obsidian & Cyber Dark Palette
val BluBlack = Color(0xFF04060E)
val BluBlackAlt = Color(0xFF080D1D)
val BluDarkNavy = Color(0xFF0D162B)
val BluSurface = Color(0xFF13203C)
val BluSurfaceElevated = Color(0xFF1A2A4E)
val BluSurfaceHover = Color(0xFF223663)

// Border tokens with cyber glow
val BluBorder = Color(0xFF1E3A8A)
val BluBorderSubtle = Color(0xFF1A2A4A)
val BluBorderGlow = Color(0xFF38BDF8)

// Neon & Accent Colors
val BluElectric = Color(0xFF2563EB)
val BluElectricBright = Color(0xFF3B82F6)
val BluCyan = Color(0xFF38BDF8)
val BluCyanNeon = Color(0xFF00F0FF)
val BluCyanGlow = Color(0xFF06B6D4)

// Semantic State Colors
val BluSuccess = Color(0xFF10B981)
val BluSuccessBright = Color(0xFF34D399)
val BluWarning = Color(0xFFF59E0B)
val BluWarningBright = Color(0xFFFBBF24)
val BluError = Color(0xFFEF4444)
val BluErrorBright = Color(0xFFF87171)

// Typography Text Tokens
val BluTextPrimary = Color(0xFFF8FAFC)
val BluTextSecondary = Color(0xFF94A3B8)
val BluTextTertiary = Color(0xFF64748B)
val BluTextCyan = Color(0xFF7DD3FC)

// Dynamic Tactical Gradients
val BluBackgroundGradient = Brush.verticalGradient(
    colors = listOf(BluBlackAlt, BluBlack)
)
val BluCardGradient = Brush.verticalGradient(
    colors = listOf(BluDarkNavy, BluBlackAlt)
)
val BluCardElevatedGradient = Brush.verticalGradient(
    colors = listOf(BluSurface, BluDarkNavy)
)
val BluAccentGradient = Brush.horizontalGradient(
    colors = listOf(BluCyan, BluElectricBright)
)
val BluEmergencyGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF7F1D1D), Color(0xFF450A0A))
)

