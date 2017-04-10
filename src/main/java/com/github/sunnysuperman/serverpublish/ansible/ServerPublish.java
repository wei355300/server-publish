package com.github.sunnysuperman.serverpublish.ansible;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.github.sunnysuperman.commons.bean.Bean;
import com.github.sunnysuperman.commons.utils.FormatUtil;
import com.github.sunnysuperman.commons.utils.JSONUtil;
import com.github.sunnysuperman.commons.utils.StringUtil;
import com.github.sunnysuperman.serverpublish.L;
import com.github.sunnysuperman.serverpublish.U;
import com.github.sunnysuperman.serverpublish.loadbalance.BackendServer;
import com.github.sunnysuperman.serverpublish.loadbalance.LoadBalanceService;
import com.github.sunnysuperman.serverpublish.loadbalance.LoadBalanceServiceFactory;

public class ServerPublish {
	public static class PublishServerSubTask {
		private String name;
		private boolean disabled;
		private String hostMode;
		private String asserts = "~~~update successfully~~~";
		private Map<String, Object> args;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isDisabled() {
			return disabled;
		}

		public void setDisabled(boolean disabled) {
			this.disabled = disabled;
		}

		public String getHostMode() {
			return hostMode;
		}

		public void setHostMode(String hostMode) {
			this.hostMode = hostMode;
		}

		public String getAsserts() {
			return asserts;
		}

		public void setAsserts(String asserts) {
			this.asserts = asserts;
		}

		public Map<String, Object> getArgs() {
			return args;
		}

		public void setArgs(Map<String, Object> args) {
			this.args = args;
		}

	}

	private static final String HOST_MODE_SINGLE = "single";
	private static final String HOST_MODE_GROUP = "group";
	private static final String HOST_MODE_BACKEND = "backend";

	private String projectHome;
	private String projectName;
	private String profile;
	private String version;

	public ServerPublish(String projectHome, String projectName, String profile, String version) {
		super();
		this.projectHome = projectHome;
		this.projectName = projectName;
		this.profile = profile;
		this.version = version;
	}

	public final int execute() throws Exception {
		long t1 = System.currentTimeMillis();
		int ret = doExecute();
		long t2 = System.currentTimeMillis();
		L.info("AnsibleTasks done using " + ((t2 - t1) / 1000) + " seconds");
		return ret;
	}

	private int doExecute() throws Exception {
		int ret;

		if (StringUtil.isEmpty(projectHome)) {
			throw new RuntimeException("project home should be set");
		}
		File projectHomeFile = new File(projectHome);
		if (!projectHomeFile.isDirectory()) {
			throw new RuntimeException("project home is not a directory: " + projectHome);
		}
		projectName = StringUtil.isNotEmpty(projectName) ? projectName : projectHomeFile.getName();

		if (StringUtil.isEmpty(profile)) {
			throw new RuntimeException("profile should be set");
		}

		File configDir = new File(projectHome + "/config");
		List<String> configNames = new ArrayList<>(3);
		configNames.add(projectName);
		configNames.add(projectName + "-" + profile);
		if (StringUtil.isNotEmpty(version)) {
			configNames.add(projectName + "-" + profile + "-" + version);
		}
		Map<String, Object> args = new HashMap<String, Object>();
		for (String configName : configNames) {
			File configFile = new File(configDir, configName + ".properties");
			if (!configFile.exists()) {
				throw new RuntimeException("config file " + configFile.getName() + " does not exists");
			}
			args.putAll(U.readPropertiesAsMap(new FileInputStream(configFile)));
		}
		if (args.isEmpty()) {
			throw new RuntimeException("bad config");
		}
		args.put("project_home", projectHome);
		args.put("project_name", projectName);
		args.put("profile", profile);
		List<PublishServerSubTask> tasks = Bean.fromJson(FormatUtil.parseString(args.remove("tasks")),
				PublishServerSubTask.class);
		args = Collections.unmodifiableMap(args);
		L.info("args: " + JSON.toJSONString(args, true));

		for (PublishServerSubTask task : tasks) {
			if (task.isDisabled()) {
				continue;
			}
			Map<String, Object> myArgs = new HashMap<>(args);
			if (task.getArgs() != null) {
				myArgs.putAll(task.getArgs());
			}
			myArgs.put("task_name", task.getName());
			AnsibleTaskConfig cfg = new AnsibleTaskConfig().setArgs(myArgs);
			if (task.getAsserts() != null) {
				if (task.getAsserts().isEmpty()) {
					cfg.setAssertConfig(null);
				} else {
					cfg.setAssertConfig(new AssertConfig().setAsserts(Collections.singletonList(task.getAsserts())));
				}
			}
			String hostMode = FormatUtil.parseString(task.getHostMode(), HOST_MODE_SINGLE);
			if (hostMode.equals(HOST_MODE_SINGLE)) {
				// execute directly
				ret = Ansible.execute(cfg);
				if (ret != 0) {
					return ret;
				}
			} else if (hostMode.equals(HOST_MODE_GROUP)) {
				ret = publishGroupServers(cfg);
				if (ret != 0) {
					return ret;
				}
			} else if (hostMode.equals(HOST_MODE_BACKEND)) {
				ret = publishBackendServers(cfg);
				if (ret != 0) {
					return ret;
				}
			} else {
				throw new RuntimeException("Bad hostMode " + hostMode);
			}
		}

		return 0;
	}

	private int publishGroupServers(AnsibleTaskConfig cfg) throws Exception {
		Map<String, Object> args = cfg.getArgs();
		String groupName = args.get("host").toString();
		List<BackendServer> updateServers = Bean.fromJson(args.get(groupName + "_servers").toString(),
				BackendServer.class);
		int ret;
		for (BackendServer server : updateServers) {
			args.put("host", FormatUtil.parseString(server.getIp()));
			ret = Ansible.execute(cfg);
			if (ret != 0) {
				return ret;
			}
		}
		return 0;
	}

	private int publishBackendServers(AnsibleTaskConfig cfg) throws Exception {
		Map<String, Object> args = cfg.getArgs();
		String groupName = args.get("host").toString();
		String loadBalancerType = FormatUtil.parseString(args.get(groupName + "_loadbalancer_type"));
		LoadBalanceService loadBalanceService = null;
		if (!loadBalancerType.equals("none")) {
			Map<?, ?> lbConfig = JSONUtil.parseJSONObject(FormatUtil.parseString(args.get(groupName
					+ "_loadbalancer_config")));
			if (lbConfig == null) {
				lbConfig = JSONUtil
						.parseJSONObject(FormatUtil.parseString(args.get("loadbalancer_" + loadBalancerType)));
			}
			loadBalanceService = LoadBalanceServiceFactory.getInstance(loadBalancerType, lbConfig);
		}

		List<BackendServer> updateServers = Bean.fromJson(args.get(groupName + "_servers").toString(),
				BackendServer.class);
		int ret;
		if (loadBalanceService == null) {
			for (BackendServer server : updateServers) {
				args.put("host", FormatUtil.parseString(server.getIp()));
				ret = Ansible.execute(cfg);
				if (ret != 0) {
					return ret;
				}
			}
		} else {
			String[] loadbalancers = StringUtil.splitAsArray(args.get(groupName + "_loadbalancer_id").toString(), ",");
			Set<String> healthyServers = loadBalanceService.getHealthyBackendServers(loadbalancers[0]);
			LinkedList<BackendServer> safeUpdateServers = new LinkedList<BackendServer>();
			for (BackendServer server : updateServers) {
				// 把不健康(或者新加入)的实例放到前面更新
				if (!healthyServers.contains(server.getId())) {
					safeUpdateServers.addFirst(server);
				} else {
					safeUpdateServers.addLast(server);
				}
			}
			for (BackendServer server : safeUpdateServers) {
				// remove backend server
				for (String loadbalancer : loadbalancers) {
					while (true) {
						try {
							loadBalanceService.removeBackendServer(loadbalancer, server.getId());
							break;
						} catch (Exception e) {
							L.error(e);
						}
					}
				}

				// publish
				args.put("host", FormatUtil.parseString(server.getIp()));
				ret = Ansible.execute(cfg);
				if (ret != 0) {
					return ret;
				}

				// add backend server
				for (String loadbalancer : loadbalancers) {
					while (true) {
						try {
							loadBalanceService.addBackendServer(loadbalancer, server.getId());
							while (!loadBalanceService.getHealthyBackendServers(loadbalancer).contains(server.getId())) {
								L.info("Wait for server {" + server.toString() + "} restarts in loadbalancer "
										+ loadbalancer);
								U.sleep(1000);
							}
							break;
						} catch (Exception e) {
							L.error(e);
						}
					}
				}
			}
		}
		return 0;
	}
}
