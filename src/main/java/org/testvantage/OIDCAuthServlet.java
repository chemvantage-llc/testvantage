package org.testvantage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Handles OIDC Authorization Response
 * This generates the id_token JWT and returns it to the tool
 */
public class OIDCAuthServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String state = req.getParameter("state");
        String nonce = req.getParameter("nonce");
        String redirect_uri = req.getParameter("redirect_uri");
        String login_hint = req.getParameter("login_hint");
        String lti_message_hint = req.getParameter("lti_message_hint");
        
        HttpSession session = req.getSession(false);
        if (session == null || !state.equals(session.getAttribute("oidc_state"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid state");
            return;
        }
        
        // Determine message type from hint or session
        String messageType = "LtiResourceLinkRequest";
        if (lti_message_hint != null && lti_message_hint.contains("deeplink")) {
            messageType = "LtiDeepLinkingRequest";
        }
        
        // Get stored target link URI
        String targetLinkUri = (String) session.getAttribute("target_link_uri");
        if (targetLinkUri == null) {
            targetLinkUri = redirect_uri;
        }
        
        try {
            // Generate LTI message JWT
            String idToken = LTIMessageBuilder.buildLTIMessage(
                req, 
                nonce, 
                messageType,
                login_hint
            );
            
            // Return as form post
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();
            
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head><title>Launching...</title></head>");
            out.println("<body>");
            out.println("<form id='ltiform' method='POST' action='" + targetLinkUri + "'>");
            out.println("<input type='hidden' name='id_token' value='" + idToken + "'/>");
            out.println("<input type='hidden' name='state' value='" + state + "'/>");
            out.println("</form>");
            out.println("<script>document.getElementById('ltiform').submit();</script>");
            out.println("<p>Launching to tool...</p>");
            out.println("</body>");
            out.println("</html>");
            
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Failed to generate id_token: " + e.getMessage());
        }
    }
}
