package com.nocturnal.taximeter.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nocturnal.taximeter.DAO.RideDao
import com.nocturnal.taximeter.DAO.TaxiDriverDao
import com.nocturnal.taximeter.data.Ride
import com.nocturnal.taximeter.data.TaxiDriver

@Database(entities = [TaxiDriver::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taxiDriverDao(): TaxiDriverDao

}

@Database(entities = [TaxiDriver::class, Ride::class], version = 2)
abstract class TaxiDatabase : RoomDatabase() {
    abstract fun taxiDriverDao(): TaxiDriverDao
    abstract fun rideDao(): RideDao
}