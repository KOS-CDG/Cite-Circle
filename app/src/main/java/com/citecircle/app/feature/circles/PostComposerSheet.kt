package com.citecircle.app.feature.circles

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.Circle
import com.citecircle.app.core.model.Post
import com.citecircle.app.core.model.PostFlair
import com.citecircle.app.core.model.PostType
import com.citecircle.app.core.data.PostRepository
import com.citecircle.app.core.data.UserRepository
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// ComposerViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class ComposerViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    fun createCirclePost(circle: Circle, content: String, flair: PostFlair, pdfName: String?, onComplete: () -> Unit) {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { currentUser ->
                val newPost = Post(
                    id = "post_${System.currentTimeMillis()}",
                    author = currentUser,
                    content = content,
                    type = PostType.DISCUSSION,
                    flair = flair,
                    circleId = circle.id,
                    circleName = circle.name,
                    timestamp = System.currentTimeMillis()
                )
                postRepository.createPost(newPost)
                onComplete()
                return@collect
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// PostComposerSheet Composable
// ──────────────────────────────────────────────────────────────────────────────

val flairOptions = listOf(
    PostFlair.DISCUSSION,
    PostFlair.QUESTION,
    PostFlair.PAPER_FEEDBACK,
    PostFlair.RESOURCE
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostComposerSheet(
    circle: Circle,
    onDismiss: () -> Unit,
    onPostCreated: () -> Unit,
    viewModel: ComposerViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var content by remember { mutableStateOf("") }
    var selectedFlair by remember { mutableStateOf(PostFlair.DISCUSSION) }
    var selectedPdfName by remember { mutableStateOf<String?>(null) }
    var selectedPdfSize by remember { mutableStateOf<String?>(null) }

    // Real device file picker with 10 MB limit and ZIP rejection validation
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val projection = arrayOf(
                android.provider.OpenableColumns.DISPLAY_NAME,
                android.provider.OpenableColumns.SIZE
            )
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            var displayName = uri.lastPathSegment ?: "document.pdf"
            var sizeBytes: Long? = null
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIndex >= 0) displayName = c.getString(nameIndex)
                    if (sizeIndex >= 0) sizeBytes = c.getLong(sizeIndex)
                }
            }

            val lowerName = displayName.lowercase()
            val invalidExtensions = listOf(".zip", ".rar", ".7z", ".tar", ".gz", ".tgz", ".zipx")
            if (invalidExtensions.any { lowerName.endswith(it) }) {
                errorMessage = "ZIP and archive files are invalid for posting. Please upload PDF or image files."
                android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }

            val maxSizeBytes = 10 * 1024 * 1024L // 10 MB
            if (sizeBytes != null && sizeBytes > maxSizeBytes) {
                errorMessage = "File exceeds the 10 MB maximum limit for posting."
                android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }

            errorMessage = null
            val sizeKb = if (sizeBytes != null) sizeBytes / 1024L else null
            val sizeLabel = when {
                sizeKb == null  -> ""
                sizeKb < 1024   -> "${sizeKb} KB"
                else            -> "${"%.1f".format(sizeKb / 1024.0)} MB"
            }
            selectedPdfName = displayName
            selectedPdfSize = sizeLabel
        }
    }


    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Post in c/${circle.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close composer")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Flair Selector Chips
            Text(
                text = "Select post tag:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.ccColors.marginGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                flairOptions.forEach { flair ->
                    val isSelected = selectedFlair == flair
                    val label = when (flair) {
                        PostFlair.DISCUSSION -> "Discussion"
                        PostFlair.QUESTION -> "Question"
                        PostFlair.PAPER_FEEDBACK -> "Feedback"
                        PostFlair.RESOURCE -> "Resource"
                        PostFlair.NONE -> ""
                    }
                    CcChip(
                        label = label,
                        selected = isSelected,
                        onClick = { selectedFlair = flair }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Text content input
            CcTextField(
                value = content,
                onValueChange = { content = it },
                label = "Share your thoughts or question...",
                singleLine = false,
                maxLines = 10,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                placeholder = "What would you like to discuss with the community?"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Optional PDF upload stubs
            if (selectedPdfName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Description, contentDescription = "PDF file", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = selectedPdfName!!, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(text = selectedPdfSize!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ccColors.marginGray)
                    }
                    IconButton(onClick = {
                        selectedPdfName = null
                        selectedPdfSize = null
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove PDF", tint = CcColors.CoralPop)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                CcSecondaryButton(
                    text = "Attach Manuscript PDF (Optional)",
                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            CcPrimaryButton(
                text = "Publish Post",
                onClick = {
                    if (content.isNotBlank()) {
                        viewModel.createCirclePost(
                            circle = circle,
                            content = content,
                            flair = selectedFlair,
                            pdfName = selectedPdfName,
                            onComplete = onPostCreated
                        )
                    }
                },
                enabled = content.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
