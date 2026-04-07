package org.testvantage;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ObjectifyBootstrapListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		OfyService.init();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}
}