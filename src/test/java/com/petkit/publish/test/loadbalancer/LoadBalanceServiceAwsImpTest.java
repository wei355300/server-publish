package com.petkit.publish.test.loadbalancer;

import java.util.Set;

import junit.framework.TestCase;

import com.github.sunnysuperman.commons.utils.JSONUtil;
import com.github.sunnysuperman.serverpublish.loadbalance.LoadBalanceServiceAwsImp;

public class LoadBalanceServiceAwsImpTest extends TestCase {
	private String loadBalancerId = "us-west-1/xx";
	private String serverId = "xx";

	public LoadBalanceServiceAwsImp getInstance() {
		return new LoadBalanceServiceAwsImp(null);
	}

	public void test_addBackendServer() throws Exception {
		getInstance().addBackendServer(loadBalancerId, serverId);
	}

	public void test_removeBackendServer() throws Exception {
		getInstance().removeBackendServer(loadBalancerId, serverId);
	}

	public void test_getHealthyBackendServers() throws Exception {
		Set<String> servers = getInstance().getHealthyBackendServers(loadBalancerId);
		System.out.println(JSONUtil.toJSONString(servers));
	}
}
