package com.apptime.code.challenges

import com.apptime.code.common.dbTransaction
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*

/**
 * Seed initial challenges data
 */
object ChallengeSeedData {
    
    fun seedChallenges() {
        dbTransaction {
            // Check if challenges already exist
            val existingCount = Challenges.selectAll().count()
            
            if (existingCount > 0) {
                println("✅ Challenges already exist ($existingCount challenges). Skipping seed.")
                return@dbTransaction
            }
            
            val now = Clock.System.now()
            val oneDayInMs = 24 * 60 * 60 * 1000L
            val oneWeekInMs = 7 * oneDayInMs
            val twoWeeksInMs = 14 * oneDayInMs
            val oneMonthInMs = 30 * oneDayInMs
            
            // Challenge 1: Reduce Screen Time - 2 weeks challenge
            Challenges.insert {
                it[title] = "Digital Detox Challenge"
                it[description] = "Reduce your daily screen time by 30% over the next 2 weeks. Track your progress and compete with others to minimize phone usage!"
                it[reward] = "Digital Wellness Badge + Premium Features (1 month)"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + twoWeeksInMs)
                it[thumbnail] = "https://example.com/thumbnails/digital-detox.jpg"
                it[challengeType] = "LESS_SCREENTIME"
                it[isActive] = true
            }
            
            // Challenge 2: Focus Mode Marathon - 1 month challenge
            Challenges.insert {
                it[title] = "Focus Mode Marathon"
                it[description] = "Complete 100 hours of focus mode sessions in one month. Build better focus habits and track your productivity journey!"
                it[reward] = "Focus Master Badge + Exclusive Profile Badge"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + oneMonthInMs)
                it[thumbnail] = "https://example.com/thumbnails/focus-marathon.jpg"
                it[challengeType] = "MORE_SCREENTIME"
                it[isActive] = true
            }
            
            // Challenge 3: Weekend Warrior - 1 week challenge
            Challenges.insert {
                it[title] = "Weekend Warrior Challenge"
                it[description] = "Keep your screen time under 2 hours per day during weekends. Perfect for those who want to enjoy real-world activities!"
                it[reward] = "Weekend Warrior Badge"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + oneWeekInMs)
                it[thumbnail] = "https://example.com/thumbnails/weekend-warrior.jpg"
                it[challengeType] = "LESS_SCREENTIME"
                it[isActive] = true
            }
            
            // Challenge 4: Productivity Power Hour - 2 weeks challenge
            Challenges.insert {
                it[title] = "Productivity Power Hour"
                it[description] = "Accumulate 50 hours of productive app usage (work, study, learning apps) in 2 weeks. Quality over quantity!"
                it[reward] = "Productivity Pro Badge + Learning Resources Pack"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + twoWeeksInMs)
                it[thumbnail] = "https://example.com/thumbnails/productivity-power.jpg"
                it[challengeType] = "MORE_SCREENTIME"
                it[isActive] = true
            }
            
            // Challenge 5: Evening Unplug Challenge - 1 week challenge
            Challenges.insert {
                it[title] = "Evening Unplug Challenge"
                it[description] = "Keep your phone usage under 30 minutes after 8 PM for a week. Improve your sleep quality and evening routine!"
                it[reward] = "Sleep Well Badge + Meditation App Subscription (1 month)"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + oneWeekInMs)
                it[thumbnail] = "https://example.com/thumbnails/evening-unplug.jpg"
                it[challengeType] = "LESS_SCREENTIME"
                it[isActive] = true
            }
            
            // Challenge 6: Learning Streak - 1 month challenge
            Challenges.insert {
                it[title] = "30-Day Learning Streak"
                it[description] = "Spend at least 1 hour daily on educational apps (courses, reading, language learning) for 30 days straight!"
                it[reward] = "Lifelong Learner Badge + Course Discount (50% off)"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + oneMonthInMs)
                it[thumbnail] = "https://example.com/thumbnails/learning-streak.jpg"
                it[challengeType] = "MORE_SCREENTIME"
                it[isActive] = true
            }
            
            // Challenge 7: Social Media Detox - 1 week challenge
            Challenges.insert {
                it[title] = "Social Media Detox Week"
                it[description] = "Limit social media apps (Instagram, Facebook, Twitter, TikTok) to less than 30 minutes per day for a week!"
                it[reward] = "Social Detox Champion Badge"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + oneWeekInMs)
                it[thumbnail] = "https://example.com/thumbnails/social-detox.jpg"
                it[challengeType] = "LESS_SCREENTIME"
                it[isActive] = true
            }
            
            // Challenge 8: Deep Work Challenge - 2 weeks challenge
            Challenges.insert {
                it[title] = "Deep Work Challenge"
                it[description] = "Complete 40 hours of uninterrupted focus sessions (minimum 2 hours each) in 2 weeks. Master the art of deep work!"
                it[reward] = "Deep Work Master Badge + Productivity Tools Bundle"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + twoWeeksInMs)
                it[thumbnail] = "https://example.com/thumbnails/deep-work.jpg"
                it[challengeType] = "MORE_SCREENTIME"
                it[isActive] = true
            }
            
            println("✅ Seeded ${Challenges.selectAll().count()} challenges")
        }
    }
}

