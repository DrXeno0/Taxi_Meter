package com.nocturnal.taximeter.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nocturnal.taximeter.data.Ride

@Dao
interface RideDao {

    @Insert
    suspend fun insertRide(ride: Ride)

    @Query("SELECT * FROM ride_history_table ORDER BY id DESC")
    suspend fun getRideHistory(): List<Ride>
}
