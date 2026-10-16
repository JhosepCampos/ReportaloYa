package com.cibertec.reporteincidenciasurbanas.data.repository

import com.cibertec.reporteincidenciasurbanas.data.model.Incident
import com.cibertec.reporteincidenciasurbanas.data.model.IncidentStatus
import com.cibertec.reporteincidenciasurbanas.data.model.StatusHistoryEntry
import com.cibertec.reporteincidenciasurbanas.utils.Constants
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.util.Random

class IncidentRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IncidentRepository {

    override fun createIncident(incident: Incident): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val collection = firestore.collection(Constants.COLLECTION_INCIDENTS)
            val docRef = collection.document()
            val now = Timestamp.now()

            val randomSuffix = 10000 + Random().nextInt(90000)
            val codigo = "INC-$randomSuffix"

            val initialHistory = listOf(
                StatusHistoryEntry(
                    estado = IncidentStatus.PENDIENTE.name,
                    fecha = now,
                    cambiadoPor = incident.usuarioId,
                    nombreAdmin = incident.usuarioNombre,
                    observacion = "Reporte registrado inicialmente por el ciudadano"
                )
            )

            val newIncident = incident.copy(
                id = docRef.id,
                codigo = codigo,
                estado = IncidentStatus.PENDIENTE.name,
                fechaCreacion = now,
                fechaActualizacion = now,
                historialEstados = initialHistory
            )

            docRef.set(newIncident).await()
            emit(Resource.Success(docRef.id))
        } catch (e: Exception) {
            emit(Resource.Error("Error al registrar la incidencia: ${e.localizedMessage}", e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getCitizenIncidents(citizenId: String): Flow<Resource<List<Incident>>> = flow {
        emit(Resource.Loading)
        try {
            val snapshot = firestore.collection(Constants.COLLECTION_INCIDENTS)
                .whereEqualTo("usuarioId", citizenId)
                .get()
                .await()

            val incidents = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Incident::class.java)?.copy(id = doc.id)
            }.sortedByDescending { it.fechaCreacion }

            emit(Resource.Success(incidents))
        } catch (e: Exception) {
            emit(Resource.Error("Error al cargar reportes ciudadanos: ${e.localizedMessage}", e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getAllIncidents(
        filterStatus: String?,
        filterCategory: String?
    ): Flow<Resource<List<Incident>>> = flow {
        emit(Resource.Loading)
        try {
            var query: Query = firestore.collection(Constants.COLLECTION_INCIDENTS)

            if (!filterStatus.isNullOrBlank() && filterStatus != "TODOS") {
                query = query.whereEqualTo("estado", filterStatus)
            }
            if (!filterCategory.isNullOrBlank() && filterCategory != "TODAS") {
                query = query.whereEqualTo("categoriaId", filterCategory)
            }

            val snapshot = query.get().await()
            val incidents = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Incident::class.java)?.copy(id = doc.id)
            }.sortedByDescending { it.fechaCreacion }

            emit(Resource.Success(incidents))
        } catch (e: Exception) {
            emit(Resource.Error("Error al consultar la bandeja de incidencias: ${e.localizedMessage}", e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getIncidentById(incidentId: String): Flow<Resource<Incident>> = flow {
        emit(Resource.Loading)
        try {
            val doc = firestore.collection(Constants.COLLECTION_INCIDENTS)
                .document(incidentId)
                .get()
                .await()

            if (doc.exists()) {
                val incident = doc.toObject(Incident::class.java)?.copy(id = doc.id)
                    ?: throw Exception("Error al mapear la información del reporte")
                emit(Resource.Success(incident))
            } else {
                emit(Resource.Error("Incidencia no encontrada en la base de datos"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error al obtener detalle: ${e.localizedMessage}", e))
        }
    }.flowOn(Dispatchers.IO)

    override fun updateIncidentStatus(
        incidentId: String,
        newStatus: String,
        adminId: String,
        adminName: String,
        observation: String?
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val now = Timestamp.now()
            val docRef = firestore.collection(Constants.COLLECTION_INCIDENTS).document(incidentId)

            val newHistoryEntry = StatusHistoryEntry(
                estado = newStatus,
                fecha = now,
                cambiadoPor = adminId,
                nombreAdmin = adminName.ifBlank { "Administrador Municipal" },
                observacion = observation ?: "Actualización técnica de estado"
            )

            docRef.update(
                mapOf(
                    "estado" to newStatus,
                    "fechaActualizacion" to now,
                    "administradorId" to adminId,
                    "historialEstados" to FieldValue.arrayUnion(newHistoryEntry)
                )
            ).await()

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al actualizar estado: ${e.localizedMessage}", e))
        }
    }.flowOn(Dispatchers.IO)
}
