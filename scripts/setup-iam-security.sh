#!/bin/bash

################################################################################
# GCP IAM Security Setup for Test Vantage
# 
# This script configures GCP IAM roles to restrict access to the test-vantage
# application across the authorized projects:
#   - test-vantage (development)
#   - dev-vantage-hrd (development/staging)
#   - chem-vantage-hrd (production)
#
# Prerequisites:
#   - gcloud CLI installed and authenticated
#   - Project Owner or Editor role in the target projects
#   - Appropriate GCP quotas and billing enabled
#
# Usage:
#   ./setup-iam-security.sh [project-id] [admin-user-email] [service-account-email]
#
# Examples:
#   # For test-vantage project with specific admin and service account
#   ./setup-iam-security.sh test-vantage admin@example.com test-vantage@test-vantage.iam.gserviceaccount.com
#
#   # For production with different admin
#   ./setup-iam-security.sh chem-vantage-hrd prod-admin@example.com default@chem-vantage-hrd.iam.gserviceaccount.com
################################################################################

set -euo pipefail

# Color output for readability
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ID="${1:-}"
ADMIN_EMAIL="${2:-}"
SERVICE_ACCOUNT="${3:-}"

# Validate input
if [[ -z "$PROJECT_ID" ]]; then
    echo -e "${RED}Error: PROJECT_ID is required${NC}"
    echo "Usage: $0 <project-id> <admin-email> [service-account-email]"
    exit 1
fi

if [[ -z "$ADMIN_EMAIL" ]]; then
    echo -e "${RED}Error: ADMIN_EMAIL is required${NC}"
    echo "Usage: $0 <project-id> <admin-email> [service-account-email]"
    exit 1
fi

# If service account not provided, use default for the project
if [[ -z "$SERVICE_ACCOUNT" ]]; then
    SERVICE_ACCOUNT="${PROJECT_ID}@appspot.gserviceaccount.com"
fi

echo -e "${YELLOW}=== GCP IAM Security Setup for Test Vantage ===${NC}"
echo "Project ID: $PROJECT_ID"
echo "Admin Email: $ADMIN_EMAIL"
echo "Service Account: $SERVICE_ACCOUNT"
echo ""

# Validate project exists
echo -e "${YELLOW}Validating project...${NC}"
if ! gcloud projects describe "$PROJECT_ID" > /dev/null 2>&1; then
    echo -e "${RED}Error: Project '$PROJECT_ID' not found or not accessible${NC}"
    exit 1
fi

# Set the current project
gcloud config set project "$PROJECT_ID"
echo -e "${GREEN}✓ Project set to $PROJECT_ID${NC}"
echo ""

# Step 1: Grant admin user App Engine Admin role
echo -e "${YELLOW}Step 1: Granting App Engine Admin role to $ADMIN_EMAIL...${NC}"
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="user:$ADMIN_EMAIL" \
    --role="roles/appengine.admin" \
    --condition=None \
    2>/dev/null && echo -e "${GREEN}✓ App Engine Admin role granted${NC}" || echo -e "${YELLOW}⚠ App Engine Admin role may already be assigned${NC}"

# Step 2: Grant admin user Service Account User role
echo -e "${YELLOW}Step 2: Granting Service Account User role to $ADMIN_EMAIL...${NC}"
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="user:$ADMIN_EMAIL" \
    --role="roles/iam.serviceAccountUser" \
    --condition=None \
    2>/dev/null && echo -e "${GREEN}✓ Service Account User role granted${NC}" || echo -e "${YELLOW}⚠ Service Account User role may already be assigned${NC}"

# Step 3: Grant admin user Cloud Run Admin role (for potential Cloud Run deployments)
echo -e "${YELLOW}Step 3: Granting Cloud Run Admin role to $ADMIN_EMAIL...${NC}"
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="user:$ADMIN_EMAIL" \
    --role="roles/run.admin" \
    --condition=None \
    2>/dev/null && echo -e "${GREEN}✓ Cloud Run Admin role granted${NC}" || echo -e "${YELLOW}⚠ Cloud Run Admin role may already be assigned${NC}"

# Step 4: Grant default App Engine service account necessary IAM roles
echo -e "${YELLOW}Step 4: Configuring Default App Engine service account ($SERVICE_ACCOUNT)...${NC}"

# Grant service account necessary permissions for App Engine
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$SERVICE_ACCOUNT" \
    --role="roles/logging.logWriter" \
    --condition=None \
    2>/dev/null && echo -e "${GREEN}✓ Logging Writer role granted to service account${NC}" || echo -e "${YELLOW}⚠ Logging Writer role may already be assigned${NC}"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$SERVICE_ACCOUNT" \
    --role="roles/monitoring.metricWriter" \
    --condition=None \
    2>/dev/null && echo -e "${GREEN}✓ Monitoring Metric Writer role granted to service account${NC}" || echo -e "${YELLOW}⚠ Monitoring Metric Writer role may already be assigned${NC}"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$SERVICE_ACCOUNT" \
    --role="roles/cloudtrace.agent" \
    --condition=None \
    2>/dev/null && echo -e "${GREEN}✓ Cloud Trace Agent role granted to service account${NC}" || echo -e "${YELLOW}⚠ Cloud Trace Agent role may already be assigned${NC}"

echo ""
echo -e "${GREEN}=== IAM Security Setup Complete ===${NC}"
echo ""
echo "Summary of permissions granted:"
echo "  • $ADMIN_EMAIL:"
echo "    - roles/appengine.admin (deploy and manage App Engine)"
echo "    - roles/iam.serviceAccountUser (use service accounts)"
echo "    - roles/run.admin (manage Cloud Run services)"
echo ""
echo "  • $SERVICE_ACCOUNT:"
echo "    - roles/logging.logWriter (write application logs)"
echo "    - roles/monitoring.metricWriter (write metrics)"
echo "    - roles/cloudtrace.agent (write traces)"
echo ""
echo -e "${YELLOW}Important Notes:${NC}"
echo "1. The app.yaml requires 'login: admin' for all endpoints"
echo "2. Only users with admin access to the project can deploy"
echo "3. Service account permissions are configured for observability only"
echo "4. Additional cross-project service account setup may be needed"
echo ""
echo -e "${YELLOW}To verify permissions:${NC}"
echo "  gcloud projects get-iam-policy $PROJECT_ID"
echo ""
echo -e "${YELLOW}To remove a user's access:${NC}"
echo "  gcloud projects remove-iam-policy-binding $PROJECT_ID \\
    --member=user:$ADMIN_EMAIL \\
    --role=roles/appengine.admin"
