import com.apptime.code.rewards.RewardRepository
import com.apptime.code.rewards.RewardType
import com.apptime.code.rewards.RewardSource
import DatabaseFactory

fun main() {
    val userId = "a2fd3076-9ab6-43b4-c7fa-9f3c0157668c"
    
    println("🔄 Initializing database connection...")
    DatabaseFactory.init()
    
    println("🎁 Adding rewards for user: $userId")
    
    val repository = RewardRepository()
    
    try {
        // Add a points reward
        val pointsRewardId = repository.createReward(
            userId = userId,
            type = RewardType.POINTS,
            source = RewardSource.ADMIN_GRANT,
            title = "Welcome Bonus Points",
            description = "Admin granted welcome bonus points",
            amount = 1000L
        )
        println("✅ Created points reward with ID: $pointsRewardId")
        
        // Add a badge reward
        val badgeRewardId = repository.createReward(
            userId = userId,
            type = RewardType.BADGE,
            source = RewardSource.ACHIEVEMENT,
            title = "Early Adopter Badge",
            description = "Badge for being an early user of the platform",
            amount = 0L
        )
        println("✅ Created badge reward with ID: $badgeRewardId")
        
        // Add a trophy reward
        val trophyRewardId = repository.createReward(
            userId = userId,
            type = RewardType.TROPHY,
            source = RewardSource.ADMIN_GRANT,
            title = "Champion Trophy",
            description = "Special trophy awarded by admin",
            amount = 0L
        )
        println("✅ Created trophy reward with ID: $trophyRewardId")
        
        println("\n🎉 Successfully added 3 rewards for user: $userId")
        println("   - Points: 1000")
        println("   - Badge: Early Adopter Badge")
        println("   - Trophy: Champion Trophy")
        
    } catch (e: Exception) {
        println("❌ Error adding rewards: ${e.message}")
        e.printStackTrace()
    }
}

