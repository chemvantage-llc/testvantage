# GCP IAM Security Configuration Guide

## Overview

This document outlines the security configuration for the Test Vantage application across three GCP projects:
- **test-vantage** (development)
- **dev-vantage-hrd** (development/staging)
- **chem-vantage-hrd** (production)

## Security Architecture

### 1. App Engine Level (`app.yaml`)
All endpoints require admin login via the `login: admin` directive in app.yaml. This ensures that only authenticated Google Cloud admin users can access the service.

### 2. GCP IAM Level
Additional IAM role assignments restrict who can deploy, manage, and use the application.

### 3. Service Account Level
The Default App Engine service account is configured with minimal required permissions for observability (logging, monitoring, tracing).

## Quick Setup

### Prerequisites
```bash
# Install gcloud CLI
# https://cloud.google.com/sdk/docs/install

# Authenticate with Google Cloud
gcloud auth login

# Verify you have access to the projects
gcloud projects list
```

### One-Command Setup for Each Project

```bash
# For test-vantage (development)
./scripts/setup-iam-security.sh test-vantage your-admin@example.com

# For dev-vantage-hrd (staging)
./scripts/setup-iam-security.sh dev-vantage-hrd your-admin@example.com

# For chem-vantage-hrd (production)
./scripts/setup-iam-security.sh chem-vantage-hrd your-admin@example.com
```

Or with explicit service account:
```bash
./scripts/setup-iam-security.sh chem-vantage-hrd admin@example.com default@chem-vantage-hrd.iam.gserviceaccount.com
```

## Detailed Role Assignments

### For Admin Users (e.g., your-admin@example.com)

| Role | Purpose | Permissions |
|------|---------|-------------|
| `roles/appengine.admin` | Deploy and manage App Engine | - Deploy applications<br>- Manage versions<br>- View logs and metrics |
| `roles/iam.serviceAccountUser` | Use service accounts | - Impersonate service accounts<br>- Run operations as service account |
| `roles/run.admin` | Manage Cloud Run (future-proofing) | - Deploy and manage Cloud Run services<br>- Manage service revisions |

### For Default App Engine Service Account

| Role | Purpose | Permissions |
|------|---------|-------------|
| `roles/logging.logWriter` | Write application logs | - Write logs to Cloud Logging<br>- Enable structured logging |
| `roles/monitoring.metricWriter` | Write custom metrics | - Send metrics to Cloud Monitoring<br>- Track application performance |
| `roles/cloudtrace.agent` | Send trace data | - Write trace spans<br>- Enable distributed tracing |

## Verifying the Configuration

### View Current IAM Policy
```bash
# See all IAM bindings for a project
gcloud projects get-iam-policy test-vantage

# See roles for a specific user
gcloud projects get-iam-policy test-vantage \
  --flatten="bindings[].members" \
  --filter="bindings.members:user:your-admin@example.com"
```

### Test Access
```bash
# Deploy to a project (requires appengine.admin role)
gcloud app deploy --project=test-vantage

# View application logs
gcloud app logs read --project=test-vantage

# View metrics in Cloud Console
# https://console.cloud.google.com/monitoring?project=test-vantage
```

## Removing Access

To revoke admin access for a user:

```bash
# Remove all roles from a user
gcloud projects remove-iam-policy-binding test-vantage \
  --member=user:former-admin@example.com \
  --role=roles/appengine.admin

gcloud projects remove-iam-policy-binding test-vantage \
  --member=user:former-admin@example.com \
  --role=roles/iam.serviceAccountUser

gcloud projects remove-iam-policy-binding test-vantage \
  --member=user:former-admin@example.com \
  --role=roles/run.admin
```

## Cross-Project Access

### For Services in Other Projects

If you need services in another GCP project to call this application, add their service accounts:

```bash
# Allow service account from another project
gcloud projects add-iam-policy-binding test-vantage \
  --member="serviceAccount:OTHER-PROJECT-SA@OTHER-PROJECT.iam.gserviceaccount.com" \
  --role="roles/servicemanagement.user"
```

### For External Service Callers

If external services need to call Test Vantage, consider:
1. Using **OAuth 2.0 service account credentials** with restricted scopes
2. Configuring **Cloud IAP (Identity-Aware Proxy)** for additional security
3. Using **API keys** with IP restrictions (less secure, fallback only)

## Monitoring and Auditing

### View Audit Logs
```bash
# Check deployment history
gcloud app describe --project=test-vantage

# View audit logs in Cloud Logging
gcloud logging read "resource.type=gae_app" \
  --project=test-vantage \
  --limit=50
```

### Enable Cloud Audit Logs
```bash
# Enable Admin Activity logs (already enabled by default)
# Enable Data Access logs (configure in Cloud Console)
# - IAM & Admin > Audit Logs > Select resource type
```

## Security Best Practices

1. **Principle of Least Privilege**
   - Only grant roles that are necessary
   - Use conditions to restrict access by time/IP where possible
   - Regularly audit and remove unused roles

2. **Service Account Management**
   - Never use user credentials in CI/CD pipelines
   - Use separate service accounts for different environments
   - Rotate service account keys regularly

3. **Monitoring**
   - Enable Cloud Logging and Cloud Monitoring
   - Set up alerts for failed deployments
   - Monitor service account usage

4. **Code Security**
   - App Engine enforces `login: admin` for all endpoints
   - Never commit credentials to source control
   - Use Cloud Secret Manager for sensitive data

5. **Network Security**
   - Consider Cloud IAP for additional authentication layer
   - Use Cloud Armor for DDoS protection in production
   - Enable VPC Service Controls for data residency

## Troubleshooting

### "Permission denied" when deploying
```bash
# Verify you have appengine.admin role
gcloud projects get-iam-policy PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:user:YOUR_EMAIL"

# If missing, re-run the setup script
./scripts/setup-iam-security.sh PROJECT_ID YOUR_EMAIL
```

### Service logs not appearing
```bash
# Verify service account has logging.logWriter role
gcloud projects get-iam-policy PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:serviceAccount:*appspot*"

# Check application logs directly
gcloud app logs read --project=PROJECT_ID --limit=10
```

### Cross-project access issues
```bash
# Verify service account in other project has necessary roles
gcloud projects get-iam-policy YOUR_PROJECT \
  --filter="bindings.members:serviceAccount:OTHER_SA@*"
```

## Additional Resources

- [Google Cloud App Engine Security](https://cloud.google.com/appengine/docs/standard/java-gen2/runtime#security)
- [GCP IAM Documentation](https://cloud.google.com/iam/docs)
- [Cloud Identity and Access Management Best Practices](https://cloud.google.com/iam/docs/best-practices)
- [App Engine Admin API Reference](https://cloud.google.com/appengine/docs/admin-api)

## Questions or Issues?

For security-related questions or incidents:
1. Check Cloud Audit Logs for suspicious activity
2. Review the GCP Security Command Center
3. Contact your GCP Cloud Support team
