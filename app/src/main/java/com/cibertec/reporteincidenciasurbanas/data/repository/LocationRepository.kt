package com.cibertec.reporteincidenciasurbanas.data.repository

import com.cibertec.reporteincidenciasurbanas.data.model.Coordinates
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getCurrentCoordinates(): Flow<Resource<Coordinates>>
    suspend fun getAddressFromLocation(lat: Double, lng: Double): String
}
