package org.testvantage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Home servlet - provides UI for regression testing
 */
public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        
        String baseUrl = req.getScheme() + "://" + req.getServerName() 
                + (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "");
        
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Test Vantage - LTI Advantage Regression Tester</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; max-width: 1200px; margin: 50px auto; padding: 20px; }");
        out.println("h1 { color: #333; }");
        out.println(".section { background: #f5f5f5; padding: 20px; margin: 20px 0; border-radius: 5px; }");
        out.println("button { background: #4CAF50; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; margin: 5px; }");
        out.println("button:hover { background: #45a049; }");
        out.println(".endpoint { background: white; padding: 10px; margin: 10px 0; border-left: 4px solid #4CAF50; }");
        out.println(".code { background: #272822; color: #f8f8f2; padding: 15px; border-radius: 5px; overflow-x: auto; }");
        out.println("table { border-collapse: collapse; width: 100%; margin: 10px 0; }");
        out.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        out.println("th { background-color: #4CAF50; color: white; }");
        out.println(".status { padding: 15px; margin: 10px 0; border-radius: 5px; }");
        out.println(".success { background: #d4edda; border: 1px solid #c3e6cb; color: #155724; }");
        out.println(".error { background: #f8d7da; border: 1px solid #f5c6cb; color: #721c24; }");
        out.println(".info { background: #d1ecf1; border: 1px solid #bee5eb; color: #0c5460; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>🧪 Test Vantage - LTI Advantage Regression Tester</h1>");
        out.println("<p>This application acts as an external LMS to test the ChemVantage LTI Advantage implementation.</p>");
        
        // Target Information
        out.println("<div class='section'>");
        out.println("<h2>Target Application</h2>");
        out.println("<p><strong>ChemVantage URL:</strong> <a href='https://www.chemvantage.org' target='_blank'>https://www.chemvantage.org</a></p>");
        out.println("</div>");
        
        // Platform Endpoints
        out.println("<div class='section'>");
        out.println("<h2>Platform Endpoints (This App)</h2>");
        out.println("<table>");
        out.println("<tr><th>Endpoint</th><th>URL</th><th>Purpose</th></tr>");
        out.println("<tr><td>Registration URL</td><td><code>" + baseUrl + "/test/registration</code></td><td>Dynamic Registration initiation</td></tr>");
        out.println("<tr><td>OIDC Login</td><td><code>" + baseUrl + "/oidc/login</code></td><td>OIDC authentication initiation</td></tr>");
        out.println("<tr><td>OIDC Auth Response</td><td><code>" + baseUrl + "/oidc/auth</code></td><td>Redirect URI for authentication</td></tr>");
        out.println("<tr><td>JWKS</td><td><code>" + baseUrl + "/jwks</code></td><td>Public key set</td></tr>");
        out.println("<tr><td>Deep Linking Return</td><td><code>" + baseUrl + "/deeplink</code></td><td>Deep linking content return</td></tr>");
        out.println("<tr><td>NRPS Service</td><td><code>" + baseUrl + "/nrps/context/{contextId}/memberships</code></td><td>Names & Role Provisioning</td></tr>");
        out.println("<tr><td>AGS Line Items</td><td><code>" + baseUrl + "/ags/context/{contextId}/lineitems</code></td><td>Assignment & Grade Services</td></tr>");
        out.println("</table>");
        out.println("</div>");
        
        // Test Scenarios
        out.println("<div class='section'>");
        out.println("<h2>Test Scenarios</h2>");
        
        out.println("<h3>1. Dynamic Registration</h3>");
        out.println("<p>Initiate the Dynamic Registration flow with ChemVantage.</p>");
        out.println("<button onclick=\"window.location='/test/registration?action=start'\">Start Dynamic Registration</button>");
        out.println("<div class='info' style='margin-top: 10px;'>");
        out.println("<strong>Note:</strong> This will POST platform registration data to ChemVantage's registration endpoint.");
        out.println("</div>");

        out.println("<h3>1a. JWKS Endpoint Test</h3>");
        out.println("<p>Run an automated assertion-based test against the ChemVantage JWKS endpoint.</p>");
        out.println("<button onclick=\"window.location='/test/jwks'\">Open JWKS Test</button>");

        out.println("<h3>1b. Auth Token Endpoint Test</h3>");
        out.println("<p>Run the ChemVantage auth token workflow test: GET with iss/login_hint/target_link_uri and verify HTML redirects using window.location.replace(...).</p>");
        out.println("<button onclick=\"window.location='/test/auth-token'\">Open Auth Token Test</button>");

        out.println("<h3>1c. LTI Launch Test (4 Scenarios)</h3>");
        out.println("<p>Run automated tests for LTI resource link launch with different user roles and assignment contexts:</p>");
        out.println("<ul>");
        out.println("<li>Instructor launch for a known assignment</li>");
        out.println("<li>Student launch for a known assignment</li>");
        out.println("<li>Instructor launch for a new assignment</li>");
        out.println("<li>Student launch for a new assignment</li>");
        out.println("</ul>");
        out.println("<button onclick=\"window.location='/test/launch'\">Open Launch Test</button>");
        
        out.println("<h3>2. Deep Linking (Assignment Creation)</h3>");
        out.println("<p>Test deep linking to select and create content in ChemVantage.</p>");
        out.println("<button onclick=\"window.location='/launch?type=deeplink'\">Initiate Deep Linking</button>");
        
        out.println("<h3>3. LTI Resource Link Launch</h3>");
        out.println("<p>Test standard LTI resource link launch.</p>");
        out.println("<button onclick=\"window.location='/launch?type=resourcelink'\">Launch Resource Link</button>");
        
        out.println("<h3>4. NRPS (Names and Role Provisioning Service)</h3>");
        out.println("<p>Test roster/membership retrieval via NRPS.</p>");
        out.println("<button onclick=\"window.location='/launch?type=resourcelink&includeNRPS=true'\">Launch with NRPS</button>");
        
        out.println("<h3>5. AGS (Assignment and Grade Service)</h3>");
        out.println("<p>Test grade passback via AGS.</p>");
        out.println("<button onclick=\"window.location='/launch?type=resourcelink&includeAGS=true'\">Launch with AGS</button>");
        
        out.println("<h3>6. Full Integration Test</h3>");
        out.println("<p>Test all LTI Advantage services together.</p>");
        out.println("<button onclick=\"window.location='/launch?type=resourcelink&includeNRPS=true&includeAGS=true'\">Full Integration Launch</button>");
        
        out.println("</div>");
        
        // Configuration
        out.println("<div class='section'>");
        out.println("<h2>Platform Configuration</h2>");
        out.println("<table>");
        out.println("<tr><th>Property</th><th>Value</th></tr>");
        out.println("<tr><td>Issuer</td><td><code>" + baseUrl + "</code></td></tr>");
        out.println("<tr><td>Client ID</td><td><code>test-vantage-client</code></td></tr>");
        out.println("<tr><td>Deployment ID</td><td><code>1</code></td></tr>");
        out.println("<tr><td>Platform ID (GUID)</td><td><code>test-vantage-platform-guid</code></td></tr>");
        out.println("</table>");
        out.println("</div>");
        
        // Test Users
        out.println("<div class='section'>");
        out.println("<h2>Test Users</h2>");
        out.println("<table>");
        out.println("<tr><th>Role</th><th>Name</th><th>Email</th><th>User ID</th></tr>");
        out.println("<tr><td>Instructor</td><td>Test Instructor</td><td>instructor@test-vantage.org</td><td>test-instructor-001</td></tr>");
        out.println("<tr><td>Student</td><td>Test Student</td><td>student@test-vantage.org</td><td>test-student-001</td></tr>");
        out.println("<tr><td>Admin</td><td>Test Admin</td><td>admin@test-vantage.org</td><td>test-admin-001</td></tr>");
        out.println("</table>");
        out.println("</div>");
        
        // Logs
        out.println("<div class='section'>");
        out.println("<h2>Recent Activity</h2>");
        out.println("<div id='activity'>");
        out.println("<p><em>Activity will be logged here...</em></p>");
        out.println("</div>");
        out.println("</div>");

        out.println("<div class='section'>");
        out.println("<h2>Admin Integration</h2>");
        out.println("<p>Status JSON for ChemVantage Admin: <code>" + baseUrl + "/api/status</code></p>");
        out.println("<p>Compact banner JSON: <code>" + baseUrl + "/api/status?view=banner</code></p>");
        out.println("<p>Example filtered banner: <code>" + baseUrl + "/api/status?view=banner&amp;suiteId=jwks&amp;target=prod</code></p>");
        out.println("</div>");
        
        out.println("<script>");
        out.println("// Auto-refresh activity every 5 seconds");
        out.println("// In production, this would fetch from a logging endpoint");
        out.println("</script>");
        
        out.println("</body>");
        out.println("</html>");
    }
}
