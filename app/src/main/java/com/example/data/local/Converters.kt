package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.DocType
import com.example.data.model.ObligationStatus

class Converters {
    @TypeConverter
    fun fromDocType(value: DocType?): String? = value?.name

    @TypeConverter
    fun toDocType(value: String?): DocType? =
        value?.let { runCatching { DocType.valueOf(it) }.getOrDefault(DocType.RC) }

    @TypeConverter
    fun fromObligationStatus(value: ObligationStatus?): String? = value?.name

    @TypeConverter
    fun toObligationStatus(value: String?): ObligationStatus? =
        value?.let { runCatching { ObligationStatus.valueOf(it) }.getOrDefault(ObligationStatus.OK) }
}
