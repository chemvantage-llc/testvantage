package org.testvantage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * OAuth 2.0 Token endpoint
 * Provides access tokens for AGS and NRPS service calls
 */
public class OAuth2TokenServlet extends HttpServlet {
    
    private static final Gson gson = new Gson();
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String grantType = req.getParameter("grant_type");
        String scope = req.getParameter("scope");
        
        // Validate grant type
        if (!"client_credentials".equals(grantType)) {
            sendError(resp, "unsupported_grant_type", 
                     "Only client_credentials grant type is supported");
            return;
        }
        
        // In production, validate client credentials
        // For testing, we'll accept any credentials
        
        // Generate access token
        String accessToken = "test-vantage-token-" + UUID.randomUUID().toString();
        
        // Build response
        JsonObject response = new JsonObject();
        response.addProperty("access_token", accessToken);
        response.addProperty("token_type", "Bearer");
        response.addProperty("expires_in", 3600);
        
        if (scope != null) {
            response.addProperty("scope", scope);
        }
        
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("Pragma", "no-cache");
        resp.getWriter().write(gson.toJson(response));
    }
    
    private void sendError(HttpServletResponse resp, String error, String description) 
            throws IOException {
        JsonObject errorResponse = new JsonObject();
        errorResponse.addProperty("error", error);
        errorResponse.addProperty("error_description", description);
        
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(errorResponse));
    }
}
