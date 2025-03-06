package ru.maychurch.maychurchsong.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import ru.maychurch.maychurchsong.data.model.Song
import ru.maychurch.maychurchsong.data.preferences.UserPreferences
import ru.maychurch.maychurchsong.ui.theme.Favorite
import ru.maychurch.maychurchsong.ui.theme.FavoriteOff
import ru.maychurch.maychurchsong.ui.theme.FontSizes

@Composable
fun SongItem(
    song: Song,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    fontSize: Int = UserPreferences.FONT_SIZE_MEDIUM
) {
    // Выбираем типографику в зависимости от настроек размера шрифта
    val typography = when (fontSize) {
        UserPreferences.FONT_SIZE_SMALL -> FontSizes.Small
        UserPreferences.FONT_SIZE_LARGE -> FontSizes.Large
        else -> FontSizes.Medium
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                // Создаем аннотированную строку, где номер песни выделен специальным стилем
                val titleText = buildAnnotatedString {
                    // Добавляем номер песни
                    withStyle(style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )) {
                        append("№ ${song.id} ")
                    }
                    
                    // Добавляем заголовок песни
                    append(song.title ?: "")
                }
                
                Text(
                    text = titleText,
                    style = when (fontSize) {
                        UserPreferences.FONT_SIZE_SMALL -> MaterialTheme.typography.titleSmall
                        UserPreferences.FONT_SIZE_LARGE -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                Text(
                    text = song.text?.take(100)?.plus(if ((song.text?.length ?: 0) > 100) "..." else "") ?: "",
                    style = typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (song.isFavorite) "Удалить из избранного" else "Добавить в избранное",
                    tint = if (song.isFavorite) Favorite else FavoriteOff
                )
            }
        }
    }
} 