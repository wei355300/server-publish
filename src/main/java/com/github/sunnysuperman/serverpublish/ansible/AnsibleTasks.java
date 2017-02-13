package com.github.sunnysuperman.serverpublish.ansible;

import com.github.sunnysuperman.serverpublish.L;

public abstract class AnsibleTasks {

	public final int execute(final String profile) throws Exception {
		long t1 = System.currentTimeMillis();
		int ret = doExecute(profile);
		long t2 = System.currentTimeMillis();
		L.info("AnsibleTasks done using " + ((t2 - t1) / 1000) + " seconds");
		return ret;
	}

	protected abstract int doExecute(final String profile) throws Exception;

}
