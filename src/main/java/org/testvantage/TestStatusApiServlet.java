package org.testvantage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class TestStatusApiServlet extends HttpServlet {

	private static final Gson GSON = new Gson();
	private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

	static {
		ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		List<TestResult> results = filterResults(
			OfyService.ofy().load().type(TestResult.class).list(),
			req.getParameter("suiteId"),
			resolveTargetFilter(req.getParameter("target")));
		boolean bannerOnly = "banner".equalsIgnoreCase(req.getParameter("view"));

		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");

		JsonObject payload = new JsonObject();
		payload.addProperty("generatedAt", formatDate(new Date()));

		if (results.isEmpty()) {
			payload.addProperty("overallStatus", TestResult.Status.UNKNOWN.name());
			payload.addProperty("message", "No test results found.");
			if (!bannerOnly) {
				payload.add("tests", new JsonArray());
				payload.add("failures", new JsonArray());
			}
			resp.getWriter().write(GSON.toJson(payload));
			return;
		}

		Date latestCompletedAt = null;
		boolean allPassed = true;
		JsonArray tests = new JsonArray();
		JsonArray failures = new JsonArray();

		for (TestResult result : results) {
			Date completedAt = result.getCompletedAt() != null ? result.getCompletedAt() : result.getStartTime();
			if (completedAt != null && (latestCompletedAt == null || completedAt.after(latestCompletedAt))) {
				latestCompletedAt = completedAt;
			}

			JsonObject test = new JsonObject();
			test.addProperty("title", result.getTitle());
			test.addProperty("suiteId", result.getSuiteId());
			test.addProperty("scenarioId", result.getScenarioId());
			test.addProperty("targetUrl", result.getTargetUrl());
			test.addProperty("status", result.getStatus());
			test.addProperty("startTime", formatDate(result.getStartTime()));
			test.addProperty("completedAt", formatDate(completedAt));
			test.addProperty("elapsedTimeMs", result.getElapsedTime());
			test.addProperty("summary", result.getSummary());
			test.addProperty("details", result.getDetails());
			tests.add(test);

			if (!result.isPassedTest()) {
				allPassed = false;
				failures.add(test.deepCopy());
			}
		}

		payload.addProperty("overallStatus", allPassed ? TestResult.Status.PASSED.name() : TestResult.Status.FAILED.name());
		payload.addProperty("lastRunAt", formatDate(latestCompletedAt));
		if (allPassed) {
			payload.addProperty("message", "All tests passed on " + formatDate(latestCompletedAt));
		} else {
			JsonObject firstFailure = failures.get(0).getAsJsonObject();
			payload.addProperty(
				"message",
				"[" + firstFailure.get("title").getAsString() + "] failed on "
						+ firstFailure.get("completedAt").getAsString());
		}
		if (!bannerOnly) {
			payload.add("tests", tests);
			payload.add("failures", failures);
		}

		resp.getWriter().write(GSON.toJson(payload));
	}

	private List<TestResult> filterResults(List<TestResult> results, String suiteId, String targetUrl) {
		List<TestResult> filtered = new ArrayList<TestResult>();
		for (TestResult result : results) {
			if (suiteId != null && !suiteId.isEmpty()) {
				if (result.getSuiteId() == null || !suiteId.equalsIgnoreCase(result.getSuiteId())) {
					continue;
				}
			}
			if (targetUrl != null && !targetUrl.isEmpty()) {
				if (result.getTargetUrl() == null || !targetUrl.equalsIgnoreCase(result.getTargetUrl())) {
					continue;
				}
			}
			filtered.add(result);
		}
		return filtered;
	}

	private String resolveTargetFilter(String target) {
		if (target == null || target.isEmpty()) {
			return null;
		}
		return ChemVantageTargets.resolve(target);
	}

	private String formatDate(Date date) {
		return date == null ? null : ISO_FORMAT.format(date);
	}
}