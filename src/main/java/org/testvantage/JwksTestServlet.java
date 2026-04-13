package org.testvantage;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@WebServlet(urlPatterns = "/test/jwks")
public class JwksTestServlet extends HttpServlet {

	private static final Gson GSON = new Gson();
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private static final String TEST_TITLE = "ChemVantage JWKS Endpoint";
	private static final String SUITE_ID = "jwks";
	private static final String SCENARIO_ID = "jwks-fetch-and-validate";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();
		String selectedTarget = req.getParameter("target");
		if (selectedTarget == null || selectedTarget.isEmpty()) {
			selectedTarget = "prod";
		}

		TestResult result = TestResult.load(TEST_TITLE);

		out.println("<!DOCTYPE html><html><head>");
		out.println("<title>JWKS Test</title>");
		out.println("<style>");
		out.println("body { font-family: Arial, sans-serif; max-width: 960px; margin: 50px auto; padding: 20px; }");
		out.println(".panel { background: #f5f5f5; border-radius: 6px; padding: 18px; margin: 18px 0; }");
		out.println(".success { background: #d4edda; border: 1px solid #c3e6cb; color: #155724; }");
		out.println(".error { background: #f8d7da; border: 1px solid #f5c6cb; color: #721c24; }");
		out.println(".code { background: #2d2d2d; color: #f8f8f2; padding: 15px; border-radius: 5px; white-space: pre-wrap; word-wrap: break-word; }");
		out.println("button { background: #1f6feb; color: white; padding: 10px 18px; border: none; border-radius: 4px; cursor: pointer; }");
		out.println("select { padding: 8px; margin-right: 10px; }");
		out.println("</style></head><body>");
		out.println("<h1>JWKS Endpoint Test</h1>");
		out.println("<p>Validates that ChemVantage publishes a parseable JWKS with at least one RSA signing key.</p>");

		out.println("<form method='POST' action='/test/jwks'>");
		out.println("<label for='target'>Target:</label>");
		out.println("<select name='target' id='target'>");
		out.println("<option value='prod'" + ("prod".equals(selectedTarget) ? " selected" : "") + ">Production</option>");
		out.println("<option value='dev'" + ("dev".equals(selectedTarget) ? " selected" : "") + ">Development</option>");
		out.println("</select>");
		out.println("<button type='submit'>Run JWKS Test</button>");
		out.println("</form>");

		if (result != null) {
			String panelClass = result.isPassedTest() ? "panel success" : "panel error";
			out.println("<div class='" + panelClass + "'>");
			out.println("<h2>Latest Result</h2>");
			out.println("<p><strong>Status:</strong> " + result.getStatus() + "</p>");
			out.println("<p><strong>Target:</strong> " + safe(result.getTargetUrl()) + "</p>");
			out.println("<p><strong>Completed:</strong> " + safe(String.valueOf(result.getCompletedAt())) + "</p>");
			out.println("<p><strong>Summary:</strong> " + safe(result.getSummary()) + "</p>");
			if (result.getDetails() != null) {
				out.println("<div class='code'>" + escapeHtml(result.getDetails()) + "</div>");
			}
			out.println("</div>");
		}

		out.println("</body></html>");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String targetUrl = ChemVantageTargets.resolve(req.getParameter("target"));
		TestResult result = TestResult.load(TEST_TITLE);
		if (result == null) {
			result = new TestResult();
			result.setTitle(TEST_TITLE);
		}

		result.setSuiteId(SUITE_ID);
		result.setScenarioId(SCENARIO_ID);
		result.setTargetUrl(targetUrl);
		Date startTime = new Date();
		result.setStartTime(startTime);

		try {
			JwksValidation validation = validateJwks(targetUrl + "/jwks");
			result.setElapsedTime(new Date().getTime() - startTime.getTime());
			result.setResponseText(validation.responseBody);
			if (validation.passed) {
				result.markPassed(validation.summary);
			} else {
				result.markFailed(validation.summary, validation.details);
			}
			result.setDetails(validation.details);
			result.save();
		} catch (Exception e) {
			result.setElapsedTime(new Date().getTime() - startTime.getTime());
			result.markError("JWKS validation failed with an exception.", e.getMessage());
			result.save();
		}

		resp.sendRedirect("/test/jwks?target=" + ChemVantageTargets.labelFor(targetUrl));
	}

	private JwksValidation validateJwks(String endpointUrl) throws IOException {
		HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
		HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(endpointUrl));
		request.setThrowExceptionOnExecuteError(false);
		HttpResponse response = request.execute();
		int statusCode = response.getStatusCode();
		String body = response.parseAsString();
		HttpHeaders headers = response.getHeaders();

		if (statusCode != HttpServletResponse.SC_OK) {
			return new JwksValidation(false, body,
					"JWKS endpoint returned HTTP " + statusCode + ".",
					"Expected HTTP 200 from " + endpointUrl + " but received " + statusCode + ".\nResponse:\n" + body);
		}

		if (headers.getContentType() == null || !headers.getContentType().toLowerCase().contains("json")) {
			return new JwksValidation(false, body,
					"JWKS endpoint did not return a JSON content type.",
					"Content-Type was: " + headers.getContentType());
		}

		JsonElement parsedBody;
		try {
			parsedBody = JsonParser.parseString(body);
		} catch (Exception e) {
			return new JwksValidation(false, body,
					"JWKS response was not valid JSON.",
					"JSON parse error: " + e.getMessage() + "\nResponse:\n" + body);
		}

		if (!parsedBody.isJsonObject()) {
			return new JwksValidation(false, body,
					"JWKS response was not a JSON object.",
					"Expected a top-level JSON object with a keys array.");
		}

		JsonObject jwks = parsedBody.getAsJsonObject();
		if (!jwks.has("keys") || !jwks.get("keys").isJsonArray()) {
			return new JwksValidation(false, body,
					"JWKS response did not contain a keys array.",
					"Expected a top-level keys array in the JWKS response.");
		}

		JsonArray keys = jwks.getAsJsonArray("keys");
		if (keys.size() == 0) {
			return new JwksValidation(false, body,
					"JWKS response contained no keys.",
					"Expected at least one JWK in the keys array.");
		}

		JsonObject firstRsaKey = null;
		for (JsonElement keyElement : keys) {
			if (keyElement.isJsonObject()) {
				JsonObject key = keyElement.getAsJsonObject();
				if (hasNonBlank(key, "kid") && hasNonBlank(key, "kty") && "RSA".equals(key.get("kty").getAsString())) {
					firstRsaKey = key;
					break;
				}
			}
		}

		if (firstRsaKey == null) {
			return new JwksValidation(false, body,
					"JWKS did not contain an RSA key with a kid.",
					"Expected at least one RSA signing key with a non-empty kid field.");
		}

		if (!hasNonBlank(firstRsaKey, "n") || !hasNonBlank(firstRsaKey, "e")) {
			return new JwksValidation(false, body,
					"RSA key was missing modulus or exponent.",
					"Required RSA fields n and e were not both present.\nKey:\n" + GSON.toJson(firstRsaKey));
		}

		String summary = "JWKS published " + keys.size() + " key(s) and exposed a valid RSA signing key.";
		String details = "Validated endpoint: " + endpointUrl + "\nRSA kid: " + firstRsaKey.get("kid").getAsString();
		return new JwksValidation(true, body, summary, details);
	}

	private boolean hasNonBlank(JsonObject jsonObject, String fieldName) {
		return jsonObject.has(fieldName)
				&& !jsonObject.get(fieldName).isJsonNull()
				&& !jsonObject.get(fieldName).getAsString().trim().isEmpty();
	}

	private String safe(String value) {
		return value == null ? "N/A" : escapeHtml(value);
	}

	private String escapeHtml(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static final class JwksValidation {
		private final boolean passed;
		private final String responseBody;
		private final String summary;
		private final String details;

		private JwksValidation(boolean passed, String responseBody, String summary, String details) {
			this.passed = passed;
			this.responseBody = responseBody;
			this.summary = summary;
			this.details = details;
		}
	}
}