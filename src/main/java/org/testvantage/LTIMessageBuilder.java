package org.testvantage;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.PrivateKey;
import java.util.*;

/**
 * Builds LTI 1.3 JWT messages
 */
public class LTIMessageBuilder {
    
    public static String buildLTIMessage(HttpServletRequest req, String nonce, 
                                        String messageType, String loginHint) throws Exception {
        
        String baseUrl = req.getScheme() + "://" + req.getServerName() 
                + (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "");
        
        HttpSession session = req.getSession();
        boolean includeNRPS = Boolean.TRUE.equals(session.getAttribute("include_nrps"));
        boolean includeAGS = Boolean.TRUE.equals(session.getAttribute("include_ags"));
        
        // Build claims
        Map<String, Object> claims = new HashMap<>();
        
        // Standard OIDC claims
        claims.put("iss", baseUrl);
        claims.put("sub", getUserId(loginHint));
        claims.put("aud", "test-vantage-client");
        claims.put("exp", System.currentTimeMillis() / 1000 + 3600);
        claims.put("iat", System.currentTimeMillis() / 1000);
        claims.put("nonce", nonce);
        
        // LTI specific claims
        claims.put("https://purl.imsglobal.org/spec/lti/claim/message_type", messageType);
        claims.put("https://purl.imsglobal.org/spec/lti/claim/version", "1.3.0");
        claims.put("https://purl.imsglobal.org/spec/lti/claim/deployment_id", "1");
        
        // Target link URI
        claims.put("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", 
                  "https://www.chemvantage.org");
        
        // User information
        Map<String, Object> userInfo = getUserInfo(loginHint);
        claims.put("name", userInfo.get("name"));
        claims.put("given_name", userInfo.get("given_name"));
        claims.put("family_name", userInfo.get("family_name"));
        claims.put("email", userInfo.get("email"));
        
        // Roles
        List<String> roles = getRoles(loginHint);
        claims.put("https://purl.imsglobal.org/spec/lti/claim/roles", roles);
        
        // Context (course)
        Map<String, Object> context = new HashMap<>();
        context.put("id", "test-context-001");
        context.put("label", "CHEM-101");
        context.put("title", "Introduction to Chemistry");
        context.put("type", Arrays.asList("http://purl.imsglobal.org/vocab/lis/v2/course#CourseOffering"));
        claims.put("https://purl.imsglobal.org/spec/lti/claim/context", context);
        
        // Platform
        Map<String, Object> platform = new HashMap<>();
        platform.put("guid", "test-vantage-platform-guid");
        platform.put("name", "Test Vantage LMS");
        platform.put("version", "1.0");
        platform.put("product_family_code", "test-vantage");
        claims.put("https://purl.imsglobal.org/spec/lti/claim/tool_platform", platform);
        
        // Launch presentation
        Map<String, Object> launchPresentation = new HashMap<>();
        launchPresentation.put("document_target", "iframe");
        launchPresentation.put("return_url", baseUrl + "/launch");
        launchPresentation.put("locale", "en-US");
        claims.put("https://purl.imsglobal.org/spec/lti/claim/launch_presentation", launchPresentation);
        
        // LIS (Learning Information Services)
        Map<String, Object> lis = new HashMap<>();
        lis.put("person_sourcedid", getUserId(loginHint));
        lis.put("course_offering_sourcedid", "CHEM-101-2026-SPRING");
        claims.put("https://purl.imsglobal.org/spec/lti/claim/lis", lis);
        
        // Message-type specific claims
        if ("LtiDeepLinkingRequest".equals(messageType)) {
            Map<String, Object> deepLinking = new HashMap<>();
            deepLinking.put("deep_link_return_url", baseUrl + "/deeplink");
            deepLinking.put("accept_types", Arrays.asList("link", "ltiResourceLink"));
            deepLinking.put("accept_presentation_document_targets", Arrays.asList("iframe", "window"));
            deepLinking.put("accept_multiple", true);
            deepLinking.put("auto_create", false);
            claims.put("https://purl.imsglobal.org/spec/lti-dl/claim/deep_linking_settings", deepLinking);
        } else {
            // Resource link for standard launch
            Map<String, Object> resourceLink = new HashMap<>();
            resourceLink.put("id", "test-resource-link-001");
            resourceLink.put("title", "Test Assignment");
            resourceLink.put("description", "A test assignment for ChemVantage");
            claims.put("https://purl.imsglobal.org/spec/lti/claim/resource_link", resourceLink);
        }
        
        // NRPS (Names and Role Provisioning Service)
        if (includeNRPS) {
            Map<String, Object> nrps = new HashMap<>();
            nrps.put("context_memberships_url", 
                    baseUrl + "/nrps/context/test-context-001/memberships");
            nrps.put("service_versions", Arrays.asList("2.0"));
            claims.put("https://purl.imsglobal.org/spec/lti-nrps/claim/namesroleservice", nrps);
        }
        
        // AGS (Assignment and Grade Service)
        if (includeAGS) {
            Map<String, Object> ags = new HashMap<>();
            ags.put("scope", Arrays.asList(
                "https://purl.imsglobal.org/spec/lti-ags/scope/lineitem",
                "https://purl.imsglobal.org/spec/lti-ags/scope/result.readonly",
                "https://purl.imsglobal.org/spec/lti-ags/scope/score"
            ));
            ags.put("lineitems", baseUrl + "/ags/context/test-context-001/lineitems");
            claims.put("https://purl.imsglobal.org/spec/lti-ags/claim/endpoint", ags);
        }
        
        // Custom parameters (optional)
        Map<String, Object> custom = new HashMap<>();
        custom.put("test_mode", "true");
        custom.put("regression_test", "true");
        claims.put("https://purl.imsglobal.org/spec/lti/claim/custom", custom);
        
        // Build and sign JWT
        KeyManager keyManager = KeyManager.getInstance();
        PrivateKey privateKey = keyManager.getPrivateKey();
        
        return Jwts.builder()
                .setHeader(Map.of(
                    "kid", keyManager.getKeyId(),
                    "typ", "JWT",
                    "alg", "RS256"
                ))
                .setClaims(claims)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
    
    private static String getUserId(String loginHint) {
        if (loginHint != null && loginHint.startsWith("test-")) {
            return loginHint;
        }
        return "test-student-001";
    }
    
    private static Map<String, Object> getUserInfo(String loginHint) {
        Map<String, Object> info = new HashMap<>();
        
        if (loginHint != null && loginHint.contains("instructor")) {
            info.put("name", "Test Instructor");
            info.put("given_name", "Test");
            info.put("family_name", "Instructor");
            info.put("email", "instructor@test-vantage.org");
        } else if (loginHint != null && loginHint.contains("admin")) {
            info.put("name", "Test Admin");
            info.put("given_name", "Test");
            info.put("family_name", "Admin");
            info.put("email", "admin@test-vantage.org");
        } else {
            info.put("name", "Test Student");
            info.put("given_name", "Test");
            info.put("family_name", "Student");
            info.put("email", "student@test-vantage.org");
        }
        
        return info;
    }
    
    private static List<String> getRoles(String loginHint) {
        if (loginHint != null && loginHint.contains("instructor")) {
            return Arrays.asList(
                "http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor",
                "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Instructor"
            );
        } else if (loginHint != null && loginHint.contains("admin")) {
            return Arrays.asList(
                "http://purl.imsglobal.org/vocab/lis/v2/membership#Administrator",
                "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Administrator"
            );
        } else {
            return Arrays.asList(
                "http://purl.imsglobal.org/vocab/lis/v2/membership#Learner",
                "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Student"
            );
        }
    }
}
