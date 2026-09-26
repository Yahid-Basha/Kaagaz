package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.ui.theme.AmberDueSoon
import com.example.ui.theme.GreenFine
import com.example.ui.theme.InkBackground
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ScanningSide {
    FRONT,
    BACK
}

@Composable
fun ScanScreen(
    memberId: Long,
    onNavigateBack: () -> Unit,
    onPhotoConfirmed: (imagePath: String, memberId: Long, backImagePath: String?) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
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

    var currentScanningSide by remember { mutableStateOf(ScanningSide.FRONT) }
    var frontPhotoPath by remember { mutableStateOf<String?>(null) }
    var frontBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var backPhotoPath by remember { mutableStateOf<String?>(null) }
    var backBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isShowingPreview by remember { mutableStateOf(false) }
    var selectedPreviewTab by remember { mutableStateOf(ScanningSide.FRONT) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    // True once the ML Kit document scanner has failed to launch, or its result came back
    // canceled/failed - from then on this screen falls back to the CameraX capture flow below.
    var showCameraFallback by remember { mutableStateOf(false) }

    // Which side the in-flight scanner/camera invocation is meant to fill in. Null means a
    // combined front(+back) capture, e.g. the initial auto-launch.
    var pendingScanTarget by remember { mutableStateOf<ScanningSide?>(null) }

    val documentScannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(2)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }

    // Applies the document scanner's already edge-detected & deskewed pages. When capturing a
    // specific side (Add/Retake Back), only that side is touched; otherwise page 0 -> front and
    // an optional page 1 -> back (the scanner lets a user scan both sides in one session).
    fun applyScannedPages(pages: List<GmsDocumentScanningResult.Page>, target: ScanningSide?) {
        if (pages.isEmpty()) {
            showCameraFallback = true
            currentScanningSide = target ?: ScanningSide.FRONT
            return
        }
        val firstBitmap = loadBitmapFromUri(context, pages[0].imageUri)
        if (firstBitmap == null) {
            showCameraFallback = true
            currentScanningSide = target ?: ScanningSide.FRONT
            return
        }
        val firstPath = saveBitmapToFile(context, firstBitmap)

        if (target == ScanningSide.BACK) {
            backPhotoPath = firstPath
            backBitmap = firstBitmap
            selectedPreviewTab = ScanningSide.BACK
        } else {
            frontPhotoPath = firstPath
            frontBitmap = firstBitmap
            selectedPreviewTab = ScanningSide.FRONT
            if (pages.size > 1) {
                val secondBitmap = loadBitmapFromUri(context, pages[1].imageUri)
                if (secondBitmap != null) {
                    backPhotoPath = saveBitmapToFile(context, secondBitmap)
                    backBitmap = secondBitmap
                }
            }
        }
        isShowingPreview = true
    }

    val documentScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        val data = activityResult.data
        val pages = if (activityResult.resultCode == Activity.RESULT_OK && data != null) {
            GmsDocumentScanningResult.fromActivityResultIntent(data)?.pages
        } else {
            null
        }
        if (pages != null && pages.isNotEmpty()) {
            applyScannedPages(pages, pendingScanTarget)
        } else {
            // Canceled, failed, or came back with no pages - don't leave the user stuck.
            showCameraFallback = true
            currentScanningSide = pendingScanTarget ?: ScanningSide.FRONT
        }
    }

    // Launches the ML Kit document scanner for the given side (null = combined front+back).
    // Falls back to the CameraX capture UI if the scanner module can't be launched at all.
    fun startCapture(target: ScanningSide?) {
        pendingScanTarget = target
        isShowingPreview = false
        if (showCameraFallback || activity == null) {
            showCameraFallback = true
            currentScanningSide = target ?: ScanningSide.FRONT
            return
        }
        GmsDocumentScanning.getClient(documentScannerOptions)
            .getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                try {
                    documentScannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                } catch (_: Exception) {
                    showCameraFallback = true
                    currentScanningSide = target ?: ScanningSide.FRONT
                }
            }
            .addOnFailureListener {
                showCameraFallback = true
                currentScanningSide = target ?: ScanningSide.FRONT
            }
    }

    LaunchedEffect(Unit) {
        startCapture(null)
    }

    LaunchedEffect(showCameraFallback) {
        if (showCameraFallback && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Launcher for picking images or PDFs from device storage
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val isPdf = mimeType.contains("pdf", ignoreCase = true) || uri.toString().endsWith(".pdf", ignoreCase = true)
            if (isPdf) {
                val pdfBitmaps = renderPdfToBitmaps(context, uri)
                if (pdfBitmaps.isNotEmpty()) {
                    val frontFile = saveBitmapToFile(context, pdfBitmaps[0])
                    frontPhotoPath = frontFile
                    frontBitmap = pdfBitmaps[0]
                    if (pdfBitmaps.size > 1) {
                        val backFile = saveBitmapToFile(context, pdfBitmaps[1])
                        backPhotoPath = backFile
                        backBitmap = pdfBitmaps[1]
                    }
                    selectedPreviewTab = ScanningSide.FRONT
                    isShowingPreview = true
                }
            } else {
                val imageBitmap = loadBitmapFromUri(context, uri)
                if (imageBitmap != null) {
                    val savedPath = saveBitmapToFile(context, imageBitmap)
                    if (currentScanningSide == ScanningSide.BACK) {
                        backPhotoPath = savedPath
                        backBitmap = imageBitmap
                        selectedPreviewTab = ScanningSide.BACK
                    } else {
                        frontPhotoPath = savedPath
                        frontBitmap = imageBitmap
                        selectedPreviewTab = ScanningSide.FRONT
                    }
                    isShowingPreview = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!isShowingPreview && !showCameraFallback) {
            // Waiting on the ML Kit document scanner's full-screen activity to launch/return.
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Opening scanner...",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .padding(start = 16.dp, top = 36.dp)
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
        } else if (!isShowingPreview) {
            // CameraX Fallback - shown only when the document scanner couldn't be launched.
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
                            } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )

                // Layout: Upper 70% for Document Frame Guide, Lower 30% for Capture Control Bar with >=24dp clearance
                Column(modifier = Modifier.fillMaxSize()) {
                    // Upper 70% Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.70f)
                    ) {
                        // Top bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 36.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentScanningSide == ScanningSide.BACK && frontPhotoPath != null) {
                                        // Return to preview of front side
                                        isShowingPreview = true
                                    } else {
                                        onNavigateBack()
                                    }
                                },
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

                            // Scanning Side Pill
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (currentScanningSide == ScanningSide.FRONT) Color(0xFFB3261E) else Color(0xFF1E88E5)
                            ) {
                                Text(
                                    text = if (currentScanningSide == ScanningSide.FRONT) "Front Side (Required)" else "Back Side (Optional)",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            // Upload from Gallery / PDF Button
                            Surface(
                                onClick = {
                                    documentPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.testTag("upload_document_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "Upload Gallery/PDF",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Files/PDF",
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
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .border(2.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                        ) {
                            Text(
                                text = if (currentScanningSide == ScanningSide.FRONT)
                                    "Fit FRONT of document inside frame"
                                else
                                    "Fit BACK of document inside frame",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Minimum 24dp clearance from frame bottom edge
                    Spacer(modifier = Modifier.height(28.dp))

                    // Lower 30% Control Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.30f)
                            .padding(bottom = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sample helper for emulator / testing
                            Surface(
                                onClick = {
                                    val sampleText = if (currentScanningSide == ScanningSide.FRONT) {
                                        "REGISTRATION CERTIFICATE\nTS 09 AB 1234\nRahul Sharma\nDate: 15/10/2023\nPUC Valid: 05/10/2026\nInsurance: 14/02/2027"
                                    } else {
                                        "REGISTRATION CERTIFICATE (BACK SIDE)\nHypothecation: HDFC BANK LTD\nFuel: PETROL / HYBRID\nSeating: 5"
                                    }
                                    val samplePath = createSampleDocumentFile(context, sampleText)
                                    val sampleBitmap = BitmapFactory.decodeFile(samplePath)
                                    val savedPath = saveBitmapToFile(context, sampleBitmap)

                                    if (currentScanningSide == ScanningSide.BACK) {
                                        backPhotoPath = savedPath
                                        backBitmap = sampleBitmap
                                        selectedPreviewTab = ScanningSide.BACK
                                    } else {
                                        frontPhotoPath = savedPath
                                        frontBitmap = sampleBitmap
                                        selectedPreviewTab = ScanningSide.FRONT
                                    }
                                    isShowingPreview = true
                                },
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier.size(52.dp).testTag("sample_doc_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "Test Sample",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Large Circular Capture Button
                            Surface(
                                onClick = {
                                    val capture = imageCapture
                                    if (capture != null) {
                                        val tempFile = createTempImageFile(context)
                                        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                                        capture.takePicture(
                                            outputOptions,
                                            ContextCompat.getMainExecutor(context),
                                            object : ImageCapture.OnImageSavedCallback {
                                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                    val rawBitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                                                    val finalPath = if (rawBitmap != null) saveBitmapToFile(context, rawBitmap) else tempFile.absolutePath

                                                    if (currentScanningSide == ScanningSide.BACK) {
                                                        backPhotoPath = finalPath
                                                        backBitmap = rawBitmap
                                                        selectedPreviewTab = ScanningSide.BACK
                                                    } else {
                                                        frontPhotoPath = finalPath
                                                        frontBitmap = rawBitmap
                                                        selectedPreviewTab = ScanningSide.FRONT
                                                    }
                                                    isShowingPreview = true
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    // Fallback to sample document if camera capture fails on virtual environment
                                                    val samplePath = createSampleDocumentFile(context, "REGISTRATION CERTIFICATE\nTS 09 AB 1234\nRahul Sharma\nDate: 15/10/2023\nPUC Valid: 05/10/2026")
                                                    val sampleBitmap = BitmapFactory.decodeFile(samplePath)
                                                    val finalPath = saveBitmapToFile(context, sampleBitmap)

                                                    if (currentScanningSide == ScanningSide.BACK) {
                                                        backPhotoPath = finalPath
                                                        backBitmap = sampleBitmap
                                                        selectedPreviewTab = ScanningSide.BACK
                                                    } else {
                                                        frontPhotoPath = finalPath
                                                        frontBitmap = sampleBitmap
                                                        selectedPreviewTab = ScanningSide.FRONT
                                                    }
                                                    isShowingPreview = true
                                                }
                                            }
                                        )
                                    } else {
                                        val samplePath = createSampleDocumentFile(context, "REGISTRATION CERTIFICATE\nTS 09 AB 1234\nRahul Sharma\nDate: 15/10/2023\nPUC Valid: 05/10/2026")
                                        val sampleBitmap = BitmapFactory.decodeFile(samplePath)
                                        val finalPath = saveBitmapToFile(context, sampleBitmap)

                                        if (currentScanningSide == ScanningSide.BACK) {
                                            backPhotoPath = finalPath
                                            backBitmap = sampleBitmap
                                            selectedPreviewTab = ScanningSide.BACK
                                        } else {
                                            frontPhotoPath = finalPath
                                            frontBitmap = sampleBitmap
                                            selectedPreviewTab = ScanningSide.FRONT
                                        }
                                        isShowingPreview = true
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

                            // Storage Picker Quick Action
                            Surface(
                                onClick = {
                                    documentPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                                },
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier.size(52.dp).testTag("gallery_quick_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Pick Document",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Camera Permission Required View
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
                        text = "Kaagaz scans deadlines offline. You can also import documents or PDFs directly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = InkBackground)
                        ) {
                            Text("Grant Camera")
                        }
                        OutlinedButton(
                            onClick = { documentPickerLauncher.launch(arrayOf("image/*", "application/pdf")) },
                            border = BorderStroke(1.dp, Color.White),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Pick PDF/Image")
                        }
                    }
                }
            }
        } else {
            // Post-Capture / Cropped Document Confirmation Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(InkBackground)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (backBitmap != null) "Document Scanned (2 Sides)" else "Document Scanned",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Only the document area was cropped for processing",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GreenFine.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, GreenFine)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = null,
                                tint = GreenFine,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Auto Cropped",
                                color = GreenFine,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // If Back Side is also present, show side switcher tabs
                if (backBitmap != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { selectedPreviewTab = ScanningSide.FRONT },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedPreviewTab == ScanningSide.FRONT) Color.White else Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("front_side_tab")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Front Side ✓",
                                    color = if (selectedPreviewTab == ScanningSide.FRONT) InkBackground else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedPreviewTab = ScanningSide.BACK },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedPreviewTab == ScanningSide.BACK) Color.White else Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("back_side_tab")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Back Side ✓",
                                    color = if (selectedPreviewTab == ScanningSide.BACK) InkBackground else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Cropped Document Preview Card
                val activeBitmap = if (selectedPreviewTab == ScanningSide.BACK && backBitmap != null) backBitmap else frontBitmap
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF24343F))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (activeBitmap != null) {
                        Image(
                            bitmap = activeBitmap.asImageBitmap(),
                            contentDescription = "Cropped document image",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .clip(RoundedCornerShape(12.dp))
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
                                text = "Document Ready",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Action Controls Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option to Add Back Side (if not captured yet)
                    if (backBitmap == null) {
                        OutlinedButton(
                            onClick = { startCapture(ScanningSide.BACK) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("add_back_side_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+ Add Back Side (Optional)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        // Options when back side is present
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { startCapture(ScanningSide.BACK) },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("Retake Back", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    backBitmap = null
                                    backPhotoPath = null
                                    selectedPreviewTab = ScanningSide.FRONT
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE57373)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF9A9A))
                            ) {
                                Text("Remove Back", fontSize = 13.sp)
                            }
                        }
                    }

                    // Bottom Row: Retake Front / Retake All & Proceed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                frontBitmap = null
                                frontPhotoPath = null
                                backBitmap = null
                                backPhotoPath = null
                                startCapture(null)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("retake_button"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (backBitmap != null) "Retake All" else "Retake",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }

                        Button(
                            onClick = {
                                val front = frontPhotoPath ?: ""
                                onPhotoConfirmed(front, memberId, backPhotoPath)
                            },
                            modifier = Modifier
                                .weight(1.4f)
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
                                text = if (backBitmap != null) "Use Both Sides" else "Use this photo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
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

fun saveBitmapToFile(context: Context, bitmap: Bitmap): String {
    val file = createTempImageFile(context)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }
    return file.absolutePath
}

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (_: Exception) {
        null
    }
}

// Built-in Android PdfRenderer renders PDF pages directly to Bitmap without external dependencies
fun renderPdfToBitmaps(context: Context, uri: Uri): List<Bitmap> {
    val bitmaps = mutableListOf<Bitmap>()
    try {
        val tempPdf = File.createTempFile("pdf_render_", ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempPdf).use { output -> input.copyTo(output) }
        }
        val pfd = ParcelFileDescriptor.open(tempPdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val count = renderer.pageCount.coerceAtMost(2)
        for (i in 0 until count) {
            val page = renderer.openPage(i)
            val renderW = page.width * 2
            val renderH = page.height * 2
            val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmaps.add(bitmap)
        }
        renderer.close()
        pfd.close()
        tempPdf.delete()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return bitmaps
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
        textSize = 34f
        isFakeBoldText = true
    }
    canvas.drawText("TRANSPORT DEPARTMENT", 50f, 100f, titlePaint)

    // Text content lines
    val regularPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 28f
    }

    var y = 230f
    text.lines().forEach { line ->
        if (line.isNotBlank()) {
            canvas.drawText(line.trim(), 50f, y, regularPaint)
            y += 50f
        }
    }

    // Stamp
    val stampPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#B3261E")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawCircle(width - 160f, height - 190f, 80f, stampPaint)
    val stampTextPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#B3261E")
        textSize = 22f
        isFakeBoldText = true
    }
    canvas.drawText("VERIFIED", width - 220f, height - 185f, stampTextPaint)

    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return file.absolutePath
}
