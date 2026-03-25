#!/bin/bash

# Test Vantage Deployment Script
# Deploys the LTI Advantage regression tester to Google App Engine

set -e

PROJECT_ID="test-vantage"
REGION="us-central1"

echo "========================================="
echo "Test Vantage Deployment Script"
echo "========================================="
echo ""

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo "ERROR: gcloud CLI is not installed"
    echo "Please install from: https://cloud.google.com/sdk/docs/install"
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed"
    echo "Please install Maven"
    exit 1
fi

echo "✓ Prerequisites check passed"
echo ""

# Authenticate
echo "Step 1: Authenticating with Google Cloud..."
gcloud auth login --quiet || echo "Already authenticated"

# Set project
echo "Step 2: Setting project to $PROJECT_ID..."
gcloud config set project $PROJECT_ID

# Enable required APIs
echo "Step 3: Enabling required APIs..."
gcloud services enable appengine.googleapis.com
gcloud services enable cloudbuild.googleapis.com

# Create App Engine app if it doesn't exist
echo "Step 4: Checking App Engine app..."
if ! gcloud app describe &> /dev/null; then
    echo "Creating App Engine app in region $REGION..."
    gcloud app create --region=$REGION
else
    echo "✓ App Engine app already exists"
fi

# Build the application
echo "Step 5: Building application..."
mvn clean package -DskipTests

# Deploy to App Engine
echo "Step 6: Deploying to App Engine..."
mvn appengine:deploy

# Get the deployed URL
APP_URL=$(gcloud app browse --no-launch-browser 2>&1 | grep -o 'https://[^ ]*' || echo "https://${PROJECT_ID}.appspot.com")

echo ""
echo "========================================="
echo "✓ Deployment Complete!"
echo "========================================="
echo ""
echo "Your Test Vantage app is now available at:"
echo "$APP_URL"
echo ""
echo "Platform Configuration:"
echo "  Issuer: $APP_URL"
echo "  JWKS: $APP_URL/jwks"
echo "  OIDC Auth: $APP_URL/oidc/auth"
echo "  Token Endpoint: $APP_URL/oauth2/token"
echo ""
echo "Next Steps:"
echo "1. Visit $APP_URL to access the test interface"
echo "2. Configure ChemVantage with the platform details above"
echo "3. Run regression tests"
echo ""
echo "View logs with: gcloud app logs tail -s default"
echo "========================================="
