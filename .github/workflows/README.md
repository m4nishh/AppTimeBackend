# GitHub Actions Workflows

## Ngrok Deployment

This workflow automatically deploys your Ktor server to ngrok whenever you push to the `main` or `master` branch.

### Setup Instructions

1. **Get your Ngrok Authtoken:**
   - Sign up at https://ngrok.com (free account works)
   - Go to https://dashboard.ngrok.com/get-started/your-authtoken
   - Copy your authtoken

2. **Add the token to GitHub Secrets:**
   - Go to your repository on GitHub
   - Navigate to: Settings → Secrets and variables → Actions
   - Click "New repository secret"
   - Name: `NGROK_AUTHTOKEN`
   - Value: Paste your ngrok authtoken
   - Click "Add secret"

3. **Push to trigger:**
   - Push to `main` or `master` branch to trigger automatically
   - Or manually trigger from the Actions tab → "Deploy to Ngrok" → "Run workflow"

### How it works

1. Builds your Ktor application
2. Starts the server on port 8080
3. Creates an ngrok tunnel exposing port 8080
4. Displays the public URL in the workflow logs

### Limitations

- Free ngrok accounts: 2-hour session limit
- GitHub Actions: 6-hour workflow timeout (free tier)
- The URL changes each time you deploy (unless you have a paid ngrok plan with static domains)

### Alternative: Use ngrok with static domain

If you have a paid ngrok plan, you can modify the workflow to use a static domain by adding:
```yaml
ngrok http 8080 --domain=your-static-domain.ngrok.io
```

