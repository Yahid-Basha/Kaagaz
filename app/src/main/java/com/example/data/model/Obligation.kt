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
    val status: ObligationStatus,
    val lastVerifiedAt: Long = System.currentTimeMillis()
)
