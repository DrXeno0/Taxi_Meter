package com.nocturnal.taximeter.repository

import com.nocturnal.taximeter.DAO.TaxiDriverDao
import com.nocturnal.taximeter.data.TaxiDriver

class TaxiDriverRepository(private val dao: TaxiDriverDao) {

    suspend fun insertTaxiDriver(driver: TaxiDriver) {
        dao.insertTaxiDriver(driver)
    }

    suspend fun getTaxiDriver(): TaxiDriver? {
        return dao.getTaxiDriver()
    }
}
