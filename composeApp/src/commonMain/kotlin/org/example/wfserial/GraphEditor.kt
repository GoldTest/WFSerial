package org.example.wfserial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import kotlinx.datetime.Clock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GraphEditor(viewModel: SerializerViewModel) {
    var graphName by remember { mutableStateOf("") }
    var nodes by remember { mutableStateOf(mapOf<String, Node>()) }
    var startNodeId by remember { mutableStateOf("") }
    
    var showNodeDialog by remember { mutableStateOf(false) }
    var editingNode by remember { mutableStateOf<Node?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = graphName,
            onValueChange = { graphName = it },
            label = { Text("图名称") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("节点列表", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Button(onClick = { 
                editingNode = Node(id = (nodes.size + 1).toString(), description = "")
                showNodeDialog = true 
            }) {
                Icon(Icons.Default.Add, null)
                Text("添加节点")
            }
        }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(nodes.values.toList()) { node ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(node.description, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("ID: ${node.id} | ${if(node.isConclusion) "结论: ${node.result}" else "Yes: ${node.yesNodeId}, No: ${node.noNodeId}"}")
                        }
                        IconButton(onClick = { editingNode = node; showNodeDialog = true }) {
                            Icon(Icons.Default.Edit, null)
                        }
                        IconButton(onClick = { nodes = nodes - node.id }) {
                            Icon(Icons.Default.Delete, null)
                        }
                    }
                }
            }
        }
        
        Button(
            onClick = {
                if (graphName.isNotBlank() && nodes.isNotEmpty()) {
                    val newGraph = Graph(
                        id = Clock.System.now().toEpochMilliseconds().toString(),
                        name = graphName,
                        nodes = nodes,
                        startNodeId = nodes.keys.first() // Default to first node
                    )
                    viewModel.addGraph(newGraph)
                    viewModel.isEditing = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = graphName.isNotBlank() && nodes.isNotEmpty()
        ) {
            Text("保存图")
        }
    }

    if (showNodeDialog && editingNode != null) {
        NodeDialog(
            node = editingNode!!,
            onDismiss = { showNodeDialog = false },
            onSave = { newNode ->
                nodes = nodes + (newNode.id to newNode)
                showNodeDialog = false
            }
        )
    }
}

@Composable
fun NodeDialog(node: Node, onDismiss: () -> Unit, onSave: (Node) -> Unit) {
    var description by remember { mutableStateOf(node.description) }
    var isConclusion by remember { mutableStateOf(node.isConclusion) }
    var result by remember { mutableStateOf(node.result ?: "") }
    var yesId by remember { mutableStateOf(node.yesNodeId ?: "") }
    var noId by remember { mutableStateOf(node.noNodeId ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑节点") },
        text = {
            Column {
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述/问题") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isConclusion, onCheckedChange = { isConclusion = it })
                    Text("是结论节点")
                }
                if (isConclusion) {
                    OutlinedTextField(value = result, onValueChange = { result = it }, label = { Text("结论结果") })
                } else {
                    OutlinedTextField(value = yesId, onValueChange = { yesId = it }, label = { Text("YES 下一个节点 ID") })
                    OutlinedTextField(value = noId, onValueChange = { noId = it }, label = { Text("NO 下一个节点 ID") })
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
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

