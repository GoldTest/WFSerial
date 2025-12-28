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

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow

@Composable
fun App(viewModel: SerializerViewModel) {
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

                if (viewModel.showGraphDetails) {
                    GraphDetailsOverlay(viewModel)
                }
            }
        }
    }
}

@Composable
fun GraphDetailsOverlay(viewModel: SerializerViewModel) {
    val graph = viewModel.activeGraph ?: return

    AlertDialog(
        onDismissRequest = { viewModel.showGraphDetails = false },
        confirmButton = {
            TextButton(onClick = { viewModel.showGraphDetails = false }) {
                Text("关闭")
            }
        },
        title = {
            Column {
                Text(graph.name, style = MaterialTheme.typography.headlineSmall)
                Text("ID: ${graph.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("节点列表 (${graph.nodes.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(graph.nodes.values.toList()) { node ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (node.id == graph.startNodeId) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (node.isConclusion) "🏁 结论" else "📝 节点",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (node.isConclusion) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary
                                    )
                                    if (node.id == graph.startNodeId) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text("入口", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (node.isConclusion) node.result ?: "" else node.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!node.isConclusion) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("YES →", style = MaterialTheme.typography.labelSmall, color = Color.Green.copy(alpha = 0.7f))
                                            Text(
                                                graph.nodes[node.yesNodeId]?.description?.take(15)?.let { "$it..." } ?: "未连接",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("NO →", style = MaterialTheme.typography.labelSmall, color = Color.Red.copy(alpha = 0.7f))
                                            Text(
                                                graph.nodes[node.noNodeId]?.description?.take(15)?.let { "$it..." } ?: "未连接",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
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
            IconButton(onClick = { viewModel.showGraphDetails = true }) {
                    Icon(Icons.Default.Info, "图详情")
                }
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
    val activeGraph = viewModel.activeGraph

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = (-20).dp),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic background stack effect
        if (node != null) {
            // Third layer
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .height(560.dp)
                    .offset(y = 32.dp)
                    .scale(0.85f)
                    .graphicsLayer { alpha = 0.3f }
                    .clip(RoundedCornerShape(40.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
            // Second layer
            Box(
                modifier = Modifier
                    .width(380.dp)
                    .height(600.dp)
                    .offset(y = 16.dp)
                    .scale(0.92f)
                    .graphicsLayer { alpha = 0.6f }
                    .clip(RoundedCornerShape(40.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
        }

        AnimatedContent(
            targetState = node ?: conclusion,
            transitionSpec = {
                (fadeIn(animationSpec = tween(600)) + 
                 scaleIn(initialScale = 0.85f, animationSpec = tween(600, easing = BackOutEasing)) +
                 slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(600)))
                .togetherWith(
                    fadeOut(animationSpec = tween(400)) + 
                    scaleOut(targetScale = 1.05f, animationSpec = tween(400))
                )
            }
        ) { state ->
            when (state) {
                is Node -> ActualCard(state, viewModel)
                is String -> ConclusionCard(state, viewModel)
            }
        }
    }
}

// BackOutEasing is similar to Overshoot
private val BackOutEasing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)

@Composable
fun ActualCard(node: Node, viewModel: SerializerViewModel) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
    )
    
    val rotation = animatedOffsetX / 25f
    val swipeProgress = (offsetX / 200f).coerceIn(-1f, 1f)
    
    val borderColor = when {
        swipeProgress > 0.1f -> Color.Green.copy(alpha = swipeProgress)
        swipeProgress < -0.1f -> Color.Red.copy(alpha = -swipeProgress)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    }

    Card(
        modifier = Modifier
            .offset { IntOffset(animatedOffsetX.roundToInt(), (kotlin.math.abs(animatedOffsetX) * 0.1f).roundToInt()) }
            .rotate(rotation)
            .width(400.dp)
            .height(640.dp)
            .pointerInput(node.id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                    },
                    onDragEnd = {
                        if (offsetX > 180) {
                            // Swipe Right -> YES
                            viewModel.onChoice(true)
                        } else if (offsetX < -180) {
                            // Swipe Left -> NO
                            viewModel.onChoice(false)
                        }
                        offsetX = 0f
                    }
                )
            },
        shape = RoundedCornerShape(40.dp),
        border = BorderStroke(2.dp, Brush.linearGradient(
            colors = listOf(borderColor, borderColor.copy(alpha = 0.3f))
        )),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    )
                )
                .drawBehind {
                    // Subtle glow effect
                    drawCircle(
                        color = borderColor.copy(alpha = 0.05f),
                        radius = size.maxDimension / 2,
                        center = center
                    )
                }
                .padding(32.dp)
        ) {
            // Question Label
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Text(
                    "DECISION",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
            }

            Text(
                text = node.description,
                style = MaterialTheme.typography.headlineMedium.copy(
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.3f), blurRadius = 8f, offset = Offset(2f, 2f))
                ),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )
            
            // Interaction Indicators
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // NO Side (Left)
                InteractionIcon(
                    icon = Icons.Default.Close,
                    label = "NO",
                    color = Color.Red,
                    progress = (-swipeProgress).coerceIn(0f, 1f),
                    isLeft = true
                )

                // YES Side (Right)
                InteractionIcon(
                    icon = Icons.Default.Check,
                    label = "YES",
                    color = Color.Green,
                    progress = swipeProgress.coerceIn(0f, 1f),
                    isLeft = false
                )
            }
        }
    }
}

@Composable
fun InteractionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, progress: Float, isLeft: Boolean) {
    val scale = 0.9f + (progress * 0.4f)
    val alpha = 0.3f + (progress * 0.7f)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .drawBehind {
                    if (progress > 0.1f) {
                        drawCircle(color = color.copy(alpha = progress * 0.25f), radius = size.minDimension * 0.9f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(48.dp))
        }
        Text(
            label,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
fun ConclusionCard(result: String, viewModel: SerializerViewModel) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .width(340.dp)
            .height(520.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Glow effect
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(glowScale)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                    )
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(Modifier.height(32.dp))
                
                Text(
                    text = "恭喜！达成结论",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 4.sp
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = result,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Spacer(Modifier.height(64.dp))
                
                Button(
                    onClick = { viewModel.resetGraph() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(12.dp))
                    Text("再次开启决策之旅", fontWeight = FontWeight.Bold)
                }
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
    var editingGraph by remember { mutableStateOf<Graph?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        editingGraph != null -> "编辑图: ${editingGraph?.name}"
                        showCreator -> "创建新图"
                        else -> "图列表"
                    }, 
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.weight(1f))
                if (showCreator || editingGraph != null) {
                    TextButton(onClick = { 
                        showCreator = false
                        editingGraph = null
                    }) { Text("返回列表") }
                }
                TextButton(onClick = { viewModel.isEditing = false }) { Text("关闭") }
            }
            
            if (showCreator || editingGraph != null) {
                GraphEditor(viewModel, initialGraph = editingGraph)
            } else {
                // Add a simple default graph for testing if empty
                if (viewModel.graphs.isEmpty()) {
                    Button(onClick = {
                        val testGraph = Graph(
                            id = "test",
                            name = "天气决策器",
                            startNodeId = "1",
                            nodes = mapOf(
                                "1" to Node("1", "今天外面在下雨吗？", yesNodeId = "2", noNodeId = "3", visualX = 200f, visualY = 50f),
                                "2" to Node("2", "你有雨伞吗？", yesNodeId = "4", noNodeId = "5", visualX = 100f, visualY = 200f),
                                "3" to Node("3", "你想出去玩吗？", yesNodeId = "6", noNodeId = "7", visualX = 300f, visualY = 200f),
                                "4" to Node("4", "你可以出门。", isConclusion = true, result = "带伞出门", visualX = 50f, visualY = 350f),
                                "5" to Node("5", "待在家里吧。", isConclusion = true, result = "留在室内", visualX = 150f, visualY = 350f),
                                "6" to Node("6", "出发吧！", isConclusion = true, result = "愉快出游", visualX = 250f, visualY = 350f),
                                "7" to Node("7", "睡个午觉。", isConclusion = true, result = "在家休息", visualX = 350f, visualY = 350f)
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
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    graph.name, 
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row {
                                    IconButton(onClick = { viewModel.selectGraph(graph); viewModel.isEditing = false }) {
                                        Icon(Icons.Default.PlayArrow, "运行", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { editingGraph = graph }) {
                                        Icon(Icons.Default.Edit, "编辑")
                                    }
                                }
                            }
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
                if (viewModel.history.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearAllHistory() }) { 
                        Text("清空全部", color = MaterialTheme.colorScheme.error) 
                    }
                }
                TextButton(onClick = { viewModel.showHistory = false }) { Text("关闭") }
            }
            
            LazyColumn {
                items(viewModel.history) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.graphName, fontWeight = FontWeight.Bold)
                                Text("结果: ${entry.result}", color = MaterialTheme.colorScheme.secondary)
                                Text("路径: ${entry.path.joinToString(" -> ")}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { viewModel.deleteHistoryEntry(entry) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除记录",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

