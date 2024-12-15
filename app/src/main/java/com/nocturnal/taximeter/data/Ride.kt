package com.nocturnal.taximeter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ride_history_table")
data class Ride(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val time: String,
    val duration: String,
    val fee: Double
)
