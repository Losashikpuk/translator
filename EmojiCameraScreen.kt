package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.camera.EmojiMask
import com.example.data.camera.EmojiMosaicProcessor
import com.example.data.camera.EmojiPalette
import com.example.ui.viewmodel.MainViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiCameraScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPalette by viewModel.cameraPalette.collectAsState()
    val cameraMask by viewModel.cameraMask.collectAsState()
    val capturedBitmap by viewModel.capturedBitmap.collectAsState()
    val capturedEmojiArt by viewModel.capturedEmojiArt.collectAsState()
    val showCaptureDialog by viewModel.showCaptureDialog.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Photo picker launcher for testing with any gallery photo
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        viewModel.onPhotoCaptured(bitmap)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Не удалось загрузить изображение: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var liveEmojiGrid by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var isLiveFilterActive by remember { mutableStateOf(true) }
    var cameraControlRef by remember { mutableStateOf<CameraControl?>(null) }
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }

    // Fallback demo animation frame if hardware camera is unattached
    LaunchedEffect(hasCameraPermission, cameraPalette, cameraMask) {
        if (!hasCameraPermission) {
            // Generate a demo canvas bitmap with shapes and gradient to demonstrate live emoji filter
            val demoBitmap = createDemoBitmap()
            val grid = EmojiMosaicProcessor.processBitmapToEmojiGrid(
                demoBitmap,
                gridCols = 18,
                gridRows = 22,
                palette = cameraPalette,
                mask = cameraMask
            )
            liveEmojiGrid = grid
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Эмодзи-Камера",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "📸✨", fontSize = 20.sp)
                }
                Text(
                    text = "Фильтр цветовой палитры и наложение эмодзи-масок",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Gallery Import button
            IconButton(
                onClick = { photoPickerLauncher.launch("image/*") },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Выбрать фото из галереи",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Viewport Box (CameraX or Live Emoji Mosaic)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val cameraExecutor = Executors.newSingleThreadExecutor()

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCaptureRef = imageCapture

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val bitmap = imageProxy.toBitmap()
                                imageProxy.close()
                                if (bitmap != null && isLiveFilterActive) {
                                    val grid = EmojiMosaicProcessor.processBitmapToEmojiGrid(
                                        bitmap = bitmap,
                                        gridCols = 18,
                                        gridRows = 22,
                                        palette = cameraPalette,
                                        mask = cameraMask
                                    )
                                    liveEmojiGrid = grid
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture,
                                    imageAnalysis
                                )
                                cameraControlRef = camera.cameraControl
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Live Emoji Mosaic Overlay Layer
                if (isLiveFilterActive && liveEmojiGrid.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.88f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            liveEmojiGrid.forEach { row ->
                                Text(
                                    text = row.joinToString(""),
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                // Permission Request / Demo Fallback View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (liveEmojiGrid.isNotEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            liveEmojiGrid.take(14).forEach { row ->
                                Text(
                                    text = row.joinToString(""),
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = "Требуется доступ к камере",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Разрешите камеру для живого эмодзи-видоискателя или выберите фото из галереи",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Разрешить камеру")
                    }
                }
            }

            // Top Status Badge: Active Mask & Filter indicator
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = cameraMask.icon, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${cameraPalette.title} • ${cameraMask.title}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Toggle Live Filter view button
            IconButton(
                onClick = { isLiveFilterActive = !isLiveFilterActive },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
            ) {
                Icon(
                    imageVector = if (isLiveFilterActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Переключить отображение фильтра",
                    tint = if (isLiveFilterActive) MaterialTheme.colorScheme.primary else Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Palette Selector
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Цветовая палитра эмодзи:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(EmojiPalette.values()) { palette ->
                    val isSelected = cameraPalette == palette
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCameraPalette(palette) },
                        label = {
                            Text(
                                text = palette.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Mask Selector
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Эмодзи-маски в реальном времени:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(EmojiMask.values()) { mask ->
                    val isSelected = cameraMask == mask
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCameraMask(mask) },
                        leadingIcon = { Text(text = mask.icon) },
                        label = {
                            Text(
                                text = mask.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Shutter Button Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 96.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (hasCameraPermission && imageCaptureRef != null) {
                        val imageCapture = imageCaptureRef!!
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                    val bitmap = imageProxy.toBitmap()
                                    imageProxy.close()
                                    if (bitmap != null) {
                                        viewModel.onPhotoCaptured(bitmap)
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Toast.makeText(context, "Сбой съемки: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else {
                        // Capture demo frame
                        val demoBitmap = createDemoBitmap()
                        viewModel.onPhotoCaptured(demoBitmap)
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .size(72.dp)
                    .testTag("capture_emoji_photo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "Сделать фото эмодзи",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }

    // Photo Capture Result Modal Dialog
    if (showCaptureDialog && capturedBitmap != null) {
        val bitmap = capturedBitmap!!
        val emojiArt = capturedEmojiArt ?: ""

        AlertDialog(
            onDismissRequest = { viewModel.dismissCaptureDialog() },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📸 Эмодзи-Фото готово!", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { viewModel.dismissCaptureDialog() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emojiArt,
                            fontSize = 8.sp,
                            lineHeight = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Copy Emoji Art Text
                        FilledTonalButton(
                            onClick = {
                                viewModel.copyToClipboard(
                                    context,
                                    emojiArt,
                                    "Эмодзи-арт скопирован! Можно отправлять в мессенджеры."
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Копировать", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Share Image / Text
                        Button(
                            onClick = {
                                shareEmojiPhoto(context, bitmap, emojiArt)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Поделиться", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissCaptureDialog() }) {
                    Text("Готово")
                }
            }
        )
    }
}

private fun createDemoBitmap(): Bitmap {
    val width = 240
    val height = 300
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Gradient background
    val paint = Paint().apply {
        isAntiAlias = true
    }

    paint.color = AndroidColor.rgb(30, 41, 59)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    // Glowing circle
    paint.color = AndroidColor.rgb(234, 179, 8)
    canvas.drawCircle(width / 2f, height / 3f, 60f, paint)

    // Cool neon shapes
    paint.color = AndroidColor.rgb(239, 68, 68)
    canvas.drawRect(40f, height / 2f, 90f, height * 0.8f, paint)

    paint.color = AndroidColor.rgb(59, 130, 246)
    canvas.drawRect(140f, height / 2f, 200f, height * 0.8f, paint)

    return bitmap
}

private fun shareEmojiPhoto(context: Context, bitmap: Bitmap, emojiArt: String) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "emoji_photo_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_TEXT, "Смотри мое фото из эмодзи:\n\n$emojiArt")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться эмодзи-фото"))
    } catch (e: Exception) {
        // Fallback to text share
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, emojiArt)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться эмодзи-артом"))
    }
}
