package com.nocturnal.taximeter.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocturnal.taximeter.TaxiApp
import com.nocturnal.taximeter.data.Ride
import kotlinx.coroutines.launch

class RideViewModel : ViewModel() {
    private val rideDao = TaxiApp.rideDatabase.rideDao()


    val rideHistory = MutableLiveData<List<Ride>>()

    fun fetchRideHistory() {
        viewModelScope.launch {
            rideHistory.postValue(rideDao.getRideHistory())
        }
    }


    fun saveRide(date: String, time: String, duration: String, fee: Double) {
        viewModelScope.launch {
            val ride = Ride(date = date, time = time, duration = duration, fee = fee)
            rideDao.insertRide(ride)
        }
    }
}
