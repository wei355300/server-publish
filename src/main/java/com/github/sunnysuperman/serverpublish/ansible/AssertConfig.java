package com.github.sunnysuperman.serverpublish.ansible;

import java.util.List;

public class AssertConfig {
	private List<String> asserts;
	private boolean ignoreCase;

	public List<String> getAsserts() {
		return asserts;
	}

	public AssertConfig setAsserts(List<String> asserts) {
		this.asserts = asserts;
		return this;
	}

	public boolean isIgnoreCase() {
		return ignoreCase;
	}

	public AssertConfig setIgnoreCase(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
		return this;
	}

}
