package org.testvantage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handles Deep Linking content return from tool
 */
public class DeepLinkingServlet extends HttpServlet {
    
    private static final Gson gson = new Gson();
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String jwt = req.getParameter("JWT");
        
        if (jwt == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing JWT parameter");
            return;
        }
        
        try {
            // In production, verify the JWT signature
            // For testing, we'll parse it without verification to see the content
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JWT format");
                return;
            }
            
            // Decode payload (base64url decode)
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            JsonObject claims = gson.fromJson(payload, JsonObject.class);
            
            // Extract deep linking content items
            String message = claims.toString();
            
            // Display the result
            resp.setContentType("text/html");
            resp.getWriter().println("<!DOCTYPE html>");
            resp.getWriter().println("<html><head>");
            resp.getWriter().println("<title>Deep Linking Response</title>");
            resp.getWriter().println("<style>");
            resp.getWriter().println("body { font-family: Arial; max-width: 900px; margin: 50px auto; padding: 20px; }");
            resp.getWriter().println(".success { background: #d4edda; padding: 15px; border-radius: 5px; margin: 15px 0; }");
            resp.getWriter().println(".content { background: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0; }");
            resp.getWriter().println("pre { white-space: pre-wrap; word-wrap: break-word; }");
            resp.getWriter().println("</style>");
            resp.getWriter().println("</head><body>");
            resp.getWriter().println("<h1>Deep Linking Content Received</h1>");
            resp.getWriter().println("<div class='success'>");
            resp.getWriter().println("<p>✓ Successfully received deep linking response from ChemVantage</p>");
            resp.getWriter().println("</div>");
            resp.getWriter().println("<h2>Content Items</h2>");
            resp.getWriter().println("<div class='content'>");
            resp.getWriter().println("<pre>" + message + "</pre>");
            resp.getWriter().println("</div>");
            resp.getWriter().println("<p><a href='/'>Back to Home</a></p>");
            resp.getWriter().println("</body></html>");
            
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Failed to process deep linking response: " + e.getMessage());
        }
    }
}
