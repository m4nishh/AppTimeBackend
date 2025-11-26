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
                it[prize] = "<div><strong>Rank 1:</strong> 100 points</div><div><strong>Rank 2:</strong> 50 points</div><div><strong>Rank 3:</strong> 10 points</div>"
                it[rules] = "<div><strong>Rules:</strong></div><ul><li>Track your screen time daily</li><li>Reduce usage by at least 30% from baseline</li><li>Submit stats regularly to maintain ranking</li><li>Only active app usage counts</li></ul>"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + twoWeeksInMs)
                it[thumbnail] = "https://fastly.picsum.photos/id/866/200/300.jpg?hmac=rcadCENKh4rD6MAp6V_ma-AyWv641M4iiOpe1RyFHeI"
                it[challengeType] = "LESS_SCREENTIME"
                it[packageNames] = null // All apps
                it[displayType] = "FEATURE"
                it[tags] = "social media,wellness"
                it[sponsor] = "AppTime"
                it[isActive] = true
            }
            
            // Challenge 2: Focus Mode Marathon - 1 month challenge
            Challenges.insert {
                it[title] = "Focus Mode Marathon"
                it[description] = "Complete 100 hours of focus mode sessions in one month. Build better focus habits and track your productivity journey!"
                it[reward] = "Focus Master Badge + Exclusive Profile Badge"
                it[prize] = "<div><strong>Rank 1:</strong> 100 points</div><div><strong>Rank 2:</strong> 50 points</div><div><strong>Rank 3:</strong> 10 points</div>"
                it[rules] = "<div><strong>Rules:</strong></div><ul><li>Complete focus mode sessions of minimum 30 minutes</li><li>Track all focus sessions accurately</li><li>Submit stats daily for best results</li><li>Total 100 hours required to qualify</li></ul>"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + oneMonthInMs)
                it[thumbnail] = "https://fastly.picsum.photos/id/866/200/300.jpg?hmac=rcadCENKh4rD6MAp6V_ma-AyWv641M4iiOpe1RyFHeI"
                it[challengeType] = "MORE_SCREENTIME"
                it[packageNames] = null // All apps
                it[displayType] = "SPECIAL"
                it[tags] = "study,productivity"
                it[sponsor] = "Focus Pro"
                it[isActive] = true
            }
            
            // Challenge 3: Weekend Warrior - 1 week challenge
            Challenges.insert {
                it[title] = "Weekend Warrior Challenge"
                it[description] = "Keep your screen time under 2 hours per day during weekends. Perfect for those who want to enjoy real-world activities!"
                it[reward] = "Weekend Warrior Badge"
                it[prize] = "<div><strong>Rank 1:</strong> 100 points</div><div><strong>Rank 2:</strong> 50 points</div><div><strong>Rank 3:</strong> 10 points</div>"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + oneWeekInMs)
                it[thumbnail] = "https://fastly.picsum.photos/id/866/200/300.jpg?hmac=rcadCENKh4rD6MAp6V_ma-AyWv641M4iiOpe1RyFHeI"
                it[challengeType] = "LESS_SCREENTIME"
                it[packageNames] = null // All apps
                it[displayType] = "QUICK_JOIN"
                it[tags] = "wellness"
                it[sponsor] = "Wellness Co"
                it[isActive] = true
            }
            
            // Challenge 4: Productivity Power Hour - 2 weeks challenge
            Challenges.insert {
                it[title] = "Productivity Power Hour"
                it[description] = "Accumulate 50 hours of productive app usage (work, study, learning apps) in 2 weeks. Quality over quantity!"
                it[reward] = "Productivity Pro Badge + Learning Resources Pack"
                it[prize] = "<div><strong>Rank 1:</strong> 100 points</div><div><strong>Rank 2:</strong> 50 points</div><div><strong>Rank 3:</strong> 10 points</div>"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + twoWeeksInMs)
                it[thumbnail] = "https://fastly.picsum.photos/id/866/200/300.jpg?hmac=rcadCENKh4rD6MAp6V_ma-AyWv641M4iiOpe1RyFHeI"
                it[challengeType] = "MORE_SCREENTIME"
                it[packageNames] = "com.microsoft.office.word,com.microsoft.office.excel,com.microsoft.office.powerpoint,com.google.android.apps.docs,com.google.android.apps.sheets,com.notion.id,com.todoist,com.evernote"
                it[displayType] = "TRENDING"
                it[tags] = "study,productivity"
                it[sponsor] = "Productivity Plus"
                it[isActive] = true
            }
            
            // Challenge 5: Evening Unplug Challenge - 1 week challenge
            Challenges.insert {
                it[title] = "Evening Unplug Challenge"
                it[description] = "Keep your phone usage under 30 minutes after 8 PM for a week. Improve your sleep quality and evening routine!"
                it[reward] = "Sleep Well Badge + Meditation App Subscription (1 month)"
                it[prize] = "<div><strong>Rank 1:</strong> 100 points</div><div><strong>Rank 2:</strong> 50 points</div><div><strong>Rank 3:</strong> 10 points</div>"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + oneWeekInMs)
                it[thumbnail] = "https://fastly.picsum.photos/id/866/200/300.jpg?hmac=rcadCENKh4rD6MAp6V_ma-AyWv641M4iiOpe1RyFHeI"
                it[challengeType] = "LESS_SCREENTIME"
                it[packageNames] = null // All apps
                it[displayType] = "QUICK_JOIN"
                it[tags] = "wellness"
                it[sponsor] = "SleepWell"
                it[isActive] = true
            }
            
            // Challenge 6: Learning Streak - 1 month challenge
            Challenges.insert {
                it[title] = "30-Day Learning Streak"
                it[description] = "Spend at least 1 hour daily on educational apps (courses, reading, language learning) for 30 days straight!"
                it[reward] = "Lifelong Learner Badge + Course Discount (50% off)"
                it[prize] = "<div><strong>Rank 1:</strong> 100 points</div><div><strong>Rank 2:</strong> 50 points</div><div><strong>Rank 3:</strong> 10 points</div>"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + oneMonthInMs)
                it[thumbnail] = "https://fastly.picsum.photos/id/866/200/300.jpg?hmac=rcadCENKh4rD6MAp6V_ma-AyWv641M4iiOpe1RyFHeI"
                it[challengeType] = "MORE_SCREENTIME"
                it[packageNames] = "com.udemy.android,com.coursera.android,com.duolingo,com.khanacademy.android,com.babbel.mobile.android.en,com.memrise.android.memrisecompanion,com.amazon.kindle,com.google.android.apps.playbooks"
                it[displayType] = "FEATURE"
                it[tags] = "study,learning"
                it[sponsor] = "EduLearn"
                it[isActive] = true
            }
            
            // Challenge 7: Social Media Detox - 1 week challenge
            Challenges.insert {
                it[title] = "Social Media Detox Week"
                it[description] = "Limit social media apps (Instagram, Facebook, Twitter, TikTok) to less than 30 minutes per day for a week!"
                it[reward] = "Social Detox Champion Badge"
                it[prize] = "<div><strong>Rank 1:</strong> 100 points</div><div><strong>Rank 2:</strong> 50 points</div><div><strong>Rank 3:</strong> 10 points</div>"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + oneWeekInMs)
                it[thumbnail] = "https://fastly.picsum.photos/id/866/200/300.jpg?hmac=rcadCENKh4rD6MAp6V_ma-AyWv641M4iiOpe1RyFHeI"
                it[challengeType] = "LESS_SCREENTIME"
                it[packageNames] = "com.instagram.android,com.facebook.katana,com.twitter.android,com.zhiliaoapp.musically,com.snapchat.android,com.pinterest,com.linkedin.android,com.reddit.frontpage"
                it[displayType] = "TRENDING"
                it[tags] = "social media,wellness"
                it[sponsor] = "Digital Balance"
                it[isActive] = true
            }
            
            // Challenge 8: Deep Work Challenge - 2 weeks challenge
            Challenges.insert {
                it[title] = "Deep Work Challenge"
                it[description] = "Complete 40 hours of uninterrupted focus sessions (minimum 2 hours each) in 2 weeks. Master the art of deep work!"
                it[reward] = "Deep Work Master Badge + Productivity Tools Bundle"
                it[prize] = "<div><strong>Rank 1:</strong> 100 points</div><div><strong>Rank 2:</strong> 50 points</div><div><strong>Rank 3:</strong> 10 points</div>"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + twoWeeksInMs)
                it[thumbnail] = "https://fastly.picsum.photos/id/866/200/300.jpg?hmac=rcadCENKh4rD6MAp6V_ma-AyWv641M4iiOpe1RyFHeI"
                it[challengeType] = "MORE_SCREENTIME"
                it[packageNames] = null // All apps
                it[displayType] = "SPECIAL"
                it[tags] = "study,productivity"
                it[sponsor] = "DeepWork Labs"
                it[isActive] = true
            }
            
            // Challenge 9: Phone App Usage Reduction - 1 week challenge
            Challenges.insert {
                it[title] = "Phone App Usage Reduction Challenge"
                it[description] = "Reduce your overall phone app usage by 40% this week. Track all apps and minimize screen time to build healthier digital habits!"
                it[reward] = "Phone Freedom Badge + Digital Wellness Guide"
                it[prize] = "<div><strong>Rank 1:</strong> 100 points</div><div><strong>Rank 2:</strong> 50 points</div><div><strong>Rank 3:</strong> 10 points</div>"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + oneWeekInMs)
                it[thumbnail] = "https://fastly.picsum.photos/id/866/200/300.jpg?hmac=rcadCENKh4rD6MAp6V_ma-AyWv641M4iiOpe1RyFHeI"
                it[challengeType] = "LESS_SCREENTIME"
                it[packageNames] = null // All phone apps
                it[displayType] = "QUICK_JOIN"
                it[tags] = "wellness"
                it[sponsor] = "PhoneFree"
                it[isActive] = true
            }
            
            // Challenge 10: Chrome Less Usage Challenge - 1 week challenge
            Challenges.insert {
                it[title] = "Chrome Less Usage Challenge"
                it[description] = "Reduce your Chrome browser usage by 50% this week. Break the habit of endless browsing and reclaim your time!"
                it[reward] = "Browser Break Badge + Focus Extension (1 month)"
                it[prize] = "<div><strong>Rank 1:</strong> 100 points</div><div><strong>Rank 2:</strong> 50 points</div><div><strong>Rank 3:</strong> 10 points</div>"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + oneWeekInMs)
                it[thumbnail] = "https://fastly.picsum.photos/id/866/200/300.jpg?hmac=rcadCENKh4rD6MAp6V_ma-AyWv641M4iiOpe1RyFHeI"
                it[challengeType] = "LESS_SCREENTIME"
                it[packageNames] = "com.android.chrome,com.google.android.apps.chrome,com.chrome.beta,com.chrome.dev"
                it[displayType] = "TRENDING"
                it[tags] = "browser,wellness"
                it[sponsor] = "BrowserGuard"
                it[isActive] = true
            }
            
            // Challenge 11: Minimal Phone Usage - 2 weeks challenge
            Challenges.insert {
                it[title] = "Minimal Phone Usage Challenge"
                it[description] = "Keep your total phone app usage under 2 hours per day for 2 weeks. Focus on real-world activities and meaningful connections!"
                it[reward] = "Minimalist Badge + Mindfulness App Subscription (1 month)"
                it[prize] = "<div><strong>Rank 1:</strong> 100 points</div><div><strong>Rank 2:</strong> 50 points</div><div><strong>Rank 3:</strong> 10 points</div>"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + twoWeeksInMs)
                it[thumbnail] = "https://fastly.picsum.photos/id/866/200/300.jpg?hmac=rcadCENKh4rD6MAp6V_ma-AyWv641M4iiOpe1RyFHeI"
                it[challengeType] = "LESS_SCREENTIME"
                it[packageNames] = null // All phone apps
                it[displayType] = "FEATURE"
                it[tags] = "wellness"
                it[sponsor] = "Minimalist Life"
                it[isActive] = true
            }
            
            // Challenge 12: Chrome-Free Browsing Week - 1 week challenge
            Challenges.insert {
                it[title] = "Chrome-Free Browsing Week"
                it[description] = "Limit Chrome browser usage to less than 30 minutes per day for a week. Use alternative browsers or reduce browsing time significantly!"
                it[reward] = "Chrome-Free Champion Badge + Productivity Toolkit"
                it[prize] = "<div><strong>Rank 1:</strong> 100 points</div><div><strong>Rank 2:</strong> 50 points</div><div><strong>Rank 3:</strong> 10 points</div>"
                it[startTime] = now
                it[endTime] = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + oneWeekInMs)
                it[thumbnail] = "https://fastly.picsum.photos/id/866/200/300.jpg?hmac=rcadCENKh4rD6MAp6V_ma-AyWv641M4iiOpe1RyFHeI"
                it[challengeType] = "LESS_SCREENTIME"
                it[packageNames] = "com.android.chrome,com.google.android.apps.chrome,com.chrome.beta,com.chrome.dev"
                it[displayType] = "QUICK_JOIN"
                it[tags] = "browser,wellness"
                it[sponsor] = "BrowseLess"
                it[isActive] = true
            }
            
            println("✅ Seeded ${Challenges.selectAll().count()} challenges")
        }
    }
}

