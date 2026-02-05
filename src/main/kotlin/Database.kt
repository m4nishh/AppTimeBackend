import com.apptime.code.blockeddomains.BlockedDomainGroups
import com.apptime.code.blockeddomains.BlockedDomains
import com.apptime.code.challenges.ChallengeParticipantStats
import com.apptime.code.challenges.ChallengeParticipants
import com.apptime.code.challenges.ChallengeSeedData
import com.apptime.code.challenges.Challenges
import com.apptime.code.clans.Clans
import com.apptime.code.clans.ClanMembers
import com.apptime.code.clans.ClanStats
import com.apptime.code.clans.ClanInvites
import com.apptime.code.clans.ClanBadges
import com.apptime.code.clans.ClanJoinRequests
import com.apptime.code.clans.UserAppCategories
import com.apptime.code.clans.AppCategoryUsage
import com.apptime.code.clans.ClanVault
import com.apptime.code.clans.ClanChallenges
import com.apptime.code.clans.ClanChallengeParticipants
import com.apptime.code.clans.ClanWars
import com.apptime.code.clans.ClanEducationLeaderboard
import com.apptime.code.common.EnvLoader
import com.apptime.code.consents.ConsentSeedData
import com.apptime.code.consents.ConsentTemplates
import com.apptime.code.consents.UserConsents
import feedback.Feedback
import com.apptime.code.features.FeatureFlags
import com.apptime.code.focus.FocusSessions
import com.apptime.code.focus.FocusModeStats
import com.apptime.code.leaderboard.LeaderboardStats
import com.apptime.code.appstats.AppStats
import com.apptime.code.location.UserLocations
import com.apptime.code.notifications.Notifications
import com.apptime.code.referral.Referrals
import com.apptime.code.referral.UserReferralCodes
import com.apptime.code.rewards.Coins
import com.apptime.code.rewards.RewardCatalog
import com.apptime.code.rewards.Rewards
import com.apptime.code.rewards.Transactions
import com.apptime.code.users.Users
import users.TOTPVerificationSessions
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import urlsearch.UrlSearches
import usage.AppUsageEvents

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

            // Run migrations
            runMigrations()
            
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

    private fun runMigrations() {
        transaction {
            try {
                // Migration: Remove old unique constraint on (user_id, is_active) 
                // to allow users to be members of multiple clans
                val migrationSQL = """
                    DO $$ 
                    DECLARE
                        constraint_name TEXT;
                    BEGIN
                        -- Find and drop old constraint on (user_id, is_active)
                        SELECT conname INTO constraint_name
                        FROM pg_constraint c
                        JOIN pg_class t ON c.conrelid = t.oid
                        JOIN pg_namespace n ON t.relnamespace = n.oid
                        WHERE n.nspname = 'public'
                          AND t.relname = 'clan_members'
                          AND c.contype = 'u'
                          AND array_length(c.conkey, 1) = 2
                          AND EXISTS (
                              SELECT 1 FROM pg_attribute a1
                              WHERE a1.attrelid = c.conrelid
                                AND a1.attnum = c.conkey[1]
                                AND a1.attname = 'user_id'
                          )
                          AND EXISTS (
                              SELECT 1 FROM pg_attribute a2
                              WHERE a2.attrelid = c.conrelid
                                AND a2.attnum = c.conkey[2]
                                AND a2.attname = 'is_active'
                          );
                        
                        IF constraint_name IS NOT NULL THEN
                            EXECUTE format('ALTER TABLE clan_members DROP CONSTRAINT %I', constraint_name);
                            RAISE NOTICE 'Dropped old constraint: %', constraint_name;
                        END IF;
                        
                        -- Ensure new constraint exists on (clan_id, user_id)
                        SELECT conname INTO constraint_name
                        FROM pg_constraint c
                        JOIN pg_class t ON c.conrelid = t.oid
                        JOIN pg_namespace n ON t.relnamespace = n.oid
                        WHERE n.nspname = 'public'
                          AND t.relname = 'clan_members'
                          AND c.contype = 'u'
                          AND array_length(c.conkey, 1) = 2
                          AND EXISTS (
                              SELECT 1 FROM pg_attribute a1
                              WHERE a1.attrelid = c.conrelid
                                AND a1.attnum = c.conkey[1]
                                AND a1.attname = 'clan_id'
                          )
                          AND EXISTS (
                              SELECT 1 FROM pg_attribute a2
                              WHERE a2.attrelid = c.conrelid
                                AND a2.attnum = c.conkey[2]
                                AND a2.attname = 'user_id'
                          );
                        
                        IF constraint_name IS NULL THEN
                            ALTER TABLE clan_members 
                            ADD CONSTRAINT clan_members_clan_id_user_id_unique 
                            UNIQUE (clan_id, user_id);
                            RAISE NOTICE 'Added new constraint: clan_members_clan_id_user_id_unique';
                        END IF;
                    END $$;
                """.trimIndent()
                
                // Execute the migration SQL using the connection
                transaction {
                    exec(migrationSQL)
                }


                println("✅ Database migrations completed!")
            } catch (e: Exception) {
                // Migration might fail if constraint doesn't exist or already exists, which is fine
                println("⚠️  Migration note: ${e.message}")
            }
        }
    }

    private fun createTables() {
        transaction {
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
                
                // Behavioral OS module
                UserAppCategories,
                AppCategoryUsage,
                ClanVault,
                ClanChallenges,
                ClanChallengeParticipants,
                ClanWars,
                ClanEducationLeaderboard,
                
                // Features module
                FeatureFlags,
                
                // App Stats module
                AppStats,
                
                // Feedback module
                Feedback
            )
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
