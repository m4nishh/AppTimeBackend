package urlsearch

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * URL searches table - stores web browsing/URL tracking data (VPN tracking)
 */
object UrlSearches : Table("url_searches") {
    val id = long("id").autoIncrement()
    val userId = varchar("user_id", 255).index()
    val url = text("url")
    val domain = varchar("domain", 255).index()
    val searchedAt = timestamp("searched_at")
    val searchType = varchar("search_type", 50).nullable() // "web", "app", "vpn"
    
    val createdAt = timestamp("created_at").clientDefault { kotlinx.datetime.Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(isUnique = false, userId, searchedAt)
        index(isUnique = false, userId, domain)
    }
}

