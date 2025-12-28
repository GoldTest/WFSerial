package org.example.wfserial

import kotlinx.serialization.Serializable

@Serializable
data class Node(
    val id: String,
    val description: String,
    val yesNodeId: String? = null,
    val noNodeId: String? = null,
    val isConclusion: Boolean = false,
    val result: String? = null
)

@Serializable
data class Graph(
    val id: String,
    val name: String,
    val nodes: Map<String, Node>,
    val startNodeId: String
)

@Serializable
data class HistoryEntry(
    val graphId: String,
    val graphName: String,
    val path: List<String>, // List of node descriptions
    val result: String,
    val timestamp: Long
)
