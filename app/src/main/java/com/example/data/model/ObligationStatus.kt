package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class ObligationStatus(
    val label: String,
    val description: String
) {
    OVERDUE(
        label = "Overdue",
        description = "Action required immediately"
    ),
    DUE_SOON(
        label = "Due Soon",
        description = "Deadline within 30 days"
    ),
    OK(
        label = "Fine",
        description = "Up to date & compliant"
    );

    val color: Color
        get() = when (this) {
            OVERDUE -> Color(0xFFB3261E)
            DUE_SOON -> Color(0xFFC98A0B)
            OK -> Color(0xFF2E7D5B)
        }

    val backgroundColor: Color
        get() = when (this) {
            OVERDUE -> Color(0xFFFDE8E7)
            DUE_SOON -> Color(0xFFFEF6E6)
            OK -> Color(0xFFE9F5EF)
        }
}
