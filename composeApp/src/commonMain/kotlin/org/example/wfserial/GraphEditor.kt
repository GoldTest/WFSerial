package org.example.wfserial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlin.math.roundToInt

@Composable
fun GraphEditor(viewModel: SerializerViewModel) {
    var graphName by remember { mutableStateOf("") }
    var nodes by remember { mutableStateOf(mapOf<String, Node>()) }
    var startNodeId by remember { mutableStateOf("") }
    
    var showNodeDialog by remember { mutableStateOf(false) }
    var editingNode by remember { mutableStateOf<Node?>(null) }
    var isVisualMode by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = graphName,
                onValueChange = { graphName = it },
                label = { Text("图名称") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { isVisualMode = !isVisualMode }) {
                Icon(if (isVisualMode) Icons.Default.List else Icons.Default.AccountTree, contentDescription = "切换视图")
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isVisualMode) {
                VisualEditor(
                    nodes = nodes,
                    onNodesChange = { nodes = it },
                    onEditNode = { node ->
                        editingNode = node
                        showNodeDialog = true
                    }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    ListEditor(
                        nodes = nodes,
                        onNodesChange = { nodes = it },
                        onEditNode = { node ->
                            editingNode = node
                            showNodeDialog = true
                        }
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (graphName.isNotBlank() && nodes.isNotEmpty()) {
                    val newGraph = Graph(
                        id = Clock.System.now().toEpochMilliseconds().toString(),
                        name = graphName,
                        nodes = nodes,
                        startNodeId = startNodeId.ifBlank { nodes.keys.first() }
                    )
                    viewModel.addGraph(newGraph)
                    viewModel.isEditing = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = graphName.isNotBlank() && nodes.isNotEmpty()
        ) {
            Text("保存并发布图")
        }
    }

    if (showNodeDialog && editingNode != null) {
        NodeDialog(
            node = editingNode!!,
            allNodes = nodes.values.toList(),
            onDismiss = { showNodeDialog = false },
            onSave = { newNode ->
                nodes = nodes + (newNode.id to newNode)
                showNodeDialog = false
            }
        )
    }
}

@Composable
fun VisualEditor(
    nodes: Map<String, Node>,
    onNodesChange: (Map<String, Node>) -> Unit,
    onEditNode: (Node) -> Unit
) {
    var draggingConnectionFrom by remember { mutableStateOf<Pair<Node, Boolean>?>(null) } // Node and isYes
    var dragEndOffset by remember { mutableStateOf(Offset.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val nodeWidthPx = with(density) { 160.dp.toPx() }
    val nodeHeightPx = with(density) { 100.dp.toPx() }

    // 使用 rememberUpdatedState 确保回调和数据始终是最新的，避免闭包捕获旧状态
    val currentNodes by rememberUpdatedState(nodes)
    val currentOnNodesChange by rememberUpdatedState(onNodesChange)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        // Draw Connections
        Canvas(modifier = Modifier.fillMaxSize()) {
            currentNodes.values.forEach { node ->
                // Draw YES connection (Green)
                node.yesNodeId?.let { targetId ->
                    currentNodes[targetId]?.let { target ->
                        val start = Offset(node.visualX + nodeWidthPx * 0.25f, node.visualY + nodeHeightPx * 0.8f)
                        val end = Offset(target.visualX + nodeWidthPx * 0.5f, target.visualY)
                        drawConnection(start, end, Color.Green.copy(alpha = 0.5f))
                    }
                }
                
                // Draw NO connection (Red)
                node.noNodeId?.let { targetId ->
                    currentNodes[targetId]?.let { target ->
                        val start = Offset(node.visualX + nodeWidthPx * 0.75f, node.visualY + nodeHeightPx * 0.8f)
                        val end = Offset(target.visualX + nodeWidthPx * 0.5f, target.visualY)
                        drawConnection(start, end, Color.Red.copy(alpha = 0.5f))
                    }
                }
            }

            // Draw current dragging line
            draggingConnectionFrom?.let { (sourceNode, isYes) ->
                val start = if (isYes) 
                    Offset(sourceNode.visualX + nodeWidthPx * 0.25f, sourceNode.visualY + nodeHeightPx * 0.8f)
                else 
                    Offset(sourceNode.visualX + nodeWidthPx * 0.75f, sourceNode.visualY + nodeHeightPx * 0.8f)
                drawConnection(start, dragEndOffset, if (isYes) Color.Green else Color.Red)
            }
        }

        // Draw Nodes
        for (node in currentNodes.values) {
            key(node.id) {
                NodeVisual(
                    node = node,
                    onMove = { newX, newY ->
                        currentOnNodesChange(currentNodes + (node.id to node.copy(visualX = newX, visualY = newY)))
                    },
                    onEdit = { onEditNode(node) },
                    onDelete = { currentOnNodesChange(currentNodes - node.id) },
                    onStartConnect = { isYes, startOffset ->
                        draggingConnectionFrom = node to isYes
                        dragEndOffset = startOffset
                    },
                    onDraggingConnect = { offset ->
                        dragEndOffset = offset
                    },
                    onEndConnect = { _ ->
                        // Check if dropped on another node
                        val targetNode = currentNodes.values.find { target ->
                            dragEndOffset.x >= target.visualX && dragEndOffset.x <= target.visualX + nodeWidthPx &&
                            dragEndOffset.y >= target.visualY && dragEndOffset.y <= target.visualY + nodeHeightPx &&
                            target.id != draggingConnectionFrom?.first?.id
                        }
                        
                        if (targetNode != null && draggingConnectionFrom != null) {
                            val (source, isYes) = draggingConnectionFrom!!
                            val updatedNode = if (isYes) {
                                source.copy(yesNodeId = targetNode.id, isConclusion = false)
                            } else {
                                source.copy(noNodeId = targetNode.id, isConclusion = false)
                            }
                            currentOnNodesChange(currentNodes + (source.id to updatedNode))
                        }
                        draggingConnectionFrom = null
                    }
                )
            }
        }

        // Add Node Button
        FloatingActionButton(
            onClick = {
                // 使用 4 位 36 进制字符串作为 ID，既短又足够唯一
                val newId = Clock.System.now().toEpochMilliseconds().toString(36).takeLast(4).uppercase()
                // 根据节点数量偏移，避免重叠
                val offset = (currentNodes.size * 40).toFloat() % 400
                onEditNode(Node(id = newId, description = "新节点", visualX = 100f + offset, visualY = 100f + offset))
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, "添加节点")
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConnection(start: Offset, end: Offset, color: Color) {
    val path = Path().apply {
        moveTo(start.x, start.y)
        val controlY = (start.y + end.y) / 2
        cubicTo(
            start.x, controlY,
            end.x, controlY,
            end.x, end.y
        )
    }
    drawPath(path, color, style = Stroke(width = 3f))
}

@Composable
fun NodeVisual(
    node: Node,
    onMove: (Float, Float) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartConnect: (Boolean, Offset) -> Unit,
    onDraggingConnect: (Offset) -> Unit,
    onEndConnect: (Offset) -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val nodeWidthPx = with(density) { 160.dp.toPx() }
    val nodeHeightPx = with(density) { 100.dp.toPx() }

    // 使用 rememberUpdatedState 确保闭包始终访问最新的数据和回调
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnStartConnect by rememberUpdatedState(onStartConnect)
    val currentOnDraggingConnect by rememberUpdatedState(onDraggingConnect)
    val currentOnEndConnect by rememberUpdatedState(onEndConnect)

    // 内部记录当前的坐标，减少外部状态更新的频率感知（如果需要极致流畅可以加，目前先优化更新逻辑）
    var offsetX by remember(node.id) { mutableStateOf(node.visualX) }
    var offsetY by remember(node.id) { mutableStateOf(node.visualY) }

    // 同步外部位置变化（比如撤销或其他逻辑导致的移动）
    LaunchedEffect(node.visualX, node.visualY) {
        offsetX = node.visualX
        offsetY = node.visualY
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(160.dp)
            .height(100.dp)
            .pointerInput(node.id) { 
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        currentOnMove(offsetX, offsetY)
                    }
                )
            }
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = if (node.isConclusion) 
                    MaterialTheme.colorScheme.tertiaryContainer 
                else MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ID: ${node.id}", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                    }
                }
                Text(
                    text = if (node.isConclusion) "🏁 ${node.result}" else node.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp
                )
            }
        }

        if (!node.isConclusion) {
            // Connection Ports
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // YES Port
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Green.copy(alpha = 0.7f))
                        .pointerInput(node.id) {
                            detectDragGestures(
                                onDragStart = { _ ->
                                    currentOnStartConnect(true, Offset(offsetX + nodeWidthPx * 0.25f, offsetY + nodeHeightPx * 0.8f))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val currentGlobal = Offset(offsetX + nodeWidthPx * 0.25f + change.position.x, offsetY + nodeHeightPx * 0.8f + change.position.y)
                                    currentOnDraggingConnect(currentGlobal)
                                },
                                onDragEnd = { currentOnEndConnect(Offset.Zero) },
                                onDragCancel = { currentOnEndConnect(Offset.Zero) }
                            )
                        }
                ) {
                    Text("Y", modifier = Modifier.align(Alignment.Center), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // NO Port
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.7f))
                        .pointerInput(node.id) {
                            detectDragGestures(
                                onDragStart = { _ ->
                                    currentOnStartConnect(false, Offset(offsetX + nodeWidthPx * 0.75f, offsetY + nodeHeightPx * 0.8f))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val currentGlobal = Offset(offsetX + nodeWidthPx * 0.75f + change.position.x, offsetY + nodeHeightPx * 0.8f + change.position.y)
                                    currentOnDraggingConnect(currentGlobal)
                                },
                                onDragEnd = { currentOnEndConnect(Offset.Zero) },
                                onDragCancel = { currentOnEndConnect(Offset.Zero) }
                            )
                        }
                ) {
                    Text("N", modifier = Modifier.align(Alignment.Center), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
fun ColumnScope.ListEditor(
    nodes: Map<String, Node>,
    onNodesChange: (Map<String, Node>) -> Unit,
    onEditNode: (Node) -> Unit
) {
    Button(onClick = { 
        val newId = Clock.System.now().toEpochMilliseconds().toString(36).takeLast(4).uppercase()
        onEditNode(Node(id = newId, description = "新节点"))
    }) {
        Icon(Icons.Default.Add, null)
        Text("添加节点")
    }
    
    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
        items(nodes.values.toList()) { node ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(node.description, fontWeight = FontWeight.Bold)
                        Text("ID: ${node.id} | ${if(node.isConclusion) "结论: ${node.result}" else "Yes: ${node.yesNodeId}, No: ${node.noNodeId}"}")
                    }
                    IconButton(onClick = { onEditNode(node) }) {
                        Icon(Icons.Default.Edit, null)
                    }
                    IconButton(onClick = { onNodesChange(nodes - node.id) }) {
                        Icon(Icons.Default.Delete, null)
                    }
                }
            }
        }
    }
}

@Composable
fun NodeDialog(
    node: Node, 
    allNodes: List<Node>,
    onDismiss: () -> Unit, 
    onSave: (Node) -> Unit
) {
    var description by remember { mutableStateOf(node.description) }
    var isConclusion by remember { mutableStateOf(node.isConclusion) }
    var result by remember { mutableStateOf(node.result ?: "") }
    var yesId by remember { mutableStateOf(node.yesNodeId ?: "") }
    var noId by remember { mutableStateOf(node.noNodeId ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑节点属性") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description, 
                    onValueChange = { description = it }, 
                    label = { Text("描述/问题内容") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isConclusion, onCheckedChange = { isConclusion = it })
                    Spacer(Modifier.width(8.dp))
                    Text("设为最终结论节点")
                }
                
                if (isConclusion) {
                    OutlinedTextField(
                        value = result, 
                        onValueChange = { result = it }, 
                        label = { Text("结论文本") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = yesId, 
                        onValueChange = { yesId = it }, 
                        label = { Text("YES 连接到的节点 ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noId, 
                        onValueChange = { noId = it }, 
                        label = { Text("NO 连接到的节点 ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        "可用 ID: ${allNodes.filter { it.id != node.id }.joinToString { it.id }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(node.copy(
                    description = description,
                    isConclusion = isConclusion,
                    result = if (isConclusion) result else null,
                    yesNodeId = if (!isConclusion) yesId else null,
                    noNodeId = if (!isConclusion) noId else null
                ))
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}


