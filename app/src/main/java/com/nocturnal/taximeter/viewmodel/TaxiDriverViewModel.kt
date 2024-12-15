package com.nocturnal.taximeter.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.nocturnal.taximeter.TaxiApp
import com.nocturnal.taximeter.data.TaxiDriver
import kotlinx.coroutines.launch

class TaxiDriverViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TaxiApp.database.taxiDriverDao()
    private val _driver = MutableLiveData<TaxiDriver?>()
    val driver: LiveData<TaxiDriver?> = _driver

    fun saveDriver(driver: TaxiDriver) {
        viewModelScope.launch {
            dao.insertTaxiDriver(driver)
            _driver.postValue(driver) // After saving, update LiveData
        }
    }

    fun loadDriver() {
        viewModelScope.launch {
            val savedDriver = dao.getTaxiDriver()
            _driver.postValue(savedDriver) // Load saved driver from DB
        }
    }
}
