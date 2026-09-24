package com.riftbound.recon.ui.screens

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

    // Sort cards based on selected option
    val sortedCards = remember(filteredCards, selectedSortOption) {
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

    LaunchedEffect(collectionId) {
        viewModel.loadCollection(collectionId)
    }

    val coll = collection ?: return

    Scaffold(
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
                        Icon(Icons.Default.Edit, contentDescription = "Editar Coleção")
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

            // Header Row: Count & Sort Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${filteredCards.size} cartas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sort Dropdown Button
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
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Posição",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(56.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Carta",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(42.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Nome",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.2f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Coleção / Cód.",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedCards, key = { it.id }) { item ->
                        CollectionCardRow(
                            item = item,
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

@Composable
fun CollectionCardRow(
    item: CollectionCard,
    onCardClick: () -> Unit
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
                .clickable(onClick = onCardClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position number as "#1" in bold
            Text(
                text = "#${item.scanOrder}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(56.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Thumbnail with Card Artwork
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(58.dp)
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

            Spacer(modifier = Modifier.width(10.dp))

            // Card Name
            Text(
                text = item.card.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.2f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Collection & Collector Code (e.g. Origins • 066a/298)
            Text(
                text = formatCardSetAndCode(item.card),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1.1f)
            )
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
