package com.cibertec.reporteincidenciasurbanas.data.repository

import com.cibertec.reporteincidenciasurbanas.data.model.Category
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(): Flow<Resource<List<Category>>>
}
