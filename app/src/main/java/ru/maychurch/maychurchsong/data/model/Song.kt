package ru.maychurch.maychurchsong.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "songs")
@Serializable
data class Song(
    @PrimaryKey
    val id: String, // Идентификатор песни (обычно номер)
    val title: String?, // Название песни
    val text: String?, // Текст песни
    val url: String?, // URL песни на сайте
    val isFavorite: Boolean = false, // Добавлена ли песня в избранное
    val lastAccessed: Long = 0 // Время последнего доступа к песне (для списка "Недавние")
) 