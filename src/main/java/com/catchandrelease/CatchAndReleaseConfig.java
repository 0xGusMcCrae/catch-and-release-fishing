package com.catchandrelease;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(CatchAndReleaseConfig.GROUP)
public interface CatchAndReleaseConfig extends Config
{
	String GROUP = "catchandreleasefishing";

	@ConfigItem(
		keyName = "renameDrop",
		name = "Release instead of Drop",
		description = "The inventory Drop option on matching fish is shown as Release. Clicking it still drops the fish.",
		position = 0
	)
	default boolean renameDropToRelease()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideGroundFish",
		name = "Hide fish on the ground",
		description = "Do not draw these fish when they are on the ground, on any tile.",
		position = 1
	)
	default boolean hideReleasedFish()
	{
		return true;
	}

	@ConfigSection(
		name = "Fish",
		description = "Which catches Release and ground hiding apply to",
		position = 2
	)
	String fishSection = "fish";

	@ConfigItem(
		keyName = "barbarian",
		name = "Barbarian fish",
		description = "Leaping trout, leaping salmon, and leaping sturgeon.",
		section = fishSection,
		position = 0
	)
	default boolean barbarianFish()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flyBait",
		name = "Fly and bait fish",
		description = "Raw trout, raw salmon, raw pike, raw sardine, raw herring, raw rainbow fish, raw slimy eel, and raw cave eel.",
		section = fishSection,
		position = 1
	)
	default boolean flyBaitFish()
	{
		return true;
	}
}
