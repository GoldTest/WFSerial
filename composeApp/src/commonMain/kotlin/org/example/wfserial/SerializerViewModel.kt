package org.example.wfserial

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SerializerViewModel {
    private val storage = getStorageProvider()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    var graphs by mutableStateOf<List<Graph>>(emptyList())
    var activeGraph by mutableStateOf<Graph?>(null)
    var currentNode by mutableStateOf<Node?>(null)
    var showConclusion by mutableStateOf<String?>(null)
    var currentPath = mutableListOf<String>()
    var history by mutableStateOf<List<HistoryEntry>>(emptyList())
    
    var isEditing by mutableStateOf(false)
    var showHistory by mutableStateOf(false)
    var showGraphDetails by mutableStateOf(false)

    init {
        loadData()
    }

    private fun loadData() {
        try {
            val savedJson = storage.load()
            if (savedJson != null) {
                val data = json.decodeFromString<AppData>(savedJson)
                graphs = data.graphs
                history = data.history
                activeGraph = graphs.find { it.id == data.activeGraphId }
                if (activeGraph != null) {
                    resetGraph()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveData() {
        try {
            val data = AppData(
                graphs = graphs,
                history = history,
                activeGraphId = activeGraph?.id
            )
            storage.save(json.encodeToString(data))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectGraph(graph: Graph) {
        activeGraph = graph
        resetGraph()
        saveData()
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
        saveData()
    }

    fun addGraph(graph: Graph) {
        graphs = graphs + graph
        if (activeGraph == null) {
            selectGraph(graph)
        }
        saveData()
    }

    fun updateGraph(updatedGraph: Graph) {
        graphs = graphs.map { if (it.id == updatedGraph.id) updatedGraph else it }
        if (activeGraph?.id == updatedGraph.id) {
            activeGraph = updatedGraph
        }
        saveData()
    }

    fun deleteHistoryEntry(entry: HistoryEntry) {
        history = history.filter { it !== entry }
        saveData()
    }

    fun clearAllHistory() {
        history = emptyList()
        saveData()
    }
}
