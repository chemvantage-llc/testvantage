# Quick Start Guide - Test Vantage

This guide helps you quickly get started with testing ChemVantage using Test Vantage.

## Installation (5 minutes)

### Prerequisites
```bash
# Install Google Cloud SDK
# macOS:
brew install --cask google-cloud-sdk

# Or download from: https://cloud.google.com/sdk/docs/install

# Install Maven (if not already installed)
brew install maven
```

### Deploy Test Vantage
```bash
cd test-vantage
./deploy.sh
```

The script will:
1. Authenticate with Google Cloud
2. Enable required APIs
3. Build the application
4. Deploy to App Engine

Your platform will be live at: `https://test-vantage.appspot.com`

---

## First Test (10 minutes)

### Option A: Quick Manual Test

1. **Access Test Platform**
   ```
   https://test-vantage.appspot.com
   ```

2. **Get Configuration Details**
   - Issuer: `https://test-vantage.appspot.com`
   - JWKS: `https://test-vantage.appspot.com/jwks`
   - Client ID: `test-vantage-client`

3. **Configure ChemVantage**
   - Go to ChemVantage admin
   - Add new LTI platform
   - Enter configuration details from step 2
   - Save

4. **Test Basic Launch**
   - Click "Launch Resource Link" on Test Vantage
   - Should open ChemVantage with user logged in

### Option B: Dynamic Registration (Recommended)

1. **Start Registration**
   ```
   https://test-vantage.appspot.com
   Click "Start Dynamic Registration"
   ```

2. **Copy Registration URL**
   ```
   Example:
   https://test-vantage.appspot.com/registration?openid_configuration=...
   ```

3. **Register in ChemVantage**
   - Use the registration URL in ChemVantage's dynamic registration
   - Complete the flow
   - Save the client_id returned

4. **Test Launch**
   - Click "Launch Resource Link"
   - Verify successful launch

---

## Common Test Scenarios

### Scenario 1: Test Deep Linking (Content Selection)

```
1. Go to: https://test-vantage.appspot.com
2. Click: "Initiate Deep Linking"
3. In ChemVantage: Select content items
4. Submit selection
5. Verify: Content items displayed on Test Vantage
```

**Expected Result**: Selected content items returned as JSON

### Scenario 2: Test Roster (NRPS)

```
1. Click: "Launch with NRPS"
2. In ChemVantage: Access roster/membership
3. Verify roster shows:
   - 1 Instructor (instructor@test-vantage.org)
   - 5 Students (student1-5@test-vantage.org)
```

**Expected Result**: All 6 users displayed with correct roles

### Scenario 3: Test Grade Passback (AGS)

```
1. Click: "Launch with AGS"
2. In ChemVantage: Complete an assignment
3. Submit a grade (e.g., 85/100)
4. Check Test Vantage logs:
   gcloud app logs tail -s default
```

**Expected Result**: Grade submission logged with score details

### Scenario 4: Full Integration Test

```
1. Click: "Full Integration Launch"
   (Includes NRPS + AGS)
2. In ChemVantage:
   a. View roster
   b. Complete assignment
   c. Submit grade
3. Verify all operations succeed
```

**Expected Result**: Complete workflow from launch to grade submission

---

## Troubleshooting

### Problem: "Platform not found"
**Solution**: 
```bash
# Verify deployment
gcloud app browse

# Check logs
gcloud app logs tail -s default
```

### Problem: "Invalid signature"
**Solution**:
```bash
# Verify JWKS endpoint
curl https://test-vantage.appspot.com/jwks

# Should return valid RSA public key
```

### Problem: "NRPS returns 401"
**Solution**:
```bash
# Get OAuth2 token first
curl -X POST https://test-vantage.appspot.com/oauth2/token \
  -d "grant_type=client_credentials" \
  -d "client_id=test-client" \
  -d "scope=https://purl.imsglobal.org/spec/lti-nrps/scope/contextmembership.readonly"

# Use returned token in Authorization header
curl https://test-vantage.appspot.com/nrps/context/test-context-001/memberships \
  -H "Authorization: Bearer {token}"
```

### Problem: "Deep Linking returns nothing"
**Solution**:
- Verify return URL: `https://test-vantage.appspot.com/deeplink`
- Check ChemVantage logs for errors
- Ensure ChemVantage signs return JWT correctly

---

## Viewing Logs

### Real-time Logs
```bash
gcloud app logs tail -s default
```

### Recent Logs (Last Hour)
```bash
gcloud app logs read --limit=50
```

### Filter for Errors
```bash
gcloud app logs read --severity=ERROR --limit=20
```

### Search Specific Text
```bash
gcloud app logs read --filter="textPayload:AGS"
```

---

## Testing Checklist

Use this quick checklist for each test run:

- [ ] Platform deployed and accessible
- [ ] JWKS endpoint returns valid keys
- [ ] ChemVantage configured with platform
- [ ] Basic launch works (student user)
- [ ] Launch works (instructor user)
- [ ] Deep linking content selection works
- [ ] NRPS returns roster (6 users)
- [ ] AGS receives grade submissions
- [ ] No errors in logs

---

## API Endpoints Reference

### Platform Configuration
| Endpoint | URL |
|----------|-----|
| Home | `https://test-vantage.appspot.com/` |
| OIDC Config | `https://test-vantage.appspot.com/.well-known/openid-configuration` |
| JWKS | `https://test-vantage.appspot.com/jwks` |
| OAuth2 Token | `https://test-vantage.appspot.com/oauth2/token` |

### LTI Endpoints
| Endpoint | URL |
|----------|-----|
| Registration | `https://test-vantage.appspot.com/registration` |
| OIDC Login | `https://test-vantage.appspot.com/oidc/login` |
| OIDC Auth | `https://test-vantage.appspot.com/oidc/auth` |
| Launch | `https://test-vantage.appspot.com/launch` |
| Deep Link Return | `https://test-vantage.appspot.com/deeplink` |

### Services
| Service | Endpoint |
|---------|----------|
| NRPS Memberships | `https://test-vantage.appspot.com/nrps/context/{contextId}/memberships` |
| AGS Line Items | `https://test-vantage.appspot.com/ags/context/{contextId}/lineitems` |
| AGS Scores | `https://test-vantage.appspot.com/ags/context/{contextId}/lineitems/{id}/scores` |

---

## Test Data

### Users
```
Instructor:
  ID: test-instructor-001
  Email: instructor@test-vantage.org
  Name: Test Instructor

Students (1-5):
  ID: test-student-001 through test-student-005
  Email: student1@test-vantage.org through student5@test-vantage.org
  Name: Test Student 1 through Test Student 5
```

### Context
```
Context ID: test-context-001
Course Code: CHEM-101
Course Title: Introduction to Chemistry
```

---

## Advanced: Manual API Testing

### Test JWKS Endpoint
```bash
curl https://test-vantage.appspot.com/jwks | jq
```

### Test OIDC Configuration
```bash
curl https://test-vantage.appspot.com/.well-known/openid-configuration | jq
```

### Test OAuth2 Token
```bash
curl -X POST https://test-vantage.appspot.com/oauth2/token \
  -d "grant_type=client_credentials" \
  -d "client_id=test-client" \
  | jq
```

### Test NRPS (with token)
```bash
TOKEN="your-token-here"
curl https://test-vantage.appspot.com/nrps/context/test-context-001/memberships \
  -H "Authorization: Bearer $TOKEN" \
  | jq
```

---

## Tips for Effective Testing

1. **Use Browser DevTools**
   - Network tab: See all LTI requests/responses
   - Console: Check for JavaScript errors
   - Application tab: Inspect cookies and storage

2. **Decode JWTs**
   - Use https://jwt.io to decode and inspect JWTs
   - Verify all claims are present and correct

3. **Test Different Users**
   - Always test both instructor and student roles
   - Verify role-based features work correctly

4. **Monitor Logs**
   - Keep logs open during testing
   - Watch for errors or warnings

5. **Test Edge Cases**
   - Try expired tokens
   - Test with missing parameters
   - Verify error handling

---

## Next Steps

1. **Read Full Documentation**
   - `README.md` - Complete overview
   - `CONFIGURATION.md` - Detailed configuration guide
   - `TEST_PLAN.md` - Comprehensive test plan

2. **Run Regression Tests**
   - Follow TEST_PLAN.md for complete test suite
   - Document results
   - Report issues

3. **Extend Testing**
   - Add custom test scenarios
   - Create automated tests
   - Set up CI/CD pipeline

---

## Support & Resources

- **LTI 1.3 Specification**: https://www.imsglobal.org/spec/lti/v1p3/
- **Google Cloud Console**: https://console.cloud.google.com
- **ChemVantage**: https://www.chemvantage.org
- **JWT Debugger**: https://jwt.io

---

## Quick Commands Reference

```bash
# Deploy platform
./deploy.sh

# View logs
gcloud app logs tail -s default

# Re-deploy after changes
mvn clean package appengine:deploy

# Local development
mvn appengine:run

# Open in browser
gcloud app browse
```

---

**Happy Testing! 🧪**
