package com.github.sunnysuperman.serverpublish;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import com.github.sunnysuperman.serverpublish.ansible.ServerPublish;

public class Bootstrap {

	private static String option(CommandLine commandLine, String key) {
		return commandLine.hasOption(key) ? commandLine.getOptionValue(key) : null;
	}

	public static void run(String[] args) throws Exception {
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption("log", "log", true, "Logback configuration file");
		options.addOption("home", "home", true, "Project home path");
		options.addOption("name", "name", true, "Project name");
		options.addOption("profile", "profile", true, "Profile");
		options.addOption("version", "version", true, "Version");
		CommandLine command = parser.parse(options, args);

		String logbackConfig = option(command, "log");
		if (logbackConfig != null) {
			System.setProperty("logback.configurationFile", logbackConfig);
		}

		int code = new ServerPublish(option(command, "home"), option(command, "name"), option(command, "profile"),
				option(command, "version")).execute();
		System.exit(code);
	}

	public static void main(String[] args) throws Exception {
		try {
			run(args);
		} catch (Exception e) {
			L.error(e);
			System.err.println("Failed to publish");
			System.exit(1);
		}
	}

}
