package com.example.data.repository

import com.example.data.local.KaagazDao
import com.example.data.model.DocType
import com.example.data.model.FamilyMember
import com.example.data.model.Obligation
import com.example.data.model.ObligationStatus
import com.example.data.model.ScannedDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KaagazRepository(
    private val dao: KaagazDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    init {
        // Initialize seed data if database is fresh
        scope.launch {
            checkAndSeedData()
        }
    }

    val familyMembers: Flow<List<FamilyMember>> = dao.getAllFamilyMembers()

    fun getObligationsForMember(memberId: Long): Flow<List<Obligation>> =
        dao.getObligationsForMember(memberId)

    val allObligations: Flow<List<Obligation>> = dao.getAllObligations()

    fun getObligationByIdFlow(id: Long): Flow<Obligation?> =
        dao.getObligationByIdFlow(id)

    suspend fun getObligationById(id: Long): Obligation? = withContext(Dispatchers.IO) {
        dao.getObligationById(id)
    }

    suspend fun getFamilyMemberById(id: Long): FamilyMember? = withContext(Dispatchers.IO) {
        dao.getFamilyMemberById(id)
    }

    suspend fun getDocumentById(id: Long): ScannedDocument? = withContext(Dispatchers.IO) {
        dao.getDocumentById(id)
    }

    suspend fun markObligationDone(id: Long) = withContext(Dispatchers.IO) {
        dao.updateObligationStatus(id, ObligationStatus.OK, System.currentTimeMillis())
    }

    suspend fun syncAllSources() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.updateAllLastVerified(now)
    }

    suspend fun saveScannedDocumentWithObligations(
        doc: ScannedDocument,
        obligations: List<Obligation>
    ): Long = withContext(Dispatchers.IO) {
        dao.insertDocumentWithObligations(doc, obligations)
    }

    suspend fun addFamilyMember(member: FamilyMember): Long = withContext(Dispatchers.IO) {
        dao.insertFamilyMember(member)
    }

    suspend fun deleteObligation(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteObligation(id)
    }

    private suspend fun checkAndSeedData() = withContext(Dispatchers.IO) {
        val existingMembers = dao.getAllFamilyMembers().first()
        if (existingMembers.isNotEmpty()) return@withContext

        // Seed 4 Family Members only, obligation lists start genuinely empty
        dao.insertFamilyMember(FamilyMember(name = "You", relation = "Self"))
        dao.insertFamilyMember(FamilyMember(name = "Appa", relation = "Father"))
        dao.insertFamilyMember(FamilyMember(name = "Amma", relation = "Mother"))
        dao.insertFamilyMember(FamilyMember(name = "Sister", relation = "Sister"))
    }
}
