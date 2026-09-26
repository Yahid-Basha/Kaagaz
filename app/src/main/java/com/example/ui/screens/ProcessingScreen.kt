package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AmberDueSoon
import com.example.ui.theme.InkBackground
import com.example.ui.theme.MutedText
import com.example.ui.viewmodel.KaagazViewModel

@Composable
fun ProcessingScreen(
    viewModel: KaagazViewModel,
    imagePath: String,
    memberId: Long,
    backImagePath: String? = null,
    onProcessingFinished: () -> Unit
) {
    val currentStep by viewModel.processingStep.collectAsStateWithLifecycle()

    LaunchedEffect(imagePath, backImagePath, memberId) {
        viewModel.startProcessingDocument(
            imagePath = imagePath,
            initialMemberId = memberId,
            backImagePath = backImagePath,
            onSuccess = onProcessingFinished
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBackground)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.testTag("processing_container")
        ) {
            // Elegant Paper Seal Spinner
            Surface(
                shape = CircleShape,
                color = Color(0xFF24343F),
                modifier = Modifier.size(110.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = AmberDueSoon,
                        trackColor = Color(0xFF334653),
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Text cycling smoothly through:
            // "Reading document..." -> "Working out what this is..." -> "Checking what it owes..."
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "processing_step_text"
            ) { stepText ->
                Text(
                    text = stepText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1D2A33)
            ) {
                Text(
                    text = "Running on-device OCR & AI model • No internet required",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8FA1AF),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}
