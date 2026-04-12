package org.testvantage;

public final class ChemVantageTargets {

	public static final String PRODUCTION = "https://www.chemvantage.org";
	public static final String DEVELOPMENT = "https://dev-vantage-hrd.appspot.com";

	private ChemVantageTargets() {
	}

	public static String resolve(String target) {
		if (target == null || target.isEmpty() || "prod".equalsIgnoreCase(target)
				|| "production".equalsIgnoreCase(target)) {
			return PRODUCTION;
		}
		if ("dev".equalsIgnoreCase(target) || "development".equalsIgnoreCase(target)) {
			return DEVELOPMENT;
		}
		return target;
	}

	public static String labelFor(String targetUrl) {
		if (PRODUCTION.equals(targetUrl)) {
			return "production";
		}
		if (DEVELOPMENT.equals(targetUrl)) {
			return "dev";
		}
		return targetUrl;
	}
}