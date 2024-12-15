package com.nocturnal.taximeter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "taxi_driver_table")
data class TaxiDriver(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val car: String ,
    val licenseType: String,
    val profilePhotoUri: String,
    var qrCodeContent: String
)
