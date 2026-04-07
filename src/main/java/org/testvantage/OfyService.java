package org.testvantage;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

public final class OfyService {

	private static volatile boolean initialized;

	private OfyService() {
	}

	public static void init() {
		if (initialized) {
			return;
		}

		synchronized (OfyService.class) {
			if (initialized) {
				return;
			}

			ObjectifyService.init();
			ObjectifyService.register(TestResult.class);
			initialized = true;
		}
	}

	public static Objectify ofy() {
		init();
		return ObjectifyService.ofy();
	}

	public static ObjectifyFactory factory() {
		init();
		return ObjectifyService.factory();
	}
}