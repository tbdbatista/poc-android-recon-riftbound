package com.riftbound.recon.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.riftbound.recon.data.scanner.OcrLine
import com.riftbound.recon.domain.model.Card
import com.riftbound.recon.ui.MainViewModel
import com.riftbound.recon.ui.ScanFeedback
import kotlinx.coroutines.delay
import java.util.concurrent.Executor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    // Scanner State
    val scannedCards by viewModel.scannedCards.collectAsState()
    val scanFeedback by viewModel.scanFeedback.collectAsState()
    val canUndoDeletion by viewModel.canUndoDeletion.collectAsState()
    
    // Auto-clear feedback and trigger haptics
    LaunchedEffect(scanFeedback) {
        if (scanFeedback != null) {
            when (scanFeedback) {
                is ScanFeedback.Success -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                is ScanFeedback.Error -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                null -> {}
            }
            delay(3500)
            viewModel.clearScanFeedback()
        }
    }
    
    // UI dialog states
    var showSaveDialog by remember { mutableStateOf(false) }
    var showConcludeDialog by remember { mutableStateOf(false) }
    var showDiscardAllConfirmDialog by remember { mutableStateOf(false) }
    var isProcessingPhoto by remember { mutableStateOf(false) }
    var previewCardInfo by remember { mutableStateOf<Pair<Int, Card>?>(null) }
    var cardPendingDeletion by remember { mutableStateOf<Pair<Int, Card>?>(null) }

    // Camera Permission request
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
        viewModel.startScanning() // Ensure view model clears old logs and starts session
    }

    // Set up ImageCapture
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { ContextCompat.getMainExecutor(context) }
    val recognizer = remember { TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Capturar Cartas", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Reconhecimento Óptico On-Device",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Viewfinder (PreviewView)
            if (hasCameraPermission) {
                CameraViewfinder(
                    imageCapture = imageCapture
                )
            } else {
                PermissionDeniedView(onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) })
            }

            // Dynamic Top Feedback Floating Banner on Success / Error (Drops down freshly on every scan)
            ScanFeedbackBanner(
                feedback = scanFeedback,
                onDismiss = { viewModel.clearScanFeedback() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Bottom Controller Dashboard Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Shelf of currently scanned card thumbnails
                if (scannedCards.isNotEmpty()) {
                    ScannedCardsShelf(
                        scannedCards = scannedCards,
                        onCardClick = { index, card ->
                            previewCardInfo = index to card
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Shutter and Action row controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // 1. Desfazer última exclusão
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.height(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { viewModel.restoreLastDeletedCard() },
                                enabled = canUndoDeletion,
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        if (canUndoDeletion) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Desfazer última exclusão",
                                    tint = if (canUndoDeletion) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Desfazer\nexclusão",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center,
                            minLines = 2,
                            maxLines = 2,
                            color = if (canUndoDeletion) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.6f)
                        )
                    }

                    // 2. Excluir última captura
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.height(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    if (scannedCards.isNotEmpty()) {
                                        val lastIndex = scannedCards.size - 1
                                        val lastCard = scannedCards[lastIndex]
                                        if (viewModel.shouldSkipDeleteConfirmation()) {
                                            viewModel.undoLastScan()
                                        } else {
                                            cardPendingDeletion = lastIndex to lastCard
                                        }
                                    }
                                },
                                enabled = scannedCards.isNotEmpty(),
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        if (scannedCards.isNotEmpty()) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Excluir última captura",
                                    tint = if (scannedCards.isNotEmpty()) Color.Red else Color.Gray
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Excluir\núltima",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center,
                            minLines = 2,
                            maxLines = 2,
                            color = if (scannedCards.isNotEmpty()) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.6f)
                        )
                    }

                    // 3. Capturar carta (Center Shutter Button)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Box(
                            modifier = Modifier.height(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = {
                                    if (!isProcessingPhoto && hasCameraPermission) {
                                        isProcessingPhoto = true
                                        takePhoto(
                                            imageCapture = imageCapture,
                                            executor = cameraExecutor,
                                            onImageCaptured = { inputImage ->
                                                recognizer.process(inputImage)
                                                    .addOnSuccessListener { visionText ->
                                                        val linesList = mutableListOf<OcrLine>()
                                                        for (block in visionText.textBlocks) {
                                                            for (line in block.lines) {
                                                                val rect = line.boundingBox
                                                                if (rect != null) {
                                                                    linesList.add(
                                                                        OcrLine(
                                                                            text = line.text,
                                                                            left = rect.left,
                                                                            top = rect.top,
                                                                            right = rect.right,
                                                                            bottom = rect.bottom
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        viewModel.processOcrLines(linesList)
                                                    }
                                                    .addOnFailureListener { e ->
                                                        e.printStackTrace()
                                                    }
                                                    .addOnCompleteListener {
                                                        isProcessingPhoto = false
                                                    }
                                            }
                                        )
                                    }
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isProcessingPhoto) Color.Gray else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .size(64.dp)
                                    .border(3.dp, Color.White, CircleShape)
                                    .shadow(6.dp, CircleShape),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                if (isProcessingPhoto) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Capturar\ncarta",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            minLines = 2,
                            maxLines = 2,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 4. Concluir captura (Right Check Button)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.height(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { showConcludeDialog = true },
                                enabled = scannedCards.isNotEmpty(),
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        if (scannedCards.isNotEmpty()) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Concluir captura",
                                    tint = if (scannedCards.isNotEmpty()) MaterialTheme.colorScheme.secondary else Color.Gray
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Concluir\ncaptura",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center,
                            minLines = 2,
                            maxLines = 2,
                            color = if (scannedCards.isNotEmpty()) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }

    // Dialog: Save Collection Name Prompt
    if (showSaveDialog) {
        var collectionName by remember { mutableStateOf("") }
        var collectionDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Salvar Coleção") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Você capturou ${scannedCards.size} cartas. Digite o nome para salvar essa coleção.")
                    OutlinedTextField(
                        value = collectionName,
                        onValueChange = { collectionName = it },
                        label = { Text("Título da Coleção") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = collectionDesc,
                        onValueChange = { collectionDesc = it },
                        label = { Text("Descrição (Opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (collectionName.isNotBlank()) {
                            viewModel.saveScanningSession(collectionName, collectionDesc)
                            showSaveDialog = false
                            navController.navigate("collections")
                        }
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Voltar")
                }
            }
        )
    }

    // Dialog: Conclude Session Options
    if (showConcludeDialog) {
        ConcludeSessionDialog(
            scannedCount = scannedCards.size,
            onSaveList = {
                showConcludeDialog = false
                showSaveDialog = true
            },
            onContinueCapturing = {
                showConcludeDialog = false
            },
            onDiscardAll = {
                showConcludeDialog = false
                showDiscardAllConfirmDialog = true
            },
            onDismiss = {
                showConcludeDialog = false
            }
        )
    }

    // Dialog: Confirm Discard All Captured Cards
    if (showDiscardAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardAllConfirmDialog = false },
            title = {
                Text(
                    text = "Limpar Sessão",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Tem certeza que deseja limpar todas as ${scannedCards.size} cartas capturadas? Esta ação não pode ser desfeita.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearScanningSession()
                        showDiscardAllConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Limpar Tudo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardAllConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog: Card Preview with Delete and Close options
    if (previewCardInfo != null) {
        val (cardIndex, card) = previewCardInfo!!
        ScannedCardPreviewDialog(
            card = card,
            onDismiss = {
                previewCardInfo = null
            },
            onDeleteClick = {
                if (viewModel.shouldSkipDeleteConfirmation()) {
                    viewModel.removeScannedCardAt(cardIndex)
                    previewCardInfo = null
                } else {
                    cardPendingDeletion = cardIndex to card
                }
            }
        )
    }

    // Dialog: Confirm Deletion Prompt with "Do not ask again" checkbox
    if (cardPendingDeletion != null) {
        val (cardIndex, card) = cardPendingDeletion!!
        var doNotAskAgain by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { cardPendingDeletion = null },
            title = {
                Text(
                    text = "Excluir Carta",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Tem certeza que deseja excluir \"${card.name}\" da sessão?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { doNotAskAgain = !doNotAskAgain }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = doNotAskAgain,
                            onCheckedChange = { doNotAskAgain = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Não perguntar novamente",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (doNotAskAgain) {
                            viewModel.setSkipDeleteConfirmation(true)
                        }
                        viewModel.removeScannedCardAt(cardIndex)
                        cardPendingDeletion = null
                        previewCardInfo = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { cardPendingDeletion = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CameraViewfinder(
    imageCapture: ImageCapture
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ScanFeedbackBanner(
    feedback: ScanFeedback?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = feedback,
        transitionSpec = {
            (slideInVertically(
                animationSpec = tween(durationMillis = 350),
                initialOffsetY = { -it }
            ) + fadeIn(animationSpec = tween(durationMillis = 350)))
                .togetherWith(
                    slideOutVertically(
                        animationSpec = tween(durationMillis = 250),
                        targetOffsetY = { -it }
                    ) + fadeOut(animationSpec = tween(durationMillis = 250))
                )
        },
        label = "scanFeedbackBannerTransition",
        modifier = modifier
    ) { currentFeedback ->
        if (currentFeedback != null) {
            when (currentFeedback) {
                is ScanFeedback.Success -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.5.dp, Color(0xFF00E676)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDismiss() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Thumbnail / Badge
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = "file:///android_asset/${currentFeedback.card.imageUrl}",
                                    contentDescription = currentFeedback.card.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Card Information
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Carta Reconhecida!",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E676),
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentFeedback.card.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = formatCardSetAndCode(currentFeedback.card),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                is ScanFeedback.Error -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDismiss() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Não Reconhecida",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentFeedback.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScannedCardsShelf(
    scannedCards: List<Card>,
    onCardClick: (Int, Card) -> Unit
) {
    val listState = rememberLazyListState()
    val reversedCards = remember(scannedCards) { scannedCards.asReversed() }

    LaunchedEffect(scannedCards.size) {
        if (scannedCards.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cartas na Sessão",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${scannedCards.size} capturadas",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(reversedCards) { visualIndex, card ->
                val originalIndex = (scannedCards.size - 1) - visualIndex
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(85.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .clickable { onCardClick(originalIndex, card) }
                ) {
                    AsyncImage(
                        model = "file:///android_asset/${card.imageUrl}",
                        contentDescription = card.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Position badge on thumbnail (#N)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(3.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = "#${originalIndex + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScannedCardPreviewDialog(
    card: Card,
    onDismiss: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Card name and basic info
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Coleção: ${formatCardSetAndCode(card)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Card Image Preview in larger format
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(0.71f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "file:///android_asset/${card.imageUrl}",
                        contentDescription = card.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Excluir e Fechar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excluir")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fechar")
                    }
                }
            }
        }
    }
}

@Composable
fun ConcludeSessionDialog(
    scannedCount: Int,
    onSaveList: () -> Unit,
    onContinueCapturing: () -> Unit,
    onDiscardAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Concluir Capturas",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Você capturou $scannedCount cartas nesta sessão. O que deseja fazer?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Option 1: Salvar Lista
                Button(
                    onClick = onSaveList,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar Lista de Cartas")
                }

                // Option 2: Continuar Capturando
                OutlinedButton(
                    onClick = onContinueCapturing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continuar Capturando")
                }

                // Option 3: Descartar Todas as Fotos
                OutlinedButton(
                    onClick = onDiscardAll,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Descartar Todas as Cartas")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Voltar")
            }
        }
    )
}

@Composable
fun PermissionDeniedView(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Permissão de Câmera Negada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Precisamos de permissão para utilizar a câmera do celular para capturar e ler as cartas físicas.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text("Permitir Acesso")
            }
        }
    }
}

private fun takePhoto(
    imageCapture: ImageCapture,
    executor: Executor,
    onImageCaptured: (InputImage) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )
                    onImageCaptured(image)
                }
                imageProxy.close()
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}
