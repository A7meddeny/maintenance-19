package com.masar.maintenance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

// ===== ألوان مسار =====
val Red = Color(0xFFF04D45)
val RedDark = Color(0xFFC9302A)
val Ink = Color(0xFF0C0C0C)
val Ink2 = Color(0xFF121212)
val Panel = Color(0xFF171717)
val Panel2 = Color(0xFF1E1E1E)
val Line = Color(0xFF2A2A2A)
val Line2 = Color(0xFF333333)
val Txt = Color(0xFFF2F2F2)
val Muted = Color(0xFF9A9A9A)
val Green = Color(0xFF34C759)
val Yellow = Color(0xFFFFC531)
val RedStatus = Color(0xFFFF453A)
val Blue = Color(0xFF86B0FF)

private val MasarColors = darkColorScheme(
    primary = Red,
    onPrimary = Color.White,
    secondary = RedDark,
    background = Ink,
    onBackground = Txt,
    surface = Panel,
    onSurface = Txt,
    surfaceVariant = Panel2,
    outline = Line2,
    error = RedStatus
)

@Composable
fun MasarTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides if (com.masar.maintenance.ui.I18n.isAr) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        MaterialTheme(
            colorScheme = MasarColors,
            typography = Typography(),
            content = content
        )
    }
}
