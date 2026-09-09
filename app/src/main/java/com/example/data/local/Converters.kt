package com.example.data.local

import androidx.room.TypeConverter
import com.example.model.ThreatLevel

class Converters {
    @TypeConverter
    fun fromThreatLevel(value: ThreatLevel): String {
        return value.name
    }

    @TypeConverter
    fun toThreatLevel(value: String): ThreatLevel {
        return ThreatLevel.valueOf(value)
    }
}
