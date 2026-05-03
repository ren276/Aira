package com.aira.health.presentation.assistant

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aira.health.presentation.theme.Theme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiraAssistantScreen(
    modifier: Modifier = Modifier,
    viewModel: AiraAssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch { scrollState.animateScrollToItem(uiState.messages.size - 1) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Theme.colors.dominant,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // No back button — this is a bottom-nav tab
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Theme.colors.primaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Aira",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                    focusManager.clearFocus()
                },
                isStreaming = uiState.isStreaming
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    ChatMessageBubble(message)
                }

                // Show Thinking... only while streaming and last msg is from user
                if (uiState.isStreaming && uiState.messages.lastOrNull()?.role == MessageRole.USER) {
                    item {
                        ThinkingIndicator()
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Message bubble with basic markdown rendering
// ---------------------------------------------------------------------------

@Composable
fun ChatMessageBubble(message: AssistantMessage) {
    val isUser = message.role == MessageRole.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                )
                .background(
                    if (isUser) {
                        Brush.linearGradient(
                            colors = listOf(Theme.colors.primaryContainer, Theme.colors.secondaryColor)
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(Theme.colors.surfaceContainerLow, Theme.colors.surfaceContainer)
                        )
                    }
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = parseMarkdown(message.text),
                color = if (isUser) Color.Black else Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
            )
        }
    }
}

/**
 * Converts a limited subset of markdown to AnnotatedString:
 *  - **bold** → bold span
 *  - * item or - item at line start → bullet point
 *  - Strips single asterisks used as stray formatting
 */
@Composable
private fun parseMarkdown(raw: String) = buildAnnotatedString {
    val lines = raw.split("\n")
    lines.forEachIndexed { lineIndex, line ->
        val trimmed = line.trimStart()

        // Bullet point lines: starts with "* " or "- "
        val isBullet = trimmed.startsWith("* ") || trimmed.startsWith("- ")
        val lineContent = if (isBullet) "• ${trimmed.drop(2)}" else line

        // Parse inline **bold**
        val boldRegex = Regex("""\*\*(.+?)\*\*""")
        var cursor = 0
        val matches = boldRegex.findAll(lineContent)
        for (match in matches) {
            // append normal text before the bold part
            if (match.range.first > cursor) {
                append(lineContent.substring(cursor, match.range.first))
            }
            // append bold
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            cursor = match.range.last + 1
        }
        // append remaining text after last bold match
        if (cursor < lineContent.length) {
            append(lineContent.substring(cursor))
        }

        if (lineIndex < lines.lastIndex) append("\n")
    }
}

// ---------------------------------------------------------------------------
// Input bar
// ---------------------------------------------------------------------------

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isStreaming: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Surface(
            color = Theme.colors.dominant,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(Theme.colors.surfaceContainer),
                    placeholder = { Text("Ask Aira...", color = Theme.colors.onSurfaceVariant) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Theme.colors.primaryContainer,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    enabled = !isStreaming,
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank() && !isStreaming,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (text.isNotBlank() && !isStreaming) Theme.colors.primaryContainer
                            else Theme.colors.surfaceContainer
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (text.isNotBlank() && !isStreaming) Color.Black else Theme.colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(140.dp))
    }
}

// ---------------------------------------------------------------------------
// Thinking indicator — replaces the old dot animation
// ---------------------------------------------------------------------------

@Composable
fun ThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinking alpha"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Theme.colors.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Theme.colors.primaryContainer.copy(alpha = alpha))
        )
        Text(
            text = "Thinking...",
            color = Theme.colors.primaryContainer.copy(alpha = alpha),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        )
    }
}
