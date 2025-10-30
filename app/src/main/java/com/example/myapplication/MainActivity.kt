package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.viewinterop.AndroidView

//Ethan Daly / M323114
// 17/10/25
// Android app for emotion analyser using ollama LLM and fastAPI
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                AppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScreen() {
    // ----- ViewModel + state -----
    val vm = remember { JournalViewModel() }
    val items by vm.items.collectAsState()

    var input by remember { mutableStateOf("") }
    var lastResponse by remember { mutableStateOf<JournalResponse?>(null) }
    var showChart by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Sort/search UI state
    var sortOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchEmotion by remember { mutableStateOf(Emotion.JOY) }

    // Trash overlay state
    var trashBounds by remember { mutableStateOf<Rect?>(null) }
    var trashArmed by remember { mutableStateOf(false) }

    // Help dialog
    var showHelp by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI Journal Companion") },
                actions = {
                    TextButton(onClick = { showHelp = true }) { Text("Help") }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Input
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Write your journal entry...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (input.isNotBlank()) {
                            scope.launch {
                                loading = true; errorText = null
                                try {
                                    val res = sendJournalEntry(input)
                                    lastResponse = res
                                    showChart = true

                                    val emotionEnum = safeEmotion(res.emotion)
                                    vm.add(
                                        JournalEntry(
                                            id = System.currentTimeMillis(),
                                            text = input,
                                            emotion = emotionEnum,
                                            advice = res.advice,
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                    input = ""
                                } catch (e: Exception) {
                                    errorText = e.message ?: "Network error"
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    }
                ) {
                    Text(if (loading) "Analyzing..." else "Analyze")
                }

                // Error
                errorText?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("Error: $it", color = MaterialTheme.colorScheme.error)
                }

                // Advice card
                lastResponse?.let { resp ->
                    Spacer(Modifier.height(12.dp))
                    AdviceCard(
                        emotion = safeEmotion(resp.emotion),
                        advice = resp.advice,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ----- Header + Sort -----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("History", style = MaterialTheme.typography.titleMedium)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // open the chart with all items
                        Button(onClick = { showChart = true }) {
                            Text("Show Chart")
                        }

                        Box {
                            Button(onClick = { sortOpen = true }) {
                                Text("Sort: ${vm.currentSort.name}")
                            }
                            DropdownMenu(
                                expanded = sortOpen,
                                onDismissRequest = { sortOpen = false }
                            ) {
                                SortMethod.values().forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m.name) },
                                        onClick = { sortOpen = false; vm.sort(m) }
                                    )
                                }
                            }
                        }
                    }
                }


                // ----- Search by emotion -----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // choose search algorithm
                    Box {
                        Button(onClick = { searchOpen = true }) {
                            Text("Search: ${vm.currentSearch.name}")
                        }
                        DropdownMenu(
                            expanded = searchOpen,
                            onDismissRequest = { searchOpen = false }
                        ) {
                            SearchMethod.values().forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.name) },
                                    onClick = { searchOpen = false; vm.setSearchMethod(m) }
                                )
                            }
                        }
                    }

                    // choose emotion + run search
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            val all = Emotion.entries
                            val i = (all.indexOf(searchEmotion) + 1) % all.size
                            searchEmotion = all[i]
                        }) { Text("Emotion: ${searchEmotion.name}") }

                        Button(onClick = { vm.searchByEmotion(searchEmotion) }) {
                            Text("Search")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Search results (if any)
                val results by vm.searchResults.collectAsState()
                if (results.isNotEmpty()) {
                    Text(
                        "Search results (${results.size})",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(results, key = { it.id }) { e ->
                            HistoryRow(entry = e, onDelete = { vm.remove(e.id) })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Sorted list + drag-to-trash
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .imePadding()
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { e ->
                        DraggableHistoryRow(
                            entry = e,
                            trashBounds = trashBounds,
                            onOverTrash = { over -> trashArmed = over },
                            onDropDelete = { vm.remove(e.id) }
                        )
                    }
                }
            }

            // ------------- TRASH OVERLAY (stays on top) -------------
            TrashZone(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .zIndex(10f)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        val size = coords.size.toSize()
                        trashBounds = Rect(
                            pos,
                            Offset(pos.x + size.width, pos.y + size.height)
                        )
                    },
                armed = trashArmed
            )
        } // end Box
    } // end Scaffold

    // ===== Dialogs outside Scaffold =====
    if (showChart) {
        val history = items // <- everything in the ViewModel
        EmotionChartDialog(
            entries = history,               // pass the full list, not a single item
            onDismiss = { showChart = false }
        )
    }

    if (showHelp) {
        HelpDialog(onDismiss = { showHelp = false })
    }
}
private fun safeEmotion(s: String): Emotion =
    runCatching { Emotion.valueOf(s.trim().uppercase()) }.getOrElse { Emotion.NEUTRAL }

/** POST to FastAPI and parse JSON */
suspend fun sendJournalEntry(entry: String): JournalResponse {
    return ApiClient.httpClient.post("http://10.0.2.2:8000/analyze") {
        headers { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) }
        setBody(JournalRequest(text = entry))
    }.body()
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        text = {
            // WebView inside Compose
            AndroidView(factory = { context ->
                android.webkit.WebView(context).apply {
                    settings.javaScriptEnabled = false
                    loadUrl("file:///android_asset/help.html")
                }
            }, modifier = Modifier.height(400.dp).fillMaxWidth())
        },
        title = { Text("Help") }
    )
}

@Composable
fun HistoryRow(entry: JournalEntry, onDelete: () -> Unit) {
    val emoji = emotionEmoji[entry.emotion] ?: "🙂"
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("$emoji  ${entry.emotion.name}", style = MaterialTheme.typography.titleMedium)
                Text(entry.text, style = MaterialTheme.typography.bodyMedium)
                Text(entry.advice, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

@Composable
fun AdviceCard(
    emotion: Emotion,
    advice: String,
    modifier: Modifier = Modifier
) {
    val emoji = emotionEmoji[emotion] ?: "🙂"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "$emoji  ${emotion.name}", style = MaterialTheme.typography.titleMedium)
            Text(text = advice, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun DraggableHistoryRow(
    entry: JournalEntry,
    trashBounds: Rect?,
    onOverTrash: (Boolean) -> Unit,
    onDropDelete: () -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var itemBounds by remember { mutableStateOf<Rect?>(null) }

    // Compute using the latest positions (no remember, no staleness)
    fun isOverTrashNow(): Boolean {
        val b = itemBounds ?: return false
        val t = trashBounds ?: return false
        val moved = b.translate(offset.x, offset.y)
        return moved.overlaps(t)
    }

    Box(
        modifier = Modifier
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                val size = coords.size.toSize()
                itemBounds = Rect(pos, Offset(pos.x + size.width, pos.y + size.height))
            }
            .pointerInput(trashBounds) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                        onOverTrash(isOverTrashNow())
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()           // prevent list scroll during drag
                        offset += dragAmount
                        onOverTrash(isOverTrashNow())
                    },
                    onDragEnd = {
                        val shouldDelete = isOverTrashNow()
                        onOverTrash(false)
                        dragging = false
                        if (shouldDelete) {
                            onDropDelete()          // REMOVE from VM immediately
                            // no snap-back; row will disappear on recomposition
                        } else {
                            offset = Offset.Zero    // snap back
                        }
                    },
                    onDragCancel = {
                        onOverTrash(false)
                        dragging = false
                        offset = Offset.Zero
                    }
                )
            }
            .graphicsLayer {
                translationX = offset.x
                translationY = offset.y
                shadowElevation = if (dragging) 12f else 1f
                scaleX = if (dragging) 1.02f else 1f
                scaleY = if (dragging) 1.02f else 1f
            }
            .zIndex(if (dragging) 5f else 0f)
    ) {
        HistoryRow(entry = entry, onDelete = onDropDelete)
    }
}

@Composable
fun TrashZone(modifier: Modifier = Modifier, armed: Boolean) {
    val scale by animateFloatAsState(targetValue = if (armed) 1.15f else 1f, label = "trashScale")
    val bg = if (armed) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (armed) MaterialTheme.colorScheme.onErrorContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(20.dp),
        color = bg,
        tonalElevation = if (armed) 6.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Trash", tint = fg)
            Text("Trash", color = fg, style = MaterialTheme.typography.labelLarge)
        }
    }
}







