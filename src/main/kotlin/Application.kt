package com.apptime.code

import DatabaseFactory
import com.apptime.code.admin.configureAdminRoutes
import com.apptime.code.challenges.configureChallengeRoutes
import com.apptime.code.clans.configureClanRoutes
import com.apptime.code.common.configureAuthentication
import com.apptime.code.common.configureHeaderTracking
import com.apptime.code.common.configureApiSecretKey
import com.apptime.code.consents.configureConsentRoutes
import com.apptime.code.feedback.configureFeedbackRoutes
import com.apptime.code.features.configureFeatureFlagsRoutes
import com.apptime.code.focus.configureFocusRoutes
import com.apptime.code.leaderboard.configureLeaderboardRoutes
import com.apptime.code.appstats.configureAppStatsRoutes
import com.apptime.code.location.configureLocationRoutes
import com.apptime.code.notifications.FirebaseNotificationService
import com.apptime.code.notifications.NotificationQueueService
import com.apptime.code.notifications.NotificationService
import com.apptime.code.notifications.NotificationRepository
import com.apptime.code.notifications.configureNotificationRoutes
import users.UserRepository
import kotlinx.coroutines.CoroutineScope
import com.apptime.code.referral.configureReferralRoutes
import com.apptime.code.rewards.configureRewardRoutes
import com.apptime.code.common.TranslationService
import users.configureUserRoutes
import usage.configureAppUsageEventRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.select
import java.net.URLEncoder
import java.net.URLDecoder
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.module() {
    // Initialize database
    DatabaseFactory.init()
    
    // Initialize Firebase for push notifications
    FirebaseNotificationService.initialize()
    
    // Initialize notification queue consumer
    val notificationRepository = NotificationRepository()
    val userRepository = UserRepository()
    val notificationService = NotificationService(notificationRepository, userRepository)
    
    // Start notification queue consumer (processes notifications asynchronously)
    // Application extends CoroutineScope, so we can use it directly
    val appScope = this
    try {
        NotificationQueueService.startConsumer(notificationService, appScope, maxConcurrentWorkers = 5)
        println("✅ Notification queue consumer started successfully with 5 workers")
    } catch (e: Exception) {
        println("❌ ERROR: Failed to start notification queue consumer: ${e.message}")
        e.printStackTrace()
    }
    
    // Initialize translation service (loads all translation files)
    val loadedLanguages = TranslationService.getAvailableLanguages()
    println("TranslationService initialized. Loaded languages: ${loadedLanguages.joinToString(", ")}")
    if (loadedLanguages.isEmpty()) {
        println("WARNING: No translations loaded! Check translation files in src/main/resources/translations/")
    }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        })
    }

    // Configure header tracking for all APIs (X-App-Language and X-App-Version)
    configureHeaderTracking()
    
    // Configure API secret key validation for all frontend API requests
    configureApiSecretKey()

    configureAuthentication()
    configureUserRoutes()
    configureLocationRoutes()
    configureConsentRoutes()
    configureFocusRoutes()
    configureAppUsageEventRoutes()
    configureLeaderboardRoutes()
    configureChallengeRoutes()
    configureRewardRoutes()
    configureReferralRoutes()
    configureClanRoutes(notificationService, userRepository)
    configureNotificationRoutes()
    configureFeedbackRoutes()
    configureFeatureFlagsRoutes()
    configureAdminRoutes()
    configureAppStatsRoutes()
    
    // Configure scheduled jobs (cronjobs)
    configureScheduledJobs()

    routing {
        // Serve admin dashboard
        staticResources("/admin", "admin") {
            default("index.html")
        }
        
        // Serve static assets (images, etc.)
        staticResources("/asset", "asset")
        
        get("/") {
            call.respond(
                mapOf(
                    "message" to "Ktor is working! 🚀",
                    "status" to "success",
                    "database" to "connected"
                )
            )
        }

        get("/health") {
            call.respond(
                mapOf(
                    "status" to "healthy",
                    "service" to "AppTimeBackend",
                    "database" to "connected"
                )
            )
        }
        
        // Android App Links verification
        route(".well-known") {
            get("/assetlinks.json") {
                // Set content type to application/json
                call.response.headers.append("Content-Type", "application/json")
                
                // Android App Links assetlinks.json
                val assetLinks = listOf(
                    mapOf(
                        "relation" to listOf(
                            "delegate_permission/common.handle_all_urls",
                            "delegate_permission/common.get_login_creds"
                        ),
                        "target" to mapOf(
                            "namespace" to "android_app",
                            "package_name" to "com.app.screentime",
                            "sha256_cert_fingerprints" to listOf(
                                "61:B2:93:19:28:69:7D:4B:51:24:AB:D0:83:A9:2C:3A:2E:BA:D6:0E:C7:30:95:70:E1:F9:0C:1C:2E:44:E6:E0"
                                // Add additional fingerprints here if needed (e.g., for debug/release variants)
                            )
                        )
                    )
                )
                
                call.respond(assetLinks)
            }
        }
        
        // Challenge share link handler - opens app or redirects to Play Store
        // Uses encoded token instead of revealing challenge ID and share code
        get("/challenge/{token}") {
            val tokenParam = call.parameters["token"]
            
            if (tokenParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid token")
                return@get
            }
            
            // Decode token to get challenge ID and share code
            val decoded = try {
                val decodedToken = URLDecoder.decode(tokenParam, "UTF-8")
                com.apptime.code.common.TokenEncoder.decodeChallengeShare(decodedToken)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid or corrupted token")
                return@get
            }
            
            if (decoded == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid token format")
                return@get
            }
            
            val (challengeId, shareCode) = decoded
            
            // Verify challenge exists
            val repository = com.apptime.code.challenges.ChallengeRepository()
            val challenge = repository.getChallengeById(challengeId)
            
            if (challenge == null) {
                call.respond(HttpStatusCode.NotFound, "Challenge not found")
                return@get
            }
            
            // Track click event
            try {
                val userAgent = call.request.headers["User-Agent"]
                val ipAddress = call.request.origin.remoteHost
                repository.trackShareEvent(
                    shareCode = shareCode,
                    eventType = "CLICK",
                    deviceId = null,
                    userAgent = userAgent,
                    ipAddress = ipAddress
                )
            } catch (e: Exception) {
                // Log but don't fail if tracking fails
                println("Failed to track click event: ${e.message}")
            }
            
            // Get base URL for fallback
            val scheme = call.request.origin.scheme
            val host = call.request.host()
            val port = call.request.port()
            val baseUrl = if (port == 80 || port == 443) {
                "$scheme://$host"
            } else {
                "$scheme://$host:$port"
            }
            
            // Build deeplink with token (app will decode it)
            val encodedToken = URLEncoder.encode(tokenParam, "UTF-8")
            val deeplink = "apptime://screen/challenge_detail/$encodedToken"
            val intentUrl = "intent://screen/challenge_detail/$encodedToken#Intent;scheme=apptime;package=com.app.screentime;end"
            val playStoreUrl = "https://play.google.com/store/apps/details?id=com.app.screentime"
            
            // HTML page that tries to open the app, then redirects to Play Store if app is not installed
            val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Join Challenge - AppTime</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            margin: 0;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #333;
        }
        .container {
            background: white;
            border-radius: 16px;
            padding: 32px;
            max-width: 400px;
            text-align: center;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        h1 {
            margin: 0 0 16px 0;
            color: #667eea;
            font-size: 24px;
        }
        p {
            margin: 16px 0;
            color: #666;
            line-height: 1.6;
        }
        .spinner {
            border: 3px solid #f3f3f3;
            border-top: 3px solid #667eea;
            border-radius: 50%;
            width: 40px;
            height: 40px;
            animation: spin 1s linear infinite;
            margin: 20px auto;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        .fallback-link {
            display: inline-block;
            margin-top: 20px;
            padding: 12px 24px;
            background: #667eea;
            color: white;
            text-decoration: none;
            border-radius: 8px;
            font-weight: 600;
        }
        .fallback-link:hover {
            background: #5568d3;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Opening Challenge...</h1>
        <p>If you have AppTime installed, the app will open automatically.</p>
        <div class="spinner"></div>
        <p style="font-size: 14px; color: #999;">If the app doesn't open, <a href="$playStoreUrl" class="fallback-link">Download AppTime</a></p>
    </div>
    
    <script>
        // Try to open the app using Android Intent URL
        function openApp() {
            // Try intent:// URL first (Android)
            window.location.href = "$intentUrl";
            
            // Fallback: Try universal link
            setTimeout(function() {
                window.location.href = "$deeplink";
            }, 500);
            
            // If app doesn't open within 2 seconds, redirect to Play Store
            setTimeout(function() {
                // Check if we're still on the page (app didn't open)
                if (document.hasFocus()) {
                    window.location.href = "$playStoreUrl";
                }
            }, 2000);
        }
        
        // Try to open app immediately
        openApp();
        
        // Also try on page visibility change (handles some edge cases)
        document.addEventListener('visibilitychange', function() {
            if (document.hidden) {
                // Page became hidden, app might have opened
                return;
            }
        });
        
        // Fallback button click handler
        document.querySelector('.fallback-link').addEventListener('click', function(e) {
            e.preventDefault();
            window.location.href = "$playStoreUrl";
        });
    </script>
</body>
</html>
            """.trimIndent()
            
            call.response.headers.append(HttpHeaders.ContentType, ContentType.Text.Html.toString())
            call.respondText(html)
        }
        
        // Public route for clan share links
        // Uses encoded token instead of revealing clan ID and share code
        get("/clan/{token}") {
            val tokenParam = call.parameters["token"]
            
            if (tokenParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid token")
                return@get
            }
            
            // Decode token to get clan ID and share code
            val decoded = try {
                val decodedToken = URLDecoder.decode(tokenParam, "UTF-8")
                com.apptime.code.common.TokenEncoder.decodeClanShare(decodedToken)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid or corrupted token")
                return@get
            }
            
            if (decoded == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid token format")
                return@get
            }
            
            val (clanId, shareCode) = decoded
            
            // Verify clan exists
            val clanRepository = com.apptime.code.clans.ClanRepository()
            val clan = clanRepository.getClanById(clanId)
            
            if (clan == null) {
                call.respond(HttpStatusCode.NotFound, "Clan not found")
                return@get
            }
            
            // Track click event
            try {
                val userAgent = call.request.headers["User-Agent"]
                val ipAddress = call.request.origin.remoteHost
                clanRepository.trackClanShareEvent(
                    shareCode = shareCode,
                    eventType = "CLICK",
                    joinerUserId = null,
                    deviceId = null,
                    userAgent = userAgent,
                    ipAddress = ipAddress
                )
            } catch (e: Exception) {
                // Log but don't fail if tracking fails
                println("Failed to track click event: ${e.message}")
            }
            
            // Get base URL for fallback
            val scheme = call.request.origin.scheme
            val host = call.request.host()
            val port = call.request.port()
            val baseUrl = if (port == 80 || port == 443) {
                "$scheme://$host"
            } else {
                "$scheme://$host:$port"
            }
            
            // Build deeplink with token (app will decode it)
            val encodedToken = URLEncoder.encode(tokenParam, "UTF-8")
            val deeplink = "apptime://screen/clan_detail/$encodedToken"
            val intentUrl = "intent://screen/clan_detail/$encodedToken#Intent;scheme=apptime;package=com.app.screentime;end"
            val playStoreUrl = "https://play.google.com/store/apps/details?id=com.app.screentime"
            
            // HTML page that tries to open the app, then redirects to Play Store if app is not installed
            val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Join Clan - AppTime</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            margin: 0;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #333;
        }
        .container {
            background: white;
            border-radius: 16px;
            padding: 32px;
            max-width: 400px;
            text-align: center;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
        }
        h1 {
            margin: 0 0 16px 0;
            color: #667eea;
        }
        p {
            margin: 16px 0;
            color: #666;
        }
        .spinner {
            border: 3px solid #f3f3f3;
            border-top: 3px solid #667eea;
            border-radius: 50%;
            width: 40px;
            height: 40px;
            animation: spin 1s linear infinite;
            margin: 20px auto;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        .fallback-link {
            color: #667eea;
            text-decoration: none;
            font-weight: 600;
        }
        .fallback-link:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Join Clan</h1>
        <p>Opening AppTime...</p>
        <p>If you have AppTime installed, the app will open automatically.</p>
        <div class="spinner"></div>
        <p style="font-size: 14px; color: #999;">If the app doesn't open, <a href="$playStoreUrl" class="fallback-link">Download AppTime</a></p>
    </div>
    
    <script>
        // Try to open the app using Android Intent URL
        function openApp() {
            // Try intent:// URL first (Android)
            window.location.href = "$intentUrl";
            
            // Fallback: Try universal link
            setTimeout(function() {
                window.location.href = "$deeplink";
            }, 500);
            
            // If app doesn't open within 2 seconds, redirect to Play Store
            setTimeout(function() {
                // Check if we're still on the page (app didn't open)
                if (document.hasFocus()) {
                    window.location.href = "$playStoreUrl";
                }
            }, 2000);
        }
        
        // Try to open app immediately
        openApp();
        
        // Also try on page visibility change (handles some edge cases)
        document.addEventListener('visibilitychange', function() {
            if (document.hidden) {
                // Page became hidden, app might have opened
                return;
            }
        });
        
        // Fallback button click handler
        document.querySelector('.fallback-link').addEventListener('click', function(e) {
            e.preventDefault();
            window.location.href = "$playStoreUrl";
        });
    </script>
</body>
</html>
            """.trimIndent()
            
            call.response.headers.append(HttpHeaders.ContentType, ContentType.Text.Html.toString())
            call.respondText(html)
        }
        
        // Referral share link handler - opens app or redirects to Play Store
        // Uses encoded token instead of revealing referral code
        get("/referral/{token}") {
            val tokenParam = call.parameters["token"]
            
            if (tokenParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid token")
                return@get
            }
            
            // Decode token to get referral code
            val referralCode = try {
                val decodedToken = URLDecoder.decode(tokenParam, "UTF-8")
                com.apptime.code.common.TokenEncoder.decodeReferral(decodedToken)
                    ?: throw IllegalArgumentException("Invalid token format")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid or corrupted token")
                return@get
            }
            
            // Verify referral code exists (using transaction directly for synchronous access)
            val referrerId = org.jetbrains.exposed.sql.transactions.transaction {
                com.apptime.code.referral.UserReferralCodes.select {
                    com.apptime.code.referral.UserReferralCodes.referralCode eq referralCode
                }
                .map { it[com.apptime.code.referral.UserReferralCodes.userId] }
                .firstOrNull()
            }
            
            if (referrerId == null) {
                call.respond(HttpStatusCode.NotFound, "Referral code not found")
                return@get
            }
            
            // Get base URL for fallback
            val scheme = call.request.origin.scheme
            val host = call.request.host()
            val port = call.request.port()
            val baseUrl = if (port == 80 || port == 443) {
                "$scheme://$host"
            } else {
                "$scheme://$host:$port"
            }
            
            // Build deeplink with token (app will decode it)
            val encodedToken = URLEncoder.encode(tokenParam, "UTF-8")
            val deeplink = "apptime://screen/referral/$encodedToken"
            val intentUrl = "intent://screen/referral/$encodedToken#Intent;scheme=apptime;package=com.app.screentime;end"
            val playStoreUrl = "https://play.google.com/store/apps/details?id=com.app.screentime"
            
            // HTML page that tries to open the app, then redirects to Play Store if app is not installed
            val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Join AppTime - Referral</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            margin: 0;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #333;
        }
        .container {
            background: white;
            border-radius: 16px;
            padding: 32px;
            max-width: 400px;
            text-align: center;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        h1 {
            margin: 0 0 16px 0;
            color: #667eea;
            font-size: 24px;
        }
        p {
            margin: 16px 0;
            color: #666;
            line-height: 1.6;
        }
        .spinner {
            border: 3px solid #f3f3f3;
            border-top: 3px solid #667eea;
            border-radius: 50%;
            width: 40px;
            height: 40px;
            animation: spin 1s linear infinite;
            margin: 20px auto;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        .fallback-link {
            display: inline-block;
            margin-top: 20px;
            padding: 12px 24px;
            background: #667eea;
            color: white;
            text-decoration: none;
            border-radius: 8px;
            font-weight: 600;
        }
        .fallback-link:hover {
            background: #5568d3;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Opening AppTime...</h1>
        <p>If you have AppTime installed, the app will open automatically.</p>
        <div class="spinner"></div>
        <p style="font-size: 14px; color: #999;">If the app doesn't open, <a href="$playStoreUrl" class="fallback-link">Download AppTime</a></p>
    </div>
    
    <script>
        // Try to open the app using Android Intent URL
        function openApp() {
            // Try intent:// URL first (Android)
            window.location.href = "$intentUrl";
            
            // Fallback: Try universal link
            setTimeout(function() {
                window.location.href = "$deeplink";
            }, 500);
            
            // If app doesn't open within 2 seconds, redirect to Play Store
            setTimeout(function() {
                // Check if we're still on the page (app didn't open)
                if (document.hasFocus()) {
                    window.location.href = "$playStoreUrl";
                }
            }, 2000);
        }
        
        // Try to open app immediately
        openApp();
        
        // Also try on page visibility change (handles some edge cases)
        document.addEventListener('visibilitychange', function() {
            if (document.hidden) {
                // Page became hidden, app might have opened
                return;
            }
        });
        
        // Fallback button click handler
        document.querySelector('.fallback-link').addEventListener('click', function(e) {
            e.preventDefault();
            window.location.href = "$playStoreUrl";
        });
    </script>
</body>
</html>
            """.trimIndent()
            
            call.response.headers.append(HttpHeaders.ContentType, ContentType.Text.Html.toString())
            call.respondText(html)
        }
    }
}
