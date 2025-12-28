package org.example.wfserial

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock

class SerializerViewModel {
    var graphs by mutableStateOf<List<Graph>>(emptyList())
    var activeGraph by mutableStateOf<Graph?>(null)
    var currentNode by mutableStateOf<Node?>(null)
    var showConclusion by mutableStateOf<String?>(null)
    var currentPath = mutableListOf<String>()
    var history by mutableStateOf<List<HistoryEntry>>(emptyList())
    
    var isEditing by mutableStateOf(false)
    var showHistory by mutableStateOf(false)

    fun selectGraph(graph: Graph) {
        activeGraph = graph
        resetGraph()
    }

    fun resetGraph() {
        currentNode = activeGraph?.nodes?.get(activeGraph?.startNodeId)
        currentPath.clear()
        showConclusion = null
    }

    fun onChoice(isYes: Boolean) {
        if (showConclusion != null) return

        val node = currentNode ?: return
        currentPath.add(node.description)
        
        val nextId = if (isYes) node.yesNodeId else node.noNodeId
        val nextNode = activeGraph?.nodes?.get(nextId)
        
        if (nextNode != null) {
            if (nextNode.isConclusion) {
                completeGraph(nextNode.result ?: "未知结论")
            } else {
                currentNode = nextNode
            }
        } else {
            completeGraph("未定义结论")
        }
    }

    private fun completeGraph(result: String) {
        val entry = HistoryEntry(
            graphId = activeGraph?.id ?: "",
            graphName = activeGraph?.name ?: "",
            path = ArrayList(currentPath),
            result = result,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        history = listOf(entry) + history
        showConclusion = result
        currentNode = null
    }

    fun addGraph(graph: Graph) {
        graphs = graphs + graph
        if (activeGraph == null) {
            selectGraph(graph)
        }
    }
}
