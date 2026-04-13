package org.testvantage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * OpenID Connect Configuration endpoint
 * Returns platform metadata for discovery
 */
public class OIDCConfigurationServlet extends HttpServlet {
    
    private static final Gson gson = new Gson();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String baseUrl = req.getScheme() + "://" + req.getServerName() 
                + (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "");
        
        JsonObject config = new JsonObject();
        
        // Issuer
        config.addProperty("issuer", baseUrl);
        
        // Authorization endpoint
        config.addProperty("authorization_endpoint", baseUrl + "/oidc/auth");
        
        // Token endpoint
        config.addProperty("token_endpoint", baseUrl + "/oauth2/token");
        
        // JWKS URI
        config.addProperty("jwks_uri", baseUrl + "/jwks");
        
        // Registration endpoint (optional)
        config.addProperty("registration_endpoint", baseUrl + "/registration");
        
        // Supported scopes
        config.add("scopes_supported", gson.toJsonTree(new String[]{
            "openid"
        }));
        
        // Supported response types
        config.add("response_types_supported", gson.toJsonTree(new String[]{
            "id_token"
        }));
        
        // Supported response modes
        config.add("response_modes_supported", gson.toJsonTree(new String[]{
            "form_post"
        }));
        
        // Supported grant types
        config.add("grant_types_supported", gson.toJsonTree(new String[]{
            "implicit",
            "client_credentials"
        }));
        
        // Supported subject types
        config.add("subject_types_supported", gson.toJsonTree(new String[]{
            "public"
        }));
        
        // Supported signing algorithms
        config.add("id_token_signing_alg_values_supported", gson.toJsonTree(new String[]{
            "RS256"
        }));
        
        // Supported claims
        config.add("claims_supported", gson.toJsonTree(new String[]{
            "sub",
            "iss",
            "name",
            "given_name",
            "family_name",
            "email"
        }));
        
        // LTI specific - get LMS type from query parameter
        String lmsType = req.getParameter("lms");
        config.add("https://purl.imsglobal.org/spec/lti-platform-configuration", 
                  createLtiPlatformConfiguration(baseUrl, lmsType));
        
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "max-age=3600");
        resp.getWriter().write(gson.toJson(config));
    }
    
    private JsonObject createLtiPlatformConfiguration(String baseUrl, String lmsType) {
        JsonObject ltiConfig = new JsonObject();
        
        // product_family_code should match the LMS type (canvas, moodle, etc.)
        // Default to canvas if not specified
        if (lmsType == null || lmsType.isEmpty()) {
            lmsType = "canvas";
        }
        ltiConfig.addProperty("product_family_code", lmsType);
        ltiConfig.addProperty("version", "1.0");
        
        // Messages supported
        ltiConfig.add("messages_supported", gson.toJsonTree(new String[]{
            "LtiResourceLinkRequest",
            "LtiDeepLinkingRequest"
        }));
        
        // Variables
        ltiConfig.add("variables", gson.toJsonTree(new String[]{
            "User.id",
            "User.username",
            "Person.name.full",
            "Person.name.given",
            "Person.name.family",
            "Person.email.primary",
            "Context.id",
            "Context.title",
            "Context.label"
        }));
        
        return ltiConfig;
    }
}
