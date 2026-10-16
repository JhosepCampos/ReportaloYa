package com.cibertec.reporteincidenciasurbanas.data.model

data class Category(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val icono: String = "",
    val activa: Boolean = true,
    val orden: Int = 0
) {
    companion object {
        fun getInitialCategories(): List<Category> = listOf(
            Category("baches", "Baches y vías", "Huecos, pavimento dañado y hundimientos", "baches", true, 1),
            Category("alumbrado", "Alumbrado público", "Luminarias apagadas, postes dañados", "alumbrado", true, 2),
            Category("basura", "Acumulación de basura", "Residuos sólidos y desmonte en vía pública", "basura", true, 3),
            Category("fugas_agua", "Fugas de agua", "Rotura de tuberías de agua y desagüe", "fugas_agua", true, 4),
            Category("semaforos", "Semáforos o señalización", "Semáforos apagados, señales caídas", "semaforos", true, 5),
            Category("contaminacion", "Contaminación ambiental", "Emisión de humos, ruidos molestos", "contaminacion", true, 6),
            Category("otros", "Otros problemas", "Otras incidencias en la comunidad", "otros", true, 7)
        )
    }
}
