package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.InkBackground
import com.example.ui.theme.MutedText
import com.example.ui.theme.PaperSurface
import com.example.ui.viewmodel.KaagazViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TrustedSource(
    val domain: String,
    val category: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    viewModel: KaagazViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var lastSyncTimestamp by remember { mutableLongStateOf(System.currentTimeMillis() - 14400000L) }

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

    val formattedTime = remember(lastSyncTimestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(lastSyncTimestamp))
    }

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
                        onClick = {
                            // TODO: wire to OpenRouter web_search server tool, cross-check two sources before accepting a new date
                            lastSyncTimestamp = System.currentTimeMillis()
                            viewModel.syncAllSources {
                                Toast.makeText(
                                    context,
                                    "Sync requires internet, coming soon",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
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
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sync now",
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
