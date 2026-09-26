package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FamilyMember
import com.example.data.model.Obligation
import com.example.data.model.ObligationStatus
import com.example.ui.theme.InkBackground
import com.example.ui.theme.MutedText
import com.example.ui.theme.PaperSurface
import com.example.ui.viewmodel.KaagazViewModel

@Composable
fun HomeScreen(
    viewModel: KaagazViewModel,
    onNavigateToScan: (Long) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSync: () -> Unit
) {
    val members by viewModel.familyMembers.collectAsStateWithLifecycle()
    val selectedMemberId by viewModel.selectedMemberId.collectAsStateWithLifecycle()
    val obligations by viewModel.memberObligations.collectAsStateWithLifecycle()

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }
    var newMemberRelation by remember { mutableStateOf("") }

    // Ensure initial member is selected once members load
    LaunchedEffect(members) {
        if (members.isNotEmpty() && selectedMemberId == null) {
            viewModel.selectMember(members.first().id)
        }
    }

    val activeMemberId = selectedMemberId ?: members.firstOrNull()?.id ?: 1L

    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = {
                Text(
                    text = "Add Family Member",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = InkBackground
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Add a member to track their government and personal deadlines.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText
                    )
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        label = { Text("Name (e.g. Dadi, Brother)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_member_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = InkBackground,
                            focusedLabelColor = InkBackground
                        )
                    )
                    OutlinedTextField(
                        value = newMemberRelation,
                        onValueChange = { newMemberRelation = it },
                        label = { Text("Relation (e.g. Grandmother, Brother)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_member_relation_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = InkBackground,
                            focusedLabelColor = InkBackground
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newMemberName.trim()
                        val rel = newMemberRelation.trim().ifBlank { "Family" }
                        if (name.isNotBlank()) {
                            viewModel.addFamilyMember(name, rel)
                            showAddMemberDialog = false
                        }
                    },
                    enabled = newMemberName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = InkBackground),
                    modifier = Modifier.testTag("confirm_add_member_button")
                ) {
                    Text("Add Member")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddMemberDialog = false },
                    modifier = Modifier.testTag("cancel_add_member_button")
                ) {
                    Text("Cancel", color = MutedText)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToScan(activeMemberId) },
                containerColor = InkBackground,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 12.dp)
                    .testTag("scan_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan Document",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top App Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kaagaz",
                        style = MaterialTheme.typography.displayMedium,
                        color = InkBackground,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Every deadline your family can't afford to miss",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText
                    )
                }

                IconButton(
                    onClick = onNavigateToSync,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("sync_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync Sources",
                        tint = InkBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal Tab Row of Family Members
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                members.forEach { member ->
                    val isSelected = member.id == activeMemberId
                    FamilyMemberTab(
                        member = member,
                        isSelected = isSelected,
                        onClick = { viewModel.selectMember(member.id) }
                    )
                }

                // Add Member Action Chip
                Surface(
                    onClick = {
                        newMemberName = ""
                        newMemberRelation = ""
                        showAddMemberDialog = true
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6DFE6)),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("add_member_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Member",
                            tint = InkBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Member",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = InkBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Deadline Summary Bar
            val overdueCount = obligations.count { it.status == ObligationStatus.OVERDUE }
            val dueSoonCount = obligations.count { it.status == ObligationStatus.DUE_SOON }

            if (obligations.isNotEmpty() && (overdueCount > 0 || dueSoonCount > 0)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (overdueCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ObligationStatus.OVERDUE.color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$overdueCount overdue",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ObligationStatus.OVERDUE.color,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (dueSoonCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ObligationStatus.DUE_SOON.color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$dueSoonCount due soon",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ObligationStatus.DUE_SOON.color,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Vertical list of Obligations sorted soonest due date first
            if (obligations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MutedText,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No pending obligations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = InkBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the Scan button to add an RC, PUC, PAN, LPG bill or Insurance policy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(obligations, key = { it.id }) { obligation ->
                        ObligationCard(
                            obligation = obligation,
                            onClick = { onNavigateToDetail(obligation.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FamilyMemberTab(
    member: FamilyMember,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) InkBackground else PaperSurface,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6DFE6)),
        modifier = Modifier
            .height(42.dp)
            .testTag("member_tab_${member.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else InkBackground
            )
            if (member.relation.isNotEmpty() && member.relation != "Self") {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${member.relation})",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color(0xFFBAC7D5) else MutedText
                )
            }
        }
    }
}

@Composable
fun ObligationCard(
    obligation: Obligation,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PaperSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE5E9EC), RoundedCornerShape(16.dp))
            .testTag("obligation_card_${obligation.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot (red, amber, green)
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(obligation.status.color)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = obligation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Due: ${obligation.dueDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = when (obligation.status) {
                            ObligationStatus.OVERDUE -> ObligationStatus.OVERDUE.color
                            ObligationStatus.DUE_SOON -> ObligationStatus.DUE_SOON.color
                            ObligationStatus.OK -> InkBackground
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        color = MutedText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = obligation.sourceName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedText
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "View detail",
                tint = MutedText.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
