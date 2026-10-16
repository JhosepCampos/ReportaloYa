package com.cibertec.reporteincidenciasurbanas.navigation

object NavRoutes {

    const val ROOT_GRAPH = "root_graph"
    const val AUTH_GRAPH = "auth_graph"
    const val CITIZEN_GRAPH = "citizen_graph"
    const val ADMIN_GRAPH = "admin_graph"

    const val SPLASH = "splash"
    const val WELCOME = "auth/welcome"
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val FORGOT_PASSWORD = "auth/forgot_password"

    const val CITIZEN_MAIN = "citizen/main"
    const val CREATE_REPORT = "citizen/create_report"
    const val CITIZEN_DETAIL = "citizen/incident_detail/{incidentId}"

    fun citizenDetail(incidentId: String): String = "citizen/incident_detail/$incidentId"

    const val ADMIN_DASHBOARD = "admin/dashboard"
    const val ADMIN_DETAIL = "admin/incident_detail/{incidentId}"

    fun adminDetail(incidentId: String): String = "admin/incident_detail/$incidentId"
}
