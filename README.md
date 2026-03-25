# Test Vantage - LTI Advantage Regression Tester

This application acts as an external LMS (Learning Management System) to test the ChemVantage LTI Advantage implementation at https://www.chemvantage.org.

## Features

This test app simulates an LMS and provides the following test capabilities:

- **Dynamic Registration**: Test the LTI 1.3 Dynamic Registration flow
- **Deep Linking**: Test content selection and assignment creation via Deep Linking
- **LTI Resource Link Launch**: Test standard LTI resource link launches
- **NRPS (Names and Role Provisioning Service)**: Test roster/membership data exchange
- **AGS (Assignment and Grade Service)**: Test grade passback functionality

## Architecture

The application is built using:
- Java 11
- Servlet API 4.0
- Google App Engine Standard Environment
- JWT (JSON Web Tokens) for LTI 1.3 security
- Google Cloud Datastore for persistence

## Project Structure

```
test-vantage/
├── pom.xml                          # Maven configuration
├── src/
│   └── main/
│       ├── java/org/testvantage/
│       │   ├── HomeServlet.java          # Main UI
│       │   ├── RegistrationServlet.java  # Dynamic Registration
│       │   ├── OIDCLoginServlet.java     # OIDC Login Initiation
│       │   ├── OIDCAuthServlet.java      # OIDC Auth Response
│       │   ├── LaunchServlet.java        # LTI Launch Initiation
│       │   ├── DeepLinkingServlet.java   # Deep Linking Response Handler
│       │   ├── JWKSServlet.java          # JWKS Endpoint
│       │   ├── NRPSServlet.java          # Mock NRPS Service
│       │   ├── AGSServlet.java           # Mock AGS Service
│       │   ├── LTIMessageBuilder.java    # LTI JWT Message Builder
│       │   └── KeyManager.java           # RSA Key Management
│       └── webapp/
│           └── WEB-INF/
│               ├── appengine-web.xml     # App Engine config
│               ├── web.xml               # Servlet mappings
│               └── logging.properties    # Logging config
└── README.md
```

## Platform Endpoints

When deployed, this app provides the following endpoints:

| Endpoint | Purpose |
|----------|---------|
| `/` | Home page with test controls |
| `/registration` | Dynamic Registration initiation |
| `/oidc/login` | OIDC authentication initiation |
| `/oidc/auth` | OIDC authentication redirect URI |
| `/jwks` | Public key set (JWKS) |
| `/launch` | LTI launch initiation |
| `/deeplink` | Deep Linking content return |
| `/nrps/context/{contextId}/memberships` | NRPS membership service |
| `/ags/context/{contextId}/lineitems` | AGS line items |
| `/ags/context/{contextId}/lineitems/{id}/scores` | AGS score submission |

## Platform Configuration

When registering this platform with ChemVantage, use these values:

- **Issuer**: `https://test-vantage.appspot.com` (or your deployed URL)
- **Client ID**: `test-vantage-client`
- **Deployment ID**: `1`
- **Platform ID (GUID)**: `test-vantage-platform-guid`
- **Authorization Endpoint**: `https://test-vantage.appspot.com/oidc/auth`
- **Token Endpoint**: `https://test-vantage.appspot.com/oauth2/token`
- **JWKS URI**: `https://test-vantage.appspot.com/jwks`

## Deployment

### Prerequisites

1. Install Google Cloud SDK: https://cloud.google.com/sdk/docs/install
2. Authenticate with Google Cloud:
   ```bash
   gcloud auth login
   gcloud config set project test-vantage
   ```

### Build and Deploy

```bash
# Build the project
mvn clean package

# Deploy to App Engine
mvn appengine:deploy
```

Or use the App Engine Maven plugin directly:
```bash
mvn appengine:deploy -Dapp.deploy.projectId=test-vantage
```

### Local Development

To run locally for development:

```bash
mvn appengine:run
```

The app will be available at `http://localhost:8080`

## Testing Workflow

### 1. Dynamic Registration

1. Navigate to the home page
2. Click "Start Dynamic Registration"
3. Copy the provided registration URL
4. Use this URL with ChemVantage to complete registration
5. Store the returned client_id for future launches

### 2. Deep Linking Test

1. Click "Initiate Deep Linking" on the home page
2. The app will generate an LTI Deep Linking Request
3. ChemVantage should display content selection interface
4. After selection, content items are returned to `/deeplink`
5. Review the returned content items

### 3. LTI Launch Test

1. Click "Launch Resource Link"
2. The app generates a standard LTI Resource Link Request
3. ChemVantage should display the requested resource
4. Verify user information is passed correctly

### 4. NRPS Test

1. Click "Launch with NRPS"
2. The launch will include NRPS service endpoint in the JWT
3. ChemVantage can call `/nrps/context/{contextId}/memberships`
4. Verify roster data is retrieved (includes 1 instructor + 5 students)

### 5. AGS Test

1. Click "Launch with AGS"
2. The launch will include AGS service endpoint in the JWT
3. ChemVantage can:
   - Create line items at `/ags/context/{contextId}/lineitems`
   - Submit scores to `/ags/context/{contextId}/lineitems/{id}/scores`
4. Check console logs for received scores

### 6. Full Integration Test

1. Click "Full Integration Launch"
2. Tests all services together (NRPS + AGS)
3. Verify complete workflow from launch to grade passback

## Test Users

The platform provides these test users:

| Role | Name | Email | User ID |
|------|------|-------|---------|
| Instructor | Test Instructor | instructor@test-vantage.org | test-instructor-001 |
| Student | Test Student | student@test-vantage.org | test-student-001 |
| Admin | Test Admin | admin@test-vantage.org | test-admin-001 |

## Security Considerations

### Production Deployment

For production use, enhance security by:

1. **Key Management**: Store keys securely in Google Cloud Secret Manager
2. **JWT Verification**: Verify incoming JWTs from ChemVantage
3. **OAuth 2.0**: Implement proper OAuth 2.0 token endpoint for AGS/NRPS
4. **HTTPS Only**: Ensure all endpoints use HTTPS
5. **State Management**: Use secure session management or JWT-based state
6. **Rate Limiting**: Implement rate limiting on endpoints
7. **Audit Logging**: Log all LTI interactions for debugging

### Current Limitations

This is a test/development tool. Current limitations:

- Keys are generated in-memory and not persisted
- No actual OAuth 2.0 token validation
- Simplified state management
- No persistent storage of registrations
- Mock data for NRPS and AGS

## Troubleshooting

### Common Issues

**Issue**: Registration fails
- Verify the registration URL is correctly formatted
- Check that ChemVantage can reach the platform endpoints
- Ensure JWKS endpoint is accessible

**Issue**: Launch fails with invalid JWT
- Verify the JWKS endpoint returns valid keys
- Check that the JWT signature algorithm is RS256
- Ensure nonce and state are properly managed

**Issue**: NRPS/AGS endpoints return 401
- Verify the Authorization header includes a Bearer token
- In production, implement proper OAuth 2.0 token validation

## LTI 1.3 Compliance

This test platform implements:

- ✅ LTI 1.3 Core Specification
- ✅ OIDC-based authentication
- ✅ JWT-secured messages
- ✅ Dynamic Registration (IMS spec)
- ✅ Deep Linking 2.0
- ✅ Names and Role Provisioning Services (NRPS) 2.0
- ✅ Assignment and Grade Services (AGS) 2.0

## License

This is a testing tool for ChemVantage development. Use at your own discretion.

## Support

For issues related to:
- **This test platform**: Check application logs in Google Cloud Console
- **ChemVantage integration**: Contact ChemVantage support
- **LTI specification**: Refer to IMS Global Learning Consortium documentation

## Resources

- [IMS Global LTI 1.3 Specification](https://www.imsglobal.org/spec/lti/v1p3/)
- [LTI Advantage](https://www.imsglobal.org/lti-advantage-overview)
- [Google App Engine Documentation](https://cloud.google.com/appengine/docs)
- [ChemVantage](https://www.chemvantage.org)
