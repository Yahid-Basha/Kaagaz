package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scanned_documents",
    foreignKeys = [
        ForeignKey(
            entity = FamilyMember::class,
            parentColumns = ["id"],
            childColumns = ["familyMemberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("familyMemberId")]
)
data class ScannedDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val familyMemberId: Long,
    val docType: DocType,
    val imagePath: String,
    val extractedFields: String, // JSON string: holder name, document number, printed date
    val scannedAt: Long = System.currentTimeMillis()
)
