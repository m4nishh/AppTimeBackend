package feedback

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Feedback table - stores user feedback
 */
object Feedback : Table("feedback") {
    val id = long("id").autoIncrement()
    val userId = varchar("user_id", 255).index()
    val message = text("message")
    
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(isUnique = false, userId, createdAt)
    }
}


