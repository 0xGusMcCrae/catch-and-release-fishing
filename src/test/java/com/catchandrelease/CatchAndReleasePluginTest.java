package com.catchandrelease;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CatchAndReleasePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CatchAndReleasePlugin.class);
		RuneLite.main(args);
	}
}
