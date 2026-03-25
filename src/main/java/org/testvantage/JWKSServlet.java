package org.testvantage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Provides JWKS (JSON Web Key Set) endpoint
 * Returns the platform's public keys for JWT verification
 */
public class JWKSServlet extends HttpServlet {
    
    private static final Gson gson = new Gson();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        try {
            // Get or generate platform keys
            KeyManager keyManager = KeyManager.getInstance();
            
            JsonObject jwks = new JsonObject();
            JsonArray keys = new JsonArray();
            
            // Add RSA public key
            JsonObject key = new JsonObject();
            key.addProperty("kty", "RSA");
            key.addProperty("alg", "RS256");
            key.addProperty("use", "sig");
            key.addProperty("kid", keyManager.getKeyId());
            key.addProperty("n", keyManager.getPublicKeyModulus());
            key.addProperty("e", keyManager.getPublicKeyExponent());
            
            keys.add(key);
            jwks.add("keys", keys);
            
            resp.setContentType("application/json");
            resp.setHeader("Cache-Control", "max-age=3600");
            resp.getWriter().write(gson.toJson(jwks));
            
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Failed to generate JWKS: " + e.getMessage());
        }
    }
}
