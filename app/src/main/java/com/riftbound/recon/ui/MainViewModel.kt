package com.riftbound.recon.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riftbound.recon.data.local.CollectionOccurrence
import com.riftbound.recon.data.local.SearchCardResult
import com.riftbound.recon.domain.model.Card
import com.riftbound.recon.domain.model.Collection
import com.riftbound.recon.domain.model.CollectionCard
import com.riftbound.recon.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: CardRepository
) : ViewModel() {

    // --- COMPENDIUM STATE ---
    private val _compendiumSearchQuery = MutableStateFlow("")
    val compendiumSearchQuery = _compendiumSearchQuery.asStateFlow()

    private val _selectedSetFilter = MutableStateFlow("All")
    val selectedSetFilter = _selectedSetFilter.asStateFlow()

    val allCards: StateFlow<List<Card>> = repository.getAllCards()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val compendiumCards: StateFlow<List<Card>> = combine(
        allCards,
        _compendiumSearchQuery,
        _selectedSetFilter
    ) { cards, query, filter ->
        cards.filter { card ->
            val matchesQuery = card.name.contains(query, ignoreCase = true) || 
                               card.tags.any { it.contains(query, ignoreCase = true) }
            val matchesFilter = filter == "All" || card.set.equals(filter, ignoreCase = true)
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Selected Card Details for Compendium Modal
    private val _selectedCard = MutableStateFlow<Card?>(null)
    val selectedCard = _selectedCard.asStateFlow()

    private val _selectedCardOccurrences = MutableStateFlow<List<CollectionOccurrence>>(emptyList())
    val selectedCardOccurrences = _selectedCardOccurrences.asStateFlow()

    private val _selectedCardTotalCount = MutableStateFlow(0)
    val selectedCardTotalCount = _selectedCardTotalCount.asStateFlow()

    // Map to quickly find how many copies of each card are registered across all collections
    // This allows displaying the count badge directly on each card item in the grid
    private val _cardOwnershipCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val cardOwnershipCounts = _cardOwnershipCounts.asStateFlow()

    init {
        // Collect ownership counts for all cards
        viewModelScope.launch {
            repository.getAllCards().collect { cards ->
                combine(
                    cards.map { card ->
                        repository.getCardCountInCollections(card.id).map { count -> card.id to count }
                    }
                ) { pairs ->
                    pairs.toMap()
                }.collect { map ->
                    _cardOwnershipCounts.value = map
                }
            }
        }
    }

    fun selectCard(card: Card?) {
        _selectedCard.value = card
        if (card != null) {
            viewModelScope.launch {
                repository.getCollectionsWithCard(card.id).collect { occurrences ->
                    _selectedCardOccurrences.value = occurrences
                }
            }
            viewModelScope.launch {
                repository.getCardCountInCollections(card.id).collect { count ->
                    _selectedCardTotalCount.value = count
                }
            }
        } else {
            _selectedCardOccurrences.value = emptyList()
            _selectedCardTotalCount.value = 0
        }
    }

    // --- COLLECTIONS STATE ---
    val collections: StateFlow<List<Collection>> = repository.getAllCollections()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedCollection = MutableStateFlow<Collection?>(null)
    val selectedCollection = _selectedCollection.asStateFlow()

    private val _selectedCollectionCards = MutableStateFlow<List<CollectionCard>>(emptyList())
    val selectedCollectionCards = _selectedCollectionCards.asStateFlow()

    fun loadCollection(collectionId: Long) {
        viewModelScope.launch {
            val coll = collections.value.find { it.id == collectionId }
            _selectedCollection.value = coll
            repository.getCardsInCollection(collectionId).collect { list ->
                _selectedCollectionCards.value = list
            }
        }
    }

    fun updateCollectionDetails(collectionId: Long, name: String, description: String) {
        viewModelScope.launch {
            repository.updateCollection(collectionId, name, description)
            loadCollection(collectionId)
        }
    }

    fun deleteCollection(collectionId: Long) {
        viewModelScope.launch {
            repository.deleteCollection(collectionId)
            _selectedCollection.value = null
            _selectedCollectionCards.value = emptyList()
        }
    }

    fun removeCardFromCollection(collectionCardId: Long) {
        viewModelScope.launch {
            repository.removeCardFromCollection(collectionCardId)
        }
    }

    fun addCardToLoadedCollection(cardId: Int) {
        val coll = _selectedCollection.value ?: return
        viewModelScope.launch {
            repository.addCardToCollection(coll.id, cardId)
        }
    }

    // --- SCANNER STATE ---
    private val _scannedCards = MutableStateFlow<List<Card>>(emptyList())
    val scannedCards = _scannedCards.asStateFlow()

    private val _lastDetectedCard = MutableStateFlow<Card?>(null)
    val lastDetectedCard = _lastDetectedCard.asStateFlow()

    private val _isScanningPaused = MutableStateFlow(true) // Start paused until explicit user action
    val isScanningPaused = _isScanningPaused.asStateFlow()

    private val _scannerLogs = MutableStateFlow<List<String>>(emptyList())
    val scannerLogs = _scannerLogs.asStateFlow()

    private val _scanTrigger = MutableStateFlow(false)
    val scanTrigger = _scanTrigger.asStateFlow()

    private var lastScannedTime = 0L
    private var lastScannedCardId = -1

    fun startScanning() {
        _isScanningPaused.value = false
        _lastDetectedCard.value = null
        clearLogs()
    }

    fun pauseScanning() {
        _isScanningPaused.value = true
    }

    fun resumeScanning() {
        _isScanningPaused.value = false
    }

    fun triggerCapture() {
        _scanTrigger.value = true
    }

    fun clearLogs() {
        _scannerLogs.value = emptyList()
    }

    fun processOcrLines(ocrLines: List<com.riftbound.recon.data.scanner.OcrLine>) {
        _scanTrigger.value = false // Reset trigger immediately
        
        viewModelScope.launch {
            val logs = mutableListOf<String>()
            val now = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
            logs.add("[$now] --- INICIANDO ANÁLISE DE CAPTURA ---")
            logs.add("[$now] Linhas detectadas pela camera: ${ocrLines.size}")
            
            if (ocrLines.isEmpty()) {
                logs.add("[$now] FALHA: Nenhum texto identificado no enquadramento.")
                logs.add("[$now] --- FIM DA ANÁLISE ---")
                _scannerLogs.value = _scannerLogs.value + logs
                return@launch
            }
            
            // Log all read OCR lines
            for (line in ocrLines) {
                logs.add("  - \"${line.text}\" (L:${line.left}, T:${line.top}, R:${line.right}, B:${line.bottom})")
            }
            
            // Check rodapé set code match
            val codeRegex = Regex("""\b(OGN|SFD|UNL|OGS|OPP|JDG|PR|VEN)\b[^\d]*?\b([a-z]{0,2}[0-9]{1,4}[a-z]?)\b""", RegexOption.IGNORE_CASE)
            var foundSetCode = false
            for (line in ocrLines) {
                val match = codeRegex.find(line.text)
                if (match != null) {
                    val setCode = match.groupValues[1].uppercase()
                    val collectorNumStr = match.groupValues[2].lowercase()
                    logs.add("[$now] Cód. Rodapé identificado: Set $setCode, Num #$collectorNumStr")
                    foundSetCode = true
                }
            }
            if (!foundSetCode) {
                logs.add("[$now] Nenhum código de rodapé encontrado por Regex.")
            }
            
            // Analyze dimensions
            val minTop = ocrLines.minOf { it.top }
            val maxBottom = ocrLines.maxOf { it.bottom }
            val minLeft = ocrLines.minOf { it.left }
            val maxRight = ocrLines.maxOf { it.right }
            val totalHeight = maxBottom - minTop
            val totalWidth = maxRight - minLeft
            val topLimit = minTop + (totalHeight * 0.35).toInt()
            val leftLimit = minLeft + (totalWidth * 0.40).toInt()
            
            logs.add("[$now] Dimensões da carta lida: ${totalWidth}x${totalHeight}px")
            
            // Try to find energy cost candidate
            var detectedEnergyCost: Int? = null
            val numberRegex = Regex("""\b([1-9]|10)\b""")
            for (line in ocrLines) {
                if (line.top <= topLimit && line.left <= leftLimit) {
                    val match = numberRegex.find(line.text.trim())
                    if (match != null) {
                        detectedEnergyCost = match.groupValues[1].toIntOrNull()
                        logs.add("[$now] Energia Runa lida no canto sup. esq.: $detectedEnergyCost (Texto: \"${line.text}\")")
                        break
                    }
                }
            }
            
            if (detectedEnergyCost == null) {
                logs.add("[$now] Nenhuma Runa de energia lida no canto sup. esq.")
            }
            
            // Query DB cards and match
            val cardsList = repository.getAllCards().first()
            val matchedCard = com.riftbound.recon.domain.scanner.CardScannerMatcher.matchCard(ocrLines, cardsList)
            
            if (matchedCard != null) {
                logs.add("[$now] SUCESSO! Carta correspondida: \"${matchedCard.name}\"")
                logs.add("  - Custo de Energia: ${matchedCard.energyCost} (Lido: ${detectedEnergyCost ?: "N/A"})")
                logs.add("  - Código Set: ${matchedCard.setCode} #${matchedCard.collectorNumber}")
                
                // Save match
                _lastDetectedCard.value = matchedCard
                _scannedCards.value = _scannedCards.value + matchedCard
                lastScannedCardId = matchedCard.id
                lastScannedTime = System.currentTimeMillis()
            } else {
                logs.add("[$now] FALHA! Nenhuma carta encontrada com as regras do matcher.")
            }
            
            logs.add("[$now] --- FIM DA ANÁLISE ---")
            _scannerLogs.value = _scannerLogs.value + logs
        }
    }

    fun onCardDetected(card: Card) {
        if (_isScanningPaused.value) return
        
        // Write simulation log
        val now = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        _scannerLogs.value = _scannerLogs.value + listOf(
            "[$now] [SIMULADOR] Carta inserida manualmente: \"${card.name}\" (ID: ${card.id})",
            "  - Set: ${card.set}, Energia: ${card.energyCost}, Might: ${card.power}"
        )
        
        _lastDetectedCard.value = card
        _scannedCards.value = _scannedCards.value + card
        lastScannedCardId = card.id
        lastScannedTime = System.currentTimeMillis()
    }

    fun undoLastScan() {
        val currentList = _scannedCards.value
        if (currentList.isNotEmpty()) {
            _scannedCards.value = currentList.dropLast(1)
        }
        _lastDetectedCard.value = null
        lastScannedCardId = -1
        
        val now = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        _scannerLogs.value = _scannerLogs.value + "[$now] Desfazer: Ultima carta escaneada removida."
    }

    fun removeScannedCardAt(index: Int) {
        val currentList = _scannedCards.value.toMutableList()
        if (index in currentList.indices) {
            val removed = currentList.removeAt(index)
            _scannedCards.value = currentList
            
            val now = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
            _scannerLogs.value = _scannerLogs.value + "[$now] Removida da lista: \"${removed.name}\""
        }
    }

    fun clearScanningSession() {
        _scannedCards.value = emptyList()
        _lastDetectedCard.value = null
        _isScanningPaused.value = true
        lastScannedCardId = -1
        clearLogs()
    }

    fun saveScanningSession(name: String, description: String) {
        viewModelScope.launch {
            if (_scannedCards.value.isNotEmpty()) {
                repository.createCollection(name, description, _scannedCards.value)
                clearScanningSession()
            }
        }
    }

    // --- SEARCH STATE ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<SearchCardResult>> = _searchQuery
        .flatMapLatest { query ->
            if (query.length >= 2) {
                repository.searchCardInCollections(query)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun removeCardFromCollectionInSearch(collectionCardId: Long) {
        viewModelScope.launch {
            repository.removeCardFromCollection(collectionCardId)
        }
    }

    // Compendium state helpers
    fun updateCompendiumQuery(query: String) {
        _compendiumSearchQuery.value = query
    }

    fun updateSetFilter(filter: String) {
        _selectedSetFilter.value = filter
    }
}
