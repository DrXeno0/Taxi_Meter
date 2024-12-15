package com.nocturnal.taximeter.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nocturnal.taximeter.data.TaxiDriver

@Dao
interface TaxiDriverDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaxiDriver(taxiDriver: TaxiDriver)

    @Query("SELECT * FROM taxi_driver_table LIMIT 1")
    suspend fun getTaxiDriver(): TaxiDriver?

}
