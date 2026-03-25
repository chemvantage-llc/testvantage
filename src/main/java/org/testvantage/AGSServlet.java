package org.testvantage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock AGS (Assignment and Grade Service) endpoint
 * Handles line items and scores
 */
public class AGSServlet extends HttpServlet {
    
    private static final Gson gson = new Gson();
    
    // In-memory storage for testing
    private static final Map<String, JsonObject> lineItems = new ConcurrentHashMap<>();
    private static final Map<String, JsonArray> scores = new ConcurrentHashMap<>();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        
        // Verify bearer token (in production)
        String authorization = req.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid authorization");
            return;
        }
        
        if (pathInfo.endsWith("/lineitems")) {
            // GET line items
            getLineItems(req, resp);
        } else if (pathInfo.contains("/lineitems/") && pathInfo.endsWith("/scores")) {
            // GET scores for a line item
            getScores(req, resp);
        } else if (pathInfo.contains("/lineitems/")) {
            // GET specific line item
            getLineItem(req, resp, pathInfo);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        
        // Verify bearer token
        String authorization = req.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid authorization");
            return;
        }
        
        if (pathInfo.endsWith("/lineitems")) {
            // POST new line item
            createLineItem(req, resp);
        } else if (pathInfo.contains("/scores")) {
            // POST score
            createScore(req, resp, pathInfo);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    private void getLineItems(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        
        String contextId = extractContextId(req.getPathInfo());
        
        JsonArray items = new JsonArray();
        for (Map.Entry<String, JsonObject> entry : lineItems.entrySet()) {
            if (entry.getKey().startsWith(contextId)) {
                items.add(entry.getValue());
            }
        }
        
        resp.setContentType("application/vnd.ims.lis.v2.lineitemcontainer+json");
        resp.getWriter().write(gson.toJson(items));
    }
    
    private void getLineItem(HttpServletRequest req, HttpServletResponse resp, String pathInfo) 
            throws IOException {
        
        String lineItemId = extractLineItemId(pathInfo);
        JsonObject lineItem = lineItems.get(lineItemId);
        
        if (lineItem == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        resp.setContentType("application/vnd.ims.lis.v2.lineitem+json");
        resp.getWriter().write(gson.toJson(lineItem));
    }
    
    private void createLineItem(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        
        JsonObject lineItem = gson.fromJson(req.getReader(), JsonObject.class);
        
        String contextId = extractContextId(req.getPathInfo());
        String lineItemId = contextId + "-li-" + UUID.randomUUID().toString();
        
        String baseUrl = req.getScheme() + "://" + req.getServerName() 
                + (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "");
        
        String lineItemUrl = baseUrl + "/ags/context/" + contextId + "/lineitems/" + lineItemId;
        lineItem.addProperty("id", lineItemUrl);
        
        lineItems.put(lineItemId, lineItem);
        
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.setHeader("Location", lineItemUrl);
        resp.setContentType("application/vnd.ims.lis.v2.lineitem+json");
        resp.getWriter().write(gson.toJson(lineItem));
    }
    
    private void getScores(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        
        String lineItemId = extractLineItemId(req.getPathInfo());
        JsonArray scoresArray = scores.get(lineItemId);
        
        if (scoresArray == null) {
            scoresArray = new JsonArray();
        }
        
        resp.setContentType("application/vnd.ims.lis.v1.scorecontainer+json");
        resp.getWriter().write(gson.toJson(scoresArray));
    }
    
    private void createScore(HttpServletRequest req, HttpServletResponse resp, String pathInfo) 
            throws IOException {
        
        JsonObject score = gson.fromJson(req.getReader(), JsonObject.class);
        
        String lineItemId = extractLineItemId(pathInfo);
        
        JsonArray scoresArray = scores.get(lineItemId);
        if (scoresArray == null) {
            scoresArray = new JsonArray();
            scores.put(lineItemId, scoresArray);
        }
        
        score.addProperty("timestamp", new Date().toInstant().toString());
        scoresArray.add(score);
        
        System.out.println("Score received for line item " + lineItemId + ": " + gson.toJson(score));
        
        resp.setStatus(HttpServletResponse.SC_OK);
    }
    
    private String extractContextId(String pathInfo) {
        if (pathInfo != null && pathInfo.contains("/context/")) {
            String[] parts = pathInfo.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("context".equals(parts[i])) {
                    return parts[i + 1];
                }
            }
        }
        return "default-context";
    }
    
    private String extractLineItemId(String pathInfo) {
        if (pathInfo != null && pathInfo.contains("/lineitems/")) {
            String[] parts = pathInfo.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("lineitems".equals(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1].split("/")[0]; // Remove trailing path like /scores
                }
            }
        }
        return null;
    }
}
