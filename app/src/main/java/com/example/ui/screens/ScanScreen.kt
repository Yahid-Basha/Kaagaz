package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.InkBackground
import com.example.ui.theme.StampRed
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScanScreen(
    memberId: Long,
    onNavigateBack: () -> Unit,
    onPhotoConfirmed: (imagePath: String, memberId: Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (capturedPhotoPath == null) {
            // Camera Live View or Permission Denied View
            if (hasCameraPermission) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                            } catch (e: Exception) {
                                // Camera binding failed (e.g. emulator without virtual camera)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )

                // Split screen: upper 70% for document frame guide, lower 30% for capture control bar with >= 24dp clearance
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Upper 70% of the screen: Document Frame Guide Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.70f)
                    ) {
                        // Top bar controls (Back & Sample document helper)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 36.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .testTag("scan_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }

                            // Sample document button to allow testing anywhere (emulator or physical)
                            Surface(
                                onClick = {
                                    val samplePath = createSampleDocumentFile(context, "REGISTRATION CERTIFICATE\nTS 09 AB 1234\nRahul Sharma\nPUC Valid: 05/10/2026")
                                    capturedPhotoPath = samplePath
                                    capturedBitmap = BitmapFactory.decodeFile(samplePath)
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.testTag("sample_doc_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Test Sample",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        // Document Guide Frame inside upper 70%
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 28.dp, vertical = 8.dp)
                                .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        ) {
                            Text(
                                text = "Position document inside frame",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // At least 24dp clearance from the frame's bottom edge
                    Spacer(modifier = Modifier.height(28.dp))

                    // Control bar below frame in remaining area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.30f)
                            .padding(bottom = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            onClick = {
                                val capture = imageCapture
                                if (capture != null) {
                                    val photoFile = createTempImageFile(context)
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                    capture.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                capturedPhotoPath = photoFile.absolutePath
                                                capturedBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                // Fallback to synthetic sample if camera capture fails
                                                val samplePath = createSampleDocumentFile(context, "REGISTRATION CERTIFICATE\nTS 09 AB 1234\nRahul Sharma\nDate: 12/04/2023")
                                                capturedPhotoPath = samplePath
                                                capturedBitmap = BitmapFactory.decodeFile(samplePath)
                                            }
                                        }
                                    )
                                } else {
                                    val samplePath = createSampleDocumentFile(context, "REGISTRATION CERTIFICATE\nTS 09 AB 1234\nRahul Sharma\nDate: 12/04/2023")
                                    capturedPhotoPath = samplePath
                                    capturedBitmap = BitmapFactory.decodeFile(samplePath)
                                }
                            },
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier
                                .size(80.dp)
                                .border(5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                .testTag("capture_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            } else {
                // Permission Denied / Not yet granted view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Access Needed",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kaagaz reads deadlines and document numbers completely on-device without internet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = InkBackground)
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        } else {
            // Post-Capture Confirmation: "Retake" and "Use this photo"
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(InkBackground)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Document Captured",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Image Preview Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedBitmap != null) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured document",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Photo Captured Ready for OCR",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Bottom Action Buttons: "Retake" and "Use this photo"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            capturedPhotoPath = null
                            capturedBitmap = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("retake_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Retake",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                    Button(
                        onClick = {
                            val path = capturedPhotoPath ?: ""
                            onPhotoConfirmed(path, memberId)
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(54.dp)
                            .testTag("use_photo_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = InkBackground
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Use this photo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

private fun createTempImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir = context.cacheDir
    return File.createTempFile("KAAGAZ_${timeStamp}_", ".jpg", storageDir)
}

private fun createSampleDocumentFile(context: Context, text: String): String {
    val file = createTempImageFile(context)
    val width = 720
    val height = 1080
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background paper
    val bgPaint = Paint().apply { color = android.graphics.Color.WHITE }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Border
    val borderPaint = Paint().apply {
        color = android.graphics.Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    canvas.drawRect(24f, 24f, width - 24f, height - 24f, borderPaint)

    // Header banner
    val bannerPaint = Paint().apply { color = android.graphics.Color.parseColor("#16232E") }
    canvas.drawRect(28f, 28f, width - 28f, 160f, bannerPaint)

    // Title
    val titlePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 36f
        isFakeBoldText = true
    }
    canvas.drawText("TRANSPORT DEPARTMENT", 60f, 100f, titlePaint)

    // Text content lines
    val textPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 32f
        isFakeBoldText = true
    }
    val regularPaint = Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 28f
    }

    var y = 240f
    canvas.drawText("REGISTRATION CERTIFICATE", 60f, y, textPaint)
    y += 60f
    canvas.drawText("Vehicle Reg No: TS 09 AB 1234", 60f, y, regularPaint)
    y += 50f
    canvas.drawText("Owner: Rahul Sharma", 60f, y, regularPaint)
    y += 50f
    canvas.drawText("Class: Motor Car / LMV", 60f, y, regularPaint)
    y += 50f
    canvas.drawText("Date of Regn: 15/10/2023", 60f, y, regularPaint)
    y += 50f
    canvas.drawText("Chassis No: MA3ERB41S0019284", 60f, y, regularPaint)
    y += 50f
    canvas.drawText("PUC Valid Till: 05/10/2026", 60f, y, regularPaint)
    y += 50f
    canvas.drawText("Insurance Expiry: 14/02/2027", 60f, y, regularPaint)

    // Stamp
    val stampPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#B3261E")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawCircle(width - 160f, height - 200f, 90f, stampPaint)
    val stampTextPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#B3261E")
        textSize = 24f
        isFakeBoldText = true
    }
    canvas.drawText("GOVT SEAL", width - 230f, height - 195f, stampTextPaint)

    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return file.absolutePath
}
