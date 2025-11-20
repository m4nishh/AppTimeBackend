package urlsearch

import kotlinx.serialization.Serializable

@Serializable
data class URLSearchSubmissionRequest(
    val url: String,
    val domain: String,
    val searchedAt: String, // ISO 8601 format
    val searchType: String? = null // "web", "app", "vpn"
)

@Serializable
data class BatchURLSearchSubmissionRequest(
    val urlSearches: List<URLSearchSubmissionRequest>
)

@Serializable
data class URLSearchHistoryRequest(
    val startDate: String,
    val endDate: String,
    val domain: String? = null
)

@Serializable
data class URLSearchHistoryItem(
    val id: Long? = null,
    val url: String,
    val domain: String,
    val searchedAt: String,
    val searchType: String? = null,
    val createdAt: String? = null
)

@Serializable
data class URLSearchHistoryResponse(
    val searches: List<URLSearchHistoryItem>,
    val totalCount: Int
)

