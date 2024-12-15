package com.nocturnal.taximeter

import android.app.Application
import androidx.room.Room
import com.nocturnal.taximeter.database.AppDatabase
import com.nocturnal.taximeter.database.TaxiDatabase

class TaxiApp : Application() {
    companion object {
        lateinit var database: AppDatabase
        lateinit var rideDatabase: TaxiDatabase
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "taxi_driver_database"
        ).build()
        rideDatabase = Room.databaseBuilder(
            this,
            TaxiDatabase::class.java,
            "ride_history_database"
        ).build()
    }
}
