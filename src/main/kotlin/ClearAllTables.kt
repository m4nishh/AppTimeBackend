import com.apptime.code.blockeddomains.BlockedDomainGroups
import com.apptime.code.blockeddomains.BlockedDomains
import com.apptime.code.challenges.ChallengeParticipantStats
import com.apptime.code.challenges.ChallengeParticipants
import com.apptime.code.challenges.Challenges
import com.apptime.code.consents.ConsentTemplates
import com.apptime.code.consents.UserConsents
import com.apptime.code.focus.FocusSessions
import com.apptime.code.focus.FocusModeStats
import com.apptime.code.leaderboard.LeaderboardStats
import com.apptime.code.notifications.Notifications
import com.apptime.code.users.Users
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import urlsearch.UrlSearches
import usage.AppUsageEvents
import DatabaseFactory

fun main() {
    println("🔄 Initializing database connection...")
    DatabaseFactory.init()
    
    println("🗑️  Clearing all tables...")
    
    transaction {
        try {
            // Delete in order to respect foreign key constraints
            // Delete child tables first, then parent tables
            
            println("  - Clearing app_usage_events...")
            AppUsageEvents.deleteAll()
            
            println("  - Clearing challenge_participant_stats...")
            ChallengeParticipantStats.deleteAll()
            
            println("  - Clearing challenge_participants...")
            ChallengeParticipants.deleteAll()
            
            println("  - Clearing challenges...")
            Challenges.deleteAll()
            
            println("  - Clearing focus_mode_stats...")
            FocusModeStats.deleteAll()
            
            println("  - Clearing leaderboard_stats...")
            LeaderboardStats.deleteAll()
            
            println("  - Clearing focus_sessions...")
            FocusSessions.deleteAll()
            
            println("  - Clearing url_searches...")
            UrlSearches.deleteAll()
            
            println("  - Clearing user_consents...")
            UserConsents.deleteAll()
            
            println("  - Clearing blocked_domains...")
            BlockedDomains.deleteAll()
            
            println("  - Clearing blocked_domain_groups...")
            BlockedDomainGroups.deleteAll()
            
            println("  - Clearing notifications...")
            Notifications.deleteAll()
            
            println("  - Clearing consent_templates...")
            ConsentTemplates.deleteAll()
            
            println("  - Clearing users...")
            Users.deleteAll()
            
            println("✅ All tables cleared successfully!")
        } catch (e: Exception) {
            println("❌ Error clearing tables: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}

