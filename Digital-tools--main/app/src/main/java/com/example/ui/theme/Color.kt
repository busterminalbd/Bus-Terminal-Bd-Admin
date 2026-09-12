package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==============================
// 🎨 MODERN COLOR SYSTEM & THEMES
// ==============================

data class AppThemePalette(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val primary: Color,
    val darkPrimary: Color,
    val accent: Color,
    val container: Color,
    val onContainer: Color,
    val darkPrimaryAccent: Color,
    val previewGradient: List<Color>
)

// Pre-defined Theme Color Palettes
val ThemePalettes = listOf(
    AppThemePalette(
        id = "emerald",
        nameEn = "Emerald Forest",
        nameBn = "পান্না সবুজ",
        primary = Color(0xFF0F766E), // Deep Emerald / Teal 700
        darkPrimary = Color(0xFF134E4A),
        accent = Color(0xFF14B8A6),
        container = Color(0xFFCCFBF1),
        onContainer = Color(0xFF0F766E),
        darkPrimaryAccent = Color(0xFF2DD4BF),
        previewGradient = listOf(Color(0xFF134E4A), Color(0xFF0F766E), Color(0xFF14B8A6))
    ),
    AppThemePalette(
        id = "indigo",
        nameEn = "Deep Indigo",
        nameBn = "ডিপ ইন্ডিগো",
        primary = Color(0xFF4F46E5), // Indigo 600
        darkPrimary = Color(0xFF3730A3),
        accent = Color(0xFF818CF8),
        container = Color(0xFFE0E7FF),
        onContainer = Color(0xFF3730A3),
        darkPrimaryAccent = Color(0xFF818CF8),
        previewGradient = listOf(Color(0xFF3730A3), Color(0xFF4F46E5), Color(0xFF818CF8))
    ),
    AppThemePalette(
        id = "cobalt",
        nameEn = "Royal Cobalt",
        nameBn = "রয়্যাল ব্লু",
        primary = Color(0xFF2563EB), // Blue 600
        darkPrimary = Color(0xFF1D4ED8),
        accent = Color(0xFF38BDF8),
        container = Color(0xFFDBEAFE),
        onContainer = Color(0xFF1E40AF),
        darkPrimaryAccent = Color(0xFF60A5FA),
        previewGradient = listOf(Color(0xFF1D4ED8), Color(0xFF2563EB), Color(0xFF38BDF8))
    ),
    AppThemePalette(
        id = "violet",
        nameEn = "Royal Violet",
        nameBn = "রাজকীয় ভায়োলেট",
        primary = Color(0xFF7C3AED), // Violet 600
        darkPrimary = Color(0xFF5B21B6),
        accent = Color(0xFFA78BFA),
        container = Color(0xFFEDE9FE),
        onContainer = Color(0xFF5B21B6),
        darkPrimaryAccent = Color(0xFFA78BFA),
        previewGradient = listOf(Color(0xFF5B21B6), Color(0xFF7C3AED), Color(0xFFA78BFA))
    ),
    AppThemePalette(
        id = "rose",
        nameEn = "Ruby Crimson",
        nameBn = "রুবি লাল",
        primary = Color(0xFFE11D48), // Rose 600
        darkPrimary = Color(0xFF9F1239),
        accent = Color(0xFFFB7185),
        container = Color(0xFFFFE4E6),
        onContainer = Color(0xFF9F1239),
        darkPrimaryAccent = Color(0xFFFB7185),
        previewGradient = listOf(Color(0xFF9F1239), Color(0xFFE11D48), Color(0xFFFB7185))
    ),
    AppThemePalette(
        id = "amber",
        nameEn = "Sunset Amber",
        nameBn = "সানসেট গোল্ড",
        primary = Color(0xFFD97706), // Amber 600
        darkPrimary = Color(0xFF92400E),
        accent = Color(0xFFFBBF24),
        container = Color(0xFFFEF3C7),
        onContainer = Color(0xFF92400E),
        darkPrimaryAccent = Color(0xFFFBBF24),
        previewGradient = listOf(Color(0xFF92400E), Color(0xFFD97706), Color(0xFFFBBF24))
    ),
    AppThemePalette(
        id = "classic_vintage",
        nameEn = "Classic Vintage",
        nameBn = "ক্ল্যাসিক ভিন্টেজ (প্রিমিয়াম)",
        primary = Color(0xFF1E3A2F), // Deep vintage forest green
        darkPrimary = Color(0xFF11221B),
        accent = Color(0xFFB8860B), // Dark Goldenrod
        container = Color(0xFFE8ECE9),
        onContainer = Color(0xFF1E3A2F),
        darkPrimaryAccent = Color(0xFFD4AF37),
        previewGradient = listOf(Color(0xFF11221B), Color(0xFF1E3A2F), Color(0xFFD4AF37))
    ),
    AppThemePalette(
        id = "onyx_gold",
        nameEn = "Onyx & Gold",
        nameBn = "অনিল ও গোল্ড (লাক্সারি)",
        primary = Color(0xFF1F2421), // Luxurious Onyx Slate
        darkPrimary = Color(0xFF141715),
        accent = Color(0xFFC5A059), // Rich Champagne Gold
        container = Color(0xFFF4F1EA),
        onContainer = Color(0xFF1F2421),
        darkPrimaryAccent = Color(0xFFE5C158),
        previewGradient = listOf(Color(0xFF141715), Color(0xFF262D2A), Color(0xFFC5A059))
    )
)

fun getThemePalette(id: String): AppThemePalette {
    return ThemePalettes.find { it.id == id } ?: ThemePalettes.first()
}

// Default Modern Emerald Palette values
val PrimaryEmerald = Color(0xFF0F766E)
val DarkPrimaryEmerald = Color(0xFF134E4A)
val MintAccent = Color(0xFF14B8A6)
val LightMintBg = Color(0xFFF0FDFA)

// Neutral Palette (Clean Modern Slate)
val MainBackground = Color(0xFFF8FAFC)
val SurfaceCard = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val TextPlaceholder = Color(0xFF94A3B8)
val BorderColor = Color(0xFFE2E8F0)
val DividerColor = Color(0xFFF1F5F9)

// Semantic State Palette
val SuccessGreen = Color(0xFF16A34A)
val SuccessBg = Color(0xFFDCFCE7)
val ErrorRed = Color(0xFFDC2626)
val ErrorBg = Color(0xFFFEE2E2)
val WarningOrange = Color(0xFFEA580C)
val WarningBg = Color(0xFFFFEDD5)

// Dark Mode Neutral Palette (Sleek Dark Slate)
val DarkMainBackground = Color(0xFF0B0F19)
val DarkSurfaceCard = Color(0xFF111827)
val DarkElevatedSurface = Color(0xFF1F2937)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkBorderColor = Color(0xFF374151)
val DarkSuccessGreen = Color(0xFF4ADE80)
val DarkErrorRed = Color(0xFFF87171)
val DarkWarningOrange = Color(0xFFFB923C)

// Backward Compatibility Aliases
val PrimaryBlue = PrimaryEmerald
val DarkPrimaryBlue = DarkPrimaryEmerald
val SkyBlueAccent = MintAccent
val LightBlueBg = LightMintBg
val DarkModePrimaryBlue = Color(0xFF2DD4BF)
val DarkAccentBlue = MintAccent

val DarkForestGreen = PrimaryEmerald
val LightForestGreen = DarkPrimaryEmerald
val ForestGreenText = TextPrimary

val CreamPaperBg = SurfaceCard
val WarmBorderColor = BorderColor

val BrassAccent = MintAccent
val StampBlue = PrimaryEmerald
val StampBlueDark = DarkPrimaryEmerald
val LedgerRed = ErrorRed

val MaroonHeaderColor = DarkPrimaryEmerald
val MaroonTextColor = PrimaryEmerald

