package org.testvantage;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.http.javanet.NetHttpTransport;

public class LaunchTestServlet extends HttpServlet {

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final String TEST_TITLE = "ChemVantage LTI Launch";
    private static final String SUITE_ID = "launch";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        String selectedTarget = req.getParameter("target");
        if (selectedTarget == null || selectedTarget.isEmpty()) {
            selectedTarget = "prod";
        }

        String selectedUseCase = req.getParameter("useCase");
        if (selectedUseCase == null || selectedUseCase.isEmpty()) {
            selectedUseCase = "instructor/known_assignment";
        }

        String baseUrl = req.getScheme() + "://" + req.getServerName()
                + (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "");
        String suggestedIssuer = getSuggestedIssuer(baseUrl);

        TestResult result = TestResult.load(TEST_TITLE + " - " + selectedUseCase);

        out.println("<!DOCTYPE html><html><head>");
        out.println("<title>LTI Launch Test</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; max-width: 960px; margin: 50px auto; padding: 20px; }");
        out.println(".panel { background: #f5f5f5; border-radius: 6px; padding: 18px; margin: 18px 0; }");
        out.println(".success { background: #d4edda; border: 1px solid #c3e6cb; color: #155724; }");
        out.println(".error { background: #f8d7da; border: 1px solid #f5c6cb; color: #721c24; }");
        out.println(".info { background: #d1ecf1; border: 1px solid #bee5eb; color: #0c5460; }");
        out.println(".code { background: #2d2d2d; color: #f8f8f2; padding: 15px; border-radius: 5px; white-space: pre-wrap; word-wrap: break-word; }");
        out.println("button { background: #1f6feb; color: white; padding: 10px 18px; border: none; border-radius: 4px; cursor: pointer; }");
        out.println("input, select { padding: 8px; margin: 4px 0 12px 0; width: 100%; box-sizing: border-box; }");
        out.println("label { display: block; font-weight: bold; margin-top: 10px; }");
        out.println(".use-case-desc { font-size: 0.9em; color: #555; margin-left: 20px; }");
        out.println("</style></head><body>");

        out.println("<h1>LTI Launch Test</h1>");
        out.println("<p>Simulates a platform sending an LTI 1.3 resource link launch to ChemVantage with different user roles and assignment contexts.</p>");

        out.println("<form method='POST' action='/test/launch'>");

        out.println("<label for='target'>Target</label>");
        out.println("<select name='target' id='target'>");
        out.println("<option value='prod'" + ("prod".equals(selectedTarget) ? " selected" : "") + ">Production</option>");
        out.println("<option value='dev'" + ("dev".equals(selectedTarget) ? " selected" : "") + ">Development</option>");
        out.println("</select>");

        out.println("<label for='role'>Use Case</label>");
        out.println("<select name='role' id='role'>");
        out.println("<option value='instructor'" + (selectedUseCase.contains("instructor") ? " selected" : "") + ">Instructor Launch</option>");
        out.println("<div class='role-desc'>User is an instructor launching an assignment</div>");
        out.println("<option value='student'" + (selectedUseCase.contains("student") ? " selected" : "") + ">Student Launch</option>");
        out.println("<div class='role-desc'>User is a student launching an assignment</div>");
        out.println("</select>");

        out.println("<label for='issuer'>Issuer (iss)</label>");
        out.println("<input type='text' name='issuer' id='issuer' value='" + safeAttribute(suggestedIssuer) + "'>");

        out.println("<label for='resourceLinkId'>Resource Link ID</label>");
        
        out.println("<select name='resourceLinkId' id='resourceLinkId'>");
        out.println("<option value='test-resource-link-001'>test-resource-link-001 (known assignment)</option>");
        out.println("<option value='test-resource-link-002'" + (selectedUseCase.contains("unknown_assignment") ? " selected" : "") + ">test-resource-link-002 (unknown assignment)</option>");
        out.println("</select>");

        out.println("<button type='submit'>Run Launch Test</button>");
        out.println("</form>");

        out.println("<div class='panel info'>");
        out.println("<strong>Pass logic:</strong> Must return HTTP 200 and valid HTML content. Response should contain course context and role-appropriate content.");
        out.println("</div>");

        if (result != null) {
            String panelClass = result.isPassedTest() ? "panel success" : "panel error";
            out.println("<div class='" + panelClass + "'>");
            out.println("<h2>Latest Result</h2>");
            out.println("<p><strong>Status:</strong> " + safe(result.getStatus()) + "</p>");
            out.println("<p><strong>Target:</strong> " + safe(result.getTargetUrl()) + "</p>");
            out.println("<p><strong>Completed:</strong> " + safe(String.valueOf(result.getCompletedAt())) + "</p>");
            out.println("<p><strong>Summary:</strong> " + safe(result.getSummary()) + "</p>");
            if (result.getDetails() != null) {
                out.println("<div class='code'>" + escapeHtml(result.getDetails()) + "</div>");
            }
            out.println("</div>");
        }

        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String targetUrl = ChemVantageTargets.resolve(req.getParameter("target"));
        String role = trimToNull(req.getParameter("role"));
        String issuer = trimToNull(req.getParameter("issuer"));
        String resourceLinkId = trimToNull(req.getParameter("resourceLinkId"));
        String useCase = "instructor/known_assignment"; // default use case

        if (issuer == null) {
            issuer = "https://test-vantage.appspot.com";
        }
        
        if (role == null || role.isEmpty() || role.equals("instructor")) {
            useCase = "instructor";
        } else {
            useCase = "student";
        }

        if (resourceLinkId == null) resourceLinkId = "test-resource-link-001";
        if (resourceLinkId.equals("test-resource-link-001")) {
            useCase = useCase + "/known_assignment";
        } else {
            useCase = useCase + "/unknown_assignment";
        }

        String resultTitle = TEST_TITLE + " - " + useCase;
        TestResult result = TestResult.load(resultTitle);
        if (result == null) {
            result = new TestResult();
            result.setTitle(resultTitle);
        }

        result.setSuiteId(SUITE_ID);
        result.setScenarioId("launch-" + useCase);
        result.setTargetUrl(targetUrl);
        Date startTime = new Date();
        result.setStartTime(startTime);
        String debugDetails = "target=" + targetUrl + "\nuseCase=" + useCase + "\nissuer=" + issuer;

        try {
            LaunchScenarioDetails scenario = getLaunchScenarioDetails(useCase);
            AuthTokenState authTokenState = fetchLaunchStateToken(targetUrl, issuer, targetUrl + "/lti/launch");
            String idToken = buildLaunchJwt(issuer, scenario, resourceLinkId);
            debugDetails = debugDetails
                + "\nauth_redirect_url=" + safe(authTokenState.redirectUrl)
                + "\nid_token_header=" + safe(getJwtHeaderForDebug(idToken));
            LaunchValidation validation = validateLaunchWorkflow(
                    targetUrl,
                    idToken,
                    authTokenState.state,
                    authTokenState.redirectUrl,
                    useCase);
            
            result.setElapsedTime(new Date().getTime() - startTime.getTime());
            result.setResponseText(validation.responseBody);
            result.setDetails(validation.details);
            if (validation.passed) {
                result.markPassed(validation.summary);
            } else {
                result.markFailed(validation.summary, validation.details);
            }
            result.save();
        } catch (Exception e) {
            result.setElapsedTime(new Date().getTime() - startTime.getTime());
            result.markError("Launch validation failed with an exception.",
                    debugDetails + "\nexception=" + e.getMessage());
            result.save();
        }

        resp.sendRedirect("/test/launch?target=" + ChemVantageTargets.labelFor(targetUrl) + "&useCase=" + useCase);
    }

    private String buildLaunchJwt(String issuer, LaunchScenarioDetails scenario, String resourceLinkId) throws Exception {
        KeyManager keyManager = KeyManager.getInstance();
        PrivateKey privateKey = keyManager.getPrivateKey();

        long now = System.currentTimeMillis() / 1000;
        Map<String, Object> claims = new HashMap<>();

        // Standard OIDC claims
        claims.put("iss", issuer);
        claims.put("sub", scenario.userId);
        claims.put("aud", "test-vantage-client");
        claims.put("exp", now + 3600);
        claims.put("iat", now);
        claims.put("nonce", "test-nonce-" + System.currentTimeMillis());

        // LTI specific claims
        claims.put("https://purl.imsglobal.org/spec/lti/claim/message_type", "LtiResourceLinkRequest");
        claims.put("https://purl.imsglobal.org/spec/lti/claim/version", "1.3.0");
        claims.put("https://purl.imsglobal.org/spec/lti/claim/deployment_id", "1");
        claims.put("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", 
                  "https://www.chemvantage.org/lti/launch");

        // User information
        claims.put("name", scenario.name);
        claims.put("given_name", scenario.givenName);
        claims.put("family_name", scenario.familyName);
        claims.put("email", scenario.email);

        // Roles
        claims.put("https://purl.imsglobal.org/spec/lti/claim/roles", scenario.roles);

        // Context (course)
        Map<String, Object> context = new HashMap<>();
        context.put("id", "test-context-001");
        context.put("label", "CHEM-101");
        context.put("title", "Introduction to Chemistry");
        context.put("type", Arrays.asList("http://purl.imsglobal.org/vocab/lis/v2/course#CourseOffering"));
        claims.put("https://purl.imsglobal.org/spec/lti/claim/context", context);

        // Platform
        Map<String, Object> platform = new HashMap<>();
        platform.put("guid", "test-vantage-platform-guid");
        platform.put("name", "Test Vantage LMS");
        platform.put("version", "1.0");
        claims.put("https://purl.imsglobal.org/spec/lti/claim/tool_platform", platform);

        // Launch presentation
        Map<String, Object> launchPresentation = new HashMap<>();
        launchPresentation.put("document_target", "iframe");
        launchPresentation.put("locale", "en-US");
        claims.put("https://purl.imsglobal.org/spec/lti/claim/launch_presentation", launchPresentation);

        // Resource link
        Map<String, Object> resourceLink = new HashMap<>();
        resourceLink.put("id", resourceLinkId);
        resourceLink.put("title", "Test Assignment");
        resourceLink.put("description", "A test assignment for ChemVantage");
        claims.put("https://purl.imsglobal.org/spec/lti/claim/resource_link", resourceLink);

        // Build and sign JWT
        String token = Jwts.builder()
                .setHeaderParam("kid", keyManager.getKeyId())
                .setHeaderParam("typ", "JWT")
                .setClaims(claims)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        return token;
    }

            private LaunchValidation validateLaunchWorkflow(
                String targetBaseUrl,
                String idToken,
                String state,
                String authRedirectUrl,
                String useCase)
            throws IOException {
        String endpointUrl = targetBaseUrl + "/lti/launch";
        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
            Map<String, String> formFields = new HashMap<>();
            formFields.put("id_token", idToken);
            formFields.put("state", state);
            HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(endpointUrl), new UrlEncodedContent(formFields));
        request.getHeaders().setAccept("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setThrowExceptionOnExecuteError(false);

        HttpResponse response = request.execute();
        int statusCode = response.getStatusCode();
        String contentType = response.getContentType();
        String body = response.parseAsString();
        String idTokenHeader = getJwtHeaderForDebug(idToken);

        if (statusCode != HttpServletResponse.SC_OK) {
            return new LaunchValidation(false, body,
                "Launch request was rejected by target (HTTP " + statusCode + ").",
                "Target rejected request to " + endpointUrl + "\n"
                    + "Auth redirect URL: " + safe(authRedirectUrl) + "\n"
                    + "id_token_header: " + safe(idTokenHeader) + "\n"
                    + "Expected HTTP 200 but received HTTP " + statusCode + ".\n"
                    + "Response Preview:\n" + fullResponseForFailure(body));
        }

        if (contentType == null || !contentType.toLowerCase().contains("html")) {
            return new LaunchValidation(false, body,
                    "Launch response was not HTML.",
                "Auth redirect URL: " + safe(authRedirectUrl) + "\n"
                    + "id_token_header: " + safe(idTokenHeader) + "\n"
                    + "Expected an HTML response but Content-Type was: " + contentType + "\n"
                    + "Response Preview:\n" + fullResponseForFailure(body));
        }

        if (body == null || body.trim().isEmpty()) {
            return new LaunchValidation(false, body,
                    "Launch response was empty.",
                "Auth redirect URL: " + safe(authRedirectUrl) + "\n"
                    + "id_token_header: " + safe(idTokenHeader) + "\n"
                    + "Expected HTML content but response body was empty.");
        }

        String expectedText = expectedBodyTextForUseCase(useCase);
        if (!body.contains(expectedText)) {
            return new LaunchValidation(false, body,
                    "Launch page did not contain expected scenario text.",
                    "Use case: " + useCase + "\n"
                    + "Auth redirect URL: " + safe(authRedirectUrl) + "\n"
                        + "id_token_header: " + safe(idTokenHeader) + "\n"
                            + "Expected page text: \"" + expectedText + "\"\n"
                    + "Response Preview:\n" + fullResponseForFailure(body));
        }

        String summary = "Launch request succeeded: HTTP 200 with expected scenario content.";
        String details = "Request endpoint: " + endpointUrl + "\n"
                + "Use case: " + useCase + "\n"
                + "State length: " + (state == null ? 0 : state.length()) + "\n"
                + "Auth redirect URL: " + safe(authRedirectUrl) + "\n"
                + "id_token_header: " + safe(idTokenHeader) + "\n"
                + "Expected page text: \"" + expectedText + "\"\n"
                + "Response length: " + body.length() + " characters\n"
                + "Response preview: " + summarizeBody(body);
        return new LaunchValidation(true, body, summary, details);
    }

    private AuthTokenState fetchLaunchStateToken(String targetBaseUrl, String issuer, String targetLinkUri)
            throws IOException {
        String endpointUrl = targetBaseUrl + "/auth/token";
        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
        GenericUrl authTokenUrl = new GenericUrl(endpointUrl);
        authTokenUrl.put("iss", issuer);
        authTokenUrl.put("login_hint", "1");
        authTokenUrl.put("target_link_uri", targetLinkUri);

        HttpRequest request = requestFactory.buildGetRequest(authTokenUrl);
        request.getHeaders().setAccept("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setThrowExceptionOnExecuteError(false);

        HttpResponse response = request.execute();
        int statusCode = response.getStatusCode();
        String contentType = response.getContentType();
        String body = response.parseAsString();

        if (statusCode != HttpServletResponse.SC_OK) {
            throw new IOException("/auth/token returned HTTP " + statusCode + ". Response: " + summarizeBody(body));
        }
        if (contentType == null || !contentType.toLowerCase().contains("html")) {
            throw new IOException("/auth/token did not return HTML. Content-Type: " + contentType);
        }

        String marker = "window.location.replace(";
        int markerIndex = body.indexOf(marker);
        if (markerIndex < 0) {
            throw new IOException("/auth/token response did not include window.location.replace(...).");
        }

        String redirectUrl = extractRedirectUrl(body, markerIndex + marker.length());
        if (redirectUrl == null || redirectUrl.isEmpty()) {
            throw new IOException("Could not parse redirect URL from /auth/token response.");
        }

        String state = extractQueryParam(redirectUrl, "state");
        if (state == null || state.trim().isEmpty()) {
            throw new IOException(
                    "Redirect URL from /auth/token did not include a non-empty state parameter. Auth redirect URL: "
                            + redirectUrl);
        }

        return new AuthTokenState(state, redirectUrl);
    }

    private String extractQueryParam(String url, String name) {
        if (url == null || name == null) {
            return null;
        }

        int queryIndex = url.indexOf('?');
        if (queryIndex < 0 || queryIndex == url.length() - 1) {
            return null;
        }

        String query = url.substring(queryIndex + 1);
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }
            int equalsIndex = pair.indexOf('=');
            String rawKey = equalsIndex >= 0 ? pair.substring(0, equalsIndex) : pair;
            String rawValue = equalsIndex >= 0 ? pair.substring(equalsIndex + 1) : "";
            String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
            if (name.equals(key)) {
                return URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private String extractRedirectUrl(String body, int startIndex) {
        int i = startIndex;
        while (i < body.length() && Character.isWhitespace(body.charAt(i))) {
            i++;
        }
        if (i >= body.length()) {
            return null;
        }

        char quote = body.charAt(i);
        if (quote != '\'' && quote != '"') {
            return null;
        }

        int end = body.indexOf(quote, i + 1);
        if (end < 0) {
            return null;
        }

        return body.substring(i + 1, end);
    }

    private String expectedBodyTextForUseCase(String useCase) {
        if ("instructor/known_assignment".equals(useCase)) {
            return "Instructor Page";
        }
        if ("student/known_assignment".equals(useCase)) {
            return "Homework";
        }
        if ("instructor/unknown_assignment".equals(useCase)) {
            return "Assignment Setup Page";
        }
        if ("student/unknown_assignment".equals(useCase)) {
            return "Please ask your instructor to click the same link in your LMS to complete the setup.";
        }
        return "Homework";
    }

    private LaunchScenarioDetails getLaunchScenarioDetails(String useCase) {
        if (useCase.contains("instructor")) {
            return new LaunchScenarioDetails("instructor-1", "1", "Dr. Smith", "Dr.", "Smith", 
                "smith@testlms.edu", Arrays.asList("http://purl.imsglobal.org/vocab/lis/v2/institution/person#Instructor"));
        } else if (useCase.contains("student")) {
            return new LaunchScenarioDetails("student-2", "2", "Alice Johnson", "Alice", "Johnson", 
                "alice@testlms.edu", Arrays.asList("http://purl.imsglobal.org/vocab/lis/v2/institution/person#Learner"));
        } else {
            return new LaunchScenarioDetails("user-0", "0", "Test User", "Test", "User", 
                "user@testlms.edu", Arrays.asList("http://purl.imsglobal.org/vocab/lis/v2/institution/person#Learner"));
        }
    }

    private String summarizeBody(String body) {
        if (body == null) {
            return "(empty)";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        int maxLen = 400;
        return compact.length() <= maxLen ? compact : compact.substring(0, maxLen) + "...";
    }

    private String fullResponseForFailure(String body) {
        return body == null ? "(empty)" : body;
    }

    private String getJwtHeaderForDebug(String jwt) {
        if (jwt == null) {
            return "(null)";
        }
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                return "(invalid compact JWT)";
            }
            String normalized = parts[0].replace('-', '+').replace('_', '/');
            int padding = (4 - (normalized.length() % 4)) % 4;
            StringBuilder padded = new StringBuilder(normalized);
            for (int i = 0; i < padding; i++) {
                padded.append('=');
            }
            return new String(Base64.getDecoder().decode(padded.toString()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "(header decode error: " + e.getMessage() + ")";
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String getSuggestedIssuer(String baseUrl) {
        if (baseUrl == null) {
            return "https://test-vantage.appspot.com";
        }
        String lower = baseUrl.toLowerCase();
        if (lower.contains("localhost") || lower.contains("127.0.0.1")) {
            return "https://test-vantage.appspot.com";
        }
        return baseUrl;
    }

    private String safe(String value) {
        return value == null ? "N/A" : escapeHtml(value);
    }

    private String safeAttribute(String value) {
        return value == null ? "" : escapeHtml(value).replace("\"", "&quot;");
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final class LaunchValidation {
        private final boolean passed;
        private final String responseBody;
        private final String summary;
        private final String details;

        private LaunchValidation(boolean passed, String responseBody, String summary, String details) {
            this.passed = passed;
            this.responseBody = responseBody;
            this.summary = summary;
            this.details = details;
        }
    }

    private static final class LaunchScenarioDetails {
        private final String userId;
        private final String name;
        private final String givenName;
        private final String familyName;
        private final String email;
        private final List<String> roles;

        private LaunchScenarioDetails(String userId, String subClaim, String name, String givenName, 
                                       String familyName, String email, List<String> roles) {
            this.userId = userId;
            this.name = name;
            this.givenName = givenName;
            this.familyName = familyName;
            this.email = email;
            this.roles = roles;
        }
    }

    private static final class AuthTokenState {
        private final String state;
        private final String redirectUrl;

        private AuthTokenState(String state, String redirectUrl) {
            this.state = state;
            this.redirectUrl = redirectUrl;
        }
    }
}
