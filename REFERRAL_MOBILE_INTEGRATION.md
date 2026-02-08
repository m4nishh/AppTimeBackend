# Referral Service - Mobile App Integration Guide

This guide helps you integrate the referral service into your Android/iOS mobile app.

## 📱 User Flows

### Flow 1: Share Referral Code

```
User Profile/Settings Screen
    ↓
[Referrals Button]
    ↓
Referrals Screen
    ↓
Display: "Your Referral Code: ABC123XYZ"
    ↓
[Share Button] → Share via WhatsApp, SMS, Email, etc.
```

### Flow 2: Apply Referral Code (New User)

```
App Launch (First Time)
    ↓
Welcome/Onboarding Screen
    ↓
[Optional] "Have a referral code?"
    ↓
User enters code: "ABC123XYZ"
    ↓
API: POST /api/referrals/apply
    ↓
Show success: "You'll receive 200 coins!"
    ↓
Continue onboarding
    ↓
Complete first challenge
    ↓
API: POST /api/referrals/complete (automatic)
    ↓
Show notification: "Welcome bonus! 200 coins added"
```

### Flow 3: View Referral Stats

```
Referrals Screen
    ↓
Display:
- Your Code: ABC123XYZ
- Total Referrals: 5
- Coins Earned: 2,500
- Pending: 2 | Completed: 3
    ↓
[View Details] → List of referrals with status
[Leaderboard] → Top referrers
```

## 🔌 API Integration

### 1. Get User's Referral Code

**Kotlin (Android)**
```kotlin
// API Service
interface ReferralApiService {
    @GET("api/referrals/my-code")
    suspend fun getMyReferralCode(
        @Header("Authorization") token: String
    ): ApiResponse<UserReferralCode>
}

// ViewModel
class ReferralsViewModel : ViewModel() {
    fun loadReferralCode() {
        viewModelScope.launch {
            try {
                val response = apiService.getMyReferralCode("Bearer $token")
                if (response.success) {
                    _referralCode.value = response.data.referralCode
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}

// UI (Compose)
@Composable
fun ReferralsScreen(viewModel: ReferralsViewModel) {
    val referralCode by viewModel.referralCode.collectAsState()
    
    Column {
        Text("Your Referral Code")
        Text(
            text = referralCode ?: "Loading...",
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold
        )
        
        Button(onClick = { shareReferralCode(referralCode) }) {
            Text("Share Code")
        }
    }
}
```

**Swift (iOS)**
```swift
// API Service
class ReferralService {
    func getMyReferralCode(completion: @escaping (Result<UserReferralCode, Error>) -> Void) {
        let url = URL(string: "\(baseURL)/api/referrals/my-code")!
        var request = URLRequest(url: url)
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            // Handle response
        }.resume()
    }
}

// ViewModel
class ReferralsViewModel: ObservableObject {
    @Published var referralCode: String = ""
    
    func loadReferralCode() {
        referralService.getMyReferralCode { result in
            switch result {
            case .success(let data):
                self.referralCode = data.referralCode
            case .failure(let error):
                print("Error: \(error)")
            }
        }
    }
}

// View
struct ReferralsView: View {
    @StateObject var viewModel = ReferralsViewModel()
    
    var body: some View {
        VStack {
            Text("Your Referral Code")
            Text(viewModel.referralCode)
                .font(.largeTitle)
                .bold()
            
            Button("Share Code") {
                shareReferralCode(viewModel.referralCode)
            }
        }
        .onAppear {
            viewModel.loadReferralCode()
        }
    }
}
```

### 2. Share Referral Code

**Android**
```kotlin
fun shareReferralCode(context: Context, code: String) {
    val shareText = """
        Join me on AppTime and get 200 coins! 
        Use my referral code: $code
        Download: https://play.google.com/store/apps/details?id=com.app.screentime
    """.trimIndent()
    
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    
    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
}

// Or share via specific apps
fun shareViaWhatsApp(context: Context, code: String) {
    val shareText = "Join me on AppTime! Use code: $code"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        setPackage("com.whatsapp")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // WhatsApp not installed
        shareReferralCode(context, code)
    }
}
```

**iOS**
```swift
func shareReferralCode(_ code: String) {
    let shareText = """
        Join me on AppTime and get 200 coins!
        Use my referral code: \(code)
        Download: https://apps.apple.com/app/apptime/id123456789
        """
    
    let activityVC = UIActivityViewController(
        activityItems: [shareText],
        applicationActivities: nil
    )
    
    // Present activity view controller
    UIApplication.shared.windows.first?.rootViewController?
        .present(activityVC, animated: true)
}
```

### 3. Apply Referral Code (Onboarding)

**Android**
```kotlin
// Onboarding Screen
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    var referralCode by remember { mutableStateOf("") }
    var showReferralInput by remember { mutableStateOf(false) }
    
    Column {
        // ... other onboarding content
        
        TextButton(onClick = { showReferralInput = true }) {
            Text("Have a referral code?")
        }
        
        if (showReferralInput) {
            OutlinedTextField(
                value = referralCode,
                onValueChange = { referralCode = it.uppercase() },
                label = { Text("Referral Code") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = { viewModel.applyReferralCode(referralCode) },
                enabled = referralCode.isNotBlank()
            ) {
                Text("Apply Code")
            }
        }
    }
}

// ViewModel
class OnboardingViewModel : ViewModel() {
    fun applyReferralCode(code: String) {
        viewModelScope.launch {
            try {
                val response = apiService.applyReferralCode(
                    ApplyReferralCodeRequest(referralCode = code)
                )
                
                if (response.success) {
                    // Show success message
                    _message.value = "Success! You'll receive ${response.data.bonusCoins} coins"
                    
                    // Continue onboarding
                    _navigateNext.value = true
                } else {
                    _error.value = response.error
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
```

**iOS**
```swift
// Onboarding View
struct OnboardingView: View {
    @StateObject var viewModel = OnboardingViewModel()
    @State private var referralCode = ""
    @State private var showReferralInput = false
    
    var body: some View {
        VStack {
            // ... other onboarding content
            
            Button("Have a referral code?") {
                showReferralInput.toggle()
            }
            
            if showReferralInput {
                TextField("Referral Code", text: $referralCode)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .autocapitalization(.allCharacters)
                
                Button("Apply Code") {
                    viewModel.applyReferralCode(referralCode)
                }
                .disabled(referralCode.isEmpty)
            }
        }
    }
}

// ViewModel
class OnboardingViewModel: ObservableObject {
    func applyReferralCode(_ code: String) {
        referralService.applyReferralCode(code) { result in
            switch result {
            case .success(let response):
                // Show success message
                self.showMessage("Success! You'll receive \(response.bonusCoins) coins")
                self.navigateNext()
            case .failure(let error):
                self.showError(error.localizedDescription)
            }
        }
    }
}
```

### 4. Complete Referral (Automatic)

**Trigger Points** (choose one or more):
- After completing onboarding
- After first challenge completion
- After first 24 hours of usage
- After earning first 100 coins

**Android**
```kotlin
// After user completes required action
class ChallengeViewModel : ViewModel() {
    fun onChallengeCompleted() {
        viewModelScope.launch {
            // ... other challenge completion logic
            
            // Check if user needs to complete referral
            if (userPrefs.hasAppliedReferralCode() && !userPrefs.hasCompletedReferral()) {
                completeReferral()
            }
        }
    }
    
    private suspend fun completeReferral() {
        try {
            val response = apiService.completeReferral(
                CompleteReferralRequest(referredUserId = userId)
            )
            
            if (response.success) {
                // Mark as completed
                userPrefs.setReferralCompleted(true)
                
                // Show notification or dialog
                showWelcomeBonus(response.data.referredReward)
            }
        } catch (e: Exception) {
            // Log error but don't block user
            Log.e("Referral", "Failed to complete referral", e)
        }
    }
}
```

**iOS**
```swift
// After user completes required action
class ChallengeViewModel: ObservableObject {
    func onChallengeCompleted() {
        // ... other challenge completion logic
        
        // Check if user needs to complete referral
        if UserDefaults.hasAppliedReferralCode && !UserDefaults.hasCompletedReferral {
            completeReferral()
        }
    }
    
    private func completeReferral() {
        referralService.completeReferral(userId: userId) { result in
            switch result {
            case .success(let response):
                // Mark as completed
                UserDefaults.hasCompletedReferral = true
                
                // Show notification or dialog
                self.showWelcomeBonus(response.referredReward)
            case .failure(let error):
                // Log error but don't block user
                print("Failed to complete referral: \(error)")
            }
        }
    }
}
```

### 5. Display Referral Stats

**Android**
```kotlin
@Composable
fun ReferralStatsScreen(viewModel: ReferralsViewModel) {
    val info by viewModel.referralInfo.collectAsState()
    
    Column(modifier = Modifier.padding(16.dp)) {
        // Header
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Your Referral Code", style = MaterialTheme.typography.subtitle1)
                Text(
                    text = info?.referralCode ?: "Loading...",
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold
                )
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { shareCode(info?.referralCode) }) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share")
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    Button(onClick = { copyToClipboard(info?.referralCode) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Copy")
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Stats
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCard(
                title = "Total Referrals",
                value = info?.totalReferrals?.toString() ?: "0",
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            StatCard(
                title = "Coins Earned",
                value = info?.totalCoinsEarned?.toString() ?: "0",
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCard(
                title = "Pending",
                value = info?.pendingReferrals?.toString() ?: "0",
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            StatCard(
                title = "Completed",
                value = info?.completedReferrals?.toString() ?: "0",
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Referrals List
        Text("Your Referrals", style = MaterialTheme.typography.h6)
        
        LazyColumn {
            items(info?.referrals ?: emptyList()) { referral ->
                ReferralItem(referral)
            }
        }
    }
}

@Composable
fun ReferralItem(referral: ReferralDetails) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = referral.referredUsername ?: "User",
                    style = MaterialTheme.typography.subtitle1
                )
                Text(
                    text = formatDate(referral.createdAt),
                    style = MaterialTheme.typography.caption
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(referral.status)
                if (referral.coinsEarned > 0) {
                    Text(
                        text = "+${referral.coinsEarned} coins",
                        style = MaterialTheme.typography.caption,
                        color = Color.Green
                    )
                }
            }
        }
    }
}
```

### 6. Leaderboard

**Android**
```kotlin
@Composable
fun ReferralLeaderboardScreen(viewModel: ReferralsViewModel) {
    val leaderboard by viewModel.leaderboard.collectAsState()
    
    Column {
        // My Rank Card
        leaderboard?.myRank?.let { rank ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                backgroundColor = MaterialTheme.colors.primary
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your Rank", color = Color.White)
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.h3,
                        color = Color.White
                    )
                    leaderboard?.myStats?.let { stats ->
                        Text(
                            text = "${stats.totalReferrals} referrals • ${stats.totalCoinsEarned} coins",
                            color = Color.White
                        )
                    }
                }
            }
        }
        
        // Leaderboard List
        Text(
            "Top Referrers",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(16.dp)
        )
        
        LazyColumn {
            items(leaderboard?.leaderboard ?: emptyList()) { entry ->
                LeaderboardItem(entry)
            }
        }
    }
}

@Composable
fun LeaderboardItem(entry: ReferralLeaderboardEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(getRankColor(entry.rank), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${entry.rank}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column {
                Text(
                    text = entry.username ?: "User",
                    style = MaterialTheme.typography.subtitle1
                )
                Text(
                    text = "${entry.totalReferrals} referrals",
                    style = MaterialTheme.typography.caption
                )
            }
        }
        
        Text(
            text = "${entry.totalCoinsEarned} coins",
            style = MaterialTheme.typography.subtitle2,
            color = MaterialTheme.colors.primary
        )
    }
}
```

## 🎨 UI/UX Best Practices

### 1. Make Referral Code Easy to Find
- Add "Referrals" button in main navigation
- Show referral code in user profile
- Display stats prominently

### 2. Make Sharing Easy
- One-tap share button
- Support multiple share methods (WhatsApp, SMS, Email)
- Pre-fill share text with code and download link

### 3. Show Value Proposition
- Display coin rewards clearly
- Show what users can do with coins
- Highlight successful referrals

### 4. Gamification
- Show leaderboard rankings
- Add badges for referral milestones
- Celebrate referral success with animations

### 5. Clear Status Indicators
- Use color-coded badges (Pending, Completed, Rewarded)
- Show progress bars for pending referrals
- Display notifications for successful referrals

## 📊 Analytics Events

Track these events for analytics:

```kotlin
// Android (Firebase Analytics)
analytics.logEvent("referral_code_viewed") {
    param("user_id", userId)
}

analytics.logEvent("referral_code_shared") {
    param("user_id", userId)
    param("share_method", "whatsapp") // or "sms", "email", etc.
}

analytics.logEvent("referral_code_applied") {
    param("user_id", userId)
    param("referrer_id", referrerId)
}

analytics.logEvent("referral_completed") {
    param("user_id", userId)
    param("referrer_id", referrerId)
    param("coins_earned", coinsEarned)
}

analytics.logEvent("referral_leaderboard_viewed") {
    param("user_id", userId)
    param("user_rank", rank)
}
```

## 🔔 Push Notifications

Handle referral notifications:

```kotlin
// Android (FCM)
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        when (message.data["type"]) {
            "referral_success" -> {
                showReferralSuccessNotification(
                    referredUsername = message.data["referred_username"],
                    coinsEarned = message.data["coins_earned"]
                )
            }
            "welcome_bonus" -> {
                showWelcomeBonusNotification(
                    coinsEarned = message.data["coins_earned"]
                )
            }
        }
    }
}
```

## ✅ Checklist

- [ ] Add referrals screen to app navigation
- [ ] Implement "Get My Code" API call
- [ ] Add share functionality
- [ ] Add referral code input in onboarding
- [ ] Implement "Apply Code" API call
- [ ] Add automatic referral completion trigger
- [ ] Display referral stats
- [ ] Implement leaderboard
- [ ] Handle push notifications
- [ ] Add analytics events
- [ ] Test complete workflow
- [ ] Handle error cases
- [ ] Add loading states
- [ ] Implement offline support (cache)

---

**Happy Coding! 🚀**

