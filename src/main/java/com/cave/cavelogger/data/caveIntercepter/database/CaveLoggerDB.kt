package com.cave.cavelogger.data.caveIntercepter.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [CaveLogger::class], version = 6, exportSchema = false)
@TypeConverters(MapTypeConverter::class)
abstract class CaveLoggerDB : RoomDatabase() {
    abstract val caveLoggerDao: CaveLoggerDAO
}
