package org.testvantage;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
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

/**
 * Tests Dynamic Registration workflow with ChemVantage
 * Two-stage process: Initial form request -> Form submission with contact info
 */
@WebServlet(urlPatterns = {"/test/registration", "/registration"})
public class RegistrationServlet extends HttpServlet {

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final String TEST_TITLE = "ChemVantage Dynamic Registration";
    private static final String SUITE_ID = "registration";
    private static final String REGISTRATION_CODE_DEV = "bfc12c656d84";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        String selectedTarget = req.getParameter("target");
        if (selectedTarget == null || selectedTarget.isEmpty()) {
            selectedTarget = "prod";
        }

        String selectedLms = req.getParameter("lms");
        if (selectedLms == null || selectedLms.isEmpty()) {
            selectedLms = "canvas";
        }

        String useCaseKey = selectedTarget + "/" + selectedLms;
        TestResult result = TestResult.load(TEST_TITLE + " - " + useCaseKey);

        out.println("<!DOCTYPE html><html><head>");
        out.println("<title>Dynamic Registration Test</title>");
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
        out.println(".use-case-desc { font-size: 0.9em; color: #555; margin-left: 20px; margin-top: -8px; }");
        out.println(".hidden { display: none; }");
        out.println("</style></head><body>");

        out.println("<h1>Dynamic Registration Test</h1>");
        out.println("<p>Tests the LTI 1.3 Dynamic Registration workflow with ChemVantage (two-stage process).</p>");

        out.println("<form method='POST' action='/test/registration'>");

        out.println("<label for='target'>Target</label>");
        out.println("<select name='target' id='target' onchange='toggleRegistrationCode()'>");
        out.println("<option value='prod'" + ("prod".equals(selectedTarget) ? " selected" : "") + ">Production</option>");
        out.println("<option value='dev'" + ("dev".equals(selectedTarget) ? " selected" : "") + ">Development</option>");
        out.println("</select>");

        out.println("<label for='lms'>LMS Platform</label>");
        out.println("<select name='lms' id='lms'>");
        out.println("<option value='canvas'" + ("canvas".equals(selectedLms) ? " selected" : "") + ">Canvas (Trusted)</option>");
        out.println("<option value='moodle'" + ("moodle".equals(selectedLms) ? " selected" : "") + ">Moodle (Untrusted)</option>");
        out.println("</select>");

        String registrationCodeDisplay = "dev".equals(selectedTarget) ? "" : " hidden";
        out.println("<div id='registrationCodeDiv' class='" + registrationCodeDisplay + "'>");
        out.println("<label for='registrationCode'>Registration Code (Dev Only)</label>");
        out.println("<input type='text' name='registrationCode' id='registrationCode' value='" + REGISTRATION_CODE_DEV + "' placeholder='Enter code for dev registration'>");
        out.println("</div>");

        out.println("<button type='submit'>Start Registration</button>");
        out.println("</form>");

        out.println("<div class='panel info'>");
        out.println("<strong>Stage 1 (Form Request):</strong> Must return HTTP 200 and HTML with 'LTI Advantage Dynamic Registration'<br>");
        out.println("<strong>Stage 2 (Form Submission):</strong> Expected text: 'Your Deployment is Active'<br/>");   // depends on target and LMS:<br>");
        /*
        out.println("<ul>");
        out.println("<li><strong>prod/canvas:</strong> 'Your Deployment is Active'</li>");
        out.println("<li><strong>prod/moodle:</strong> 'Your Deployment is Active'</li>");
        out.println("<li><strong>dev/canvas:</strong> 'Your Deployment is Active'</li>");
        out.println("<li><strong>dev/moodle:</strong> 'Your Deployment is Active'</li>");
        out.println("</ul>");
        */
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

        out.println("<script>");
        out.println("function toggleRegistrationCode() {");
        out.println("  var target = document.getElementById('target').value;");
        out.println("  var codeDiv = document.getElementById('registrationCodeDiv');");
        out.println("  if (target === 'dev') {");
        out.println("    codeDiv.classList.remove('hidden');");
        out.println("  } else {");
        out.println("    codeDiv.classList.add('hidden');");
        out.println("  }");
        out.println("}");
        out.println("</script>");

        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Check if this is a registration request from ChemVantage (identified by Bearer token or JSON content type)
        String authHeader = req.getHeader("Authorization");
        String contentType = req.getContentType();
        boolean isChemVantageRegistrationRequest = (authHeader != null && authHeader.startsWith("Bearer ")) ||
                (contentType != null && contentType.contains("application/json"));
        
        if (isChemVantageRegistrationRequest) {
            // Handle ChemVantage registration callback request
            handleChemVantageRegistration(req, resp);
            return;
        }
        
        // Handle test UI form submission (original logic)
        String targetUrl = ChemVantageTargets.resolve(req.getParameter("target"));
        String lms = trimToNull(req.getParameter("lms"));
        String registrationCode = trimToNull(req.getParameter("registrationCode"));
        String target = req.getParameter("target");

        if (lms == null) {
            lms = "canvas";
        }

        String useCaseKey = target + "/" + lms;
        String resultTitle = TEST_TITLE + " - " + useCaseKey;
        TestResult result = TestResult.load(resultTitle);
        if (result == null) {
            result = new TestResult();
            result.setTitle(resultTitle);
        }

        result.setSuiteId(SUITE_ID);
        result.setScenarioId("registration-" + useCaseKey);
        result.setTargetUrl(targetUrl);
        Date startTime = new Date();
        result.setStartTime(startTime);
        String debugDetails = "target=" + targetUrl + "\nuseCase=" + useCaseKey + "\ncontactEmail=" + "admin@testlms.edu";

        try {
            // Stage 1: Initiate registration and get the registration form
            RegistrationValidation stage1 = initiateRegistration(targetUrl, useCaseKey);
            if (!stage1.passed) {
                result.setElapsedTime(new Date().getTime() - startTime.getTime());
                result.markFailed(stage1.summary, debugDetails + "\n" + stage1.details);
                result.save();
                resp.sendRedirect("/test/registration?target=" + target + "&lms=" + lms);
                return;
            }

            // Stage 2: Submit registration form with contact information
            RegistrationValidation stage2 = submitRegistration(
                    targetUrl,
                    stage1.responseBody,
                    "TestVantage Admin",
                    "admin@testlms.edu",
                    registrationCode,
                    useCaseKey);

            result.setElapsedTime(new Date().getTime() - startTime.getTime());
            result.setResponseText(stage2.responseBody);
            result.setDetails(stage2.details);
            if (stage2.passed) {
                result.markPassed(stage2.summary);
            } else {
                result.markFailed(stage2.summary, stage2.details);
            }
            result.save();
        } catch (Exception e) {
            result.setElapsedTime(new Date().getTime() - startTime.getTime());
            result.markError("Registration validation failed with an exception.",
                    debugDetails + "\nexception=" + e.getMessage());
            result.save();
        }

        resp.sendRedirect("/test/registration?target=" + target + "&lms=" + lms);
    }
    
    private void handleChemVantageRegistration(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // This is a registration request from ChemVantage
        // Respond with JSON containing client_id, deployment_id, and LMS endpoints
        resp.setContentType("application/json");
        
        try {
            // Read the request body
            java.io.BufferedReader reader = req.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            // Build response JSON with registration details
            com.google.gson.JsonObject responseJson = new com.google.gson.JsonObject();
            responseJson.addProperty("client_id", "test-vantage-client-id-001");
            
            com.google.gson.JsonObject ltiToolConfig = new com.google.gson.JsonObject();
            ltiToolConfig.addProperty("deployment_id", "test-vantage-deployment-001");
            responseJson.add("https://purl.imsglobal.org/spec/lti-tool-configuration", ltiToolConfig);
            
            // Add LMS endpoints (placeholder values - customize as needed)
            responseJson.addProperty("auth_login_url", "https://test-vantage.appspot.com/oidc/login");
            responseJson.addProperty("auth_token_url", "https://test-vantage.appspot.com/oauth2/token");
            responseJson.addProperty("key_set_url", "https://test-vantage.appspot.com/jwks");
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(new com.google.gson.Gson().toJson(responseJson));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            com.google.gson.JsonObject errorJson = new com.google.gson.JsonObject();
            errorJson.addProperty("error", "Invalid registration request: " + e.getMessage());
            resp.getWriter().write(new com.google.gson.Gson().toJson(errorJson));
        }
    }

    private RegistrationValidation initiateRegistration(String targetUrl, String useCaseKey) throws IOException {
        String endpointUrl = targetUrl + "/lti/registration";
        
        // Extract LMS type from useCaseKey (format: "target/lms")
        String lmsType = extractLmsType(useCaseKey);
        
        // The openid_configuration parameter points to a configuration endpoint on the TESTER
        // ChemVantage will fetch the tester's configuration from this URL
        // Include the LMS type as a query parameter so the config reflects the correct product_family_code
        String openidConfigUrl = "https://test-vantage.appspot.com/.well-known/openid-configuration?lms=" + lmsType;
        
        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
        
        // Build GET request with openid_configuration as a query parameter
        GenericUrl url = new GenericUrl(endpointUrl);
        url.put("openid_configuration", openidConfigUrl);
        
        HttpRequest request = requestFactory.buildGetRequest(url);
        request.setThrowExceptionOnExecuteError(false);

        HttpResponse response = request.execute();
        int statusCode = response.getStatusCode();
        String contentType = response.getContentType();
        String body = response.parseAsString();

        if (statusCode != HttpServletResponse.SC_OK) {
            return new RegistrationValidation(false, body,
                "Stage 1: Registration initiation rejected by target (HTTP " + statusCode + ").",
                "Target rejected GET request to " + endpointUrl + "\n"
                    + "openid_configuration parameter: " + openidConfigUrl + "\n"
                    + "Expected HTTP 200 but received HTTP " + statusCode + ".\n"
                    + "Response:\n" + fullResponseForFailure(body));
        }

        if (contentType == null || !contentType.toLowerCase().contains("html")) {
            return new RegistrationValidation(false, body,
                "Stage 1: Registration response was not HTML.",
                "Expected an HTML response but Content-Type was: " + contentType + "\n"
                    + "Response:\n" + fullResponseForFailure(body));
        }

        if (body == null || body.trim().isEmpty()) {
            return new RegistrationValidation(false, body,
                "Stage 1: Registration response was empty.",
                "Expected HTML content but response body was empty.");
        }

        String expectedText = "</div><h1>LTI Advantage Dynamic Registration";
        if (!body.contains(expectedText)) {
            return new RegistrationValidation(false, body,
                "Stage 1: Registration page did not contain expected text.",
                "Expected page text: \"" + expectedText + "\"\n"
                    + "Response:\n" + fullResponseForFailure(body));
        }

        String summary = "Stage 1: Registration form loaded successfully with expected text.";
        String details = "Request endpoint: " + endpointUrl + " (GET)\n"
                + "openid_configuration parameter (tester's config URL): " + openidConfigUrl + "\n"
                + "Use case: " + useCaseKey + "\n"
                + "Expected page text: \"" + expectedText + "\"\n"
                + "Response length: " + body.length() + " characters\n"
                + "Response preview: " + summarizeBody(body);
        return new RegistrationValidation(true, body, summary, details);
    }

    private RegistrationValidation submitRegistration(
            String targetUrl,
            String stage1Body,
            String contactName,
            String contactEmail,
            String registrationCode,
            String useCaseKey)
            throws IOException {
        
        // Extract form action from stage 1 response
        String formAction = extractFormAction(stage1Body);
        if (formAction == null || formAction.isEmpty()) {
            return new RegistrationValidation(false, stage1Body,
                "Stage 2: Could not find form action in registration form.",
                "Expected HTML form with action attribute in Stage 1 response.");
        }
        
        // Resolve relative URLs to absolute
        if (formAction.startsWith("/")) {
            formAction = targetUrl + formAction;
        } else if (!formAction.startsWith("http")) {
            // Relative path without leading slash
            int lastSlash = targetUrl.lastIndexOf("/");
            formAction = targetUrl.substring(0, lastSlash) + "/" + formAction;
        }
        
        // Extract registration_token and openid_configuration from stage 1 response
        String registrationToken = extractFormHiddenField(stage1Body, "registration_token");
        String openidConfigUrl = extractFormHiddenField(stage1Body, "openid_configuration");
        
        // If openid_configuration is not found in the form, construct it (fallback)
        if (openidConfigUrl == null || openidConfigUrl.isEmpty()) {
            String lmsType = extractLmsType(useCaseKey);
            openidConfigUrl = "https://test-vantage.appspot.com/.well-known/openid-configuration?lms=" + lmsType;
        }
        
        String endpointUrl = formAction;
        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();

        Map<String, String> params = new HashMap<>();
        params.put("name", contactName != null ? contactName : "");
        params.put("email", contactEmail != null ? contactEmail : "");
        params.put("org", "ChemVantage Regression Tester");
        params.put("url", "https://test-vantage.appspot.com");
        params.put("AcceptChemVantageTOS", "true");
        params.put("openid_configuration", openidConfigUrl);
        if (registrationToken != null && !registrationToken.isEmpty()) {
            params.put("registration_token", registrationToken);
        }
        if (useCaseKey.startsWith("dev") && registrationCode != null && !registrationCode.isEmpty()) {
            params.put("reg_code", registrationCode);
        }

        HttpRequest request = requestFactory.buildPostRequest(
                new GenericUrl(endpointUrl),
                new UrlEncodedContent(params));
        request.setThrowExceptionOnExecuteError(false);

        HttpResponse response = request.execute();
        int statusCode = response.getStatusCode();
        String contentType = response.getContentType();
        String body = response.parseAsString();

        if (statusCode != HttpServletResponse.SC_OK) {
            return new RegistrationValidation(false, body,
                "Stage 2: Registration submission rejected by target (HTTP " + statusCode + ").",
                "Target rejected request to " + endpointUrl + "\n"
                    + "Expected HTTP 200 but received HTTP " + statusCode + ".\n"
                    + "Response:\n" + fullResponseForFailure(body));
        }

        if (contentType == null || !contentType.toLowerCase().contains("html")) {
            return new RegistrationValidation(false, body,
                "Stage 2: Registration confirmation response was not HTML.",
                "Expected an HTML response but Content-Type was: " + contentType);
        }

        if (body == null || body.trim().isEmpty()) {
            return new RegistrationValidation(false, body,
                "Stage 2: Registration confirmation response was empty.",
                "Expected HTML content but response body was empty.");
        }

        String expectedText = getExpectedConfirmationText(useCaseKey);
        if (!body.contains(expectedText)) {
            return new RegistrationValidation(false, body,
                "Stage 2: Registration confirmation page did not contain expected text.",
                "Use case: " + useCaseKey + "\n"
                    + "Expected page text: \"" + expectedText + "\"\n"
                    + "Response:\n" + fullResponseForFailure(body));
        }

        String summary = "Stage 2: Registration submitted and confirmed successfully.";
        String details = "Request endpoint: " + endpointUrl + "\n"
                + "Use case: " + useCaseKey + "\n"
                + "Contact: " + contactEmail + "\n"
                + "Expected confirmation text: \"" + expectedText + "\"\n"
                + "Response length: " + body.length() + " characters\n"
                + "Response preview: " + summarizeBody(body);
        return new RegistrationValidation(true, body, summary, details);
    }
    
    private String extractLmsType(String useCaseKey) {
        // Extract LMS from useCaseKey format "target/lms"
        int slashIndex = useCaseKey.indexOf("/");
        if (slashIndex >= 0 && slashIndex < useCaseKey.length() - 1) {
            return useCaseKey.substring(slashIndex + 1);
        }
        return "canvas";
    }
    
    private String extractFormHiddenField(String html, String fieldName) {
        if (html == null || html.isEmpty()) {
            return null;
        }
        
        // Look for hidden input field with name and value (quoted)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "<input[^>]*name=[\"']?" + java.util.regex.Pattern.quote(fieldName) + "[\"']?[^>]*value=[\"']([^\"']*)[\"']",
            java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(html);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Also try with value before name (quoted)
        pattern = java.util.regex.Pattern.compile(
            "<input[^>]*value=[\"']([^\"']*)[\"'][^>]*name=[\"']?" + java.util.regex.Pattern.quote(fieldName) + "[\"']?",
            java.util.regex.Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(html);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Try unquoted value (for URLs with query parameters)
        // Match: name=field_name ... value=url_without_quotes
        pattern = java.util.regex.Pattern.compile(
            "<input[^>]*name=[\"']?" + java.util.regex.Pattern.quote(fieldName) + "[\"']?[^>]*value=([^\\s>]+)",
            java.util.regex.Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(html);
        
        if (matcher.find()) {
            String value = matcher.group(1);
            // Remove trailing > if present
            if (value.endsWith(">")) {
                value = value.substring(0, value.length() - 1);
            }
            return value;
        }
        
        // Also try value before name (unquoted)
        pattern = java.util.regex.Pattern.compile(
            "<input[^>]*value=([^\\s>]+)[^>]*name=[\"']?" + java.util.regex.Pattern.quote(fieldName) + "[\"']?",
            java.util.regex.Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(html);
        
        if (matcher.find()) {
            String value = matcher.group(1);
            // Remove trailing > if present
            if (value.endsWith(">")) {
                value = value.substring(0, value.length() - 1);
            }
            return value;
        }
        
        return null;
    }

    private String getExpectedConfirmationText(String useCaseKey) {
        return "Your Deployment is Active";
        /* 
        if (useCaseKey.contains("dev") || useCaseKey.contains("canvas")) {
            return "Your Deployment is Active";
        }
        return "Your Deployment is Currently Under Review";
        */
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
    
    private String extractFormAction(String html) {
        if (html == null || html.isEmpty()) {
            return null;
        }
        
        // Use regex to find form action - handles quoted and unquoted values
        // Matches: action="..." action='...' or action=...
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "<form[^>]+action=([\"']?)([^\"'\\s>]+)\\1",
            java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(html);
        
        if (matcher.find()) {
            return matcher.group(2);
        }
        
        return null;
    }

    private String safe(String value) {
        return value == null ? "N/A" : escapeHtml(value);
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final class RegistrationValidation {
        private final boolean passed;
        private final String responseBody;
        private final String summary;
        private final String details;

        private RegistrationValidation(boolean passed, String responseBody, String summary, String details) {
            this.passed = passed;
            this.responseBody = responseBody;
            this.summary = summary;
            this.details = details;
        }
    }
}
