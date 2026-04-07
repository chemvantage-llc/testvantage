package org.testvantage;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestReportServlet extends HttpServlet {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<TestResult> results = OfyService.ofy().load().type(TestResult.class).list();

        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        out.println("<!DOCTYPE html><html><head>");
        out.println("<title>Test Report</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; max-width: 900px; margin: 50px auto; padding: 20px; }");
        out.println(".ok { background: #d4edda; border: 1px solid #c3e6cb; color: #155724; padding: 16px; border-radius: 5px; }");
        out.println(".warn { background: #fff3cd; border: 1px solid #ffeeba; color: #856404; padding: 16px; border-radius: 5px; }");
        out.println("ul { margin-top: 10px; }");
        out.println("li { margin: 8px 0; }");
        out.println("</style>");
        out.println("</head><body>");
        out.println("<h1>Test Report</h1>");

        if (results.isEmpty()) {
            out.println("<div class='warn'>No test results found.</div>");
            out.println("</body></html>");
            return;
        }

        Date latestStart = null;
        boolean allPassed = true;

        for (TestResult result : results) {
            Date start = result.getStartTime();
            if (start != null && (latestStart == null || start.after(latestStart))) {
                latestStart = start;
            }

            if (!result.isPassedTest()) {
                allPassed = false;
            }
        }

        if (allPassed) {
            String when = latestStart == null ? "unknown time" : DATE_FORMAT.format(latestStart);
            out.println("<div class='ok'>All tests passed at " + when + "</div>");
        } else {
            out.println("<div class='warn'>One or more tests failed:</div>");
            out.println("<ul>");
            for (TestResult result : results) {
                if (!result.isPassedTest()) {
                    String title = result.getTitle() == null ? "Unknown" : result.getTitle();
                    Date start = result.getStartTime();
                    String when = start == null ? "unknown time" : DATE_FORMAT.format(start);
                    out.println("<li>[" + title + "] test failed at " + when + "</li>");
                }
            }
            out.println("</ul>");
        }

        out.println("</body></html>");
    }
}