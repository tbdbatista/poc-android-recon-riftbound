package com.riftbound.recon.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.riftbound.recon.data.scanner.OcrLine
import com.riftbound.recon.domain.model.Card
import com.riftbound.recon.ui.MainViewModel
import java.util.concurrent.Executor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    
    // Scanner State
    val scannedCards by viewModel.scannedCards.collectAsState()
    val scannerLogs by viewModel.scannerLogs.collectAsState()
    
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
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capturar Cartas TCG", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
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

            // Card alignment guide frame in the center of camera preview
            CardGuideFrame()

            // Console Logs terminal overlay
            ConsoleLogsOverlay(scannerLogs = scannerLogs)

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
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Delete Last Captured Card
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
                            .size(52.dp)
                            .background(
                                if (scannedCards.isNotEmpty()) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Apagar última captura",
                            tint = if (scannedCards.isNotEmpty()) Color.Red else Color.Gray
                        )
                    }

                    // Center: Large Shutter Capture Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
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
                                .size(74.dp)
                                .border(4.dp, Color.White, CircleShape)
                                .shadow(8.dp, CircleShape),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            if (isProcessingPhoto) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }

                    // Right: Conclude Session & Open Options Dialog
                    IconButton(
                        onClick = { showConcludeDialog = true },
                        enabled = scannedCards.isNotEmpty(),
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                if (scannedCards.isNotEmpty()) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Concluir",
                            tint = if (scannedCards.isNotEmpty()) MaterialTheme.colorScheme.secondary else Color.Gray
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
fun CardGuideFrame() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Outer box of viewfinder card shape helper overlay
        Box(
            modifier = Modifier
                .width(260.dp)
                .height(370.dp)
                .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        )
    }
}

@Composable
fun ConsoleLogsOverlay(scannerLogs: List<String>) {
    val listState = rememberLazyListState()
    LaunchedEffect(scannerLogs.size) {
        if (scannerLogs.isNotEmpty()) {
            listState.animateScrollToItem(scannerLogs.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(scannerLogs) { log ->
                Text(
                    text = log,
                    color = if (log.contains("SUCESSO")) Color.Green
                    else if (log.contains("FALHA") || log.contains("Erro")) Color.Red
                    else if (log.contains("Cód. Rodapé") || log.contains("Energia Runa")) Color.Cyan
                    else if (log.contains("INICIANDO") || log.contains("FIM")) Color.Yellow
                    else Color.LightGray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
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
        Text(
            text = "Cartas na Sessão (${scannedCards.size})",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
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
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        .clickable { onCardClick(originalIndex, card) }
                ) {
                    AsyncImage(
                        model = "file:///android_asset/${card.imageUrl}",
                        contentDescription = card.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
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
                    text = "Coleção: ${card.set} • #${card.collectorNumber}",
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
