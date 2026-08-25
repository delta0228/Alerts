package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.entities.AlertHistoryEntity
import com.example.data.local.entities.AlertRuleEntity
import com.example.data.local.entities.StockEntity

@Database(
    entities = [
        StockEntity::class,
        AlertRuleEntity::class,
        AlertHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StockDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun alertRuleDao(): AlertRuleDao
    abstract fun alertHistoryDao(): AlertHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: StockDatabase? = null

        fun getDatabase(context: Context): StockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    "stock_alert_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
