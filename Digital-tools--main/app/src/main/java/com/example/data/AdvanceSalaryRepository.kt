package com.example.data

import kotlinx.coroutines.flow.Flow

class AdvanceSalaryRepository(private val dao: AdvanceSalaryDao) {

    fun getAllApplications(): Flow<List<AdvanceSalaryEntity>> {
        return dao.getAllApplications()
    }

    fun searchApplications(query: String): Flow<List<AdvanceSalaryEntity>> {
        return dao.searchApplications(query)
    }

    suspend fun getApplicationById(id: Long): AdvanceSalaryEntity? {
        return dao.getApplicationById(id)
    }

    suspend fun insertApplication(entity: AdvanceSalaryEntity): Long {
        return dao.insertApplication(entity)
    }

    suspend fun updateApplication(entity: AdvanceSalaryEntity) {
        dao.updateApplication(entity)
    }

    suspend fun deleteApplicationById(id: Long) {
        dao.deleteApplicationById(id)
    }

    suspend fun deleteAllApplications() {
        dao.deleteAllApplications()
    }
}
