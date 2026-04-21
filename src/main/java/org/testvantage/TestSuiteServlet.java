package org.testvantage;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.http.javanet.NetHttpTransport;

/**
 * Comprehensive test suite that runs all test variations on production
 */
@WebServlet(urlPatterns = "/test/suite")
public class TestSuiteServlet extends HttpServlet {

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final String SUITE_TITLE = "ChemVantage Complete Test Suite";


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Check if we should run the test suite
        String runParam = req.getParameter("run");
        if ("true".equals(runParam)) {
            runAllTests(req);
        }

        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        // Load all test results
        List<TestResultSummary> results = loadAllTestResults();

        out.println("<!DOCTYPE html><html><head>");
        out.println("<title>Complete Test Suite</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; max-width: 1200px; margin: 50px auto; padding: 20px; }");
        out.println(".panel { background: #f5f5f5; border-radius: 6px; padding: 18px; margin: 18px 0; }");
        out.println(".success { background: #d4edda; border: 1px solid #c3e6cb; color: #155724; }");
        out.println(".error { background: #f8d7da; border: 1px solid #f5c6cb; color: #721c24; }");
        out.println(".info { background: #d1ecf1; border: 1px solid #bee5eb; color: #0c5460; }");
        out.println(".warning { background: #fff3cd; border: 1px solid #ffeaa7; color: #856404; }");
        out.println(".code { background: #2d2d2d; color: #f8f8f2; padding: 15px; border-radius: 5px; white-space: pre-wrap; word-wrap: break-word; font-size: 0.85em; }");
        out.println("button { background: #1f6feb; color: white; padding: 12px 24px; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; }");
        out.println("button:hover { background: #1557b0; }");
        out.println("button:disabled { background: #6c757d; cursor: not-allowed; }");
        out.println("table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        out.println("th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }");
        out.println("th { background-color: #f8f9fa; font-weight: bold; }");
        out.println("tr:hover { background-color: #f5f5f5; }");
        out.println(".status-pass { color: #155724; font-weight: bold; }");
        out.println(".status-fail { color: #721c24; font-weight: bold; }");
        out.println(".status-unknown { color: #856404; font-weight: bold; }");
        out.println(".test-details { font-size: 0.9em; color: #555; }");
        out.println(".expand-btn { background: #6c757d; padding: 4px 12px; font-size: 12px; margin-left: 10px; }");
        out.println(".details-row { display: none; background: #f8f9fa; }");
        out.println(".details-content { padding: 15px; }");
        out.println("</style>");
        out.println("<script>");
        out.println("function toggleDetails(id) {");
        out.println("  var row = document.getElementById('details-' + id);");
        out.println("  if (row.style.display === 'none' || row.style.display === '') {");
        out.println("    row.style.display = 'table-row';");
        out.println("  } else {");
        out.println("    row.style.display = 'none';");
        out.println("  }");
        out.println("}");
        out.println("function runSuite() {");
        out.println("  document.getElementById('runBtn').disabled = true;");
        out.println("  document.getElementById('runBtn').textContent = 'Running Tests...';");
        out.println("  document.getElementById('status').innerHTML = '<div class=\"panel info\">Test suite is running. This may take 30-60 seconds...</div>';");
        out.println("  window.location.href = '/test/suite?run=true';");
        out.println("}");
        out.println("</script>");
        out.println("</head><body>");

        out.println("<h1>" + SUITE_TITLE + "</h1>");
        out.println("<p>Runs all test variations on production, including Registration (Canvas/Moodle) and Launch (Instructor/Student × Known/Unknown Assignment).</p>");

        out.println("<div id='status'>");
        if (req.getParameter("run") != null) {
            out.println("<div class='panel info'>Test suite completed. Results shown below.</div>");
        }
        out.println("</div>");

        out.println("<button id='runBtn' onclick='runSuite()'>Run Complete Test Suite</button>");

        // Test summary table
        out.println("<h2>Test Results Summary</h2>");
        int passCount = 0;
        int failCount = 0;
        int unknownCount = 0;

        for (TestResultSummary result : results) {
            if (result.isPassed()) passCount++;
            else if (result.isFailed()) failCount++;
            else unknownCount++;
        }

        out.println("<div class='panel'>");
        out.println("<strong>Total Tests:</strong> " + results.size() + " | ");
        out.println("<span class='status-pass'>Passed: " + passCount + "</span> | ");
        out.println("<span class='status-fail'>Failed: " + failCount + "</span> | ");
        out.println("<span class='status-unknown'>Unknown: " + unknownCount + "</span>");
        out.println("</div>");

        // Detailed results table
        out.println("<table>");
        out.println("<thead>");
        out.println("<tr>");
        out.println("<th>Test Name</th>");
        out.println("<th>Status</th>");
        out.println("<th>Summary</th>");
        out.println("<th>Completed</th>");
        out.println("<th>Actions</th>");
        out.println("</tr>");
        out.println("</thead>");
        out.println("<tbody>");

        int rowId = 0;
        for (TestResultSummary result : results) {
            rowId++;
            String statusClass = result.isPassed() ? "status-pass" : (result.isFailed() ? "status-fail" : "status-unknown");
            String statusText = result.getStatus();

            out.println("<tr>");
            out.println("<td><strong>" + safe(result.getTitle()) + "</strong></td>");
            out.println("<td class='" + statusClass + "'>" + statusText + "</td>");
            out.println("<td>" + safe(result.getSummary()) + "</td>");
            out.println("<td>" + safe(result.getCompletedAt()) + "</td>");
            out.println("<td><button class='expand-btn' onclick='toggleDetails(" + rowId + ")'>Details</button></td>");
            out.println("</tr>");

            // Hidden details row
            out.println("<tr id='details-" + rowId + "' class='details-row'>");
            out.println("<td colspan='5'>");
            out.println("<div class='details-content'>");
            out.println("<p><strong>Target:</strong> " + safe(result.getTargetUrl()) + "</p>");
            out.println("<p><strong>Elapsed Time:</strong> " + result.getElapsedTime() + " ms</p>");
            if (result.getDetails() != null && !result.getDetails().isEmpty()) {
                out.println("<p><strong>Details:</strong></p>");
                out.println("<div class='code'>" + escapeHtml(result.getDetails()) + "</div>");
            }
            out.println("</div>");
            out.println("</td>");
            out.println("</tr>");
        }

        out.println("</tbody>");
        out.println("</table>");

        out.println("<div class='panel info'>");
        out.println("<strong>Note:</strong> Tests are run against production (www.chemvantage.org). Individual test pages:");
        out.println("<ul>");
        out.println("<li><a href='/test/registration'>Registration Test</a></li>");
        out.println("<li><a href='/test/auth-token'>Auth Token Test</a></li>");
        out.println("<li><a href='/test/launch'>Launch Test</a></li>");
        out.println("<li><a href='/jwks'>JWKS Endpoint</a></li>");
        out.println("</ul>");
        out.println("</div>");

        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Redirect to GET
        resp.sendRedirect("/test/suite");
    }

    /**
     * Run all tests in the suite by making HTTP requests to individual test servlets
     */
    private void runAllTests(HttpServletRequest req) {
        String baseUrl = req.getScheme() + "://" + req.getServerName()
                + (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "");
        String issuer = baseUrl;

        try {
            // Registration tests: prod/canvas and prod/moodle
            triggerTestViaPost(baseUrl + "/test/registration", "target", "prod", "lms", "canvas");
            triggerTestViaPost(baseUrl + "/test/registration", "target", "prod", "lms", "moodle");

            // Launch tests: 4 variations
            triggerTestViaPost(baseUrl + "/test/launch", "target", "prod", "role", "instructor", "resourceLinkId", "test-resource-link-001", "issuer", issuer);
            triggerTestViaPost(baseUrl + "/test/launch", "target", "prod", "role", "instructor", "resourceLinkId", "test-resource-link-002", "issuer", issuer);
            triggerTestViaPost(baseUrl + "/test/launch", "target", "prod", "role", "student", "resourceLinkId", "test-resource-link-001", "issuer", issuer);
            triggerTestViaPost(baseUrl + "/test/launch", "target", "prod", "role", "student", "resourceLinkId", "test-resource-link-002", "issuer", issuer);

            // Auth token test
            triggerTestViaPost(baseUrl + "/test/auth-token", "target", "prod", "issuer", issuer, "deploymentId", "test-vantage-deployment-001", "launchType", "launch");
            // Run JWKS test
            triggerTestViaPost(baseUrl + "/test/jwks", "target", "prod");
        } catch (Exception e) {
            System.err.println("Error running test suite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Trigger a test by making an HTTP POST request
     */
    private void triggerTestViaPost(String url, String... params) {
        try {
            HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
            Map<String, String> paramMap = new HashMap<>();
            
            // Convert varargs to map
            for (int i = 0; i < params.length; i += 2) {
                if (i + 1 < params.length) {
                    paramMap.put(params[i], params[i + 1]);
                }
            }
            
            HttpRequest request = requestFactory.buildPostRequest(
                new GenericUrl(url),
                new UrlEncodedContent(paramMap));
            request.setFollowRedirects(false);
            request.setThrowExceptionOnExecuteError(false);
            
            request.execute();
            // Results are automatically saved to datastore by individual test servlets
        } catch (Exception e) {
            System.err.println("Failed to trigger test at " + url + ": " + e.getMessage());
        }
    }

    /**
     * Load all test results from the datastore
     */
    private List<TestResultSummary> loadAllTestResults() {
        List<TestResultSummary> results = new ArrayList<>();

        // Registration tests: prod/canvas and prod/moodle
        String[] registrationTests = {
            "ChemVantage Dynamic Registration - prod/canvas",
            "ChemVantage Dynamic Registration - prod/moodle"
        };

        // Launch tests: 4 variations
        String[] launchTests = {
            "ChemVantage LTI Launch - instructor/known_assignment",
            "ChemVantage LTI Launch - instructor/unknown_assignment",
            "ChemVantage LTI Launch - student/known_assignment",
            "ChemVantage LTI Launch - student/unknown_assignment"
        };

        // Auth token test
        String[] authTokenTests = {
            "ChemVantage Auth Token Workflow"
        };

        // JWKS test
        String[] jwksTests = {
            "ChemVantage JWKS Endpoint"
        };

        // Load all test results
        for (String testTitle : registrationTests) {
            TestResult result = TestResult.load(testTitle);
            if (result != null) {
                results.add(new TestResultSummary(result));
            } else {
                results.add(new TestResultSummary(testTitle, "NOT_RUN", "Test has not been executed yet."));
            }
        }

        for (String testTitle : launchTests) {
            TestResult result = TestResult.load(testTitle);
            if (result != null) {
                results.add(new TestResultSummary(result));
            } else {
                results.add(new TestResultSummary(testTitle, "NOT_RUN", "Test has not been executed yet."));
            }
        }

        for (String testTitle : authTokenTests) {
            TestResult result = TestResult.load(testTitle);
            if (result != null) {
                results.add(new TestResultSummary(result));
            } else {
                results.add(new TestResultSummary(testTitle, "NOT_RUN", "Test has not been executed yet."));
            }
        }

        for (String testTitle : jwksTests) {
            TestResult result = TestResult.load(testTitle);
            if (result != null) {
                results.add(new TestResultSummary(result));
            } else {
                results.add(new TestResultSummary(testTitle, "NOT_RUN", "Test has not been executed yet."));
            }
        }

        return results;
    }

    /**
     * Helper class to summarize test results
     */
    private static class TestResultSummary {
        private final String title;
        private final String status;
        private final String summary;
        private final String targetUrl;
        private final String completedAt;
        private final long elapsedTime;
        private final String details;

        public TestResultSummary(TestResult result) {
            this.title = result.getTitle();
            this.status = result.getStatus();
            this.summary = result.getSummary();
            this.targetUrl = result.getTargetUrl();
            this.completedAt = result.getCompletedAt() != null ? result.getCompletedAt().toString() : "N/A";
            this.elapsedTime = result.getElapsedTime();
            this.details = result.getDetails();
        }

        public TestResultSummary(String title, String status, String summary) {
            this.title = title;
            this.status = status;
            this.summary = summary;
            this.targetUrl = "N/A";
            this.completedAt = "N/A";
            this.elapsedTime = 0;
            this.details = null;
        }

        public String getTitle() { return title; }
        public String getStatus() { return status; }
        public String getSummary() { return summary != null ? summary : ""; }
        public String getTargetUrl() { return targetUrl != null ? targetUrl : "N/A"; }
        public String getCompletedAt() { return completedAt; }
        public long getElapsedTime() { return elapsedTime; }
        public String getDetails() { return details; }

        public boolean isPassed() {
            return "PASSED".equals(status);
        }

        public boolean isFailed() {
            return "FAILED".equals(status) || "ERROR".equals(status);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : escapeHtml(value);
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
