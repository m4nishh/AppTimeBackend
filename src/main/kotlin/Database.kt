import com.apptime.code.appstats.AppStats
import com.apptime.code.blockeddomains.BlockedDomainGroups
import com.apptime.code.blockeddomains.BlockedDomains
import com.apptime.code.challenges.ChallengeParticipantStats
import com.apptime.code.challenges.ChallengeParticipants
import com.apptime.code.challenges.ChallengeSeedData
import com.apptime.code.challenges.Challenges
import com.apptime.code.clans.*
import com.apptime.code.common.EnvLoader
import com.apptime.code.consents.ConsentSeedData
import com.apptime.code.consents.ConsentTemplates
import com.apptime.code.consents.UserConsents
import com.apptime.code.features.FeatureFlags
import com.apptime.code.focus.FocusModeStats
import com.apptime.code.focus.FocusSessions
import com.apptime.code.leaderboard.LeaderboardStats
import com.apptime.code.location.UserLocations
import com.apptime.code.notifications.Notifications
import com.apptime.code.referral.Referrals
import com.apptime.code.referral.UserReferralCodes
import com.apptime.code.rewards.Coins
import com.apptime.code.rewards.RewardCatalog
import com.apptime.code.rewards.Rewards
import com.apptime.code.rewards.Transactions
import com.apptime.code.users.Users
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import feedback.Feedback
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.transactions.transaction
import urlsearch.UrlSearches
import usage.AppUsageEvents
import users.TOTPVerificationSessions

object DatabaseFactory {
    fun init() {
        // Load .env file if it exists (this reads from .env file)
        EnvLoader.loadEnvFile()
        
        // Get environment variables from .env file or system environment
        val jdbcUrl = EnvLoader.getEnv("DATABASE_URL")
            ?: "jdbc:postgresql://localhost:5432/screentime_db"
        val dbUser = EnvLoader.getEnv("DB_USER") ?: "postgres"
        val dbPassword = EnvLoader.getEnv("DB_PASSWORD") ?: "Sharma@11"

        // Print connection details for debugging (mask password)
        println("🔌 Attempting to connect to database...")
        println("   URL: $jdbcUrl")
        println("   User: $dbUser")
        println("   Password: ${if (dbPassword.isNotEmpty()) "***" else "(empty)"}")

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            username = dbUser
            password = dbPassword
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            minimumIdle = 2
            isAutoCommit = true  // Exposed manages transactions, so connections should be in auto-commit mode
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            
            // Connection timeout settings
            connectionTimeout = 10000 // 10 seconds
            validationTimeout = 5000 // 5 seconds
            idleTimeout = 600000 // 10 minutes
            maxLifetime = 1800000 // 30 minutes
            
            // Connection test query
            connectionTestQuery = "SELECT 1"
            
            // Leak detection
            leakDetectionThreshold = 60000 // 1 minute
            
            validate()
        }

        try {
            val dataSource = HikariDataSource(config)
            
            // Test the connection immediately
            dataSource.connection.use { conn ->
                if (conn.isValid(5)) {
                    println("✅ Database connection test successful!")
                } else {
                    throw Exception("Connection validation failed")
                }
            }

            Database.connect(
                dataSource,
                databaseConfig = DatabaseConfig {
                    defaultRepetitionAttempts = 3
                }
            )

            // Create all tables
            createTables()
            
            // Seed initial data
            seedInitialData()

            println("✅ Database connected successfully!")
        } catch (e: Exception) {
            println("❌ Database connection failed!")
            println("   Error: ${e.message}")
            println("   Cause: ${e.cause?.message ?: "Unknown"}")
            
            // Provide helpful troubleshooting information
            println("\n🔍 Troubleshooting steps:")
            println("   1. Check if PostgreSQL is running:")
            println("      - macOS: brew services list | grep postgresql")
            println("      - Linux: sudo systemctl status postgresql")
            println("      - Docker: docker ps | grep postgres")
            println("   2. Verify database exists:")
            println("      psql -U $dbUser -l | grep screentime_db")
            println("   3. Test connection manually:")
            println("      psql -U $dbUser -d screentime_db -h localhost")
            println("   4. Check connection details:")
            println("      URL: $jdbcUrl")
            println("      User: $dbUser")
            println("   5. Create .env file with correct credentials if needed")
            
            // Re-throw to prevent application from starting with broken database
            throw RuntimeException("Failed to connect to database. See error details above.", e)
        }
    }

    private fun createTables() {
        transaction {
            // Migration: Drop old constraint if it exists and add new one
            try {
                // Check if old constraint exists and drop it
                exec("""
                    DO $$ 
                    BEGIN
                        IF EXISTS (
                            SELECT 1 FROM pg_constraint 
                            WHERE conname = 'clan_members_user_id_is_active_unique'
                        ) THEN
                            ALTER TABLE clan_members 
                            DROP CONSTRAINT clan_members_user_id_is_active_unique;
                            RAISE NOTICE 'Dropped old constraint clan_members_user_id_is_active_unique';
                        END IF;
                    END $$;
                """.trimIndent())
                println("✅ Checked and dropped old constraint if it existed")
            } catch (e: Exception) {
                // If constraint doesn't exist or table doesn't exist, that's okay
                if (e.message?.contains("does not exist") == false && 
                    e.message?.contains("relation") == false &&
                    e.message?.contains("clan_members") == false) {
                    println("ℹ️  Could not drop old constraint: ${e.message}")
                }
            }
            
            // Clean up duplicate clan memberships before creating constraints
            // Users can be members of multiple clans, but only once per clan
            // The constraint requires (clan_id, user_id) to be unique
            try {
                // Get all memberships and group by (clanId, userId) to find duplicates
                val allMemberships = ClanMembers.selectAll().map { row ->
                    Triple(row[ClanMembers.id], row[ClanMembers.clanId], row[ClanMembers.userId])
                }
                
                // Group by (clanId, userId) to find duplicates
                val grouped = allMemberships.groupBy { (_, clanId, userId) -> 
                    clanId to userId 
                }
                
                var cleanedCount = 0
                
                // Clean up duplicate memberships in the same clan (keep oldest active, or oldest if all inactive)
                for ((clanId, userId) in grouped.keys) {
                    val memberships = grouped[clanId to userId] ?: continue
                    if (memberships.size > 1) {
                        // Get full membership records to check isActive and joinedAt
                        val fullMemberships = ClanMembers.select {
                            (ClanMembers.clanId eq clanId) and (ClanMembers.userId eq userId)
                        }
                            .orderBy(ClanMembers.isActive to SortOrder.DESC, ClanMembers.joinedAt to SortOrder.ASC)
                            .toList()
                        
                        // Keep the first (oldest active, or oldest if all inactive), delete the rest
                        val toDelete = fullMemberships.drop(1)
                        for (membership in toDelete) {
                            val membershipId = membership[ClanMembers.id]
                            ClanMembers.deleteWhere { ClanMembers.id eq membershipId }
                            cleanedCount++
                            println("   Deleted duplicate membership for user $userId in clan $clanId")
                        }
                    }
                }
                
                if (cleanedCount > 0) {
                    println("✅ Cleaned up $cleanedCount duplicate clan memberships!")
                }
            } catch (e: Exception) {
                // If table doesn't exist yet or query fails, that's okay - constraint will be created fresh
                // This is expected on first run when the table doesn't exist yet
                if (e.message?.contains("does not exist") == false && 
                    e.message?.contains("relation") == false) {
                    println("ℹ️  Could not check for duplicates: ${e.message}")
                }
            }
            
            // Try to create/update tables and constraints
            // If it fails due to duplicate constraint, clean up and retry once
            try {
                SchemaUtils.createMissingTablesAndColumns(
                // Users module
                Users,
                TOTPVerificationSessions,

                // Location module
                UserLocations,

                // Usage module
                AppUsageEvents,

                // Focus module
                FocusSessions,
                FocusModeStats,

                // URL Search module
                UrlSearches,

                // Blocked Domains module
                BlockedDomainGroups,
                BlockedDomains,

                // Notifications module
                Notifications,
                
                // Consents module
                ConsentTemplates,
                UserConsents,
                
                // Leaderboard module
                LeaderboardStats,
                
                // Challenges module
                Challenges,
                ChallengeParticipants,
                ChallengeParticipantStats,
                com.apptime.code.challenges.ChallengeShares,
                com.apptime.code.challenges.ChallengeShareEvents,
                
                // Rewards module
                Rewards,
                Coins,
                RewardCatalog,
                Transactions,
                
                // Referral module
                UserReferralCodes,
                Referrals,
                
                // Clans module
                Clans,
                ClanMembers,
                ClanStats,
                ClanInvites,
                ClanBadges,
                ClanJoinRequests,
                ClanShares,
                ClanShareEvents,
                
                // Features module
                FeatureFlags,
                
                // App Stats module
                AppStats,
                
                // Feedback module
                Feedback
                )
            } catch (e: Exception) {
                // If constraint creation fails due to duplicates, we need to clean up in a new transaction
                // because the current transaction is aborted
                if (e.message?.contains("duplicated") == true || 
                    e.message?.contains("unique") == true ||
                    e.cause?.message?.contains("duplicated") == true) {
                    println("⚠️  Constraint creation failed due to duplicates. Cleaning up in new transaction...")
                    
                    // Clean up duplicates in a new transaction
                    try {
                        transaction {
                            // Get all memberships and group by (clanId, userId) to find duplicates
                            val allMemberships = ClanMembers.selectAll().map { row ->
                                Triple(row[ClanMembers.id], row[ClanMembers.clanId], row[ClanMembers.userId])
                            }
                            
                            // Group by (clanId, userId) to find duplicates
                            val grouped = allMemberships.groupBy { (_, clanId, userId) -> 
                                clanId to userId 
                            }
                            
                            // Clean up duplicate memberships in the same clan
                            for ((clanId, userId) in grouped.keys) {
                                val memberships = grouped[clanId to userId] ?: continue
                                if (memberships.size > 1) {
                                    // Get full membership records to check isActive and joinedAt
                                    val fullMemberships = ClanMembers.select {
                                        (ClanMembers.clanId eq clanId) and (ClanMembers.userId eq userId)
                                    }
                                        .orderBy(ClanMembers.isActive to SortOrder.DESC, ClanMembers.joinedAt to SortOrder.ASC)
                                        .toList()
                                    
                                    // Keep the first (oldest active, or oldest if all inactive), delete the rest
                                    val toDelete = fullMemberships.drop(1)
                                    for (membership in toDelete) {
                                        val membershipId = membership[ClanMembers.id]
                                        ClanMembers.deleteWhere { ClanMembers.id eq membershipId }
                                    }
                                }
                            }
                            
                            println("✅ Cleaned up duplicates. Retrying constraint creation...")
                        }
                        
                        // Retry creating tables/constraints in a new transaction
                        transaction {
                            SchemaUtils.createMissingTablesAndColumns(
                                Users, TOTPVerificationSessions, UserLocations, AppUsageEvents,
                                FocusSessions, FocusModeStats, UrlSearches, BlockedDomainGroups,
                                BlockedDomains, Notifications, ConsentTemplates, UserConsents,
                                LeaderboardStats, Challenges, ChallengeParticipants, ChallengeParticipantStats,
                                Rewards, Coins, RewardCatalog, Transactions, UserReferralCodes, Referrals,
                                Clans, ClanMembers, ClanStats, ClanInvites, ClanBadges, ClanJoinRequests,
                                ClanShares, ClanShareEvents,
                                FeatureFlags, AppStats, Feedback
                            )
                        }
                    } catch (retryException: Exception) {
                        println("❌ Retry failed: ${retryException.message}")
                        throw e // Throw original exception
                    }
                } else {
                    // Re-throw if it's a different error
                    throw e
                }
            }
        }
        println("✅ Database tables created/verified!")
    }
    
    private fun seedInitialData() {
        // Seed consent templates
        ConsentSeedData.seedConsentTemplates()
        
        // Seed challenges
        ChallengeSeedData.seedChallenges()
    }
}
