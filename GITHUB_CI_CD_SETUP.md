# GitHub CI/CD Pipeline Setup Guide

This guide explains how to set up the automated CI/CD pipeline for AppTimeBackend.

## Overview

The CI/CD pipeline automatically:
1. **Builds** your application on every push and pull request
2. **Deploys** your application when you push to `main` or `master` branch
3. **Runs tests** (if configured)
4. **Creates a shadow JAR** with all dependencies

## Quick Start for GCP VM Deployment

Since your application is deployed on a GCP VM with the database in the same VM, here's the quick setup:

1. **Get your GCP VM details**:
   - External IP: `gcloud compute instances describe your-vm-name --zone=your-zone --format='get(networkInterfaces[0].accessConfigs[0].natIP)'`
   - SSH user: Usually your GCP username or the default user for your VM image

2. **Set up SSH key for GitHub Actions**:
   ```bash
   ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/github_actions_deploy
   # Copy public key to GCP VM
   ssh-copy-id -i ~/.ssh/github_actions_deploy.pub user@your-vm-external-ip
   ```

3. **Add GitHub Secrets** (Repository → Settings → Secrets → Actions):
   - `DEPLOY_HOST`: Your GCP VM external IP
   - `DEPLOY_USER`: Your SSH username
   - `DEPLOY_SSH_KEY`: Content of `~/.ssh/github_actions_deploy` (private key)
   - `DATABASE_URL`: `jdbc:postgresql://localhost:5432/screentime_db`
   - `DB_USER`: `postgres` (or your DB user)
   - `DB_PASSWORD`: Your database password

4. **Push to main branch** - deployment will start automatically!

For detailed instructions, see the sections below.

## Workflow File

The workflow is defined in `.github/workflows/deploy.yml` and runs on:
- Push to `main` or `master` branch
- Pull requests to `main` or `master` branch
- Manual trigger via GitHub Actions UI

## Deployment Options

### Option 1: SSH Deployment to GCP VM (Recommended for Your Setup)

Deploy to a GCP VM via SSH. Since your database is in the same VM, this is the optimal deployment method.

#### Prerequisites

1. A GCP VM instance with SSH access
2. Java 17+ installed on the VM
3. PostgreSQL database running on the same VM
4. A directory on the VM to deploy the application (e.g., `/opt/apptime-backend`)
5. Firewall rules configured to allow traffic on port 8080 (if accessing externally)

#### Setup Steps

1. **Generate SSH Key Pair** (if you don't have one):
   ```bash
   ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/github_actions_deploy
   ```

2. **Copy Public Key to Server**:
   ```bash
   ssh-copy-id -i ~/.ssh/github_actions_deploy.pub user@your-server.com
   ```

3. **Add GitHub Secrets**:
   Go to your GitHub repository → Settings → Secrets and variables → Actions → New repository secret
   
   Add the following secrets:
   - `DEPLOY_HOST`: Your GCP VM's external IP address (e.g., `34.123.45.67`) or internal IP if using VPN
   - `DEPLOY_USER`: SSH username (typically `your-username` or the default user for your VM image)
   - `DEPLOY_SSH_KEY`: Contents of your **private** SSH key (the entire content of `~/.ssh/github_actions_deploy`)
   - `DEPLOY_PORT`: SSH port (optional, defaults to 22)
   - `DEPLOY_PATH`: Deployment directory on VM (optional, defaults to `/opt/apptime-backend`)
   - `DATABASE_URL`: PostgreSQL connection URL (use `jdbc:postgresql://localhost:5432/screentime_db` since DB is in same VM)
   - `DB_USER`: Database username (typically `postgres`)
   - `DB_PASSWORD`: Database password

4. **Prepare GCP VM Directory**:
   ```bash
   # Connect to your GCP VM
   gcloud compute ssh your-vm-name --zone=your-zone
   # Or use regular SSH if you have the IP
   ssh user@your-gcp-vm-ip
   
   # Create deployment directory
   sudo mkdir -p /opt/apptime-backend
   sudo chown $USER:$USER /opt/apptime-backend
   chmod 755 /opt/apptime-backend
   ```

5. **Configure GCP VM Firewall** (if accessing externally):
   ```bash
   # Allow HTTP traffic on port 8080
   gcloud compute firewall-rules create allow-apptime-backend \
     --allow tcp:8080 \
     --source-ranges 0.0.0.0/0 \
     --description "Allow AppTimeBackend on port 8080"
   ```
   
   Or use the GCP Console:
   - Go to VPC Network → Firewall Rules
   - Create a new rule allowing TCP port 8080

6. **Verify Database is Running** (since it's in the same VM):
   ```bash
   # On your GCP VM
   sudo systemctl status postgresql
   # Or check if PostgreSQL is running
   ps aux | grep postgres
   ```

7. **Test Deployment**:
   Push to your `main` branch and check the Actions tab in GitHub.

#### GCP VM Requirements

- Java 17 or higher installed
- PostgreSQL database running on localhost
- Sufficient disk space for JAR files (at least 1GB free)
- Port 8080 available (check with `netstat -tuln | grep 8080`)
- SSH access configured (either via gcloud or SSH keys)
- Firewall rules configured if accessing externally

#### GCP VM Setup Tips

1. **Using gcloud for SSH**:
   If you prefer using `gcloud compute ssh`, you can set up SSH keys for GitHub Actions:
   ```bash
   # Generate SSH key
   ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/github_actions_deploy
   
   # Add public key to GCP VM metadata
   gcloud compute instances add-metadata your-vm-name \
     --zone=your-zone \
     --metadata-from-file ssh-keys=~/.ssh/github_actions_deploy.pub
   ```

2. **Using External IP**:
   - Get your VM's external IP: `gcloud compute instances describe your-vm-name --zone=your-zone --format='get(networkInterfaces[0].accessConfigs[0].natIP)'`
   - Use this IP as `DEPLOY_HOST` in GitHub secrets

3. **Database Connection**:
   Since the database is in the same VM, use:
   - `DATABASE_URL`: `jdbc:postgresql://localhost:5432/screentime_db`
   - This ensures the app connects to the local PostgreSQL instance

### Option 2: Google Cloud Run Deployment

Deploy to Google Cloud Run for a serverless solution.

#### Setup Steps

1. **Create a Service Account**:
   - Go to Google Cloud Console → IAM & Admin → Service Accounts
   - Create a new service account with Cloud Run Admin role
   - Create and download a JSON key

2. **Add GitHub Secrets**:
   - `GCP_SA_KEY`: Contents of the service account JSON key file
   - `DATABASE_URL`: Your PostgreSQL connection URL
   - `DB_USER`: Database username
   - `DB_PASSWORD`: Database password

3. **Uncomment Cloud Run Steps**:
   Edit `.github/workflows/deploy.yml` and uncomment the Cloud Run deployment section.

4. **Configure Cloud Run Settings**:
   Update the `gcloud run deploy` command with your preferred:
   - Region (e.g., `us-central1`, `asia-south1`)
   - Memory and CPU limits
   - Other Cloud Run options

### Option 3: Other Deployment Methods

You can customize the workflow to deploy to:
- AWS Elastic Beanstalk
- Azure App Service
- Heroku
- DigitalOcean App Platform
- Any other platform that supports JAR deployment

## Required GitHub Secrets

### For SSH Deployment:
- `DEPLOY_HOST` (required)
- `DEPLOY_USER` (required)
- `DEPLOY_SSH_KEY` (required)
- `DEPLOY_PORT` (optional, default: 22)
- `DEPLOY_PATH` (optional, default: `/opt/apptime-backend`)
- `DATABASE_URL` (required)
- `DB_USER` (required)
- `DB_PASSWORD` (required)

### For Cloud Run Deployment:
- `GCP_SA_KEY` (required)
- `DATABASE_URL` (required)
- `DB_USER` (required)
- `DB_PASSWORD` (required)

## How It Works

1. **Build Job**:
   - Checks out code
   - Sets up Java 17
   - Builds the project with Gradle
   - Creates a shadow JAR (fat JAR with all dependencies)
   - Uploads the JAR as an artifact

2. **Deploy Job** (only on push to main/master):
   - Downloads the built JAR
   - Deploys to your configured server/platform
   - Restarts the application
   - Verifies the deployment

## Monitoring Deployments

1. **GitHub Actions Tab**:
   - Go to your repository → Actions tab
   - View workflow runs and their status
   - Click on a run to see detailed logs

2. **Deployment Summary**:
   - Each deployment includes a summary in the Actions UI
   - Shows deployment status, branch, and commit info

## Troubleshooting

### Build Fails

- Check the build logs in GitHub Actions
- Ensure `build.gradle.kts` is correct
- Verify all dependencies are available

### Deployment Fails

**SSH Deployment Issues:**
- Verify SSH key is correct and has proper permissions
- Check GCP VM accessibility (firewall rules, network tags)
- Ensure Java 17+ is installed on the VM: `java -version`
- Check if SSH port 22 is open in GCP firewall rules
- Verify you can SSH manually: `gcloud compute ssh your-vm-name --zone=your-zone`
- Check server logs: `tail -f /opt/apptime-backend/app.log`

**GCP-Specific Issues:**
- Verify VM is running: `gcloud compute instances list`
- Check firewall rules allow port 22 (SSH) and 8080 (app)
- Ensure external IP is assigned (if using external IP)
- Check VM logs: `gcloud compute instances get-serial-port-output your-vm-name --zone=your-zone`

**Connection Issues:**
- Verify `DEPLOY_HOST` is correct
- Check SSH port is open
- Test SSH connection manually: `ssh -i key user@host`

**Application Not Starting:**
- Check database connection settings (should be `localhost` since DB is in same VM)
- Verify PostgreSQL is running: `sudo systemctl status postgresql`
- Check database connectivity: `psql -h localhost -U postgres -d screentime_db`
- Verify environment variables are set correctly in GitHub secrets
- Check application logs on the VM: `tail -f /opt/apptime-backend/app.log`
- Verify port 8080 is not in use: `sudo lsof -i :8080`

### Application Crashes After Deployment

1. **Check Logs on GCP VM**:
   ```bash
   # Connect to VM
   gcloud compute ssh your-vm-name --zone=your-zone
   
   # View logs
   tail -f /opt/apptime-backend/app.log
   ```

2. **Verify Environment Variables**:
   - Database connection settings (should be `localhost`)
   - Port configuration (default 8080)
   - Database credentials
   - Check if variables are set: `cat /opt/apptime-backend/app.pid` (process should be running)

3. **Check Process Status**:
   ```bash
   # On the VM
   ps aux | grep java
   cat /opt/apptime-backend/app.pid
   
   # Check if port is in use
   sudo lsof -i :8080
   ```

4. **Verify Database Connection**:
   ```bash
   # Test PostgreSQL connection
   psql -h localhost -U postgres -d screentime_db -c "SELECT 1;"
   
   # Check PostgreSQL is running
   sudo systemctl status postgresql
   ```

5. **Check GCP VM Resources**:
   ```bash
   # Check disk space
   df -h
   
   # Check memory
   free -h
   
   # Check CPU usage
   top
   ```

## Manual Deployment

You can also trigger deployments manually:

1. Go to GitHub repository → Actions tab
2. Select "Build and Deploy" workflow
3. Click "Run workflow"
4. Select branch and click "Run workflow"

## Security Best Practices

1. **Never commit secrets** to the repository
2. **Use GitHub Secrets** for all sensitive data
3. **Rotate SSH keys** regularly
4. **Limit SSH key permissions** on the server
5. **Use environment-specific secrets** for different environments
6. **Review deployment logs** regularly

## Next Steps

1. Set up your GitHub secrets
2. Configure your deployment target (SSH server or Cloud Run)
3. Push to `main` branch to trigger deployment
4. Monitor the first deployment in the Actions tab
5. Verify your application is running correctly

## Support

If you encounter issues:
1. Check the GitHub Actions logs
2. Review server logs
3. Verify all secrets are set correctly
4. Test SSH connection manually (for SSH deployment)

---

**Last Updated**: 2025-01-20

