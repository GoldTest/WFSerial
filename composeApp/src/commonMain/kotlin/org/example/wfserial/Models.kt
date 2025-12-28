package org.example.wfserial

import kotlinx.serialization.Serializable

@Serializable
data class Node(
    val id: String,
    val description: String,
    val yesNodeId: String? = null,
    val noNodeId: String? = null,
    val isConclusion: Boolean = false,
    val result: String? = null,
    val visualX: Float = 0f, // For visual editor
    val visualY: Float = 0f  // For visual editor
)

@Serializable
data class Graph(
    val id: String,
    val name: String,
    val nodes: Map<String, Node>,
    val startNodeId: String,
    val customShader: String? = null
)

@Serializable
data class HistoryEntry(
    val graphId: String,
    val graphName: String,
    val path: List<String>, // List of node descriptions
    val result: String,
    val timestamp: Long
)
