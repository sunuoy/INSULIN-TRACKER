package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.example.R

@Composable
fun buildThemeScheme(
    backgroundId: Int,
    surfaceId: Int,
    cardId: Int,
    primaryId: Int,
    accentId: Int,
    textId: Int,
    mutedId: Int,
    isDark: Boolean
): ColorScheme {
    val bg = colorResource(id = backgroundId)
    val surf = colorResource(id = surfaceId)
    val crd = colorResource(id = cardId)
    val prim = colorResource(id = primaryId)
    val acc = colorResource(id = accentId)
    val txt = colorResource(id = textId)
    val mtd = colorResource(id = mutedId)

    return if (isDark) {
        darkColorScheme(
            primary = prim,
            onPrimary = bg,
            primaryContainer = crd,
            onPrimaryContainer = txt,
            secondary = acc,
            secondaryContainer = crd,
            onSecondaryContainer = txt,
            tertiary = acc,
            tertiaryContainer = crd,
            onTertiaryContainer = txt,
            background = bg,
            onBackground = txt,
            surface = surf,
            onSurface = txt,
            surfaceVariant = crd,
            onSurfaceVariant = txt,
            outline = mtd
        )
    } else {
        lightColorScheme(
            primary = prim,
            onPrimary = Color.White,
            primaryContainer = crd,
            onPrimaryContainer = txt,
            secondary = acc,
            secondaryContainer = crd,
            onSecondaryContainer = txt,
            tertiary = acc,
            tertiaryContainer = crd,
            onTertiaryContainer = txt,
            background = bg,
            onBackground = txt,
            surface = surf,
            onSurface = txt,
            surfaceVariant = crd,
            onSurfaceVariant = mtd,
            outline = mtd
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeId: String = "arctic",
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeId.lowercase()) {
        "ocean_depth" -> buildThemeScheme(
            backgroundId = R.color.ocean_depth_background,
            surfaceId = R.color.ocean_depth_surface,
            cardId = R.color.ocean_depth_card,
            primaryId = R.color.ocean_depth_primary,
            accentId = R.color.ocean_depth_accent,
            textId = R.color.ocean_depth_text,
            mutedId = R.color.ocean_depth_muted,
            isDark = true
        )
        "aurora" -> buildThemeScheme(
            backgroundId = R.color.aurora_background,
            surfaceId = R.color.aurora_surface,
            cardId = R.color.aurora_card,
            primaryId = R.color.aurora_primary,
            accentId = R.color.aurora_accent,
            textId = R.color.aurora_text,
            mutedId = R.color.aurora_muted,
            isDark = true
        )
        "midnight_carbon" -> buildThemeScheme(
            backgroundId = R.color.midnight_carbon_background,
            surfaceId = R.color.midnight_carbon_surface,
            cardId = R.color.midnight_carbon_card,
            primaryId = R.color.midnight_carbon_primary,
            accentId = R.color.midnight_carbon_accent,
            textId = R.color.midnight_carbon_text,
            mutedId = R.color.midnight_carbon_muted,
            isDark = true
        )
        "sakura" -> buildThemeScheme(
            backgroundId = R.color.sakura_background,
            surfaceId = R.color.sakura_surface,
            cardId = R.color.sakura_card,
            primaryId = R.color.sakura_primary,
            accentId = R.color.sakura_accent,
            textId = R.color.sakura_text,
            mutedId = R.color.sakura_muted,
            isDark = false
        )
        "lemon_zest" -> buildThemeScheme(
            backgroundId = R.color.lemon_zest_background,
            surfaceId = R.color.lemon_zest_surface,
            cardId = R.color.lemon_zest_card,
            primaryId = R.color.lemon_zest_primary,
            accentId = R.color.lemon_zest_accent,
            textId = R.color.lemon_zest_text,
            mutedId = R.color.lemon_zest_muted,
            isDark = false
        )
        "forest_calm" -> buildThemeScheme(
            backgroundId = R.color.forest_calm_background,
            surfaceId = R.color.forest_calm_surface,
            cardId = R.color.forest_calm_card,
            primaryId = R.color.forest_calm_primary,
            accentId = R.color.forest_calm_accent,
            textId = R.color.forest_calm_text,
            mutedId = R.color.forest_calm_muted,
            isDark = true
        )
        "desert_sand" -> buildThemeScheme(
            backgroundId = R.color.desert_sand_background,
            surfaceId = R.color.desert_sand_surface,
            cardId = R.color.desert_sand_card,
            primaryId = R.color.desert_sand_primary,
            accentId = R.color.desert_sand_accent,
            textId = R.color.desert_sand_text,
            mutedId = R.color.desert_sand_muted,
            isDark = false
        )
        "neon_noir" -> buildThemeScheme(
            backgroundId = R.color.neon_noir_background,
            surfaceId = R.color.neon_noir_surface,
            cardId = R.color.neon_noir_card,
            primaryId = R.color.neon_noir_primary,
            accentId = R.color.neon_noir_accent,
            textId = R.color.neon_noir_text,
            mutedId = R.color.neon_noir_muted,
            isDark = true
        )
        "slate_pro" -> buildThemeScheme(
            backgroundId = R.color.slate_pro_background,
            surfaceId = R.color.slate_pro_surface,
            cardId = R.color.slate_pro_card,
            primaryId = R.color.slate_pro_primary,
            accentId = R.color.slate_pro_accent,
            textId = R.color.slate_pro_text,
            mutedId = R.color.slate_pro_muted,
            isDark = false
        )
        "royal_ink" -> buildThemeScheme(
            backgroundId = R.color.royal_ink_background,
            surfaceId = R.color.royal_ink_surface,
            cardId = R.color.royal_ink_card,
            primaryId = R.color.royal_ink_primary,
            accentId = R.color.royal_ink_accent,
            textId = R.color.royal_ink_text,
            mutedId = R.color.royal_ink_muted,
            isDark = true
        )
        "coral_bloom" -> buildThemeScheme(
            backgroundId = R.color.coral_bloom_background,
            surfaceId = R.color.coral_bloom_surface,
            cardId = R.color.coral_bloom_card,
            primaryId = R.color.coral_bloom_primary,
            accentId = R.color.coral_bloom_accent,
            textId = R.color.coral_bloom_text,
            mutedId = R.color.coral_bloom_muted,
            isDark = false
        )
        else -> buildThemeScheme( // Default theme: arctic
            backgroundId = R.color.arctic_background,
            surfaceId = R.color.arctic_surface,
            cardId = R.color.arctic_card,
            primaryId = R.color.arctic_primary,
            accentId = R.color.arctic_accent,
            textId = R.color.arctic_text,
            mutedId = R.color.arctic_muted,
            isDark = false
        )
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
