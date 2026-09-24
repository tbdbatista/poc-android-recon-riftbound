package com.riftbound.recon.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.riftbound.recon.domain.model.Card
import com.riftbound.recon.domain.model.CollectionCard
import com.riftbound.recon.ui.MainViewModel
import com.riftbound.recon.ui.util.CollectionShareHelper
import kotlinx.coroutines.launch

enum class CollectionSortOption(val label: String) {
    SCAN_ORDER_ASC("1ª para última"),
    SCAN_ORDER_DESC("Última para 1ª"),
    NAME_ASC("Nome (A - Z)"),
    SET_ASC("Coleção (Set)"),
    COLLECTOR_NUMBER_ASC("Numeração (Cód.)")
}

fun formatCardCollectorCode(card: Card): String {
    val rawCollector = card.collectorNumber.trim()
    if (rawCollector.contains("/")) {
        return rawCollector
    }

    // 1. Crystal / Special Subcollection (e.g. sp1 -> SP1/006 in Vendetta)
    val spMatch = Regex("""^(?i)sp(\d+)$""").find(rawCollector)
    if (spMatch != null) {
        val num = spMatch.groupValues[1]
        return "SP$num/006"
    }

    // 2. Runes (e.g. r01 -> R01, r04 -> R04)
    val rMatch = Regex("""^(?i)r(\d+)$""").find(rawCollector)
    if (rMatch != null) {
        return "R${rMatch.groupValues[1]}"
    }

    // 3. Tokens (e.g. t01 -> T01, t01g -> T01G)
    val tMatch = Regex("""^(?i)t(.+)$""").find(rawCollector)
    if (tMatch != null) {
        return "T${tMatch.groupValues[1].uppercase()}"
    }

    // 4. Main set numbered cards (pure digits or digits with suffix like 066a, or overnumbered 299*)
    // Only cards starting with numeric digits belong to the main set numbering sequence.
    if (rawCollector.firstOrNull()?.isDigit() == true) {
        val mainTotals = mapOf(
            "OGN" to "298",
            "SFD" to "221",
            "UNL" to "219",
            "VEN" to "166",
            "OGS" to "024"
        )
        val total = mainTotals[card.setCode.uppercase()]
        if (total != null) {
            return "$rawCollector/$total"
        }
    }

    return rawCollector
}

fun formatCardSetAndCode(card: Card): String {
    val codeStr = formatCardCollectorCode(card)
    val displaySet = when (card.setCode.uppercase()) {
        "OGN" -> "Origins"
        "SFD" -> "Spiritforged"
        "UNL" -> "Unleashed"
        "VEN" -> "Vendetta"
        "OGS" -> "Proving Grounds"
        "JDG" -> "Judge Promo"
        "OPP" -> "OP Promo"
        "PR" -> "Promo"
        else -> card.set.ifBlank { card.setCode }
    }
    return "$displaySet • $codeStr"
}

fun compareCollectorNumbers(a: String, b: String): Int {
    val prefixA = a.filter { !it.isDigit() }.lowercase()
    val prefixB = b.filter { !it.isDigit() }.lowercase()
    val prefixCmp = prefixA.compareTo(prefixB)
    if (prefixCmp != 0) return if (prefixCmp < 0) -1 else 1

    val digitsA = a.filter { it.isDigit() }.toIntOrNull() ?: 0
    val digitsB = b.filter { it.isDigit() }.toIntOrNull() ?: 0
    return digitsA.compareTo(digitsB).coerceIn(-1, 1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    collectionId: Long,
    navController: NavController,
    viewModel: MainViewModel
) {
    val collection by viewModel.selectedCollection.collectAsState()
    val cards by viewModel.selectedCollectionCards.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedSortOption by remember { mutableStateOf(CollectionSortOption.SCAN_ORDER_ASC) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    var previewCard by remember { mutableStateOf<Card?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val allCards by viewModel.allCards.collectAsState()

    // Filter cards based on search query
    val filteredCards = remember(cards, searchQuery) {
        cards.filter {
            it.card.name.contains(searchQuery, ignoreCase = true) ||
            it.card.set.contains(searchQuery, ignoreCase = true) ||
            it.card.setCode.contains(searchQuery, ignoreCase = true) ||
            it.card.collectorNumber.contains(searchQuery, ignoreCase = true) ||
            formatCardSetAndCode(it.card).contains(searchQuery, ignoreCase = true) ||
            it.card.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Sort cards based on selected option (forces natural scan order when in edit mode)
    val sortedCards = remember(filteredCards, selectedSortOption, isEditMode) {
        if (isEditMode) {
            filteredCards.sortedBy { it.scanOrder }
        } else {
            when (selectedSortOption) {
                CollectionSortOption.SCAN_ORDER_ASC -> filteredCards.sortedBy { it.scanOrder }
                CollectionSortOption.SCAN_ORDER_DESC -> filteredCards.sortedByDescending { it.scanOrder }
                CollectionSortOption.NAME_ASC -> filteredCards.sortedBy { it.card.name.lowercase() }
                CollectionSortOption.SET_ASC -> filteredCards.sortedWith(
                    compareBy<CollectionCard> { it.card.set.lowercase() }
                        .thenComparator { a, b -> compareCollectorNumbers(a.card.collectorNumber, b.card.collectorNumber) }
                )
                CollectionSortOption.COLLECTOR_NUMBER_ASC -> filteredCards.sortedWith { a, b ->
                    compareCollectorNumbers(a.card.collectorNumber, b.card.collectorNumber)
                }
            }
        }
    }

    LaunchedEffect(collectionId) {
        viewModel.loadCollection(collectionId)
    }

    val coll = collection ?: return

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(coll.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    val context = LocalContext.current
                    IconButton(
                        onClick = {
                            if (cards.isNotEmpty()) {
                                CollectionShareHelper.shareCollection(context, coll, cards)
                            }
                        },
                        enabled = cards.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Compartilhar Coleção",
                            tint = if (cards.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Editar Detalhes da Coleção")
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir Coleção", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (coll.description.isNotEmpty()) {
                Text(
                    text = coll.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Local Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                placeholder = { Text("Procurar cartas nesta coleção...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // Header Row: Count & Sort Selector + Edit List Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${filteredCards.size} cartas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledTonalButton(
                        onClick = {
                            isEditMode = !isEditMode
                            if (isEditMode) {
                                selectedSortOption = CollectionSortOption.SCAN_ORDER_ASC
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = if (isEditMode) {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        }
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEditMode) "Concluir" else "Editar Lista",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Sort Dropdown Button (hidden when in edit mode)
                if (!isEditMode) {
                    Box {
                        Surface(
                            onClick = { sortMenuExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Ordenar: ${selectedSortOption.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Selecionar Ordenação",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            CollectionSortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (selectedSortOption == option) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.width(16.dp))
                                            }
                                            Text(
                                                text = option.label,
                                                fontWeight = if (selectedSortOption == option) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedSortOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedSortOption = option
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { showAddCardDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Inserir Carta", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Edit Mode Active Banner
            if (isEditMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Modo de Edição: altere a ordem ou exclua cartas",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        TextButton(
                            onClick = { showAddCardDialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("+ Inserir", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (sortedCards.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Nenhuma carta corresponde à busca." else "Coleção vazia.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Table Header (Legenda)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditMode) {
                            Spacer(modifier = Modifier.width(36.dp))
                        }
                        Text(
                            text = "Posição",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(if (isEditMode) 36.dp else 52.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Carta",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(38.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nome",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Coleção / Cód.",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.9f)
                        )
                        if (isEditMode) {
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedCards, key = { it.id }) { item ->
                        val index = sortedCards.indexOf(item)
                        SwipeableCollectionCardRow(
                            item = item,
                            isEditMode = isEditMode,
                            canMoveUp = index > 0,
                            canMoveDown = index < sortedCards.lastIndex,
                            onMoveUp = {
                                viewModel.moveCardInCollection(index, index - 1)
                            },
                            onMoveDown = {
                                viewModel.moveCardInCollection(index, index + 1)
                            },
                            onDelete = {
                                viewModel.removeCardFromCollection(item.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Carta \"${item.card.name}\" removida da coleção.")
                                }
                            },
                            onCardClick = { previewCard = item.card }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Dialog: Insert Card Manually
    if (showAddCardDialog) {
        AddCardToCollectionDialog(
            allCards = allCards,
            onAddCard = { cardId ->
                viewModel.addCardToLoadedCollection(cardId)
                scope.launch {
                    snackbarHostState.showSnackbar("Carta inserida na coleção!")
                }
            },
            onDismiss = { showAddCardDialog = false }
        )
    }

    // Modal: Enlarged Card Preview
    if (previewCard != null) {
        CollectionCardPreviewDialog(
            card = previewCard!!,
            onDismiss = { previewCard = null }
        )
    }

    // Dialog: Edit Name / Description
    if (showEditDialog) {
        var editName by remember { mutableStateOf(coll.name) }
        var editDesc by remember { mutableStateOf(coll.description) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar Informações") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Título da Coleção") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Descrição (Opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updateCollectionDetails(coll.id, editName, editDesc)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog: Confirm Delete
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Excluir Coleção") },
            text = { Text("Tem certeza que deseja excluir esta coleção? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCollection(coll.id)
                        showDeleteConfirmDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableCollectionCardRow(
    item: CollectionCard,
    isEditMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onCardClick: () -> Unit
) {
    if (isEditMode) {
        CollectionCardRow(
            item = item,
            isEditMode = true,
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onDelete = onDelete,
            onCardClick = onCardClick
        )
    } else {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                    onDelete()
                    true
                } else {
                    false
                }
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = false,
            backgroundContent = {
                val color by animateColorAsState(
                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    },
                    label = "swipe_bg_color"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(color)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.onError
                        )
                        Text(
                            text = "Excluir",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        ) {
            CollectionCardRow(
                item = item,
                isEditMode = false,
                canMoveUp = false,
                canMoveDown = false,
                onMoveUp = {},
                onMoveDown = {},
                onDelete = {},
                onCardClick = onCardClick
            )
        }
    }
}

@Composable
fun CollectionCardRow(
    item: CollectionCard,
    isEditMode: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCardClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isEditMode, onClick = onCardClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // In edit mode: Delete button on the far left
            if (isEditMode) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remover Carta",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Position number as "#1" in bold
            Text(
                text = "#${item.scanOrder}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(if (isEditMode) 36.dp else 52.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Thumbnail with Card Artwork
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "file:///android_asset/${item.card.imageUrl}",
                    contentDescription = item.card.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Card Name
            Text(
                text = item.card.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Collection & Collector Code (e.g. Origins • 066a/298)
            Text(
                text = formatCardSetAndCode(item.card),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.9f)
            )

            // In edit mode: Up / Down arrow buttons on the right
            if (isEditMode) {
                Spacer(modifier = Modifier.width(4.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Mover para cima",
                            tint = if (canMoveUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Mover para baixo",
                            tint = if (canMoveDown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddCardToCollectionDialog(
    allCards: List<Card>,
    onAddCard: (cardId: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(allCards, searchQuery) {
        if (searchQuery.isBlank()) {
            allCards.take(40)
        } else {
            allCards.filter { card ->
                card.name.contains(searchQuery, ignoreCase = true) ||
                card.set.contains(searchQuery, ignoreCase = true) ||
                card.setCode.contains(searchQuery, ignoreCase = true) ||
                card.collectorNumber.contains(searchQuery, ignoreCase = true) ||
                formatCardSetAndCode(card).contains(searchQuery, ignoreCase = true) ||
                card.tags.any { it.contains(searchQuery, ignoreCase = true) }
            }.take(50)
        }
    }
    var addedCardId by remember { mutableStateOf<Int?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .shadow(16.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Inserir Carta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Nome, set ou código (ex: Ahri, 066)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${filtered.size} cartas encontradas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { card ->
                        val wasJustAdded = addedCardId == card.id
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = "file:///android_asset/${card.imageUrl}",
                                        contentDescription = card.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = card.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatCardSetAndCode(card),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                FilledTonalIconButton(
                                    onClick = {
                                        onAddCard(card.id)
                                        addedCardId = card.id
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (wasJustAdded) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = "Adicionar",
                                        tint = if (wasJustAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Concluir")
                }
            }
        }
    }
}

@Composable
fun CollectionCardPreviewDialog(
    card: Card,
    onDismiss: () -> Unit
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

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fechar")
                }
            }
        }
    }
}
