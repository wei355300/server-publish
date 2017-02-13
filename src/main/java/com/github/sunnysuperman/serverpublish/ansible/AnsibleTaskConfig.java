package com.github.sunnysuperman.serverpublish.ansible;

import java.util.Map;

public class AnsibleTaskConfig {
	private Map<String, Object> args;
	private AssertConfig assertConfig;

	public Map<String, Object> getArgs() {
		return args;
	}

	public AnsibleTaskConfig setArgs(Map<String, Object> args) {
		this.args = args;
		return this;
	}

	public AssertConfig getAssertConfig() {
		return assertConfig;
	}

	public AnsibleTaskConfig setAssertConfig(AssertConfig assertConfig) {
		this.assertConfig = assertConfig;
		return this;
	}

}
