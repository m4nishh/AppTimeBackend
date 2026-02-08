# Admin Notification Management

## Overview

The admin portal now includes a comprehensive notification management system that allows administrators to send push notifications to users directly from the web interface.

## Features

### 1. Single User Notifications
Send targeted notifications to specific users by their User ID.

### 2. Broadcast Notifications
Send notifications to all registered users at once with a single click.

### 3. Customizable Content
- **Title**: Eye-catching notification title
- **Message**: Detailed notification text
- **Type**: Categorize notifications (general, announcement, update, promotion, reminder)
- **Image**: Optional image URL to make notifications more engaging
- **Deeplink**: Optional app deeplink for direct navigation

## How to Use

### Accessing the Notifications Tab

1. Log in to the admin portal
2. Click on the **🔔 Notifications** tab in the navigation bar

### Sending a Notification to a Specific User

1. Navigate to the Notifications tab
2. Enter the **User ID** of the recipient
3. Fill in the **Title** (required)
4. Fill in the **Message** (required)
5. Select a **Type** (optional, defaults to "general")
6. Add an **Image URL** (optional)
7. Add a **Deeplink** (optional)
8. Click **📤 Send Notification**

### Broadcasting to All Users

1. Navigate to the Notifications tab
2. Check the **"Broadcast to All Users"** checkbox
3. Fill in the **Title** (required)
4. Fill in the **Message** (required)
5. Select a **Type** (optional)
6. Add an **Image URL** (optional)
7. Add a **Deeplink** (optional)
8. Click **📤 Send Notification**
9. Confirm the broadcast action in the popup dialog

**⚠️ Warning**: Broadcasting sends notifications to ALL registered users. Use this feature carefully!

## API Endpoint

### POST `/api/admin/notifications/send`

Send a notification to a specific user or broadcast to all users.

**Request Body:**
```json
{
  "userId": "user123",           // Required if sendToAll is false
  "title": "Welcome!",           // Required
  "text": "Welcome to AppTime",  // Required
  "type": "announcement",        // Optional (general, announcement, update, promotion, reminder)
  "image": "/asset/welcome.png", // Optional
  "deeplink": "app://home",      // Optional
  "sendToAll": false             // Set to true for broadcast
}
```

**Response (Single User):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "success": true,
    "message": "Notification sent successfully to user user123",
    "sentCount": 1,
    "failedCount": 0
  },
  "message": "Notification sent successfully",
  "timestamp": "2024-01-15T10:00:00Z"
}
```

**Response (Broadcast):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "success": true,
    "message": "Broadcast notification sent to 150 users (2 failed)",
    "sentCount": 150,
    "failedCount": 2
  },
  "message": "Notification sent successfully",
  "timestamp": "2024-01-15T10:00:00Z"
}
```

**Error Response:**
```json
{
  "success": false,
  "status": 400,
  "message": "Title and text are required",
  "error": {
    "code": "error.invalid_request",
    "message": "Title and text are required"
  },
  "timestamp": "2024-01-15T10:00:00Z"
}
```

## Notification Types

| Type | Use Case | Example |
|------|----------|---------|
| `general` | General notifications | "Your profile has been updated" |
| `announcement` | Platform announcements | "New feature available!" |
| `update` | App or system updates | "Version 2.0 is now available" |
| `promotion` | Special offers or promotions | "Get 50% off on premium features" |
| `reminder` | Reminders and alerts | "Complete your daily challenge" |

## Deeplink Examples

Deeplinks allow users to navigate directly to specific screens when tapping the notification:

- `app://home` - Home screen
- `app://challenges` - Challenges list
- `app://challenge/123` - Specific challenge (ID: 123)
- `app://rewards` - Rewards screen
- `app://rewards/coins` - Coins balance
- `app://profile` - User profile
- `app://usage` - App usage stats
- `app://focus` - Focus mode

## Using Images with Notifications

### Using Assets from the Asset Library

1. Upload images using the **Assets** tab
2. Use the asset URL in the notification: `/asset/filename.png`

Example:
```
Image URL: /asset/welcome_banner.png
```

### Using External Images

You can also use external image URLs:
```
Image URL: https://example.com/images/notification.png
```

**Supported formats**: PNG, JPG, JPEG, GIF, WEBP

## Best Practices

### 1. Clear and Concise Titles
- Keep titles under 50 characters
- Use emojis sparingly for visual appeal (e.g., "🎉 New Challenge!")

### 2. Meaningful Messages
- Keep messages clear and action-oriented
- Include relevant details but keep it brief
- Use proper grammar and spelling

### 3. Appropriate Timing
- Avoid sending notifications during late night hours
- Consider user time zones for broadcasts
- Don't spam users with too many notifications

### 4. Use Deeplinks Wisely
- Always provide deeplinks for actionable notifications
- Test deeplinks before sending to all users
- Ensure the linked content exists in the app

### 5. Test Before Broadcasting
- Send a test notification to yourself first
- Verify the notification appears correctly
- Check that deeplinks work as expected

## Error Handling

The system handles various error scenarios gracefully:

- **Missing Required Fields**: Returns error message indicating which fields are required
- **Invalid User ID**: Returns error if user doesn't exist
- **Network Failures**: Continues sending to other users in case of individual failures during broadcast
- **No Firebase Token**: Notification is saved in database but push notification is not sent (logged)

## Notification Delivery

### How It Works

1. **Database Storage**: All notifications are stored in the database
2. **Push Notification**: If user has Firebase token registered, push notification is sent
3. **In-App Retrieval**: Users can retrieve notifications via API even if push fails

### Delivery Status

During broadcast:
- `sentCount`: Number of successfully sent notifications
- `failedCount`: Number of failed notifications

Failed notifications are typically due to:
- User has no Firebase token registered
- Firebase service errors
- Network connectivity issues

## Security

- Only authenticated admin users can access this feature
- Broadcast requires explicit confirmation
- All notification activities can be audited through logs

## Translation Support

Notification messages support the following languages (via X-App-Language header):
- English (en)
- Spanish (es)
- French (fr)
- Hindi (hi)
- German (de)
- Portuguese (pt)
- Italian (it)
- Japanese (ja)
- Korean (ko)
- Chinese (zh-rCN, zh-rTW)
- Arabic (ar)
- And many more...

## Troubleshooting

### Notification Not Received

1. **Check User Has Firebase Token**: Verify user has registered their device
2. **Check Database**: Notification should be stored even if push fails
3. **Check Logs**: Review server logs for any errors
4. **Verify Firebase Configuration**: Ensure Firebase is properly initialized

### Broadcast Too Slow

- Broadcasting to large numbers of users may take time
- Users are processed in batches of 100
- Consider scheduling broadcasts during off-peak hours

### Deeplink Not Working

- Verify the deeplink format matches app's routing scheme
- Test deeplinks in development environment first
- Check app version compatibility

## Examples

### Example 1: Welcome Notification
```json
{
  "userId": "new_user_123",
  "title": "Welcome to AppTime! 🎉",
  "text": "Start your journey to better screen time management today!",
  "type": "general",
  "image": "/asset/welcome.png",
  "deeplink": "app://home",
  "sendToAll": false
}
```

### Example 2: Challenge Announcement (Broadcast)
```json
{
  "title": "New Challenge Available! 🏆",
  "text": "Join our 7-day digital detox challenge and win amazing rewards!",
  "type": "announcement",
  "image": "/asset/challenge_banner.png",
  "deeplink": "app://challenges",
  "sendToAll": true
}
```

### Example 3: Promotional Notification
```json
{
  "userId": "user_456",
  "title": "Limited Time Offer! 💎",
  "text": "Get 50% extra coins on your next purchase. Valid for 24 hours only!",
  "type": "promotion",
  "image": "/asset/promo_coins.png",
  "deeplink": "app://rewards/coins",
  "sendToAll": false
}
```

### Example 4: System Update
```json
{
  "title": "App Update Available 🚀",
  "text": "Version 2.0 brings new features and improvements. Update now!",
  "type": "update",
  "deeplink": "app://settings",
  "sendToAll": true
}
```

## Support

For issues or questions about the notification system, contact the development team or refer to the main API documentation.

