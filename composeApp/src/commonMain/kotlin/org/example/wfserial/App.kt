package org.example.wfserial

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import kotlinx.datetime.Clock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun App(viewModel: SerializerViewModel = remember { SerializerViewModel() }) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFBB86FC),
            secondary = Color(0xFF03DAC6),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Header(viewModel)
                    
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.activeGraph == null) {
                            EmptyState(viewModel)
                        } else {
                            SwipeCard(viewModel)
                        }
                    }
                }

                if (viewModel.isEditing) {
                    EditorOverlay(viewModel)
                }
                
                if (viewModel.showHistory) {
                    HistoryOverlay(viewModel)
                }
            }
        }
    }
}

@Composable
fun Header(viewModel: SerializerViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Serializer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = viewModel.activeGraph?.name ?: "No Active Graph",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        
        Row {
            IconButton(onClick = { viewModel.showHistory = true }) {
                Icon(Icons.Default.History, "History")
            }
            IconButton(onClick = { viewModel.isEditing = true }) {
                Icon(Icons.Default.Settings, "Settings")
            }
        }
    }
}

@Composable
fun SwipeCard(viewModel: SerializerViewModel) {
    val node = viewModel.currentNode
    val conclusion = viewModel.showConclusion

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = (-40).dp), // Visual centering: move up slightly
        contentAlignment = Alignment.Center
    ) {
        // Background Stack effect
        Box(
            modifier = Modifier
                .width(320.dp)
                .height(480.dp)
                .offset(y = 12.dp)
                .scale(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        )

        AnimatedContent(
            targetState = node ?: conclusion,
            transitionSpec = {
                (fadeIn(animationSpec = tween(500, easing = LinearOutSlowInEasing)) + 
                 scaleIn(initialScale = 0.8f, animationSpec = tween(500)) +
                 slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(500)))
                .togetherWith(
                    fadeOut(animationSpec = tween(300)) + 
                    scaleOut(targetScale = 1.1f, animationSpec = tween(300))
                )
            }
        ) { state ->
            if (state is Node) {
                ActualCard(state, viewModel)
            } else if (state is String) {
                ConclusionCard(state, viewModel)
            }
        }
    }
}

@Composable
fun ActualCard(node: Node, viewModel: SerializerViewModel) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )
    
    val rotation = animatedOffsetX / 20f
    
    Card(
        modifier = Modifier
            .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
            .rotate(rotation)
            .width(340.dp)
            .height(520.dp)
            .pointerInput(node.id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                    },
                    onDragEnd = {
                        if (offsetX < -150) {
                            // Swipe Left -> YES
                            viewModel.onChoice(true)
                        } else if (offsetX > 150) {
                            // Swipe Right -> NO
                            viewModel.onChoice(false)
                        }
                        offsetX = 0f
                    }
                )
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Text(
                text = node.description,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            
            // YES indicator (Shows when swiping left, positioned on the right)
            Text(
                text = "YES",
                color = Color.Green.copy(alpha = ((-offsetX / 150f).coerceIn(0f, 1f))),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .rotate(15f)
            )

            // NO indicator (Shows when swiping right, positioned on the left)
            Text(
                text = "NO",
                color = Color.Red.copy(alpha = ((offsetX / 150f).coerceIn(0f, 1f))),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .rotate(-15f)
            )
        }
    }
}

@Composable
fun ConclusionCard(result: String, viewModel: SerializerViewModel) {
    Card(
        modifier = Modifier
            .width(340.dp)
            .height(520.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "达成事实",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = result,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = { viewModel.resetGraph() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("重新开始")
            }
        }
    }
}

@Composable
fun EmptyState(viewModel: SerializerViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("没有激活的图", color = Color.Gray)
        Button(
            onClick = { viewModel.isEditing = true },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("创建一个")
        }
    }
}

@Composable
fun EditorOverlay(viewModel: SerializerViewModel) {
    var showCreator by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (showCreator) "创建新图" else "图列表", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.weight(1f))
                if (showCreator) {
                    TextButton(onClick = { showCreator = false }) { Text("返回列表") }
                }
                TextButton(onClick = { viewModel.isEditing = false }) { Text("关闭") }
            }
            
            if (showCreator) {
                GraphEditor(viewModel)
            } else {
                // Add a simple default graph for testing if empty
                if (viewModel.graphs.isEmpty()) {
                    Button(onClick = {
                        val testGraph = Graph(
                            id = "test",
                            name = "天气决策器",
                            startNodeId = "1",
                            nodes = mapOf(
                                "1" to Node("1", "今天外面在下雨吗？", yesNodeId = "2", noNodeId = "3"),
                                "2" to Node("2", "你有雨伞吗？", yesNodeId = "4", noNodeId = "5"),
                                "3" to Node("3", "你想出去玩吗？", yesNodeId = "6", noNodeId = "7"),
                                "4" to Node("4", "你可以出门。", isConclusion = true, result = "带伞出门"),
                                "5" to Node("5", "待在家里吧。", isConclusion = true, result = "留在室内"),
                                "6" to Node("6", "出发吧！", isConclusion = true, result = "愉快出游"),
                                "7" to Node("7", "睡个午觉。", isConclusion = true, result = "在家休息")
                            )
                        )
                        viewModel.addGraph(testGraph)
                    }) {
                        Text("添加示例图")
                    }
                }

                Button(
                    onClick = { showCreator = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("创建新的图")
                }
                
                LazyColumn {
                    items(viewModel.graphs) { graph ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            onClick = { viewModel.selectGraph(graph); viewModel.isEditing = false }
                        ) {
                            Text(graph.name, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryOverlay(viewModel: SerializerViewModel) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("历史路径", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { viewModel.showHistory = false }) { Text("关闭") }
            }
            
            LazyColumn {
                items(viewModel.history) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(entry.graphName, fontWeight = FontWeight.Bold)
                            Text("结果: ${entry.result}", color = MaterialTheme.colorScheme.secondary)
                            Text("路径: ${entry.path.joinToString(" -> ")}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

