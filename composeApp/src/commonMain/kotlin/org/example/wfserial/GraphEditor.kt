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
            ListEditor(
                nodes = nodes,
                onNodesChange = { nodes = it },
                onEditNode = { node ->
                    editingNode = node
                    showNodeDialog = true
                }
            )
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    // Optional: Pan the whole view
                }
            }
    ) {
        // Draw Connections
        Canvas(modifier = Modifier.fillMaxSize()) {
            nodes.values.forEach { node ->
                val start = Offset(node.visualX + 80, node.visualY + 40)
                
                // Draw YES connection (Green)
                node.yesNodeId?.let { targetId ->
                    nodes[targetId]?.let { target ->
                        val end = Offset(target.visualX + 80, target.visualY + 40)
                        drawConnection(start, end, Color.Green.copy(alpha = 0.5f))
                    }
                }
                
                // Draw NO connection (Red)
                node.noNodeId?.let { targetId ->
                    nodes[targetId]?.let { target ->
                        val end = Offset(target.visualX + 80, target.visualY + 40)
                        drawConnection(start, end, Color.Red.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // Draw Nodes
        nodes.values.forEach { node ->
            NodeVisual(
                node = node,
                onMove = { newX, newY ->
                    onNodesChange(nodes + (node.id to node.copy(visualX = newX, visualY = newY)))
                },
                onEdit = { onEditNode(node) },
                onDelete = { onNodesChange(nodes - node.id) }
            )
        }

        // Add Node Button
        FloatingActionButton(
            onClick = {
                val newId = (nodes.size + 1).toString()
                onEditNode(Node(id = newId, description = "", visualX = 50f, visualY = 50f))
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
        // Bezier curve for smoother lines
        cubicTo(
            start.x, (start.y + end.y) / 2,
            end.x, (start.y + end.y) / 2,
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
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .offset { IntOffset(node.visualX.roundToInt(), node.visualY.roundToInt()) }
            .width(160.dp)
            .pointerInput(node.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMove(node.visualX + dragAmount.x, node.visualY + dragAmount.y)
                }
            },
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
}

@Composable
fun ListEditor(
    nodes: Map<String, Node>,
    onNodesChange: (Map<String, Node>) -> Unit,
    onEditNode: (Node) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Button(onClick = { 
            onEditNode(Node(id = (nodes.size + 1).toString(), description = ""))
        }) {
            Icon(Icons.Default.Add, null)
            Text("添加节点")
        }
        
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
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
                    // Simple Dropdowns or TextFields for IDs
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

