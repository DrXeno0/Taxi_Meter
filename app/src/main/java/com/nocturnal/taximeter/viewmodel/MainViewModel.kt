package com.nocturnal.taximeter.viewmodel


import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nocturnal.taximeter.model.RideModel
import com.nocturnal.taximeter.repository.FareRepository
import com.nocturnal.taximeter.utils.LocationHelper

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainViewModel(context: Context) : ViewModel() {
    private val fareRepository = FareRepository()
    private val locationHelper = LocationHelper(context)

    private val _rideData = MutableLiveData<RideModel>()
    val rideData: LiveData<RideModel> get() = _rideData

    private var startTime: Long = 0
    private var lastLocation: Location? = null
    private var totalDistance: Double = 0.0

    init {
        _rideData.value = RideModel()
    }


    fun startRide() {
        startTime = System.currentTimeMillis()
        totalDistance = 0.0
        lastLocation = null // Reset location tracking

        locationHelper.getCurrentLocation { location ->
            if (location != null) {
                lastLocation = location
                updateRideData(location)
            } else {
                locationHelper.startLocationUpdates()
            }
        }
    }


    fun updateLocation() {
        locationHelper.getCurrentLocation { location ->
            if (location != null) {
                processLocation(location)
            } else {
                locationHelper.startLocationUpdates()
            }
        }
    }


    fun endRide() {
        locationHelper.stopLocationUpdates()
    }


    private fun processLocation(location: Location) {
        if (lastLocation != null) {
            val distance = calculateDistance(lastLocation!!, location)
            totalDistance += distance
        }
        lastLocation = location

        updateRideData(location)
    }


    private fun updateRideData(location: Location) {
        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000 / 60 // Minutes
        val fare = fareRepository.calculateFare(totalDistance, elapsedTime)

        _rideData.value = RideModel(totalDistance, elapsedTime, fare)
    }

    private fun calculateDistance(start: Location, end: Location): Double {
        val earthRadius = 6371
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLon = Math.toRadians(end.longitude - start.longitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }
}