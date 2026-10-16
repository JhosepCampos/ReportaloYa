package com.cibertec.reporteincidenciasurbanas.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.cibertec.reporteincidenciasurbanas.data.model.Coordinates
import com.cibertec.reporteincidenciasurbanas.utils.PermissionUtils
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationRepositoryImpl(
    private val context: Context,
    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
) : LocationRepository {

    @SuppressLint("MissingPermission")
    override fun getCurrentCoordinates(): Flow<Resource<Coordinates>> = flow {
        emit(Resource.Loading)
        try {
            if (!PermissionUtils.hasLocationPermission(context)) {
                emit(Resource.Error("Permiso de ubicación no concedido"))
                return@flow
            }

            val cts = CancellationTokenSource()
            val location = fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).await() ?: fusedClient.lastLocation.await()

            if (location != null) {
                val address = getAddressFromLocation(location.latitude, location.longitude)
                val coordinates = Coordinates(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = address,
                    accuracy = location.accuracy
                )
                emit(Resource.Success(coordinates))
            } else {
                emit(Resource.Error("No se pudo obtener la posición satelital GPS. Verifique que el GPS esté encendido."))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error al capturar ubicación: ${e.localizedMessage}", e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getAddressFromLocation(lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale("es", "PE"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    return@withContext "${addr.thoroughfare ?: addr.featureName ?: "Vía pública"}, ${addr.subLocality ?: addr.locality ?: "Lima"}"
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    return@withContext "${addr.thoroughfare ?: addr.featureName ?: "Vía pública"}, ${addr.subLocality ?: addr.locality ?: "Lima"}"
                }
            }
        } catch (e: Exception) {
        }
        return@withContext "Ubicación fijada (${String.format(Locale.US, "%.5f", lat)}, ${String.format(Locale.US, "%.5f", lng)})"
    }
}
