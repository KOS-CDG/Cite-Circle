package com.citecircle.app.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.SearchRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// SearchViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow(SearchResults())
    val searchResults = _searchResults.asStateFlow()

    val recentSearches = searchRepository.getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            _searchResults.value = searchRepository.search(query)
        }
    }

    fun addRecentSearch(query: String) {
        viewModelScope.launch {
            searchRepository.addRecentSearch(query)
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            searchRepository.removeRecentSearch(query)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// SearchScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onPaperClick: (String) -> Unit,
    onCircleClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    CcSearchBar(
                        query = query,
                        onQueryChange = { viewModel.updateQuery(it) }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.ccColors.paperCream)
                .padding(paddingValues)
        ) {
            if (query.isBlank()) {
                // Recent searches chip rows
                RecentSearchesSection(
                    recentSearches = recentSearches,
                    onSearchSelect = { viewModel.updateQuery(it) },
                    onRemove = { viewModel.removeRecentSearch(it) }
                )
            } else {
                // Result tabs
                CcTabRow(
                    tabs = listOf("All", "People", "Papers", "Circles", "Posts"),
                    selectedIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    SearchResultsTabContent(
                        tabIndex = selectedTab,
                        query = query,
                        results = results,
                        onUserClick = {
                            viewModel.addRecentSearch(query)
                            onUserClick(it)
                        },
                        onPaperClick = {
                            viewModel.addRecentSearch(query)
                            onPaperClick(it)
                        },
                        onCircleClick = {
                            viewModel.addRecentSearch(query)
                            onCircleClick(it)
                        },
                        onPostClick = {
                            viewModel.addRecentSearch(query)
                            onPostClick(it)
                        }
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Helpers / Layout parts
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecentSearchesSection(
    recentSearches: List<String>,
    onSearchSelect: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = "Recent Searches",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (recentSearches.isEmpty()) {
            Text(
                text = "No recent searches found.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.ccColors.marginGray
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                recentSearches.forEach { search ->
                    InputChip(
                        selected = false,
                        onClick = { onSearchSelect(search) },
                        label = { Text(search) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove recent search",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onRemove(search) }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultsTabContent(
    tabIndex: Int,
    query: String,
    results: SearchResults,
    onUserClick: (String) -> Unit,
    onPaperClick: (String) -> Unit,
    onCircleClick: (String) -> Unit,
    onPostClick: (String) -> Unit
) {
    val isEmpty = when (tabIndex) {
        0 -> results.people.isEmpty() && results.papers.isEmpty() && results.circles.isEmpty() && results.posts.isEmpty()
        1 -> results.people.isEmpty()
        2 -> results.papers.isEmpty()
        3 -> results.circles.isEmpty()
        4 -> results.posts.isEmpty()
        else -> true
    }

    if (isEmpty) {
        CcEmptyState(
            emoji = "🔍",
            title = "No results found",
            subtitle = "We couldn't find any match for \"$query\". Check spelling or explore other tabs."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            when (tabIndex) {
                0 -> {
                    // All combined
                    if (results.people.isNotEmpty()) {
                        item { HeaderLabel(text = "People") }
                        items(results.people.take(3)) { user ->
                            SearchUserItem(user = user, query = query, onClick = { onUserClick(user.id) })
                        }
                    }
                    if (results.papers.isNotEmpty()) {
                        item { HeaderLabel(text = "Papers") }
                        items(results.papers.take(3)) { paper ->
                            SearchPaperItem(paper = paper, query = query, onClick = { onPaperClick(paper.id) })
                        }
                    }
                }
                1 -> {
                    items(results.people) { user ->
                        SearchUserItem(user = user, query = query, onClick = { onUserClick(user.id) })
                    }
                }
                2 -> {
                    items(results.papers) { paper ->
                        SearchPaperItem(paper = paper, query = query, onClick = { onPaperClick(paper.id) })
                    }
                }
                3 -> {
                    items(results.circles) { circle ->
                        CcCard(modifier = Modifier.fillMaxWidth().clickable { onCircleClick(circle.id) }) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = circle.iconEmoji, fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = circle.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                4 -> {
                    items(results.posts) { post ->
                        CcCard(modifier = Modifier.fillMaxWidth().clickable { onPostClick(post.id) }) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = post.author.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = post.content, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.ccColors.marginGray,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun SearchUserItem(
    user: User,
    query: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CcAvatar(user = user, size = 40.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = highlightText(fullText = user.name, query = query),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = user.institution,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.ccColors.marginGray
            )
        }
    }
}

@Composable
fun SearchPaperItem(
    paper: Paper,
    query: String,
    onClick: () -> Unit
) {
    CcCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = highlightText(fullText = paper.title, query = query),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "By ${paper.authors.firstOrNull()?.name ?: "Unknown"} • ${paper.year}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.ccColors.marginGray
            )
        }
    }
}

@Composable
fun highlightText(fullText: String, query: String): AnnotatedString {
    return buildAnnotatedString {
        val q = query.lowercase()
        val text = fullText.lowercase()
        var startIdx = 0
        while (true) {
            val idx = text.indexOf(q, startIdx)
            if (idx == -1) {
                append(fullText.substring(startIdx))
                break
            }
            append(fullText.substring(startIdx, idx))
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, background = CcColors.HighlighterYellow.copy(alpha = 0.4f))) {
                append(fullText.substring(idx, idx + query.length))
            }
            startIdx = idx + query.length
        }
    }
}
