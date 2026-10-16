package com.cibertec.reporteincidenciasurbanas.utils

object Constants {

    const val COLLECTION_USERS = "usuarios"
    const val COLLECTION_CATEGORIES = "categorias"
    const val COLLECTION_INCIDENTS = "incidencias"
    const val COLLECTION_CHATS = "mensajes_comunidad"

    const val STORAGE_INCIDENTS_PATH = "incidencias"

    const val ROLE_CITIZEN = "CIUDADANO"
    const val ROLE_ADMIN = "ADMINISTRADOR"

    const val STATUS_PENDING = "PENDIENTE"
    const val STATUS_IN_REVIEW = "EN_REVISION"
    const val STATUS_IN_PROGRESS = "EN_ATENCION"
    const val STATUS_RESOLVED = "RESUELTO"

    const val MAX_IMAGE_SIZE_BYTES = 500 * 1024
    const val MIN_DESCRIPTION_LENGTH = 10
    const val MAX_DESCRIPTION_LENGTH = 300
}
