package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.network.SyncFinding
import com.example.ui.theme.InkBackground
import com.example.ui.theme.MutedText
import com.example.ui.theme.PaperSurface
import com.example.ui.viewmodel.KaagazViewModel
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TrustedSource(
    val domain: String,
    val category: String,
    val description: String
)

private fun formatSyncTime(timestamp: Long?): String {
    if (timestamp == null) return "Never"
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun hostOf(url: String): String =
    try {
        URI(url).host ?: url
    } catch (_: Exception) {
        url
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    viewModel: KaagazViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncFindings by viewModel.syncFindings.collectAsStateWithLifecycle()
    val syncUnavailableReason by viewModel.syncUnavailableReason.collectAsStateWithLifecycle()
    val lastSyncedAt by viewModel.lastSyncedAt.collectAsStateWithLifecycle()

    val sources = remember {
        listOf(
            TrustedSource(
                domain = "incometax.gov.in",
                category = "Government Portal",
                description = "PAN-Aadhaar linking deadlines & ITR filing status"
            ),
            TrustedSource(
                domain = "parivahan.gov.in",
                category = "Government Portal",
                description = "Vahan & Sarathi: PUC, RC fitness & DL renewals"
            ),
            TrustedSource(
                domain = "uidai.gov.in",
                category = "Government Portal",
                description = "Aadhaar mandatory document revalidation rules"
            ),
            TrustedSource(
                domain = "your LPG provider",
                category = "Utility Provider",
                description = "Indane / BharatGas / HPCL biometric e-KYC deadlines"
            ),
            TrustedSource(
                domain = "The Economic Times",
                category = "News Source",
                description = "Regulatory notifications & statutory deadline gazettes"
            ),
            TrustedSource(
                domain = "LiveMint Finance",
                category = "News Source",
                description = "Tax & personal finance compliance alert tracking"
            )
        )
    }

    val formattedTime = remember(lastSyncedAt) { formatSyncTime(lastSyncedAt) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trusted Sources",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = InkBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("sync_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = InkBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0)),
                color = PaperSurface,
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { viewModel.syncAllSources() },
                        enabled = !isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("sync_now_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InkBackground,
                            contentColor = Color.White
                        )
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSyncing) "Checking trusted sources..." else "Sync now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val isModelLoaded = remember { viewModel.isModelLoaded }
                Spacer(modifier = Modifier.height(4.dp))
                // On-device LLM model status
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("model_status_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isModelLoaded) Color(0xFF2E7D5B) else Color(0xFF5B6770))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isModelLoaded) "On-device model loaded" else "Fallback mode only",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isModelLoaded) Color(0xFF2E7D5B) else Color(0xFF5B6770)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Regulatory & Authority Feeds",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = InkBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Kaagaz cross-verifies deadline changes from these official government portals and trusted news feeds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                when {
                    syncUnavailableReason != null -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFF3E0),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sync_unavailable_notice")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = Color(0xFFB26A00),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Sync unavailable",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB26A00)
                                    )
                                    Text(
                                        text = "${syncUnavailableReason}. Last checked: $formattedTime.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF8A5300)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    syncFindings.isNotEmpty() -> {
                        Text(
                            text = "Deadline changes found - please review",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = InkBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.testTag("sync_findings_list")
                        ) {
                            syncFindings.forEach { finding ->
                                SyncFindingCard(finding = finding, context = context)
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    lastSyncedAt != null -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sync_no_change_notice")
                        ) {
                            Text(
                                text = "No official deadline changes found. Last checked: $formattedTime.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D5B),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }

            items(sources) { source ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PaperSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (source.category.contains("Government")) Icons.Default.Verified else Icons.Default.Public,
                                contentDescription = null,
                                tint = InkBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = source.domain,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = InkBackground
                            )
                            Text(
                                text = source.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Last checked: $formattedTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D5B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SyncFindingCard(finding: SyncFinding, context: android.content.Context) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFF0C36D), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = finding.ruleTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = InkBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Was: ${finding.oldRule}",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
            Text(
                text = "Reported: ${finding.newRule}",
                style = MaterialTheme.typography.bodySmall,
                color = InkBackground,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(finding.sourceUrl)))
                        } catch (_: Exception) {
                            // No app can handle the link - nothing to do, the URL is still shown as text.
                        }
                    }
                    .testTag("sync_finding_source_link")
            ) {
                Text(
                    text = "Source: ${hostOf(finding.sourceUrl)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF1565C0),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open source",
                    tint = Color(0xFF1565C0),
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = "Confidence: ${(finding.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText
            )
        }
    }
}
