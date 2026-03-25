package org.testvantage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Handles LTI 1.3 Dynamic Registration
 */
public class RegistrationServlet extends HttpServlet {
    
    private static final Gson gson = new Gson();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String action = req.getParameter("action");
        String openid_configuration = req.getParameter("openid_configuration");
        String registration_token = req.getParameter("registration_token");
        
        if ("start".equals(action)) {
            // Display registration initiation page
            displayRegistrationStart(req, resp);
        } else if (openid_configuration != null) {
            // Handle registration request from tool
            handleRegistrationRequest(req, resp, openid_configuration, registration_token);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameters");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        // Handle registration submission from tool
        String contentType = req.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            handleToolRegistration(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expected application/json");
        }
    }
    
    private void displayRegistrationStart(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        
        String baseUrl = req.getScheme() + "://" + req.getServerName() 
                + (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "");
        
        String registrationUrl = baseUrl + "/registration?" 
                + "openid_configuration=" + baseUrl + "/.well-known/openid-configuration"
                + "&registration_token=" + UUID.randomUUID().toString();
        
        out.println("<!DOCTYPE html><html><head>");
        out.println("<title>Dynamic Registration</title>");
        out.println("<style>body { font-family: Arial; max-width: 800px; margin: 50px auto; padding: 20px; }");
        out.println(".code { background: #f5f5f5; padding: 15px; border-radius: 5px; overflow-x: auto; }</style>");
        out.println("</head><body>");
        out.println("<h1>Dynamic Registration</h1>");
        out.println("<p>Use this URL to initiate Dynamic Registration with ChemVantage:</p>");
        out.println("<div class='code'><code>" + registrationUrl + "</code></div>");
        out.println("<p><button onclick=\"navigator.clipboard.writeText('" + registrationUrl + "')\">Copy URL</button></p>");
        out.println("<p><a href='" + registrationUrl + "' target='_blank'>Or click here to continue</a></p>");
        out.println("</body></html>");
    }
    
    private void handleRegistrationRequest(HttpServletRequest req, HttpServletResponse resp,
                                          String openid_configuration, String registration_token) 
            throws IOException {
        
        // Return platform configuration for tool to use during registration
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        
        String baseUrl = req.getScheme() + "://" + req.getServerName() 
                + (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "");
        
        out.println("<!DOCTYPE html><html><head>");
        out.println("<title>Complete Registration</title>");
        out.println("<style>body { font-family: Arial; max-width: 800px; margin: 50px auto; padding: 20px; }");
        out.println(".code { background: #f5f5f5; padding: 10px; margin: 10px 0; border-radius: 5px; }</style>");
        out.println("</head><body>");
        out.println("<h1>Platform Registration Information</h1>");
        out.println("<p>Provide this information to ChemVantage:</p>");
        out.println("<div class='code'><strong>Issuer:</strong> " + baseUrl + "</div>");
        out.println("<div class='code'><strong>Authorization Endpoint:</strong> " + baseUrl + "/oidc/auth</div>");
        out.println("<div class='code'><strong>Token Endpoint:</strong> " + baseUrl + "/oauth2/token</div>");
        out.println("<div class='code'><strong>JWKS URI:</strong> " + baseUrl + "/jwks</div>");
        out.println("<div class='code'><strong>Registration Token:</strong> " + registration_token + "</div>");
        out.println("<p><a href='/'>Return to Home</a></p>");
        out.println("</body></html>");
    }
    
    private void handleToolRegistration(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        
        // Parse tool registration request
        JsonObject registration = gson.fromJson(req.getReader(), JsonObject.class);
        
        // Store registration in datastore (simplified for now)
        String clientId = "chemvantage-client-" + System.currentTimeMillis();
        
        // Build registration response
        JsonObject response = new JsonObject();
        response.addProperty("client_id", clientId);
        response.addProperty("client_name", registration.get("client_name").getAsString());
        
        String baseUrl = req.getScheme() + "://" + req.getServerName() 
                + (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "");
        
        response.addProperty("issuer", baseUrl);
        response.addProperty("jwks_uri", baseUrl + "/jwks");
        response.addProperty("token_endpoint", baseUrl + "/oauth2/token");
        response.addProperty("authorization_endpoint", baseUrl + "/oidc/auth");
        
        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(response));
    }
}
