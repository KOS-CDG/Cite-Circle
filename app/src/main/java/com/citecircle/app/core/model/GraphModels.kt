package com.citecircle.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CitationGraphNode(
    val id: String,
    val title: String,
    val abstract: String = "",
    val citationCount: Int = 0,
    val year: Int = 2024,
    val circleId: String? = null,
    val field: String = "General",
    val authors: List<User> = emptyList(),
    val doi: String = "",
    val journal: String = "",
    val isCenter: Boolean = false,
    val hopDistance: Int = 0,
    val x: Float = 0f,
    val y: Float = 0f
)

@Serializable
data class CitationGraphEdge(
    val source: String,
    val target: String,
    val type: String = "CITES"
)

@Serializable
data class CitationGraphSummary(
    val totalPapers: Int = 0,
    val totalCitations: Int = 0,
    val maxDepth: Int = 2
)

@Serializable
data class CitationGraphResponse(
    val nodes: List<CitationGraphNode> = emptyList(),
    val edges: List<CitationGraphEdge> = emptyList(),
    val summary: CitationGraphSummary = CitationGraphSummary()
)

@Serializable
data class CoauthorGraphNode(
    val id: String,
    val name: String,
    val avatarUrl: String = "",
    val institution: String = "",
    val fieldOfStudy: String = "",
    val citationCount: Int = 0,
    val hIndex: Int = 0,
    val i10Index: Int = 0,
    val clusterId: String = "",
    val isCenter: Boolean = false,
    val x: Float = 0f,
    val y: Float = 0f
)

@Serializable
data class CoauthorGraphEdge(
    val source: String,
    val target: String,
    val weight: Int = 1,
    val publications: List<String> = emptyList()
)

@Serializable
data class CoauthorCluster(
    val id: String,
    val name: String,
    val color: String = "#6C63FF",
    val memberIds: List<String> = emptyList()
)

@Serializable
data class CitationVelocityPoint(
    val year: Int,
    val count: Int
)

@Serializable
data class ResearcherAnalytics(
    val totalCitations: Int = 0,
    val hIndex: Int = 0,
    val i10Index: Int = 0,
    val citationVelocity: List<CitationVelocityPoint> = emptyList()
)

@Serializable
data class CoauthorGraphResponse(
    val nodes: List<CoauthorGraphNode> = emptyList(),
    val edges: List<CoauthorGraphEdge> = emptyList(),
    val clusters: List<CoauthorCluster> = emptyList(),
    val analytics: ResearcherAnalytics = ResearcherAnalytics()
)
