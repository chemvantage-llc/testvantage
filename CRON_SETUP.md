# Cron Job Configuration

## Overview

The application is configured to automatically run the complete test suite daily at 2:00 AM Eastern Time.

## Configuration

**File**: `cron.yaml`

```yaml
cron:
- description: "Daily Test Suite Execution"
  url: /test/suite?run=true
  schedule: every day 02:00
  timezone: America/New_York
  target: default
```

## What It Does

The cron job:
- Runs every day at 2:00 AM ET (Eastern Time)
- Executes all 8 test variations against production
- Saves results to Cloud Datastore
- Results viewable at `/test/suite`

## Deployment

Deploy the cron configuration along with your application:

```bash
# Deploy cron schedule
gcloud app deploy cron.yaml

# Or deploy everything together
mvn clean package appengine:deploy && gcloud app deploy cron.yaml
```

## Managing Cron Jobs

### View scheduled cron jobs
```bash
gcloud app cron list
```

### View cron execution logs
```bash
gcloud app logs read --service=default --limit=50 | grep "test/suite"
```

### Manually trigger the cron job (for testing)
```bash
# Via gcloud CLI
gcloud app cron run "Daily Test Suite Execution"

# Or visit the URL directly
curl "https://test-vantage.appspot.com/test/suite?run=true"
```

### Update the schedule

Edit `cron.yaml` and redeploy:
```bash
gcloud app deploy cron.yaml
```

### Remove the cron job

Remove the entry from `cron.yaml` and redeploy, or use:
```bash
# This will remove all cron jobs not in cron.yaml
gcloud app deploy cron.yaml
```

## Schedule Format Examples

If you want to change the schedule:

```yaml
# Every hour
schedule: every 1 hours

# Every 6 hours
schedule: every 6 hours

# Every Monday at 9:00 AM
schedule: every monday 09:00

# First day of month at midnight
schedule: 1 of month 00:00

# Every 30 minutes
schedule: every 30 minutes
```

## Timezone Options

The cron uses `America/New_York` which automatically handles:
- Eastern Standard Time (EST) in winter
- Eastern Daylight Time (EDT) in summer

Other timezone examples:
- `America/Los_Angeles` - Pacific Time
- `America/Chicago` - Central Time
- `UTC` - Coordinated Universal Time

## Monitoring

### Check if cron is running
1. Visit Google Cloud Console
2. Navigate to App Engine → Cron jobs
3. View execution history and status

### View test results
Visit `https://test-vantage.appspot.com/test/suite` to see the latest automated test results.

### Email notifications (optional)

To receive notifications on test failures, you can:
1. Set up Cloud Monitoring alerts for error logs
2. Modify the test suite servlet to send emails on failures
3. Use Cloud Functions to trigger notifications based on test results

## Troubleshooting

**Cron not running?**
- Check deployment: `gcloud app deploy cron.yaml`
- Verify cron list: `gcloud app cron list`
- Check logs for errors

**Tests failing?**
- Check individual test results at `/test/suite`
- Review detailed error messages in the test details
- Verify production endpoints are accessible

**Timezone issues?**
- Confirm timezone in cron.yaml matches your requirement
- Remember that cron times are in the specified timezone, not UTC
