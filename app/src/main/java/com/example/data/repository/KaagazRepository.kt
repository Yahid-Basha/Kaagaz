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

    suspend fun deleteObligation(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteObligation(id)
    }

    private suspend fun checkAndSeedData() = withContext(Dispatchers.IO) {
        val existingMembers = dao.getAllFamilyMembers().first()
        if (existingMembers.isNotEmpty()) return@withContext

        // Seed 4 Family Members
        val youId = dao.insertFamilyMember(FamilyMember(name = "You", relation = "Self"))
        val appaId = dao.insertFamilyMember(FamilyMember(name = "Appa", relation = "Father"))
        val ammaId = dao.insertFamilyMember(FamilyMember(name = "Amma", relation = "Mother"))
        val sisterId = dao.insertFamilyMember(FamilyMember(name = "Sister", relation = "Sister"))

        // Seed Scanned Documents and Obligations for "You"
        val rcFields = """{"holderName":"Rahul Sharma","documentNumber":"TS 09 AB 1234","date":"2023-04-12"}"""
        val rcDocId = dao.insertDocument(
            ScannedDocument(
                familyMemberId = youId,
                docType = DocType.RC,
                imagePath = "",
                extractedFields = rcFields,
                scannedAt = System.currentTimeMillis() - (86400000L * 15)
            )
        )
        dao.insertObligations(
            listOf(
                Obligation(
                    documentId = rcDocId,
                    familyMemberId = youId,
                    title = "FASTag KYC Verification",
                    dueDate = "2026-09-15",
                    sourceName = "NETC / NPCI",
                    sourceUrl = "https://www.netc.org.in",
                    status = ObligationStatus.OVERDUE,
                    lastVerifiedAt = System.currentTimeMillis() - (86400000L * 2)
                ),
                Obligation(
                    documentId = rcDocId,
                    familyMemberId = youId,
                    title = "PUC Certificate Renewal",
                    dueDate = "2026-10-05",
                    sourceName = "Parivahan Seva",
                    sourceUrl = "https://parivahan.gov.in",
                    status = ObligationStatus.DUE_SOON,
                    lastVerifiedAt = System.currentTimeMillis() - (86400000L * 1)
                ),
                Obligation(
                    documentId = rcDocId,
                    familyMemberId = youId,
                    title = "Comprehensive Motor Insurance",
                    dueDate = "2027-02-14",
                    sourceName = "DigiLocker / Parivahan",
                    sourceUrl = "https://parivahan.gov.in",
                    status = ObligationStatus.OK,
                    lastVerifiedAt = System.currentTimeMillis() - (86400000L * 5)
                ),
                Obligation(
                    documentId = rcDocId,
                    familyMemberId = youId,
                    title = "Driving Licence Validity",
                    dueDate = "2028-06-20",
                    sourceName = "Parivahan Sarathi",
                    sourceUrl = "https://sarathi.parivahan.gov.in",
                    status = ObligationStatus.OK,
                    lastVerifiedAt = System.currentTimeMillis() - (86400000L * 5)
                )
            )
        )

        // Seed for "Appa"
        val panFields = """{"holderName":"Suresh Sharma","documentNumber":"BNZPS1928K","date":"1988-11-20"}"""
        val panDocId = dao.insertDocument(
            ScannedDocument(
                familyMemberId = appaId,
                docType = DocType.PAN_CARD,
                imagePath = "",
                extractedFields = panFields,
                scannedAt = System.currentTimeMillis() - (86400000L * 30)
            )
        )
        dao.insertObligations(
            listOf(
                Obligation(
                    documentId = panDocId,
                    familyMemberId = appaId,
                    title = "PAN-Aadhaar Link Verification",
                    dueDate = "2026-12-31",
                    sourceName = "Income Tax Department",
                    sourceUrl = "https://www.incometax.gov.in",
                    status = ObligationStatus.OK,
                    lastVerifiedAt = System.currentTimeMillis() - (86400000L * 3)
                )
            )
        )

        val lpgFields = """{"holderName":"Suresh Sharma","documentNumber":"CX-99201488","date":"2026-08-10"}"""
        val lpgDocId = dao.insertDocument(
            ScannedDocument(
                familyMemberId = appaId,
                docType = DocType.LPG_BILL,
                imagePath = "",
                extractedFields = lpgFields,
                scannedAt = System.currentTimeMillis() - (86400000L * 10)
            )
        )
        dao.insertObligations(
            listOf(
                Obligation(
                    documentId = lpgDocId,
                    familyMemberId = appaId,
                    title = "LPG Biometric e-KYC Verification",
                    dueDate = "2026-10-12",
                    sourceName = "Ministry of Petroleum / Indane",
                    sourceUrl = "https://cx.indianoil.in",
                    status = ObligationStatus.DUE_SOON,
                    lastVerifiedAt = System.currentTimeMillis() - (86400000L * 2)
                )
            )
        )

        // Seed for "Amma"
        val insFields = """{"holderName":"Lakshmi Sharma","documentNumber":"POL-LIC-448291","date":"2021-03-01"}"""
        val insDocId = dao.insertDocument(
            ScannedDocument(
                familyMemberId = ammaId,
                docType = DocType.INSURANCE,
                imagePath = "",
                extractedFields = insFields,
                scannedAt = System.currentTimeMillis() - (86400000L * 20)
            )
        )
        dao.insertObligations(
            listOf(
                Obligation(
                    documentId = insDocId,
                    familyMemberId = ammaId,
                    title = "Life Insurance Annual Premium",
                    dueDate = "2026-10-22",
                    sourceName = "Life Insurance Corp of India",
                    sourceUrl = "https://licindia.in",
                    status = ObligationStatus.DUE_SOON,
                    lastVerifiedAt = System.currentTimeMillis() - (86400000L * 4)
                ),
                Obligation(
                    documentId = insDocId,
                    familyMemberId = ammaId,
                    title = "Nominee Details & Address Check",
                    dueDate = "2027-01-31",
                    sourceName = "Life Insurance Corp of India",
                    sourceUrl = "https://licindia.in",
                    status = ObligationStatus.OK,
                    lastVerifiedAt = System.currentTimeMillis() - (86400000L * 4)
                )
            )
        )

        // Seed for "Sister"
        val sisterPucFields = """{"holderName":"Pooja Sharma","documentNumber":"KA 04 EM 9921","date":"2026-03-15"}"""
        val sisterDocId = dao.insertDocument(
            ScannedDocument(
                familyMemberId = sisterId,
                docType = DocType.PUC,
                imagePath = "",
                extractedFields = sisterPucFields,
                scannedAt = System.currentTimeMillis() - (86400000L * 60)
            )
        )
        dao.insertObligations(
            listOf(
                Obligation(
                    documentId = sisterDocId,
                    familyMemberId = sisterId,
                    title = "Two-Wheeler PUC Expiry",
                    dueDate = "2026-09-10",
                    sourceName = "Karnataka Transport Dept",
                    sourceUrl = "https://transport.karnataka.gov.in",
                    status = ObligationStatus.OVERDUE,
                    lastVerifiedAt = System.currentTimeMillis() - (86400000L * 1)
                )
            )
        )
    }
}
