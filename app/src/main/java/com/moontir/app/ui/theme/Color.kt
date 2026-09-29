package com.moontir.app.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Palette (Image 1 Swatches)
val MoontirSageGreen = Color(0xFFD8E2AE)       // #D8E2AE: Soft Pistachio / Sage Green accent
val MoontirSageCream = Color(0xFFEBF0D5)       // #EBF0D5: Pale Sage Cream (Hero & Circle Card Background)
val MoontirWarmIvory = Color(0xFFF5F0E6)       // #F5F0E6: Warm Eggshell / Ivory Cream (Light mode background)
val MoontirDarkEspresso = Color(0xFF403A35)    // #403A35: Dark Espresso / Warm Charcoal (Light mode primary text & button)

// Dark Surfaces (Warm Dark Espresso base matching Image 3)
val DarkSurface = Color(0xFF191715)            // Warm Dark Espresso background
val DarkOnSurface = MoontirWarmIvory           // Warm Ivory Cream text
val DarkSurfaceSecondary = Color(0xFF26221F)   // Dark Card Container
val DarkOnSurfaceSecondary = Color(0xFFC7C2B8) // Secondary Muted text
val DarkSurfaceTertiary = Color(0xFF332E29)    // Elevated Surface / Item Chip
val DarkOnSurfaceTertiary = Color(0xFF9E978E)

// Light Surfaces (Warm Ivory base matching Image 2)
val LightSurface = MoontirWarmIvory            // Warm Eggshell Ivory background
val LightOnSurface = MoontirDarkEspresso       // Dark Espresso primary text
val LightSurfaceSecondary = MoontirSageCream   // Pale Sage Cream Card Container
val LightOnSurfaceSecondary = Color(0xFF5C544D)// Secondary Warm Charcoal text
val LightSurfaceTertiary = Color(0xFFE2E7CC)   // Elevated Surface / Chip
val LightOnSurfaceTertiary = Color(0xFF756C64)

// Borders & Dividers
val DarkBorder = Color(0x26F5F0E6)
val DarkBorderStrong = Color(0x66D8E2AE)
val DarkDivider = Color(0x14F5F0E6)

val LightBorder = Color(0x26403A35)
val LightBorderStrong = Color(0x66403A35)
val LightDivider = Color(0x14403A35)

// Status
val StatusSuccess = Color(0xFF10B981)
val StatusWarning = Color(0xFFF59E0B)
val StatusError = Color(0xFFEF4444)
val MutedText = Color(0xFF8C857B)
