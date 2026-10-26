package com.catchandrelease;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.MenuAction;
import net.runelite.api.gameval.ItemID;

/**
 * Un-noted ids whose inventory Drop option is Release, and which are hidden
 * as ground items. Gameval names do not always match the name in game:
 * {@code BRUT_SPAWNING_TROUT} is leaping trout, {@code BRUT_STURGEON} is
 * leaping sturgeon, {@code HUNTING_RAW_FISH_SPECIAL} is raw rainbow fish,
 * and {@code MORT_SLIMEY_EEL} is raw slimy eel.
 */
public final class ReleaseFish
{
	public static final String DROP_OPTION = "Drop";
	public static final String RELEASE_OPTION = "Release";

	public static final Set<Integer> BARBARIAN_FISH = Set.of(
		ItemID.BRUT_SPAWNING_TROUT,
		ItemID.BRUT_SPAWNING_SALMON,
		ItemID.BRUT_STURGEON
	);

	public static final Set<Integer> FLY_BAIT_FISH = Set.of(
		ItemID.RAW_TROUT,
		ItemID.RAW_SALMON,
		ItemID.RAW_PIKE,
		ItemID.RAW_SARDINE,
		ItemID.RAW_HERRING,
		ItemID.HUNTING_RAW_FISH_SPECIAL,
		ItemID.MORT_SLIMEY_EEL,
		ItemID.RAW_CAVE_EEL
	);

	private static final Set<Integer> ALL_FISH;

	static
	{
		Set<Integer> all = new HashSet<>(BARBARIAN_FISH.size() + FLY_BAIT_FISH.size());
		all.addAll(BARBARIAN_FISH);
		all.addAll(FLY_BAIT_FISH);
		ALL_FISH = Collections.unmodifiableSet(all);
	}

	private ReleaseFish()
	{
	}

	/**
	 * Ids the current category toggles apply to. The returned set is immutable.
	 */
	public static Set<Integer> enabledIds(boolean barbarian, boolean flyBait)
	{
		if (barbarian && flyBait)
		{
			return ALL_FISH;
		}
		if (barbarian)
		{
			return BARBARIAN_FISH;
		}
		if (flyBait)
		{
			return FLY_BAIT_FISH;
		}
		return Collections.emptySet();
	}

	/**
	 * Ground-item menu actions. Hiding one of these removes Take, Examine, and
	 * use-on-item from that fish. Walk here is not one of them.
	 */
	@SuppressWarnings("deprecation")
	public static boolean isGroundMenuAction(MenuAction action)
	{
		return action == MenuAction.GROUND_ITEM_FIRST_OPTION
			|| action == MenuAction.GROUND_ITEM_SECOND_OPTION
			|| action == MenuAction.GROUND_ITEM_THIRD_OPTION
			|| action == MenuAction.GROUND_ITEM_FOURTH_OPTION
			|| action == MenuAction.GROUND_ITEM_FIFTH_OPTION
			|| action == MenuAction.EXAMINE_ITEM_GROUND
			|| action == MenuAction.WIDGET_TARGET_ON_GROUND_ITEM
			|| action == MenuAction.ITEM_USE_ON_GROUND_ITEM;
	}
}
