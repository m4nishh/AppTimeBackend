import com.apptime.code.challenges.ChallengeParticipantStats
import com.apptime.code.challenges.ChallengeParticipants
import com.apptime.code.challenges.Challenges
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import DatabaseFactory

fun main() {
    println("🔄 Initializing database connection...")
    DatabaseFactory.init()
    
    println("🗑️  Clearing challenges tables...")
    
    transaction {
        try {
            // Delete in order to respect foreign key constraints
            // Delete child tables first, then parent tables
            
            println("  - Clearing challenge_participant_stats...")
            ChallengeParticipantStats.deleteAll()
            
            println("  - Clearing challenge_participants...")
            ChallengeParticipants.deleteAll()
            
            println("  - Clearing challenges...")
            Challenges.deleteAll()
            
            println("✅ All challenges tables cleared successfully!")
        } catch (e: Exception) {
            println("❌ Error clearing challenges tables: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}

