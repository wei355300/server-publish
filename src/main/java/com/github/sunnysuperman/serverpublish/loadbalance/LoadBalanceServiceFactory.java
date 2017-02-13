package com.github.sunnysuperman.serverpublish.loadbalance;

import java.util.Map;

public class LoadBalanceServiceFactory {

	public static LoadBalanceService getInstance(String type, Map<?, ?> config) {
		if (type.equals("ali")) {
			return new LoadBalanceServiceAliImp(config);
		} else if (type.equals("aws")) {
			return new LoadBalanceServiceAwsImp(config);
		}
		throw new RuntimeException("No loadbalance type " + type);
	}
}
