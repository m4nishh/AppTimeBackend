package blockeddomains

import kotlinx.serialization.Serializable

@Serializable
data class BlockedDomainGroup(
    val id: Int,
    val name: String,
    val description: String? = null,
    val domains: List<BlockedDomainItem>? = null
)

@Serializable
data class BlockedDomainItem(
    val id: Int? = null,
    val domain: String,
    val groupId: Int? = null,
    val groupName: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null
)

@Serializable
data class GetBlockedDomainsResponse(
    val domains: List<BlockedDomainItem>,
    val groups: List<BlockedDomainGroup>
)

@Serializable
data class SubmitBlockedDomainRequest(
    val domain: String,
    val groupId: Int? = null,
    val isActive: Boolean = true
)

@Serializable
data class SubmitBlockedDomainResponse(
    val success: Boolean,
    val domain: BlockedDomainItem? = null,
    val message: String? = null
)

