package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "obligations",
    foreignKeys = [
        ForeignKey(
            entity = FamilyMember::class,
            parentColumns = ["id"],
            childColumns = ["familyMemberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("familyMemberId"),
        Index("documentId")
    ]
)
data class Obligation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long = 0,
    val familyMemberId: Long,
    val title: String,
    val dueDate: String, // Formatted date string, e.g. "2026-10-05" or "Oct 05, 2026"
    val sourceName: String,
    val sourceUrl: String,
    // Verified, specific action page for the obligation (e.g. a status-check or e-filing form) -
    // only set where DocumentClassifier.getTriggeredObligations has actually confirmed one
    // exists. Null means no such page is known; callers should fall back to sourceUrl.
    val actionUrl: String? = null,
    val status: ObligationStatus,
    val lastVerifiedAt: Long = System.currentTimeMillis()
)
