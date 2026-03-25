package org.testvantage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Mock NRPS (Names and Role Provisioning Service) endpoint
 * Provides membership/roster information
 */
public class NRPSServlet extends HttpServlet {
    
    private static final Gson gson = new Gson();
    
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
        
        // Extract context ID from path
        // Path format: /nrps/context/{contextId}/memberships
        String contextId = extractContextId(pathInfo);
        
        // Build membership response
        JsonObject response = new JsonObject();
        response.addProperty("id", req.getRequestURL().toString());
        response.add("context", createContextObject(contextId));
        
        JsonArray members = new JsonArray();
        
        // Add instructor
        JsonObject instructor = new JsonObject();
        instructor.addProperty("status", "Active");
        instructor.addProperty("name", "Test Instructor");
        instructor.addProperty("picture", "");
        instructor.addProperty("given_name", "Test");
        instructor.addProperty("family_name", "Instructor");
        instructor.addProperty("email", "instructor@test-vantage.org");
        instructor.addProperty("user_id", "test-instructor-001");
        
        JsonArray instructorRoles = new JsonArray();
        instructorRoles.add("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor");
        instructor.add("roles", instructorRoles);
        members.add(instructor);
        
        // Add students
        for (int i = 1; i <= 5; i++) {
            JsonObject student = new JsonObject();
            student.addProperty("status", "Active");
            student.addProperty("name", "Test Student " + i);
            student.addProperty("picture", "");
            student.addProperty("given_name", "Test");
            student.addProperty("family_name", "Student" + i);
            student.addProperty("email", "student" + i + "@test-vantage.org");
            student.addProperty("user_id", "test-student-" + String.format("%03d", i));
            
            JsonArray studentRoles = new JsonArray();
            studentRoles.add("http://purl.imsglobal.org/vocab/lis/v2/membership#Learner");
            student.add("roles", studentRoles);
            members.add(student);
        }
        
        response.add("members", members);
        
        resp.setContentType("application/vnd.ims.lti-nrps.v2.membershipcontainer+json");
        resp.getWriter().write(gson.toJson(response));
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
    
    private JsonObject createContextObject(String contextId) {
        JsonObject context = new JsonObject();
        context.addProperty("id", contextId);
        context.addProperty("label", "TEST-101");
        context.addProperty("title", "Test Chemistry Course");
        return context;
    }
}
