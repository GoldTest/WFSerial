package org.example.wfserial

import kotlinx.serialization.Serializable

@Serializable
data class AppData(
    val graphs: List<Graph> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val activeGraphId: String? = null
)
