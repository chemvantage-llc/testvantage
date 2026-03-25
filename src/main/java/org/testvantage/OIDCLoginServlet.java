package org.testvantage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Handles OIDC Login Initiation (from tool)
 * This is called by the tool to initiate an LTI launch
 */
public class OIDCLoginServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        doPost(req, resp);
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        // Extract OIDC login parameters
        String iss = req.getParameter("iss");
        String login_hint = req.getParameter("login_hint");
        String target_link_uri = req.getParameter("target_link_uri");
        String lti_message_hint = req.getParameter("lti_message_hint");
        String client_id = req.getParameter("client_id");
        
        // Validate required parameters
        if (iss == null || login_hint == null || target_link_uri == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                "Missing required OIDC parameters");
            return;
        }
        
        // Generate state and nonce for security
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        
        // Store state and nonce in session for validation later
        req.getSession().setAttribute("oidc_state", state);
        req.getSession().setAttribute("oidc_nonce", nonce);
        req.getSession().setAttribute("target_link_uri", target_link_uri);
        req.getSession().setAttribute("login_hint", login_hint);
        
        // Build authorization redirect
        String baseUrl = req.getScheme() + "://" + req.getServerName() 
                + (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "");
        
        String redirect_uri = baseUrl + "/launch";
        
        // Redirect to platform's authorization endpoint (ourselves in this case)
        String authUrl = baseUrl + "/oidc/auth"
                + "?response_type=id_token"
                + "&scope=openid"
                + "&client_id=" + (client_id != null ? client_id : "test-client")
                + "&redirect_uri=" + java.net.URLEncoder.encode(redirect_uri, "UTF-8")
                + "&login_hint=" + java.net.URLEncoder.encode(login_hint, "UTF-8")
                + "&state=" + state
                + "&nonce=" + nonce
                + "&prompt=none"
                + "&response_mode=form_post";
        
        if (lti_message_hint != null) {
            authUrl += "&lti_message_hint=" + java.net.URLEncoder.encode(lti_message_hint, "UTF-8");
        }
        
        resp.sendRedirect(authUrl);
    }
}
