import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import usage.AppUsageEvents
import DatabaseFactory

fun main() {
    println("🔄 Initializing database connection...")
    DatabaseFactory.init()
    
    println("🗑️  Clearing app_usage_events table (synced usage data)...")
    
    transaction {
        try {
            println("  - Clearing app_usage_events...")
            AppUsageEvents.deleteAll()
            
            println("✅ app_usage_events table cleared successfully!")
        } catch (e: Exception) {
            println("❌ Error clearing app_usage_events table: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}



