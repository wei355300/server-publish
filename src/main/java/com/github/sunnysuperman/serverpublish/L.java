package com.github.sunnysuperman.serverpublish;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class L {
	private static final Logger LOG = LoggerFactory.getLogger(L.class);

	public static void info(String msg) {
		LOG.info(msg);
	}

	public static void error(String msg) {
		LOG.error(msg);
	}

	public static void error(Throwable ex) {
		LOG.error(null, ex);
	}

	public static void error(String msg, Throwable ex) {
		LOG.error(msg, ex);
	}
}
