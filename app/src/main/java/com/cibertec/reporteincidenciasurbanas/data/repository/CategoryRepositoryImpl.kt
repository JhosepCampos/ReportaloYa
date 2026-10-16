package com.cibertec.reporteincidenciasurbanas.data.repository

import com.cibertec.reporteincidenciasurbanas.data.model.Category
import com.cibertec.reporteincidenciasurbanas.utils.Constants
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

class CategoryRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CategoryRepository {

    override fun getCategories(): Flow<Resource<List<Category>>> = flow {
        emit(Resource.Loading)
        try {
            val snapshot = firestore.collection(Constants.COLLECTION_CATEGORIES)
                .whereEqualTo("activa", true)
                .orderBy("orden")
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val categories = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Category::class.java)?.copy(id = doc.id)
                }
                emit(Resource.Success(categories))
            } else {
                emit(Resource.Success(Category.getInitialCategories()))
            }
        } catch (e: Exception) {
            emit(Resource.Success(Category.getInitialCategories()))
        }
    }.flowOn(Dispatchers.IO)
}
