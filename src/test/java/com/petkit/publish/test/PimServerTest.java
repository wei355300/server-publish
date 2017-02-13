package com.petkit.publish.test;

import junit.framework.TestCase;

import com.github.sunnysuperman.serverpublish.Bootstrap;

public class PimServerTest extends TestCase {
	private String projectHome = "/path/to/project-publish-home/pimserver";

	public void test() throws Exception {
		Bootstrap.run(new String[] { "-home", projectHome, "-profile", "cn-sandbox", "-version", "test",
				"-log", "/path/to/logback.xml" });
	}

	public void test2() throws Exception {
		Bootstrap
				.run(new String[] { "-home", projectHome, "-profile", "cn-sandbox", "-version", "test2" });
	}

}
