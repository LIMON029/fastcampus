package kr.co.limon.calculator

import androidx.room.Database
import androidx.room.RoomDatabase
import kr.co.limon.calculator.dao.HistoryDao
import kr.co.limon.calculator.model.History

@Database(entities = [History::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao():HistoryDao
}