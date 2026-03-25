# ChemVantage Configuration Guide

This guide explains how to configure ChemVantage to work with the Test Vantage platform.

## Step 1: Deploy Test Vantage

First, deploy the test-vantage app to Google App Engine:

```bash
cd test-vantage
mvn clean package appengine:deploy
```

Your platform will be available at: `https://test-vantage.appspot.com`

## Step 2: Get Platform Information

Visit `https://test-vantage.appspot.com` and note the following information displayed on the home page:

### Required Platform Details

| Property | Value |
|----------|-------|
| Platform URL (Issuer) | `https://test-vantage.appspot.com` |
| Client ID | `test-vantage-client` |
| Deployment ID | `1` |
| Platform GUID | `test-vantage-platform-guid` |

### OpenID Connect Endpoints

| Endpoint | URL |
|----------|-----|
| OpenID Configuration | `https://test-vantage.appspot.com/.well-known/openid-configuration` |
| Authorization Endpoint | `https://test-vantage.appspot.com/oidc/auth` |
| Token Endpoint | `https://test-vantage.appspot.com/oauth2/token` |
| JWKS URI | `https://test-vantage.appspot.com/jwks` |

### LTI Endpoints

| Endpoint | URL |
|----------|-----|
| OIDC Login Initiation | `https://test-vantage.appspot.com/oidc/login` |
| Deep Linking Return URL | `https://test-vantage.appspot.com/deeplink` |

## Step 3: Configure ChemVantage

### Option A: Dynamic Registration (Recommended)

1. Go to `https://test-vantage.appspot.com`
2. Click **"Start Dynamic Registration"**
3. Copy the provided registration URL
4. In ChemVantage admin panel, use the dynamic registration feature with this URL
5. Complete the registration flow

### Option B: Manual Configuration

If dynamic registration is not available, manually configure ChemVantage:

1. **Login to ChemVantage Admin Panel**
   - Navigate to the LTI platform management section

2. **Add New Platform**
   - Platform Name: `Test Vantage`
   - Issuer: `https://test-vantage.appspot.com`
   - Client ID: `test-vantage-client`
   - Deployment ID: `1`
   - Platform GUID: `test-vantage-platform-guid`

3. **Configure Authentication**
   - Auth Method: `LTI 1.3 (OIDC)`
   - OIDC Login URL: `https://test-vantage.appspot.com/oidc/login`
   - Auth Endpoint: `https://test-vantage.appspot.com/oidc/auth`
   - Token Endpoint: `https://test-vantage.appspot.com/oauth2/token`
   - JWKS URL: `https://test-vantage.appspot.com/jwks`

4. **Enable Services**
   - ✅ Deep Linking
   - ✅ Names and Role Provisioning Service (NRPS)
   - ✅ Assignment and Grade Service (AGS)

5. **Configure Service Endpoints**
   - NRPS Endpoint: `https://test-vantage.appspot.com/nrps/context/{contextId}/memberships`
   - AGS Line Items: `https://test-vantage.appspot.com/ags/context/{contextId}/lineitems`

6. **Save Configuration**

## Step 4: Test the Configuration

### Test 1: OIDC Login Flow

1. From Test Vantage home page, click **"Launch Resource Link"**
2. This simulates an LTI launch from the LMS
3. Verify that ChemVantage receives and processes the launch request

### Test 2: Deep Linking

1. Click **"Initiate Deep Linking"** on Test Vantage
2. ChemVantage should display content selection interface
3. Select content and submit
4. Verify content items are returned to Test Vantage

### Test 3: NRPS (Roster Retrieval)

1. Click **"Launch with NRPS"**
2. In ChemVantage, attempt to retrieve the course roster
3. ChemVantage should call the NRPS endpoint and receive:
   - 1 Instructor (instructor@test-vantage.org)
   - 5 Students (student1@test-vantage.org through student5@test-vantage.org)

### Test 4: AGS (Grade Passback)

1. Click **"Launch with AGS"**
2. In ChemVantage, submit a grade for a student
3. Check Test Vantage logs to verify grade was received
4. Grade should appear in the AGS endpoint logs

### Test 5: Full Integration

1. Click **"Full Integration Launch"**
2. Test complete workflow:
   - Launch → Retrieve roster → Submit grades
3. Verify all services work together

## Verification Checklist

After configuration, verify:

- [ ] ChemVantage can receive LTI launch requests
- [ ] User information (name, email, role) is correctly passed
- [ ] Course/context information is available
- [ ] Deep linking content selection works
- [ ] NRPS endpoint returns roster data
- [ ] AGS endpoint receives grade submissions
- [ ] JWT signatures are valid
- [ ] OAuth2 tokens work for service calls

## Troubleshooting

### Issue: "Invalid Issuer"

**Problem**: ChemVantage rejects the launch because issuer doesn't match

**Solution**: Ensure the issuer in ChemVantage config exactly matches: `https://test-vantage.appspot.com` (no trailing slash)

### Issue: "Invalid Signature"

**Problem**: JWT signature verification fails

**Solution**: 
- Verify JWKS endpoint is accessible: `https://test-vantage.appspot.com/jwks`
- Check that ChemVantage is using the public key from JWKS
- Ensure JWT header includes correct `kid` (key ID)

### Issue: "NRPS/AGS Returns 401 Unauthorized"

**Problem**: Service endpoints require authentication

**Solution**:
- Obtain OAuth2 access token from: `https://test-vantage.appspot.com/oauth2/token`
- Use grant_type: `client_credentials`
- Include token in Authorization header: `Bearer {token}`

### Issue: "Deep Linking Returns Nothing"

**Problem**: ChemVantage doesn't return content items

**Solution**:
- Verify the deep linking return URL is configured: `https://test-vantage.appspot.com/deeplink`
- Check that the LTI message includes deep linking settings
- Ensure ChemVantage signs the return JWT correctly

## Advanced: Debugging with Logs

### View Test Vantage Logs

```bash
gcloud app logs tail -s default
```

### Enable Debug Logging

In ChemVantage, enable LTI debug logging to see:
- Incoming JWT claims
- JWKS key retrieval
- Service endpoint calls
- OAuth2 token requests

## Security Notes

For production testing:

1. **Use HTTPS**: All endpoints must use HTTPS
2. **Validate JWTs**: ChemVantage should validate all incoming JWTs
3. **Secure Tokens**: OAuth2 tokens should have limited scope and expiration
4. **Rate Limiting**: Implement rate limits on service endpoints
5. **Audit Logs**: Enable comprehensive logging for security audits

## Test Data

### Test Users

| Role | User ID | Name | Email |
|------|---------|------|-------|
| Instructor | test-instructor-001 | Test Instructor | instructor@test-vantage.org |
| Student 1 | test-student-001 | Test Student 1 | student1@test-vantage.org |
| Student 2 | test-student-002 | Test Student 2 | student2@test-vantage.org |
| Student 3 | test-student-003 | Test Student 3 | student3@test-vantage.org |
| Student 4 | test-student-004 | Test Student 4 | student4@test-vantage.org |
| Student 5 | test-student-005 | Test Student 5 | student5@test-vantage.org |

### Test Context

| Property | Value |
|----------|-------|
| Context ID | test-context-001 |
| Course Label | CHEM-101 |
| Course Title | Introduction to Chemistry |
| Course Code | CHEM-101-2026-SPRING |

### Test Resource Link

| Property | Value |
|----------|-------|
| Resource Link ID | test-resource-link-001 |
| Title | Test Assignment |
| Description | A test assignment for ChemVantage |

## Support

If you encounter issues:

1. Check Test Vantage logs in Google Cloud Console
2. Review ChemVantage debug logs
3. Verify all URLs are correct and accessible
4. Check that JWKS endpoint returns valid public keys
5. Ensure JWT exp (expiration) times are valid

## Next Steps

After successful configuration:

1. Create test scenarios for each feature
2. Document expected vs actual behavior
3. Test error handling and edge cases
4. Validate compliance with LTI 1.3 specification
5. Perform security testing

## Resources

- [LTI 1.3 Core Specification](https://www.imsglobal.org/spec/lti/v1p3/)
- [LTI Advantage](https://www.imsglobal.org/lti-advantage-overview)
- [ChemVantage Documentation](https://www.chemvantage.org/help)
- [Test Vantage GitHub](https://github.com/yourusername/test-vantage)
