package ru.maychurch.maychurchsong.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Размеры шрифта для разных размеров текста
object FontSizes {
    // Малый размер шрифта (уменьшен на 25%)
    val Small = Typography(
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 7.5.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.4.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 6.75.sp,
            lineHeight = 10.5.sp,
            letterSpacing = 0.25.sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            lineHeight = 13.5.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.sp
        )
    )
    
    // Средний размер шрифта (уменьшен на 25%)
    val Medium = Typography(
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 9.sp,
            lineHeight = 13.5.sp,
            letterSpacing = 0.4.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 8.25.sp,
            lineHeight = 12.75.sp,
            letterSpacing = 0.25.sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            lineHeight = 18.75.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.sp
        )
    )
    
    // Большой размер шрифта (уменьшен на 25%)
    val Large = Typography(
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.75.sp,
            letterSpacing = 0.4.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 10.5.sp,
            lineHeight = 16.5.sp,
            letterSpacing = 0.25.sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 22.5.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 18.75.sp,
            letterSpacing = 0.sp
        )
    )
}

// Размеры шрифта для интерфейса
object InterfaceFontSizes {
    // Малый размер шрифта интерфейса (уменьшен на 33.3%)
    val Small = Typography(
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 9.3.sp,
            lineHeight = 13.5.sp,
            letterSpacing = 0.25.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 8.sp,
            lineHeight = 10.5.sp,
            letterSpacing = 0.25.sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 13.3.sp,
            lineHeight = 18.5.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.7.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.15.sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 6.7.sp,
            lineHeight = 10.7.sp,
            letterSpacing = 0.5.sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 7.3.sp,
            lineHeight = 10.7.sp,
            letterSpacing = 0.5.sp
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 8.sp,
            lineHeight = 10.7.sp,
            letterSpacing = 0.5.sp
        )
    )
    
    // Средний размер шрифта интерфейса (уменьшен на 33.3%)
    val Medium = Typography(
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 10.7.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 9.3.sp,
            lineHeight = 13.3.sp,
            letterSpacing = 0.25.sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 14.7.sp,
            lineHeight = 18.7.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.15.sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 7.3.sp,
            lineHeight = 10.7.sp,
            letterSpacing = 0.5.sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 8.sp,
            lineHeight = 10.7.sp,
            letterSpacing = 0.5.sp
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 9.3.sp,
            lineHeight = 13.3.sp,
            letterSpacing = 0.1.sp
        )
    )
    
    // Большой размер шрифта интерфейса (уменьшен на 33.3%)
    val Large = Typography(
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.7.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 10.7.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.25.sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 17.3.sp,
            lineHeight = 21.3.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.7.sp,
            lineHeight = 18.7.sp,
            letterSpacing = 0.15.sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 8.sp,
            lineHeight = 10.7.sp,
            letterSpacing = 0.5.sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 9.3.sp,
            lineHeight = 13.3.sp,
            letterSpacing = 0.5.sp
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 10.7.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.1.sp
        )
    )
}

// По умолчанию используем средний размер шрифта
val Typography = InterfaceFontSizes.Medium

// Функция для получения типографики для текста песен на основе настроек
fun getSongTypography(fontSize: Int): Typography {
    return when (fontSize) {
        0 -> FontSizes.Small
        1 -> FontSizes.Medium
        2 -> FontSizes.Large
        else -> FontSizes.Medium
    }
}

// Функция для получения типографики для интерфейса на основе настроек
fun getInterfaceTypography(fontSize: Int): Typography {
    return when (fontSize) {
        0 -> InterfaceFontSizes.Small
        1 -> InterfaceFontSizes.Medium
        2 -> InterfaceFontSizes.Large
        else -> InterfaceFontSizes.Medium
    }
}