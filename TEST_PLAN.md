# Test Vantage - Regression Test Plan

## Overview

This document outlines the comprehensive regression test plan for verifying ChemVantage's LTI Advantage implementation using the Test Vantage platform.

## Test Environment

- **Platform**: Test Vantage (https://test-vantage.appspot.com)
- **Target**: ChemVantage (https://www.chemvantage.org)
- **Protocol**: LTI 1.3 Advantage
- **Authentication**: OAuth 2.0 + OpenID Connect

## Pre-Test Setup

### 1. Platform Deployment
- [ ] Deploy Test Vantage to App Engine
- [ ] Verify all endpoints are accessible
- [ ] Check JWKS endpoint returns valid keys
- [ ] Confirm OIDC configuration is correct

### 2. ChemVantage Configuration
- [ ] Complete Dynamic Registration OR Manual Configuration
- [ ] Verify platform credentials are stored
- [ ] Enable all LTI Advantage services (Deep Linking, NRPS, AGS)
- [ ] Configure redirect URIs correctly

### 3. Test Data Preparation
- [ ] Verify test users are available (1 instructor, 5 students)
- [ ] Confirm test context/course data
- [ ] Prepare test assignments

## Test Suite

### Test Suite 1: Dynamic Registration

#### Test 1.1: Registration Initiation
- **Objective**: Verify dynamic registration URL generation
- **Steps**:
  1. Navigate to Test Vantage home
  2. Click "Start Dynamic Registration"
  3. Copy registration URL
- **Expected**: Valid registration URL generated with openid_configuration and token
- **Status**: [ ] Pass [ ] Fail

#### Test 1.2: Platform Discovery
- **Objective**: Verify platform metadata is discoverable
- **Steps**:
  1. Access `/.well-known/openid-configuration`
  2. Verify all required fields present
- **Expected**: Valid OIDC configuration with LTI extensions
- **Status**: [ ] Pass [ ] Fail

#### Test 1.3: Registration Completion
- **Objective**: Complete registration with ChemVantage
- **Steps**:
  1. Use registration URL in ChemVantage
  2. Complete registration flow
  3. Verify client_id is returned
- **Expected**: Successful registration with valid client credentials
- **Status**: [ ] Pass [ ] Fail

---

### Test Suite 2: OIDC Authentication

#### Test 2.1: Login Initiation
- **Objective**: Verify OIDC login initiation from tool
- **Steps**:
  1. ChemVantage initiates login to `/oidc/login`
  2. Verify parameters: iss, login_hint, target_link_uri
  3. Check state and nonce generation
- **Expected**: Redirect to authorization endpoint with valid parameters
- **Status**: [ ] Pass [ ] Fail

#### Test 2.2: Authentication Response
- **Objective**: Verify id_token generation and POST
- **Steps**:
  1. Platform processes auth request
  2. Generates signed JWT (id_token)
  3. POSTs to target_link_uri
- **Expected**: Valid JWT with all required LTI claims
- **Status**: [ ] Pass [ ] Fail

#### Test 2.3: JWT Signature Verification
- **Objective**: ChemVantage verifies JWT signature
- **Steps**:
  1. ChemVantage retrieves public key from JWKS
  2. Verifies JWT signature
  3. Validates claims (iss, aud, exp, nonce)
- **Expected**: Signature valid, claims validated
- **Status**: [ ] Pass [ ] Fail

---

### Test Suite 3: LTI Resource Link Launch

#### Test 3.1: Basic Resource Link Launch (Student)
- **Objective**: Launch as student user
- **Steps**:
  1. Click "Launch Resource Link" on Test Vantage
  2. Select student user
  3. Verify launch completes
- **Expected**: 
  - User authenticated as student
  - Correct role: Learner
  - User info displayed correctly
- **Status**: [ ] Pass [ ] Fail

#### Test 3.2: Resource Link Launch (Instructor)
- **Objective**: Launch as instructor user
- **Steps**:
  1. Launch with instructor user
  2. Verify instructor role
  3. Check instructor permissions
- **Expected**:
  - User authenticated as instructor
  - Correct role: Instructor
  - Enhanced permissions available
- **Status**: [ ] Pass [ ] Fail

#### Test 3.3: Context Information
- **Objective**: Verify course/context data passed correctly
- **Steps**:
  1. Launch resource link
  2. Check context claims in JWT
  3. Verify ChemVantage displays correct course info
- **Expected**:
  - Context ID, label, title present
  - Course information displayed correctly
- **Status**: [ ] Pass [ ] Fail

#### Test 3.4: Resource Link Data
- **Objective**: Verify resource link information
- **Steps**:
  1. Launch specific resource link
  2. Check resource_link claim
  3. Verify assignment/content loads
- **Expected**:
  - Resource link ID present
  - Title and description correct
  - Content loads successfully
- **Status**: [ ] Pass [ ] Fail

---

### Test Suite 4: Deep Linking

#### Test 4.1: Deep Linking Request
- **Objective**: Initiate deep linking flow
- **Steps**:
  1. Click "Initiate Deep Linking"
  2. Verify message_type is "LtiDeepLinkingRequest"
  3. Check deep_linking_settings claim
- **Expected**:
  - Deep linking request JWT generated
  - Settings include accept_types, return_url
  - ChemVantage displays content selection UI
- **Status**: [ ] Pass [ ] Fail

#### Test 4.2: Content Selection
- **Objective**: Select content items in ChemVantage
- **Steps**:
  1. From deep linking UI, select content
  2. Submit selection
  3. Verify redirect to return_url
- **Expected**:
  - Content selection UI functional
  - Multiple items can be selected
  - Submission successful
- **Status**: [ ] Pass [ ] Fail

#### Test 4.3: Content Return
- **Objective**: Receive and process content items
- **Steps**:
  1. ChemVantage POSTs content items JWT
  2. Test Vantage receives at `/deeplink`
  3. Parse and display content items
- **Expected**:
  - JWT received with content_items claim
  - Each item has type, title, url
  - Items displayed correctly
- **Status**: [ ] Pass [ ] Fail

#### Test 4.4: Deep Linking - LTI Resource Link
- **Objective**: Verify LTI resource link content items
- **Steps**:
  1. Select LTI resource link type content
  2. Verify custom parameters included
  3. Test subsequent launch of created link
- **Expected**:
  - Resource link item created
  - Custom parameters preserved
  - Link launches successfully
- **Status**: [ ] Pass [ ] Fail

#### Test 4.5: Deep Linking - Multiple Items
- **Objective**: Handle multiple content items
- **Steps**:
  1. Select multiple content items
  2. Submit all items
  3. Verify all items returned
- **Expected**:
  - All selected items present in response
  - Order preserved
  - Each item complete
- **Status**: [ ] Pass [ ] Fail

---

### Test Suite 5: Names and Role Provisioning Service (NRPS)

#### Test 5.1: NRPS Claim in Launch
- **Objective**: Verify NRPS endpoint in launch
- **Steps**:
  1. Click "Launch with NRPS"
  2. Check JWT for namesroleservice claim
  3. Verify context_memberships_url present
- **Expected**:
  - NRPS claim present
  - Membership URL correct
  - Service version 2.0
- **Status**: [ ] Pass [ ] Fail

#### Test 5.2: OAuth2 Access Token
- **Objective**: Obtain access token for NRPS
- **Steps**:
  1. ChemVantage requests token from `/oauth2/token`
  2. Use grant_type: client_credentials
  3. Verify token returned
- **Expected**:
  - Access token generated
  - Token type: Bearer
  - Appropriate expiration time
- **Status**: [ ] Pass [ ] Fail

#### Test 5.3: Membership Retrieval
- **Objective**: Retrieve course roster via NRPS
- **Steps**:
  1. ChemVantage calls membership URL
  2. Include Bearer token in Authorization header
  3. Verify response format
- **Expected**:
  - 200 OK response
  - Content-Type: application/vnd.ims.lti-nrps.v2.membershipcontainer+json
  - Members array present
- **Status**: [ ] Pass [ ] Fail

#### Test 5.4: Member Data Validation
- **Objective**: Verify member data completeness
- **Steps**:
  1. Parse membership response
  2. Check each member for required fields
  3. Verify roles are correct
- **Expected**:
  - 6 members total (1 instructor, 5 students)
  - Each has: user_id, name, email, roles
  - Roles correctly formatted (IMS URIs)
- **Status**: [ ] Pass [ ] Fail

#### Test 5.5: NRPS Error Handling
- **Objective**: Test error scenarios
- **Steps**:
  1. Call NRPS without token
  2. Call with invalid token
  3. Call non-existent context
- **Expected**:
  - 401 Unauthorized for missing token
  - 401 for invalid token
  - 404 for non-existent context (or empty members)
- **Status**: [ ] Pass [ ] Fail

---

### Test Suite 6: Assignment and Grade Service (AGS)

#### Test 6.1: AGS Claim in Launch
- **Objective**: Verify AGS endpoint in launch
- **Steps**:
  1. Click "Launch with AGS"
  2. Check JWT for endpoint claim
  3. Verify scopes and lineitems URL
- **Expected**:
  - AGS endpoint claim present
  - Scopes include lineitem, score
  - Line items URL correct
- **Status**: [ ] Pass [ ] Fail

#### Test 6.2: Create Line Item
- **Objective**: Create a gradeable line item
- **Steps**:
  1. ChemVantage POSTs to `/ags/context/{id}/lineitems`
  2. Include label, scoreMaximum, resourceId
  3. Verify creation response
- **Expected**:
  - 201 Created status
  - Location header with line item URL
  - Line item object returned with ID
- **Status**: [ ] Pass [ ] Fail

#### Test 6.3: Retrieve Line Items
- **Objective**: Get all line items for context
- **Steps**:
  1. ChemVantage GETs `/ags/context/{id}/lineitems`
  2. Include Bearer token
  3. Verify response
- **Expected**:
  - 200 OK status
  - Array of line items
  - Content-Type: application/vnd.ims.lis.v2.lineitemcontainer+json
- **Status**: [ ] Pass [ ] Fail

#### Test 6.4: Submit Score
- **Objective**: Post score for a student
- **Steps**:
  1. ChemVantage POSTs to `/ags/.../lineitems/{id}/scores`
  2. Include userId, scoreGiven, scoreMaximum
  3. Verify acceptance
- **Expected**:
  - 200 OK status
  - Score recorded in Test Vantage logs
  - Timestamp added
- **Status**: [ ] Pass [ ] Fail

#### Test 6.5: Retrieve Scores
- **Objective**: Get scores for a line item
- **Steps**:
  1. ChemVantage GETs `/ags/.../lineitems/{id}/scores`
  2. Verify all submitted scores present
- **Expected**:
  - 200 OK status
  - Array of scores
  - Each score has userId, scoreGiven, timestamp
- **Status**: [ ] Pass [ ] Fail

#### Test 6.6: AGS Authorization
- **Objective**: Verify token-based authorization
- **Steps**:
  1. Call AGS endpoint without token
  2. Call with expired token
  3. Call with valid token
- **Expected**:
  - 401 Unauthorized without token
  - 401 with expired token
  - 200 OK with valid token
- **Status**: [ ] Pass [ ] Fail

---

### Test Suite 7: Integration Tests

#### Test 7.1: Full Workflow - Student Assignment
- **Objective**: Complete end-to-end student workflow
- **Steps**:
  1. Instructor uses deep linking to create assignment
  2. Student launches assignment via resource link
  3. ChemVantage loads roster via NRPS
  4. Student completes assignment
  5. ChemVantage submits grade via AGS
  6. Verify grade received
- **Expected**: Complete workflow successful, grade received
- **Status**: [ ] Pass [ ] Fail

#### Test 7.2: Multiple User Roles
- **Objective**: Test role-based access control
- **Steps**:
  1. Launch as instructor, student, admin
  2. Verify each sees appropriate features
  3. Test permissions are enforced
- **Expected**: Role-based access working correctly
- **Status**: [ ] Pass [ ] Fail

#### Test 7.3: Multiple Contexts
- **Objective**: Handle multiple courses/contexts
- **Steps**:
  1. Launch from different context IDs
  2. Verify NRPS returns correct rosters
  3. Verify AGS keeps grades separate
- **Expected**: Context isolation maintained
- **Status**: [ ] Pass [ ] Fail

#### Test 7.4: Concurrent Operations
- **Objective**: Test under concurrent load
- **Steps**:
  1. Launch multiple simultaneous requests
  2. Mix of launches, NRPS, AGS calls
  3. Verify all succeed
- **Expected**: Platform handles concurrency correctly
- **Status**: [ ] Pass [ ] Fail

---

### Test Suite 8: Error Handling & Edge Cases

#### Test 8.1: Invalid JWT
- **Objective**: Handle malformed JWTs
- **Steps**:
  1. Send JWT with invalid signature
  2. Send expired JWT
  3. Send JWT with missing claims
- **Expected**: ChemVantage rejects with appropriate error
- **Status**: [ ] Pass [ ] Fail

#### Test 8.2: Missing Required Parameters
- **Objective**: Handle incomplete requests
- **Steps**:
  1. OIDC login without required params
  2. Launch without state/nonce
  3. AGS/NRPS without token
- **Expected**: Appropriate error responses (400, 401)
- **Status**: [ ] Pass [ ] Fail

#### Test 8.3: Network Failures
- **Objective**: Handle timeouts and failures
- **Steps**:
  1. Simulate timeout on JWKS retrieval
  2. Simulate failure on AGS submission
  3. Verify error handling
- **Expected**: Graceful degradation, error messages
- **Status**: [ ] Pass [ ] Fail

#### Test 8.4: Token Expiration
- **Objective**: Handle expired tokens
- **Steps**:
  1. Use expired OAuth2 access token
  2. Use expired JWT nonce
  3. Verify rejection
- **Expected**: 401 Unauthorized, new token request
- **Status**: [ ] Pass [ ] Fail

---

### Test Suite 9: Security Tests

#### Test 9.1: State & Nonce Validation
- **Objective**: Verify CSRF protection
- **Steps**:
  1. Attempt launch with wrong state
  2. Reuse nonce
  3. Verify rejections
- **Expected**: Requests rejected, security maintained
- **Status**: [ ] Pass [ ] Fail

#### Test 9.2: JWT Claims Validation
- **Objective**: Verify claim validation
- **Steps**:
  1. Modify iss claim
  2. Modify aud claim
  3. Change exp to future
- **Expected**: Invalid JWTs rejected
- **Status**: [ ] Pass [ ] Fail

#### Test 9.3: Signature Verification
- **Objective**: Verify signature checking
- **Steps**:
  1. Sign JWT with wrong key
  2. Modify JWT after signing
  3. Verify rejection
- **Expected**: Signature validation prevents tampering
- **Status**: [ ] Pass [ ] Fail

#### Test 9.4: Authorization Scopes
- **Objective**: Verify AGS/NRPS scopes enforced
- **Steps**:
  1. Request token with limited scope
  2. Attempt operations outside scope
  3. Verify rejection
- **Expected**: Scope enforcement working
- **Status**: [ ] Pass [ ] Fail

---

## Performance Tests

### Test P1: Launch Latency
- **Objective**: Measure launch performance
- **Metric**: Time from OIDC login to resource display
- **Target**: < 2 seconds
- **Status**: [ ] Pass [ ] Fail

### Test P2: NRPS Response Time
- **Objective**: Measure roster retrieval speed
- **Metric**: Time to return membership data
- **Target**: < 500ms for 100 users
- **Status**: [ ] Pass [ ] Fail

### Test P3: AGS Throughput
- **Objective**: Measure grade submission capacity
- **Metric**: Grades per second
- **Target**: > 10 grades/second
- **Status**: [ ] Pass [ ] Fail

---

## Compliance Tests

### Test C1: LTI 1.3 Core Compliance
- [ ] All required JWT claims present
- [ ] Correct message types used
- [ ] Version 1.3.0 specified
- [ ] Deployment ID included

### Test C2: OIDC Compliance
- [ ] Standard OIDC flow followed
- [ ] id_token format correct
- [ ] JWKS endpoint standard-compliant
- [ ] Discovery document complete

### Test C3: Deep Linking 2.0 Compliance
- [ ] Message type correct
- [ ] Settings claim complete
- [ ] Content item format correct
- [ ] Return flow standard

### Test C4: NRPS 2.0 Compliance
- [ ] Correct content type
- [ ] Member structure standard
- [ ] Context object complete
- [ ] Service version 2.0

### Test C5: AGS 2.0 Compliance
- [ ] Correct content types
- [ ] Line item structure standard
- [ ] Score format correct
- [ ] Scope claims correct

---

## Regression Test Execution

### Pre-Execution
1. Deploy latest Test Vantage build
2. Configure ChemVantage
3. Clear previous test data
4. Prepare test environment

### Execution
1. Run tests in order (suites 1-9)
2. Document any failures
3. Re-test failed cases
4. Record timings for performance tests

### Post-Execution
1. Review all results
2. Document issues found
3. Create bug reports
4. Update test plan for new scenarios

---

## Issue Tracking

| Test ID | Issue Description | Severity | Status | Notes |
|---------|------------------|----------|--------|-------|
| | | | | |

---

## Sign-Off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Test Lead | | | |
| Developer | | | |
| Product Owner | | | |

---

## Appendix

### A. Test Data
- See CONFIGURATION.md for complete test user list
- Context IDs: test-context-001 through test-context-005
- Resource link IDs: test-resource-link-001 through test-resource-link-010

### B. Log Locations
- Test Vantage logs: Google Cloud Console → App Engine → Logs
- ChemVantage logs: (per ChemVantage documentation)

### C. Tools
- JWT Debugger: https://jwt.io
- JSON Validator: https://jsonlint.com
- HTTP Inspector: Browser DevTools Network tab

### D. References
- LTI 1.3 Spec: https://www.imsglobal.org/spec/lti/v1p3/
- OIDC Spec: https://openid.net/specs/openid-connect-core-1_0.html
- Test Vantage Docs: README.md, CONFIGURATION.md
