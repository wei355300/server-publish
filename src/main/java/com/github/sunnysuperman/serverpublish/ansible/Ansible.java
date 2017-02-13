package com.github.sunnysuperman.serverpublish.ansible;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.sunnysuperman.commons.utils.CollectionUtil;
import com.github.sunnysuperman.commons.utils.FileUtil;
import com.github.sunnysuperman.commons.utils.FormatUtil;
import com.github.sunnysuperman.commons.utils.JSONUtil;
import com.github.sunnysuperman.commons.utils.RegexUtil;
import com.github.sunnysuperman.serverpublish.L;

public class Ansible {
	private static final String ANSIBLE_TMP_DIR = "/tmp/.ansible-run";

	public static int execute(AnsibleTaskConfig config) throws Exception {
		Map<String, Object> args = config.getArgs();
		L.info("==================================ANSIBLE TASK '" + args.get("task_name") + "' start"
				+ "==================================");

		boolean debug = FormatUtil.parseBoolean(args.get("ansible_debug"), false);

		String projectHome = FormatUtil.parseString(args.get("project_home"));
		if (projectHome == null) {
			throw new RuntimeException("No project home specified");
		}

		String taskName = FormatUtil.parseString(args.get("task_name"));
		if (taskName == null) {
			throw new RuntimeException("No task name specified");
		}

		// host
		String host = FormatUtil.parseString(args.get("host"));
		if (host == null) {
			throw new RuntimeException("No hosts specified");
		}
		boolean localhost = host.equals("127.0.0.1") || host.equals("localhost");
		if (localhost) {
			File shellFile = FileUtil.getFile(new String[] { projectHome, "task", taskName + ".sh" });
			boolean runDirectly = shellFile.exists();
			if (runDirectly) {
				String command = FileUtil.read(shellFile);
				return executeCommand(command, args, config.getAssertConfig(), debug);
			}
		}
		String host_file = FormatUtil.parseString(args.get("host_file"), null);
		if (host_file == null) {
			String ansibleHostsFile = ANSIBLE_TMP_DIR + "/hosts";
			FileUtil.write(new File(ansibleHostsFile), "[servers]" + FileUtil.LINE + args.get("host").toString());
			host_file = ansibleHostsFile;
		}
		String host_profile = FormatUtil.parseString(args.get("host_profile"), null);
		if (host_profile == null) {
			args.put("host_profile", args.get("profile").toString());
		}

		// execute
		String template = "cd ${project_home}/ansible-config/${host_profile} && ${ansible_path}ansible-playbook -i "
				+ host_file + " -v ${project_home}/task/${task_name}.yml";
		StringBuilder command = new StringBuilder(RegexUtil.compile(template, args));
		{
			command.append(" --extra-vars '");
			command.append(JSONUtil.toJSONString(args));
			command.append("'");
		}
		int ret = executeCommand(command.toString(), null, config.getAssertConfig(), debug);
		String resultStatus = ret == 0 ? "DONE" : "FAILED";
		L.info("==================================ANSIBLE TASK '" + args.get("task_name") + "' " + resultStatus
				+ "==================================");
		return ret;
	}

	private static File touchExecutableFile(String path) throws Exception {
		File file = new File(path);
		FileUtil.delete(file);
		FileUtil.ensureFile(file);
		file.setExecutable(true);
		return file;
	}

	private static int executeCommand(String command, Map<String, Object> environment, AssertConfig assertConfig,
			boolean debug) throws Exception {
		File file = touchExecutableFile(ANSIBLE_TMP_DIR + "/command.sh");
		if (debug) {
			FileUtil.append(file, "set -x" + FileUtil.LINE);
		}
		FileUtil.append(file, ". /etc/profile" + FileUtil.LINE);
		FileUtil.append(file, ". ~/.bash_profile" + FileUtil.LINE);
		if (environment != null) {
			StringBuilder buf = new StringBuilder();
			for (Entry<String, Object> env : environment.entrySet()) {
				Object value = env.getValue();
				if (value == null) {
					continue;
				}
				if ((value instanceof Number) || (value instanceof Boolean)) {
					// nope
				} else {
					value = "'" + value.toString() + "'";
				}
				buf.append(env.getKey()).append("=").append(value).append(FileUtil.LINE);
			}
			File envFile = touchExecutableFile(ANSIBLE_TMP_DIR + "/env.sh");
			FileUtil.write(envFile, buf.toString());
			FileUtil.append(file, ". " + envFile.getAbsolutePath() + FileUtil.LINE);
		}
		FileUtil.append(file, command);
		Process proc = Runtime.getRuntime().exec("sh " + file.getAbsolutePath());
		List<String> leftAsserts = assertConfig == null ? null : new ArrayList<>(assertConfig.getAsserts());
		{
			InputStream in = proc.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			L.info("<Message>");
			while ((line = reader.readLine()) != null) {
				L.info(line);
				if (CollectionUtil.isNotEmpty(leftAsserts)) {
					for (Iterator<String> iter = leftAsserts.iterator(); iter.hasNext();) {
						String assertString = iter.next();
						if (line.indexOf(assertString) >= 0) {
							iter.remove();
						}
					}
				}
			}
			L.info("</Message>");
			reader.close();
		}
		{
			InputStream in = proc.getErrorStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			L.error("<ErrorMessage>");
			while ((line = reader.readLine()) != null) {
				L.error(line);
			}
			L.error("</ErrorMessage>");
		}
		int exitValue = proc.waitFor();
		L.info("Process exit value: " + exitValue);
		if (exitValue != 0) {
			return exitValue;
		}
		if (CollectionUtil.isNotEmpty(leftAsserts)) {
			L.error("Assert failed: " + JSONUtil.toJSONString(leftAsserts));
			return 1;
		}
		return exitValue;
	}
}
