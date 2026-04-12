package org.testvantage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AuthTokenTestServlet extends HttpServlet {

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final String TEST_TITLE = "ChemVantage Auth Token Workflow";
    private static final String SUITE_ID = "auth-token";
    private static final String SCENARIO_ID = "auth-token-oidc-redirect";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        String selectedTarget = req.getParameter("target");
        if (selectedTarget == null || selectedTarget.isEmpty()) {
            selectedTarget = "prod";
        }

        String selectedLaunchType = req.getParameter("launchType");
        if (selectedLaunchType == null || selectedLaunchType.isEmpty()) {
            selectedLaunchType = "launch";
        }

        String baseUrl = req.getScheme() + "://" + req.getServerName()
                + (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "");
        String suggestedIssuer = getSuggestedIssuer(baseUrl);

        TestResult result = TestResult.load(TEST_TITLE);

        out.println("<!DOCTYPE html><html><head>");
        out.println("<title>Auth Token Workflow Test</title>");
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
        out.println("</style></head><body>");

        out.println("<h1>Auth Token Workflow Test</h1>");
        out.println("<p>Validates the ChemVantage workflow you specified: GET <code>/auth/token</code> with <code>iss</code>, <code>login_hint</code>, and <code>target_link_uri</code>, then expect HTML containing <code>window.location.replace(...)</code> to platform OIDC URL.</p>");

        out.println("<form method='POST' action='/test/auth-token'>");

        out.println("<label for='target'>Target</label>");
        out.println("<select name='target' id='target'>");
        out.println("<option value='prod'" + ("prod".equals(selectedTarget) ? " selected" : "") + ">Production</option>");
        out.println("<option value='dev'" + ("dev".equals(selectedTarget) ? " selected" : "") + ">Development</option>");
        out.println("</select>");

        out.println("<label for='issuer'>Issuer (iss)</label>");
        out.println("<input type='text' name='issuer' id='issuer' value='" + safeAttribute(suggestedIssuer) + "'>");

        out.println("<label for='deploymentId'>Deployment ID (login_hint)</label>");
        out.println("<input type='text' name='deploymentId' id='deploymentId' value='1'>");

        out.println("<label for='launchType'>Target Link Endpoint</label>");
        out.println("<select name='launchType' id='launchType'>");
        out.println("<option value='launch'" + ("launch".equals(selectedLaunchType) ? " selected" : "") + ">/lti/launch</option>");
        out.println("<option value='deeplinks'" + ("deeplinks".equals(selectedLaunchType) ? " selected" : "") + ">/lti/deeplinks</option>");
        out.println("</select>");

        out.println("<button type='submit'>Run Auth Token Workflow Test</button>");
        out.println("</form>");

        out.println("<div class='panel info'>");
        out.println("<strong>Pass logic:</strong> Must return HTTP 200 HTML, contain <code>window.location.replace(...)</code>, include an OIDC redirect URL with a <code>state</code> parameter, and the <code>state</code> value must be a well-formed JWT.</strong>");
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
        String issuer = trimToNull(req.getParameter("issuer"));
        String deploymentId = trimToNull(req.getParameter("deploymentId"));
        String launchType = trimToNull(req.getParameter("launchType"));

        if (issuer == null) {
            issuer = "https://test-vantage.appspot.com";
        }
        if (deploymentId == null) {
            deploymentId = "1";
        }
        if (launchType == null || (!"launch".equals(launchType) && !"deeplinks".equals(launchType))) {
            launchType = "launch";
        }

        String targetLinkUri = targetUrl + ("deeplinks".equals(launchType) ? "/lti/deeplinks" : "/lti/launch");

        TestResult result = TestResult.load(TEST_TITLE);
        if (result == null) {
            result = new TestResult();
            result.setTitle(TEST_TITLE);
        }

        result.setSuiteId(SUITE_ID);
        result.setScenarioId(SCENARIO_ID + "-" + launchType);
        result.setTargetUrl(targetUrl);
        Date startTime = new Date();
        result.setStartTime(startTime);

        try {
            WorkflowValidation validation = validateAuthTokenWorkflow(targetUrl, issuer, deploymentId, targetLinkUri);
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
            result.markError("Auth token workflow validation failed with an exception.", e.getMessage());
            result.save();
        }

        resp.sendRedirect("/test/auth-token?target=" + ChemVantageTargets.labelFor(targetUrl) + "&launchType=" + launchType);
    }

    private WorkflowValidation validateAuthTokenWorkflow(String targetBaseUrl, String issuer, String deploymentId,
            String targetLinkUri) throws IOException {
        String endpointUrl = targetBaseUrl + "/auth/token";
        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
        GenericUrl url = new GenericUrl(endpointUrl);
        url.put("iss", issuer);
        url.put("login_hint", deploymentId);
        url.put("target_link_uri", targetLinkUri);

        HttpRequest request = requestFactory.buildGetRequest(url);
        request.getHeaders().setAccept("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setThrowExceptionOnExecuteError(false);

        HttpResponse response = request.execute();
        int statusCode = response.getStatusCode();
        String contentType = response.getContentType();
        String body = response.parseAsString();

        if (statusCode != HttpServletResponse.SC_OK) {
            return new WorkflowValidation(false, body,
                "Auth token workflow request was rejected by target (HTTP " + statusCode + ").",
                "Target rejected request URL: " + url.build() + "\n"
                    + "Expected HTTP 200 but received HTTP " + statusCode + ".\n"
                    + "Response body: " + summarizeBody(body));
        }

        if (contentType == null || !contentType.toLowerCase().contains("html")) {
            return new WorkflowValidation(false, body,
                    "Auth token workflow response was not HTML.",
                    "Expected an HTML response containing redirect script, but Content-Type was: " + contentType);
        }

        String marker = "window.location.replace(";
        int markerIndex = body.indexOf(marker);
        if (markerIndex < 0) {
            return new WorkflowValidation(false, body,
                    "Response did not include window.location.replace redirect script.",
                    "Expected HTML javascript with window.location.replace('[PLATFORM_OIDC_URL]').");
        }

        String redirectUrl = extractRedirectUrl(body, markerIndex + marker.length());
        if (redirectUrl == null) {
            return new WorkflowValidation(false, body,
                    "Could not parse redirect URL from window.location.replace.",
                    "Found redirect marker, but URL argument could not be extracted.");
        }

        boolean hasOidcPath = redirectUrl.contains("/oidc/auth");
        if (!hasOidcPath) {
            return new WorkflowValidation(false, body,
                    "Redirect URL does not appear to be a platform OIDC URL.",
                    "Expected redirect URL to include /oidc/auth, but got: " + redirectUrl);
        }

        String stateJwt = extractQueryParam(redirectUrl, "state");
        if (stateJwt == null || stateJwt.trim().isEmpty()) {
            return new WorkflowValidation(false, body,
                    "Redirect URL did not include a state parameter.",
                    "Expected state query parameter in redirect URL: " + redirectUrl);
        }

        StateJwtValidation stateValidation = validateStateJwtFormat(stateJwt);
        if (!stateValidation.passed) {
            return new WorkflowValidation(false, body,
                    "State parameter is not a well-formed JWT.",
                    "Redirect URL: " + redirectUrl + "\n" + stateValidation.details);
        }

        String summary = "Workflow passed: /auth/token returned HTML redirect and state is a well-formed JWT.";
        String details = "Request URL: " + url.build() + "\nRedirect URL: " + redirectUrl + "\n"
                + stateValidation.details;
        return new WorkflowValidation(true, body, summary, details);
    }

    private StateJwtValidation validateStateJwtFormat(String stateJwt) {
        try {
            String[] jwtParts = stateJwt.split("\\.");
            if (jwtParts.length != 3) {
                return new StateJwtValidation(false, "State value is not a compact JWS token.");
            }

            JsonObject header = parseJwtPart(jwtParts[0], "header");
            if (header == null) {
                return new StateJwtValidation(false, "JWT header could not be base64url-decoded into JSON.");
            }
            JsonObject payload = parseJwtPart(jwtParts[1], "payload");
            if (payload == null) {
                return new StateJwtValidation(false, "JWT payload could not be base64url-decoded into JSON.");
            }

            String alg = getString(header, "alg");
            if (alg == null || alg.trim().isEmpty()) {
                return new StateJwtValidation(false, "JWT header is missing alg.");
            }

            return new StateJwtValidation(true,
                    "State JWT is well-formed (3 segments; decodable JSON header/payload; alg=" + alg + ").");
        } catch (Exception e) {
            return new StateJwtValidation(false, "State JWT format check error: " + e.getMessage());
        }
    }

    private JsonObject parseJwtPart(String encodedPart, String partName) {
        try {
            String normalized = encodedPart.replace('-', '+').replace('_', '/');
            int padding = (4 - (normalized.length() % 4)) % 4;
            StringBuilder padded = new StringBuilder(normalized);
            for (int i = 0; i < padding; i++) {
                padded.append('=');
            }
            byte[] bytes = java.util.Base64.getDecoder().decode(padded.toString());
            JsonElement json = JsonParser.parseString(new String(bytes));
            return json.isJsonObject() ? json.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getString(JsonObject json, String fieldName) {
        if (json == null || !json.has(fieldName) || json.get(fieldName).isJsonNull()) {
            return null;
        }
        return json.get(fieldName).getAsString();
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

    private String summarizeBody(String body) {
        if (body == null) {
            return "(empty)";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        int maxLen = 400;
        return compact.length() <= maxLen ? compact : compact.substring(0, maxLen) + "...";
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

    private static final class WorkflowValidation {
        private final boolean passed;
        private final String responseBody;
        private final String summary;
        private final String details;

        private WorkflowValidation(boolean passed, String responseBody, String summary, String details) {
            this.passed = passed;
            this.responseBody = responseBody;
            this.summary = summary;
            this.details = details;
        }
    }

    private static final class StateJwtValidation {
        private final boolean passed;
        private final String details;

        private StateJwtValidation(boolean passed, String details) {
            this.passed = passed;
            this.details = details;
        }
    }
}
