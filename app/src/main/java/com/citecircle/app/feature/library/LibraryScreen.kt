package com.citecircle.app.feature.library

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.PaperRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.Paper
import com.citecircle.app.core.model.Shelf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ──────────────────────────────────────────────────────────────────────────────
// LibraryViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val paperRepository: PaperRepository
) : ViewModel() {

    val savedPapers: StateFlow<List<Paper>> = paperRepository.getSavedPapers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shelves: StateFlow<List<Shelf>> = paperRepository.getShelves()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun refreshLibrary() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(800)
            _isRefreshing.value = false
        }
    }

    fun createShelf(name: String, description: String) {
        viewModelScope.launch {
            paperRepository.createShelf(name, description)
        }
    }

    fun removePaperFromShelf(paperId: String, shelfId: String) {
        viewModelScope.launch {
            paperRepository.removePaperFromShelf(paperId, shelfId)
        }
    }

    fun toggleSavePaper(paperId: String) {
        viewModelScope.launch {
            paperRepository.toggleSavePaper(paperId)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// LibraryScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    onPaperClick: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val savedPapers by viewModel.savedPapers.collectAsState()
    val shelves by viewModel.shelves.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var activeShelfId by remember { mutableStateOf<String?>(null) }
    var showCreateShelfDialog by remember { mutableStateOf(false) }

    val activeShelf = shelves.find { it.id == activeShelfId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = activeShelf?.name ?: "Personal Library",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeShelfId != null) {
                            activeShelfId = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                actions = {
                    if (activeShelfId == null && selectedTab == 1) {
                        IconButton(onClick = { showCreateShelfDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "New Shelf")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (activeShelfId == null && selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showCreateShelfDialog = true },
                    containerColor = CcColors.HighlighterYellow
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New Shelf", tint = CcColors.InkNavy)
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshLibrary() },
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.ccColors.paperCream)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Search Bar (if not viewing a specific shelf)
                if (activeShelfId == null) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search title, author, tag…", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.ccColors.divider
                        ),
                        singleLine = true
                    )

                    // Tab selector
                    CcTabRow(
                        tabs = listOf("Saved Papers (${savedPapers.size})", "Shelves (${shelves.size})"),
                        selectedIndex = selectedTab,
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. Tab Content / Shelf Details
                if (activeShelf != null) {
                    ShelfDetailsContent(
                        shelf = activeShelf,
                        allPapers = savedPapers,
                        onPaperClick = onPaperClick,
                        onRemovePaper = { paperId -> viewModel.removePaperFromShelf(paperId, activeShelf.id) }
                    )
                } else {
                    when (selectedTab) {
                        0 -> SavedPapersContent(
                            papers = savedPapers,
                            searchQuery = searchQuery,
                            onPaperClick = onPaperClick,
                            onUnsave = { viewModel.toggleSavePaper(it) }
                        )
                        1 -> ShelvesContent(
                            shelves = shelves,
                            searchQuery = searchQuery,
                            onShelfClick = { activeShelfId = it }
                        )
                    }
                }
            }

            // Create Shelf Dialog
            if (showCreateShelfDialog) {
                CreateShelfDialog(
                    onDismiss = { showCreateShelfDialog = false },
                    onConfirm = { name, desc ->
                        viewModel.createShelf(name, desc)
                        showCreateShelfDialog = false
                    }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// SavedPapersContent
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun SavedPapersContent(
    papers: List<Paper>,
    searchQuery: String,
    onPaperClick: (String) -> Unit,
    onUnsave: (String) -> Unit
) {
    val filteredPapers = remember(papers, searchQuery) {
        papers.filter { paper ->
            paper.title.contains(searchQuery, ignoreCase = true) ||
                    paper.authors.any { it.name.contains(searchQuery, ignoreCase = true) } ||
                    paper.fieldTags.any { it.contains(searchQuery, ignoreCase = true) }
        }.distinctBy { it.id }
    }

    if (filteredPapers.isEmpty()) {
        CcEmptyState(
            emoji = "🔖",
            title = if (searchQuery.isNotEmpty()) "No match found" else "No saved papers",
            subtitle = if (searchQuery.isNotEmpty()) "Try refining your search keyword." else "Save papers by tapping bookmark on Paper details."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredPapers, key = { it.id }) { paper ->
                LibraryPaperCard(
                    paper = paper,
                    onPaperClick = onPaperClick,
                    onUnsave = onUnsave
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// ShelvesContent
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ShelvesContent(
    shelves: List<Shelf>,
    searchQuery: String,
    onShelfClick: (String) -> Unit
) {
    val filteredShelves = remember(shelves, searchQuery) {
        shelves.filter { shelf ->
            shelf.name.contains(searchQuery, ignoreCase = true) ||
                    shelf.description.contains(searchQuery, ignoreCase = true)
        }.distinctBy { it.id }
    }

    if (filteredShelves.isEmpty()) {
        CcEmptyState(
            emoji = "📁",
            title = if (searchQuery.isNotEmpty()) "No match found" else "No shelves yet",
            subtitle = if (searchQuery.isNotEmpty()) "Try searching a different shelf title." else "Create a shelf to group related research."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredShelves, key = { it.id }) { shelf ->
                CcCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShelfClick(shelf.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Folder visual
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(CcColors.CircleBlue, CcColors.SeafoamTeal)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Folder, contentDescription = null, tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = shelf.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (shelf.description.isNotEmpty()) {
                                Text(
                                    text = shelf.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.ccColors.marginGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Counts pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.ccColors.divider.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${shelf.paperIds.size} papers",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CcColors.InkNavy
                            )
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// ShelfDetailsContent
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ShelfDetailsContent(
    shelf: Shelf,
    allPapers: List<Paper>,
    onPaperClick: (String) -> Unit,
    onRemovePaper: (String) -> Unit
) {
    val shelfPapers = remember(shelf, allPapers) {
        allPapers.filter { shelf.paperIds.contains(it.id) }.distinctBy { it.id }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Shelf header detail banner
        if (shelf.description.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Text(
                    text = shelf.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (shelfPapers.isEmpty()) {
            CcEmptyState(
                emoji = "📄",
                title = "Empty shelf",
                subtitle = "Save some papers and append them here directly from Paper Detail screens."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(shelfPapers, key = { it.id }) { paper ->
                    LibraryPaperCard(
                        paper = paper,
                        onPaperClick = onPaperClick,
                        onUnsave = onRemovePaper,
                        unsaveIcon = Icons.Outlined.FolderOff,
                        unsaveDescription = "Remove from shelf"
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// LibraryPaperCard
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun LibraryPaperCard(
    paper: Paper,
    onPaperClick: (String) -> Unit,
    onUnsave: (String) -> Unit,
    unsaveIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.BookmarkRemove,
    unsaveDescription: String = "Remove bookmark"
) {
    CcCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPaperClick(paper.id) }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = paper.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "By ${paper.authors.joinToString { it.name }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.ccColors.marginGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${paper.journal} • ${paper.year}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.ccColors.marginGray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { onUnsave(paper.id) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = unsaveIcon,
                    contentDescription = unsaveDescription,
                    tint = MaterialTheme.ccColors.marginGray
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CreateShelfDialog
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CreateShelfDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Research Shelf", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Shelf Name") },
                    placeholder = { Text("e.g. HCI & AI") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Interaction models for agents") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), description.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
