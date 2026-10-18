package com.gymlist.app.data.local

import androidx.room.TypeConverter

/** Room no sabe guardar listas; las imágenes van como una sola cadena separada por "|". */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String = value?.joinToString("|").orEmpty()

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
}
