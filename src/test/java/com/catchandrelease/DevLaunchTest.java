package com.catchandrelease;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class DevLaunchTest
{
	@Test
	public void boltCustomJarArgsAreJvmPropertiesNotClientOptions()
	{
		DevLaunch.Split split = DevLaunch.split(new String[]{
			"-J-Duser.home=/home/user/.var/app/com.adamcake.Bolt/data/bolt-launcher"
		});

		Assert.assertEquals(
			Collections.singletonList("-Duser.home=/home/user/.var/app/com.adamcake.Bolt/data/bolt-launcher"),
			split.jvmArgs);
		Assert.assertTrue(split.clientArgs.isEmpty());
	}

	@Test
	public void configureAndSeparateJvmArgsAreStripped()
	{
		DevLaunch.Split split = DevLaunch.split(new String[]{
			"-J-Duser.home=/bolt",
			"--configure",
			"-J",
			"-Dsun.java2d.uiScale=2",
			"--developer-mode",
			"--debug"
		});

		Assert.assertEquals(Arrays.asList(
			"-Duser.home=/bolt",
			"-Dsun.java2d.uiScale=2"
		), split.jvmArgs);
		Assert.assertEquals(Arrays.asList("--developer-mode", "--debug"), split.clientArgs);
		Assert.assertFalse(split.ignored.contains("--developer-mode"));
		Assert.assertTrue(split.ignored.contains("--configure"));
	}

	@Test
	public void clientOptionsWithValuesAreKept()
	{
		DevLaunch.Split split = DevLaunch.split(new String[]{
			"--profile",
			"leagues",
			"--character=Fish",
			"--sessionfile",
			"/tmp/session",
			"--jav_config=https://example.invalid/jav_config.ws",
			"--safe-mode",
			"--insecure-write-credentials"
		});

		Assert.assertEquals(Arrays.asList(
			"--profile",
			"leagues",
			"--character=Fish",
			"--sessionfile",
			"/tmp/session",
			"--jav_config=https://example.invalid/jav_config.ws",
			"--safe-mode",
			"--insecure-write-credentials"
		), split.clientArgs);
		Assert.assertTrue(split.jvmArgs.isEmpty());
	}

	@Test
	public void launcherOnlyOptionsDoNotReachTheClient()
	{
		DevLaunch.Split split = DevLaunch.split(new String[]{
			"--scale=1.5",
			"--classpath",
			"client.jar",
			"--launch-mode",
			"JVM",
			"--hw-accel",
			"OPENGL",
			"--mode=OFF",
			"--postinstall",
			"--nodiff",
			"-psn_0_352342",
			"--not-a-runelite-option",
			"--developer-mode"
		});

		Assert.assertEquals(Collections.singletonList("--developer-mode"), split.clientArgs);
		Assert.assertEquals(Arrays.asList(
			"-Dsun.java2d.dpiaware=true",
			"-Dsun.java2d.uiScale=1.5"
		), split.jvmArgs);
		Assert.assertFalse(split.clientArgs.contains("client.jar"));
		Assert.assertFalse(split.clientArgs.contains("JVM"));
		Assert.assertFalse(split.clientArgs.contains("--configure"));
	}

	@Test
	public void rawJvmArgsAndAssertionFlagsAreNotClientOptions()
	{
		DevLaunch.Split split = DevLaunch.split(new String[]{
			"-Xmx512m",
			"-ea",
			"-da",
			"--J=-Dfoo=bar",
			"--",
			"--configure"
		});

		Assert.assertEquals(Arrays.asList("-Xmx512m", "-ea", "-Dfoo=bar"), split.jvmArgs);
		Assert.assertEquals(Arrays.asList("--", "--configure"), split.clientArgs);
		Assert.assertFalse(split.jvmArgs.contains("-da"));
	}

	@Test
	public void relaunchCarriesDeveloperModeAssertionsAndBoltHome()
	{
		List<String> cmd = DevLaunch.command(
			"/usr/bin/java",
			Collections.singletonList("-Duser.home=/bolt"),
			Arrays.asList("-Duser.home=/bolt", "-Dsun.java2d.uiScale=2"),
			"/tmp/catch-and-release-all.jar",
			"com.catchandrelease.CatchAndReleasePluginTest",
			Arrays.asList("--developer-mode", "--debug"));

		Assert.assertEquals("/usr/bin/java", cmd.get(0));
		Assert.assertTrue(cmd.contains("-ea"));
		Assert.assertTrue(cmd.contains("-Duser.home=/bolt"));
		Assert.assertTrue(cmd.contains("-Dsun.java2d.uiScale=2"));
		Assert.assertTrue(cmd.contains("-D" + DevLaunch.RELAUNCHED + "=true"));
		Assert.assertEquals(Arrays.asList(
			"-cp",
			"/tmp/catch-and-release-all.jar",
			"com.catchandrelease.CatchAndReleasePluginTest",
			"--developer-mode",
			"--debug"
		), cmd.subList(cmd.indexOf("-cp"), cmd.size()));
		Assert.assertEquals(1, count(cmd, "-Duser.home=/bolt"));
		Assert.assertFalse(cmd.contains("-J-Duser.home=/bolt"));
	}

	@Test
	public void relaunchIsSkippedWhenAssertionsAndJvmArgsAreAlreadyApplied()
	{
		Assert.assertTrue(DevLaunch.needsRelaunch(
			false,
			Collections.singletonList("-Duser.home=/bolt"),
			Collections.singletonList("-Duser.home=/bolt")));
		Assert.assertFalse(DevLaunch.needsRelaunch(
			true,
			Arrays.asList("-ea", "-Duser.home=/bolt"),
			Collections.singletonList("-Duser.home=/bolt")));
		Assert.assertTrue(DevLaunch.needsRelaunch(
			true,
			Collections.singletonList("-ea"),
			Collections.singletonList("-Dsun.java2d.uiScale=2")));
		Assert.assertFalse(DevLaunch.needsRelaunch(
			true,
			Collections.singletonList("-ea"),
			Collections.<String>emptyList()));
	}

	private static int count(List<String> values, String target)
	{
		int n = 0;
		for (String value : values)
		{
			if (target.equals(value))
			{
				n++;
			}
		}
		return n;
	}
}
