# Translation System Documentation

## Overview

The API now supports multi-language responses based on the `X-App-Language` header sent by clients. All API response messages are automatically translated to the requested language.

## How It Works

1. **Language Detection**: The system reads the `X-App-Language` header from incoming requests
2. **Translation Lookup**: Messages are looked up in translation files based on the language code
3. **Fallback**: If a translation is not found for the requested language, it falls back to English (default)

## Supported Languages

Currently supported languages:
- `en` - English (default)
- `es` - Spanish
- `fr` - French
- `hi` - Hindi

## Usage

### Client Side

Include the `X-App-Language` header in your API requests:

```bash
# English (default)
curl -H "X-App-Language: en" https://your-api.com/api/admin/stats

# Spanish
curl -H "X-App-Language: es" https://your-api.com/api/admin/stats

# French
curl -H "X-App-Language: fr" https://your-api.com/api/admin/stats

# Hindi
curl -H "X-App-Language: hi" https://your-api.com/api/admin/stats
```

### Server Side (Adding New Messages)

1. **Add Message Key**: Add a new constant in `MessageKeys` object:
   ```kotlin
   const val MY_NEW_MESSAGE = "my.new.message"
   ```

2. **Add Translations**: Add the translation to all language files:
   - `src/main/resources/translations/en.json`
   - `src/main/resources/translations/es.json`
   - `src/main/resources/translations/fr.json`
   - `src/main/resources/translations/hi.json`

3. **Use in Routes**: Use the message key in your route handlers:
   ```kotlin
   call.respondApi(data, messageKey = MessageKeys.MY_NEW_MESSAGE)
   // or
   call.respondError(HttpStatusCode.BadRequest, messageKey = MessageKeys.MY_NEW_MESSAGE)
   ```

## Example Response

### English (en)
```json
{
  "success": true,
  "status": 200,
  "data": {...},
  "message": "Login successful",
  "timestamp": "2024-01-15T10:00:00Z"
}
```

### Spanish (es)
```json
{
  "success": true,
  "status": 200,
  "data": {...},
  "message": "Inicio de sesión exitoso",
  "timestamp": "2024-01-15T10:00:00Z"
}
```

### French (fr)
```json
{
  "success": true,
  "status": 200,
  "data": {...},
  "message": "Connexion réussie",
  "timestamp": "2024-01-15T10:00:00Z"
}
```

### Hindi (hi)
```json
{
  "success": true,
  "status": 200,
  "data": {...},
  "message": "लॉगिन सफल",
  "timestamp": "2024-01-15T10:00:00Z"
}
```

## Adding New Languages

1. Create a new JSON file in `src/main/resources/translations/` with the language code (e.g., `de.json` for German)
2. Copy the structure from `en.json` and translate all messages
3. The system will automatically load the new language file on startup

## Message Keys Reference

All available message keys are defined in `MessageKeys` object:

- Admin: `ADMIN_LOGIN_SUCCESS`, `ADMIN_LOGIN_INVALID`, `ADMIN_TOKEN_VALID`, etc.
- Challenges: `CHALLENGES_RETRIEVED`, `CHALLENGE_CREATED`, `CHALLENGE_NOT_FOUND`, etc.
- Users: `USERS_RETRIEVED`, `USER_UPDATED`, `USER_BLOCKED`, etc.
- Rewards: `REWARDS_RETRIEVED`, `REWARD_RETRIEVED`, etc.
- Consents: `CONSENT_TEMPLATES_RETRIEVED`, `CONSENT_TEMPLATE_CREATED`, etc.
- Catalog: `CATALOG_RETRIEVED`, `CATALOG_ITEM_CREATED`, etc.
- Transactions: `TRANSACTIONS_RETRIEVED`, `TRANSACTION_STATUS_UPDATED`, etc.
- Errors: `INVALID_REQUEST`, `INTERNAL_SERVER_ERROR`, `UNAUTHORIZED`, etc.

## Implementation Details

- **TranslationService**: Singleton object that loads and caches translations
- **Extensions**: Updated `respondApi` and `respondError` functions support `messageKey` parameter
- **Fallback**: If translation is missing, falls back to English, then to the provided default message, then to the key itself

## Notes

- Translations are loaded once at application startup
- If a language is not supported, English is used as fallback
- Error messages with dynamic content (e.g., exception messages) will still include the original English text for debugging purposes

