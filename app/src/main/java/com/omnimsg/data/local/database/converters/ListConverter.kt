package com.omnimsg.data.local.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class ListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String?): List<String> {
        return if (value.isNullOrEmpty()) {
            emptyList()
        } else {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, listType)
        }
    }

    @TypeConverter
    fun toString(list: List<String>?): String {
        return gson.toJson(list ?: emptyList())
    }
}

class MapConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String?): Map<String, String> {
        return if (value.isNullOrEmpty()) {
            emptyMap()
        } else {
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(value, mapType)
        }
    }

    @TypeConverter
    fun toString(map: Map<String, String>?): String {
        return gson.toJson(map ?: emptyMap())
    }
}

class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

class UriConverter {
    @TypeConverter
    fun fromString(value: String?): android.net.Uri? {
        return value?.let { android.net.Uri.parse(it) }
    }

    @TypeConverter
    fun toString(uri: android.net.Uri?): String? {
        return uri?.toString()
    }
}