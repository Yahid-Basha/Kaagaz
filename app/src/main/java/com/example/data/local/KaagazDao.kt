package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.FamilyMember
import com.example.data.model.Obligation
import com.example.data.model.ObligationStatus
import com.example.data.model.ScannedDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface KaagazDao {

    // --- Family Members ---
    @Query("SELECT * FROM family_members ORDER BY id ASC")
    fun getAllFamilyMembers(): Flow<List<FamilyMember>>

    @Query("SELECT * FROM family_members WHERE id = :id LIMIT 1")
    suspend fun getFamilyMemberById(id: Long): FamilyMember?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMember): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMembers(members: List<FamilyMember>): List<Long>

    // --- Scanned Documents ---
    @Query("SELECT * FROM scanned_documents ORDER BY scannedAt DESC")
    fun getAllDocuments(): Flow<List<ScannedDocument>>

    @Query("SELECT * FROM scanned_documents WHERE familyMemberId = :memberId ORDER BY scannedAt DESC")
    fun getDocumentsForMember(memberId: Long): Flow<List<ScannedDocument>>

    @Query("SELECT * FROM scanned_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): ScannedDocument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: ScannedDocument): Long

    // --- Obligations ---
    @Query("SELECT * FROM obligations ORDER BY dueDate ASC")
    fun getAllObligations(): Flow<List<Obligation>>

    @Query("SELECT * FROM obligations WHERE familyMemberId = :memberId ORDER BY dueDate ASC")
    fun getObligationsForMember(memberId: Long): Flow<List<Obligation>>

    @Query("SELECT * FROM obligations WHERE id = :id LIMIT 1")
    fun getObligationByIdFlow(id: Long): Flow<Obligation?>

    @Query("SELECT * FROM obligations WHERE id = :id LIMIT 1")
    suspend fun getObligationById(id: Long): Obligation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObligation(obligation: Obligation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObligations(obligations: List<Obligation>): List<Long>

    @Update
    suspend fun updateObligation(obligation: Obligation)

    @Query("UPDATE obligations SET status = :status, lastVerifiedAt = :verifiedAt WHERE id = :id")
    suspend fun updateObligationStatus(id: Long, status: ObligationStatus, verifiedAt: Long)

    @Query("UPDATE obligations SET lastVerifiedAt = :timestamp")
    suspend fun updateAllLastVerified(timestamp: Long)

    @Query("DELETE FROM obligations WHERE id = :id")
    suspend fun deleteObligation(id: Long)

    @Transaction
    suspend fun insertDocumentWithObligations(
        doc: ScannedDocument,
        obligations: List<Obligation>
    ): Long {
        val docId = insertDocument(doc)
        val linkedObligations = obligations.map { it.copy(documentId = docId) }
        insertObligations(linkedObligations)
        return docId
    }
}
