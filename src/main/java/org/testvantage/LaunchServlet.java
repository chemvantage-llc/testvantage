package org.testvantage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Initiates LTI launches to ChemVantage
 */
public class LaunchServlet extends HttpServlet {
    
    private static final String CHEMVANTAGE_URL = "https://www.chemvantage.org";
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String type = req.getParameter("type");
        boolean includeNRPS = "true".equals(req.getParameter("includeNRPS"));
        boolean includeAGS = "true".equals(req.getParameter("includeAGS"));
        
        if (type == null) {
            type = "resourcelink";
        }
        
        // Store launch parameters in session
        req.getSession().setAttribute("launch_type", type);
        req.getSession().setAttribute("include_nrps", includeNRPS);
        req.getSession().setAttribute("include_ags", includeAGS);
        
        // Determine target URL based on launch type
        String targetUrl = CHEMVANTAGE_URL;
        String loginHint = "test-student-001";
        String messageHint = type;
        
        if ("deeplink".equals(type)) {
            loginHint = "test-instructor-001"; // Instructors typically do deep linking
            messageHint = "deeplink";
        }
        
        // Build OIDC login initiation request
        String baseUrl = req.getScheme() + "://" + req.getServerName() 
                + (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "");
        
        // For actual testing, this would call ChemVantage's OIDC login endpoint
        // For now, we'll simulate by going directly to our own OIDC flow
        String oidcLoginUrl = baseUrl + "/oidc/login"
                + "?iss=" + java.net.URLEncoder.encode(baseUrl, "UTF-8")
                + "&login_hint=" + loginHint
                + "&target_link_uri=" + java.net.URLEncoder.encode(targetUrl, "UTF-8")
                + "&lti_message_hint=" + messageHint
                + "&client_id=test-vantage-client";
        
        // Display launch page
        resp.setContentType("text/html");
        resp.getWriter().println("<!DOCTYPE html>");
        resp.getWriter().println("<html><head><title>Initiating Launch</title>");
        resp.getWriter().println("<style>body { font-family: Arial; max-width: 800px; margin: 50px auto; padding: 20px; }");
        resp.getWriter().println(".info { background: #e7f3ff; padding: 15px; border-radius: 5px; margin: 15px 0; }");
        resp.getWriter().println("</style></head><body>");
        resp.getWriter().println("<h1>Initiating LTI Launch</h1>");
        resp.getWriter().println("<div class='info'>");
        resp.getWriter().println("<p><strong>Launch Type:</strong> " + type + "</p>");
        resp.getWriter().println("<p><strong>Include NRPS:</strong> " + includeNRPS + "</p>");
        resp.getWriter().println("<p><strong>Include AGS:</strong> " + includeAGS + "</p>");
        resp.getWriter().println("<p><strong>Target:</strong> " + targetUrl + "</p>");
        resp.getWriter().println("</div>");
        resp.getWriter().println("<p>To complete this launch, you would typically redirect to:</p>");
        resp.getWriter().println("<pre>" + oidcLoginUrl + "</pre>");
        resp.getWriter().println("<p><strong>Note:</strong> In a real scenario, this would call ChemVantage's OIDC login endpoint. ");
        resp.getWriter().println("For manual testing, you'll need to configure ChemVantage with this platform's details and ");
        resp.getWriter().println("use ChemVantage's actual OIDC login URL.</p>");
        resp.getWriter().println("<p><a href='/'>Back to Home</a></p>");
        resp.getWriter().println("</body></html>");
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        // Handle launch response from tool
        String id_token = req.getParameter("id_token");
        
        if (id_token != null) {
            // This is a response from the tool after launch
            resp.setContentType("text/html");
            resp.getWriter().println("<!DOCTYPE html>");
            resp.getWriter().println("<html><head><title>Launch Response</title></head>");
            resp.getWriter().println("<body>");
            resp.getWriter().println("<h1>Launch Completed</h1>");
            resp.getWriter().println("<p>Received response from tool.</p>");
            resp.getWriter().println("<p><a href='/'>Back to Home</a></p>");
            resp.getWriter().println("</body></html>");
        }
    }
}
