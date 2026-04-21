# Comprehensive Test Suite Guide

## Overview

The Test Suite endpoint (`/test/suite`) provides a single interface to run all test variations on production and view aggregated results.

## Accessing the Test Suite

Navigate to: `https://test-vantage.appspot.com/test/suite`

## Tests Included

The suite runs **8 total tests**:

### 1. Registration Tests (2 variations)
- **prod/canvas** - Dynamic Registration with Canvas (trusted platform)
- **prod/moodle** - Dynamic Registration with Moodle (untrusted platform)

### 2. Launch Tests (4 variations)
- **instructor/known_assignment** - Instructor launching a known assignment
- **instructor/unknown_assignment** - Instructor launching an unknown assignment  
- **student/known_assignment** - Student launching a known assignment
- **student/unknown_assignment** - Student launching an unknown assignment

### 3. Auth Token Test (1 test)
- **Auth Token Workflow** - Tests the `/auth/token` endpoint OIDC redirect workflow

### 4. JWKS Endpoint Test (1 test)
- **JWKS Endpoint** - Validates the JWKS endpoint returns parseable JSON with valid RSA keys

## How to Use

1. **View Current Results**: Simply navigate to `/test/suite` to see the latest test results
2. **Run All Tests**: Click the "Run Complete Test Suite" button
3. **Wait for Completion**: Tests take approximately 30-60 seconds to complete
4. **Review Results**: The page will reload showing pass/fail status for each test
5. **Expand Details**: Click the "Details" button next to any test to see full execution details

## Test Result Statuses

- **PASSED** (green) - Test executed successfully and met all criteria
- **FAILED** (red) - Test executed but did not meet expected criteria
- **ERROR** (red) - Test encountered an exception during execution
- **NOT_RUN** (yellow) - Test has not been executed yet

## Results Summary

The page displays:
- Total test count
- Number of passed tests
- Number of failed tests
- Number of tests not yet run

## Individual Test Pages

You can still access individual test pages for focused testing:
- `/test/registration` - Registration Test
- `/test/auth-token` - Auth Token Test
- `/test/launch` - Launch Test
- `/test/jwks` - JWKS Test
- `/jwks` - JWKS Endpoint (direct access)

## Architecture

The test suite servlet:
1. Makes HTTP POST requests to individual test servlets
2. Each test servlet validates the ChemVantage production endpoints
3. Results are stored in Google Cloud Datastore (via Objectify)
4. The suite page aggregates and displays all results in a unified view

## Notes

- All tests target the **production** environment (`www.chemvantage.org`)
- Tests use `test-vantage.appspot.com` as the issuer/platform
- Results persist across sessions via Cloud Datastore
- Running the suite multiple times will overwrite previous results

## Troubleshooting

If tests fail:
1. Check the detailed error message by clicking "Details"
2. Verify production endpoints are accessible
3. Run individual tests to isolate issues
4. Check Cloud Datastore for stored test results
