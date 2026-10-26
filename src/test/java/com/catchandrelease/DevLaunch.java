package com.catchandrelease;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Dev-client entry used by the shadow jar.
 * <p>
 * Bolt's Linux launcher (including the Flatpak) execs a custom RuneLite jar as
 * {@code java -Duser.home=<bolt data> -jar <jar> -J-Duser.home=<bolt data>},
 * and adds {@code --configure} when the configure button is used. A custom
 * launch command may append more tokens, including {@code -J} / {@code -J-Xmx…}
 * JVM arguments. Those {@code -J} forms are for the official launcher. The
 * client parser rejects them ({@code J is not a recognized option}).
 * {@code -Duser.home} is already a real JVM argument on this process; Jagex
 * session ids arrive as {@code JX_SESSION_ID}, {@code JX_CHARACTER_ID}, and
 * {@code JX_DISPLAY_NAME}.
 */
final class DevLaunch
{
	static final String RELAUNCHED = "catchandrelease.relaunched";

	/**
	 * Options accepted by {@code RuneLite#main} in the client this project builds against.
	 */
	private static final Set<String> CLIENT_FLAGS = set(
		"developer-mode",
		"debug",
		"safe-mode",
		"insecure-skip-tls-verification",
		"disable-telemetry",
		"noupdate",
		"insecure-write-credentials",
		"help"
	);

	private static final Set<String> CLIENT_WITH_VALUE = set(
		"jav_config",
		"profile",
		"character",
		"sessionfile"
	);

	private static final Set<String> LAUNCHER_FLAGS = set(
		"configure",
		"postinstall",
		"nodiff"
	);

	private static final Set<String> LAUNCHER_WITH_VALUE = set(
		"scale",
		"classpath",
		"launch-mode",
		"hw-accel",
		"mode",
		"p"
	);

	private DevLaunch()
	{
	}

	@SuppressWarnings("unchecked")
	static void launch(String[] args) throws Exception
	{
		Split split = split(args);
		if (!split.ignored.isEmpty())
		{
			System.err.println("Catch and Release dev client: ignoring launcher arguments " + split.ignored);
		}

		boolean relaunched = Boolean.getBoolean(RELAUNCHED);
		List<String> currentJvm = ManagementFactory.getRuntimeMXBean().getInputArguments();
		if (!relaunched && needsRelaunch(assertionsEnabled(), currentJvm, split.jvmArgs))
		{
			System.err.println("Catch and Release dev client: relaunching with assertions enabled");
			int code = relaunch(split, currentJvm);
			System.exit(code);
		}
		if (!assertionsEnabled())
		{
			throw new IllegalStateException("Assertions are not enabled. Relaunch with -ea failed.");
		}

		ExternalPluginManager.loadBuiltin(CatchAndReleasePlugin.class);
		RuneLite.main(split.clientArgs.toArray(new String[0]));
	}

	static boolean assertionsEnabled()
	{
		boolean enabled = false;
		assert enabled = true;
		return enabled;
	}

	/**
	 * @param requestedJvmArgs JVM arguments extracted from {@code -J} and other launcher options
	 */
	static boolean needsRelaunch(boolean assertionsEnabled, List<String> currentJvmArgs, List<String> requestedJvmArgs)
	{
		if (!assertionsEnabled)
		{
			return true;
		}
		for (String requested : requestedJvmArgs)
		{
			if (isEnableAssertions(requested) || currentJvmArgs.contains(requested))
			{
				continue;
			}
			return true;
		}
		return false;
	}

	static Split split(String[] args)
	{
		List<String> jvmArgs = new ArrayList<>();
		List<String> clientArgs = new ArrayList<>();
		List<String> ignored = new ArrayList<>();
		if (args == null)
		{
			return new Split(jvmArgs, clientArgs, ignored);
		}

		for (int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			if (arg == null || arg.isEmpty())
			{
				continue;
			}
			if ("--".equals(arg))
			{
				clientArgs.add(arg);
				clientArgs.addAll(Arrays.asList(args).subList(i + 1, args.length));
				break;
			}

			String jvm = jvmArgument(arg);
			if (jvm != null)
			{
				ignored.add(arg);
				if (jvm.isEmpty())
				{
					if (i + 1 < args.length)
					{
						jvmArgs.add(args[++i]);
					}
				}
				else if (!isDisableAssertions(jvm))
				{
					jvmArgs.add(jvm);
				}
				continue;
			}

			String name = longName(arg);
			if (name != null && (CLIENT_FLAGS.contains(name) || CLIENT_WITH_VALUE.contains(name)))
			{
				clientArgs.add(arg);
				if (CLIENT_WITH_VALUE.contains(name) && !arg.contains("=") && i + 1 < args.length)
				{
					clientArgs.add(args[++i]);
				}
				continue;
			}

			if (name != null && (LAUNCHER_FLAGS.contains(name) || LAUNCHER_WITH_VALUE.contains(name)))
			{
				ignored.add(arg);
				String attached = attachedValue(arg);
				if ("scale".equals(name))
				{
					String scale = attached;
					if (scale == null && i + 1 < args.length)
					{
						scale = args[++i];
						ignored.add(scale);
					}
					if (scale != null && !scale.isEmpty())
					{
						jvmArgs.add("-Dsun.java2d.dpiaware=true");
						jvmArgs.add("-Dsun.java2d.uiScale=" + scale);
					}
				}
				else if (LAUNCHER_WITH_VALUE.contains(name) && attached == null && i + 1 < args.length && !args[i + 1].startsWith("-"))
				{
					ignored.add(args[++i]);
				}
				continue;
			}

			if (arg.startsWith("-"))
			{
				ignored.add(arg);
				continue;
			}
			clientArgs.add(arg);
		}
		return new Split(jvmArgs, clientArgs, ignored);
	}

	static List<String> command(
		String javaBin,
		List<String> currentJvmArgs,
		List<String> requestedJvmArgs,
		String classpath,
		String mainClass,
		List<String> clientArgs)
	{
		List<String> cmd = new ArrayList<>();
		cmd.add(javaBin);
		if (currentJvmArgs != null)
		{
			for (String arg : currentJvmArgs)
			{
				if (arg == null || isEnableAssertions(arg) || isDisableAssertions(arg))
				{
					continue;
				}
				cmd.add(arg);
			}
		}
		if (requestedJvmArgs != null)
		{
			for (String arg : requestedJvmArgs)
			{
				if (arg == null || arg.isEmpty() || isDisableAssertions(arg) || isEnableAssertions(arg) || cmd.contains(arg))
				{
					continue;
				}
				cmd.add(arg);
			}
		}
		cmd.add("-ea");
		String relaunchProp = "-D" + RELAUNCHED + "=true";
		if (!cmd.contains(relaunchProp))
		{
			cmd.add(relaunchProp);
		}
		cmd.add("-cp");
		cmd.add(classpath);
		cmd.add(mainClass);
		if (clientArgs != null)
		{
			cmd.addAll(clientArgs);
		}
		return cmd;
	}

	private static int relaunch(Split split, List<String> currentJvm) throws IOException, InterruptedException
	{
		List<String> cmd = command(
			javaBinary().toString(),
			currentJvm,
			split.jvmArgs,
			System.getProperty("java.class.path"),
			CatchAndReleasePluginTest.class.getName(),
			split.clientArgs);
		Process process = new ProcessBuilder(cmd).inheritIO().start();
		return process.waitFor();
	}

	private static Path javaBinary()
	{
		String os = System.getProperty("os.name", "");
		String exe = os.toLowerCase(Locale.ENGLISH).contains("win") ? "java.exe" : "java";
		Path bin = Paths.get(System.getProperty("java.home"), "bin", exe);
		if (Files.isRegularFile(bin))
		{
			return bin;
		}
		return Paths.get(exe);
	}

	/**
	 * @return the JVM argument to apply, empty when the next token is that argument, or null
	 */
	private static String jvmArgument(String arg)
	{
		if ("-J".equals(arg) || "--J".equals(arg))
		{
			return "";
		}
		if (arg.startsWith("--J="))
		{
			return arg.substring(4);
		}
		if (arg.startsWith("--J") && arg.length() > 3)
		{
			return arg.substring(3);
		}
		if (arg.startsWith("-J") && arg.length() > 2)
		{
			return arg.substring(2);
		}
		if (arg.startsWith("-D") || arg.startsWith("-X") || arg.startsWith("-javaagent:") || arg.startsWith("--add-")
			|| arg.startsWith("--enable-native-access") || arg.startsWith("--enable-preview"))
		{
			return arg;
		}
		if (isEnableAssertions(arg) || isDisableAssertions(arg))
		{
			return arg;
		}
		return null;
	}

	private static boolean isEnableAssertions(String arg)
	{
		return "-ea".equals(arg) || "-enableassertions".equals(arg) || "-esa".equals(arg)
			|| arg.startsWith("-ea:") || arg.startsWith("-enableassertions:");
	}

	private static boolean isDisableAssertions(String arg)
	{
		return "-da".equals(arg) || "-disableassertions".equals(arg) || "-dsa".equals(arg)
			|| arg.startsWith("-da:") || arg.startsWith("-disableassertions:");
	}

	private static String longName(String arg)
	{
		if (!arg.startsWith("--") || arg.length() < 3)
		{
			return null;
		}
		String body = arg.substring(2);
		int eq = body.indexOf('=');
		return eq >= 0 ? body.substring(0, eq) : body;
	}

	private static String attachedValue(String arg)
	{
		int eq = arg.indexOf('=');
		if (arg.startsWith("--") && eq > 2)
		{
			return arg.substring(eq + 1);
		}
		return null;
	}

	private static Set<String> set(String... values)
	{
		return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
	}

	static final class Split
	{
		final List<String> jvmArgs;
		final List<String> clientArgs;
		final List<String> ignored;

		Split(List<String> jvmArgs, List<String> clientArgs, List<String> ignored)
		{
			this.jvmArgs = jvmArgs;
			this.clientArgs = clientArgs;
			this.ignored = ignored;
		}
	}
}
