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
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.ui.unit.sp
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

    val spMatch = Regex("""^(?i)sp(\d+)$""").find(rawCollector)
    if (spMatch != null) {
        val num = spMatch.groupValues[1]
        return "SP$num/006"
    }

    val rMatch = Regex("""^(?i)r(\d+)$""").find(rawCollector)
    if (rMatch != null) {
        return "R${rMatch.groupValues[1]}"
    }

    val tMatch = Regex("""^(?i)t(.+)$""").find(rawCollector)
    if (tMatch != null) {
        return "T${tMatch.groupValues[1].uppercase()}"
    }

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
                title = {
                    Column {
                        Text(
                            text = coll.name,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${cards.size} cartas indexadas",
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
                        Icon(Icons.Default.Delete, contentDescription = "Excluir Coleção", tint = MaterialTheme.colorScheme.error)
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = coll.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Local Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                placeholder = { Text("Procurar cartas nesta coleção...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpar busca",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${filteredCards.size} cartas",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Sort Dropdown Button
                Box {
                    Surface(
                        onClick = { sortMenuExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Ordenar: ${selectedSortOption.label}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Selecionar Ordenação",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
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
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
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
                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(56.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Carta",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(42.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Nome",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.2f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Coleção / Cód.",
                            style = MaterialTheme.typography.labelSmall,
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
            title = { Text("Editar Informações", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Título da Coleção") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Descrição (Opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                    },
                    shape = RoundedCornerShape(10.dp)
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
            title = { Text("Excluir Coleção", fontWeight = FontWeight.Bold) },
            text = { Text("Tem certeza que deseja excluir esta coleção? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCollection(coll.id)
                        showDeleteConfirmDialog = false
                        navController.popBackStack()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
        shape = RoundedCornerShape(12.dp),
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
            // Position number as "#1" in bold purple
            Text(
                text = "#${item.scanOrder}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
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
                .padding(horizontal = 4.dp)
                .shadow(20.dp, RoundedCornerShape(22.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Coleção: ${formatCardSetAndCode(card)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(0.71f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fechar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
