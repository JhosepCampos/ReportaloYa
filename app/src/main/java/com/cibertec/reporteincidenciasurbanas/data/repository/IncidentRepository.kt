package com.cibertec.reporteincidenciasurbanas.data.repository

import com.cibertec.reporteincidenciasurbanas.data.model.Incident
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import kotlinx.coroutines.flow.Flow

interface IncidentRepository {
    fun createIncident(incident: Incident): Flow<Resource<String>>
    fun getCitizenIncidents(citizenId: String): Flow<Resource<List<Incident>>>
    fun getAllIncidents(filterStatus: String? = null, filterCategory: String? = null): Flow<Resource<List<Incident>>>
    fun getIncidentById(incidentId: String): Flow<Resource<Incident>>
    fun updateIncidentStatus(
        incidentId: String,
        newStatus: String,
        adminId: String,
        adminName: String,
        observation: String?
    ): Flow<Resource<Unit>>
}
