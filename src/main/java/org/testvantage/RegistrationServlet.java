package org.testvantage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.gson.GsonFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Handles LTI 1.3 Dynamic Registration
 * This platform initiates registration with the tool (ChemVantage)
 */
public class RegistrationServlet extends HttpServlet {
    
    private static final Gson gson = new Gson();
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String action = req.getParameter("action");
        
        if ("start".equals(action)) {
            // Display registration form
            displayRegistrationForm(req, resp);
        } else {
            resp.sendRedirect("/?action=start");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String toolRegistrationUrl = req.getParameter("registration_url");
        
        if (toolRegistrationUrl == null || toolRegistrationUrl.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing registration_url parameter");
            return;
        }
        
        // Perform registration with the tool
        performRegistration(req, resp, toolRegistrationUrl);
    }
    
    private void displayRegistrationForm(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        
        String baseUrl = req.getScheme() + "://" + req.getServerName() 
                + (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "");
        
        out.println("<!DOCTYPE html><html><head>");
        out.println("<title>LTI Dynamic Registration</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; max-width: 800px; margin: 50px auto; padding: 20px; }");
        out.println("h1 { color: #333; }");
        out.println(".form-group { margin: 20px 0; }");
        out.println("label { display: block; margin-bottom: 5px; font-weight: bold; }");
        out.println("input[type='text'] { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px; }");
        out.println("button { background: #4CAF50; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; }");
        out.println("button:hover { background: #45a049; }");
        out.println(".info { background: #d1ecf1; border: 1px solid #bee5eb; padding: 15px; border-radius: 5px; margin: 20px 0; }");
        out.println("</style>");
        out.println("</head><body>");
        
        out.println("<h1>Dynamic Registration with ChemVantage</h1>");
        out.println("<p>This will register Test Vantage as a platform with ChemVantage.</p>");
        
        out.println("<div class='info'>");
        out.println("<strong>Platform Information:</strong><br>");
        out.println("Issuer: " + baseUrl + "<br>");
        out.println("JWKS URI: " + baseUrl + "/jwks<br>");
        out.println("Auth Endpoint: " + baseUrl + "/oidc/auth<br>");
        out.println("Token Endpoint: " + baseUrl + "/oauth2/token");
        out.println("</div>");
        
        out.println("<form method='POST' action='/registration'>");
        out.println("<div class='form-group'>");
        out.println("<label for='registration_url'>ChemVantage Registration URL:</label>");
        out.println("<input type='text' id='registration_url' name='registration_url' ");
        out.println("placeholder='https://www.chemvantage.org/lti/registration' required>");
        out.println("</div>");
        out.println("<button type='submit'>Register Platform</button>");
        out.println("<a href='/' style='margin-left: 10px;'>Cancel</a>");
        out.println("</form>");
        
        out.println("</body></html>");
    }
    
    private void performRegistration(HttpServletRequest req, HttpServletResponse resp,
                                     String toolRegistrationUrl) throws IOException {
        
        String baseUrl = req.getScheme() + "://" + req.getServerName() 
                + (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "");
        
        // Build registration request payload
        JsonObject registrationRequest = new JsonObject();
        registrationRequest.addProperty("application_type", "web");
        registrationRequest.addProperty("grant_types", "client_credentials,implicit");
        registrationRequest.addProperty("response_types", "id_token");
        registrationRequest.addProperty("client_name", "Test Vantage LMS");
        registrationRequest.addProperty("client_uri", baseUrl);
        registrationRequest.addProperty("logo_uri", baseUrl + "/logo.png");
        registrationRequest.addProperty("tos_uri", baseUrl + "/tos");
        registrationRequest.addProperty("policy_uri", baseUrl + "/policy");
        registrationRequest.addProperty("jwks_uri", baseUrl + "/jwks");
        registrationRequest.addProperty("token_endpoint_auth_method", "private_key_jwt");
        
        JsonArray redirectUris = new JsonArray();
        redirectUris.add(baseUrl + "/oidc/auth");
        registrationRequest.add("redirect_uris", redirectUris);
        
        JsonArray scopes = new JsonArray();
        scopes.add("openid");
        registrationRequest.addProperty("scope", "openid");
        
        // LTI-specific claims
        JsonObject ltiToolConfiguration = new JsonObject();
        ltiToolConfiguration.addProperty("domain", req.getServerName());
        ltiToolConfiguration.addProperty("target_link_uri", baseUrl + "/launch");
        ltiToolConfiguration.addProperty("description", "Test Vantage - LTI Advantage Regression Testing Platform");
        
        JsonArray messages = new JsonArray();
        JsonObject resourceLink = new JsonObject();
        resourceLink.addProperty("type", "LtiResourceLinkRequest");
        resourceLink.addProperty("target_link_uri", baseUrl + "/launch");
        messages.add(resourceLink);
        
        JsonObject deepLinking = new JsonObject();
        deepLinking.addProperty("type", "LtiDeepLinkingRequest");
        deepLinking.addProperty("target_link_uri", baseUrl + "/launch");
        messages.add(deepLinking);
        
        ltiToolConfiguration.add("messages", messages);
        
        JsonArray claims = new JsonArray();
        claims.add("iss");
        claims.add("sub");
        claims.add("name");
        claims.add("given_name");
        claims.add("family_name");
        claims.add("email");
        ltiToolConfiguration.add("claims", claims);
        
        registrationRequest.add("https://purl.imsglobal.org/spec/lti-tool-configuration", ltiToolConfiguration);
        
        try {
            // Send registration request to tool
            HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
            HttpRequest httpRequest = requestFactory.buildPostRequest(
                new GenericUrl(toolRegistrationUrl),
                new JsonHttpContent(new GsonFactory(), registrationRequest)
            );
            httpRequest.getHeaders().setContentType("application/json");
            httpRequest.getHeaders().setAccept("application/json");
            
            HttpResponse httpResponse = httpRequest.execute();
            int statusCode = httpResponse.getStatusCode();
            String responseBody = httpResponse.parseAsString();
            
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("Registration failed with status " + statusCode + ": " + responseBody);
            }
            
            // Try to parse response - handle different response formats
            JsonObject registrationResponse;
            try {
                registrationResponse = gson.fromJson(responseBody, JsonObject.class);
            } catch (Exception parseException) {
                // If not valid JSON object, create a simple response with the body
                registrationResponse = new JsonObject();
                registrationResponse.addProperty("client_id", "unknown");
                registrationResponse.addProperty("response", responseBody);
            }
            
            // Display success page
            displaySuccessPage(resp, baseUrl, registrationResponse);
            
        } catch (Exception e) {
            displayErrorPage(resp, e.getMessage());
        }
    }
    
    private void displaySuccessPage(HttpServletResponse resp, String baseUrl, 
                                    JsonObject registrationResponse) throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        
        String clientId = registrationResponse.has("client_id") 
            ? registrationResponse.get("client_id").getAsString() 
            : "N/A";
        
        out.println("<!DOCTYPE html><html><head>");
        out.println("<title>Registration Successful</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; max-width: 900px; margin: 50px auto; padding: 20px; }");
        out.println(".success { background: #d4edda; border: 1px solid #c3e6cb; color: #155724; padding: 20px; border-radius: 5px; margin: 20px 0; text-align: center; }");
        out.println(".info { background: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0; }");
        out.println(".code { background: #2d2d2d; color: #f8f8f2; padding: 15px; border-radius: 5px; margin: 10px 0; overflow-x: auto; font-family: monospace; font-size: 12px; }");
        out.println("button { background: #4CAF50; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; margin: 10px; }");
        out.println("button:hover { background: #45a049; }");
        out.println("</style>");
        out.println("</head><body>");
        
        out.println("<div class='success'>");
        out.println("<h1>✓ Registration Successful!</h1>");
        out.println("<p>Test Vantage has been registered with ChemVantage.</p>");
        out.println("</div>");
        
        out.println("<div class='info'>");
        out.println("<h3>Registration Details:</h3>");
        out.println("<strong>Client ID:</strong> " + clientId + "<br>");
        out.println("<strong>Platform Issuer:</strong> " + baseUrl + "<br>");
        out.println("<strong>JWKS URI:</strong> " + baseUrl + "/jwks<br>");
        out.println("<strong>Auth Endpoint:</strong> " + baseUrl + "/oidc/auth<br>");
        out.println("<strong>Token Endpoint:</strong> " + baseUrl + "/oauth2/token");
        out.println("</div>");
        
        out.println("<div class='info'>");
        out.println("<h3>Full Response from ChemVantage:</h3>");
        out.println("<div class='code'>");
        out.println(gson.toJson(registrationResponse).replace("<", "&lt;").replace(">", "&gt;"));
        out.println("</div>");
        out.println("</div>");
        
        out.println("<div style='text-align: center;'>");
        out.println("<button onclick='window.close()'>Close Window</button>");
        out.println("<button onclick=\"window.location='/'\">Return to Home</button>");
        out.println("</div>");
        
        out.println("</body></html>");
    }
    
    private void displayErrorPage(HttpServletResponse resp, String errorMessage) 
            throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        
        out.println("<!DOCTYPE html><html><head>");
        out.println("<title>Registration Failed</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; max-width: 900px; margin: 50px auto; padding: 20px; }");
        out.println(".error { background: #f8d7da; border: 1px solid #f5c6cb; color: #721c24; padding: 20px; border-radius: 5px; margin: 20px 0; text-align: center; }");
        out.println(".details { background: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0; }");
        out.println(".code { background: #2d2d2d; color: #f8f8f2; padding: 15px; border-radius: 5px; margin: 10px 0; overflow-x: auto; font-family: monospace; font-size: 12px; white-space: pre-wrap; word-wrap: break-word; }");
        out.println("button { background: #4CAF50; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; margin: 10px; }");
        out.println("</style>");
        out.println("</head><body>");
        
        out.println("<div class='error'>");
        out.println("<h1>✗ Registration Failed</h1>");
        out.println("<p>Unable to complete registration with ChemVantage.</p>");
        out.println("</div>");
        
        out.println("<div class='details'>");
        out.println("<h3>Error Details:</h3>");
        out.println("<div class='code'>" + errorMessage.replace("<", "&lt;").replace(">", "&gt;") + "</div>");
        out.println("</div>");
        
        out.println("<div class='details'>");
        out.println("<h3>Troubleshooting:</h3>");
        out.println("<ul>");
        out.println("<li>Verify the ChemVantage registration URL is correct</li>");
        out.println("<li>Check that ChemVantage's registration endpoint is accessible</li>");
        out.println("<li>Ensure ChemVantage expects the LTI 1.3 Dynamic Registration format</li>");
        out.println("<li>Check the error message above for specific details</li>");
        out.println("</ul>");
        out.println("</div>");
        
        out.println("<div style='text-align: center;'>");
        out.println("<button onclick=\"window.location='/registration?action=start'\">Try Again</button>");
        out.println("<button onclick=\"window.location='/'\">Return to Home</button>");
        out.println("</div>");
        
        out.println("</body></html>");
    }
}
