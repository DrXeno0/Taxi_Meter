package com.nocturnal.taximeter.model

data class RideModel(
    val distance: Double = 0.0,
    val timeElapsed: Long = 0L,
    val totalFare: Double = 0.0
)