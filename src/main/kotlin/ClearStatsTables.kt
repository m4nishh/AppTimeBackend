import com.apptime.code.focus.FocusModeStats
import com.apptime.code.leaderboard.LeaderboardStats
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import DatabaseFactory

fun main() {
    println("🔄 Initializing database connection...")
    DatabaseFactory.init()
    
    println("🗑️  Clearing stats tables...")
    
    transaction {
        try {
            println("  - Clearing focus_mode_stats...")
            FocusModeStats.deleteAll()
            
            println("  - Clearing leaderboard_stats...")
            LeaderboardStats.deleteAll()
            
            println("✅ All stats tables cleared successfully!")
        } catch (e: Exception) {
            println("❌ Error clearing stats tables: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}

