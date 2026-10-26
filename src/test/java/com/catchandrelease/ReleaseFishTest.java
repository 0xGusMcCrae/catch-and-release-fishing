package com.catchandrelease;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import org.junit.Assert;
import org.junit.Test;

public class ReleaseFishTest
{
	@Test
	public void barbarianCatchesUseBrutItemIds()
	{
		Assert.assertTrue(ReleaseFish.BARBARIAN_FISH.contains(ItemID.BRUT_SPAWNING_TROUT));
		Assert.assertTrue(ReleaseFish.BARBARIAN_FISH.contains(ItemID.BRUT_SPAWNING_SALMON));
		Assert.assertTrue(ReleaseFish.BARBARIAN_FISH.contains(ItemID.BRUT_STURGEON));
	}

	@Test
	public void flyAndBaitCatchesUseRawItemIds()
	{
		Assert.assertTrue(ReleaseFish.FLY_BAIT_FISH.contains(ItemID.RAW_TROUT));
		Assert.assertTrue(ReleaseFish.FLY_BAIT_FISH.contains(ItemID.RAW_SALMON));
		Assert.assertTrue(ReleaseFish.FLY_BAIT_FISH.contains(ItemID.RAW_PIKE));
		Assert.assertTrue(ReleaseFish.FLY_BAIT_FISH.contains(ItemID.RAW_SARDINE));
		Assert.assertTrue(ReleaseFish.FLY_BAIT_FISH.contains(ItemID.RAW_HERRING));
		Assert.assertTrue(ReleaseFish.FLY_BAIT_FISH.contains(ItemID.HUNTING_RAW_FISH_SPECIAL));
		Assert.assertTrue(ReleaseFish.FLY_BAIT_FISH.contains(ItemID.MORT_SLIMEY_EEL));
		Assert.assertTrue(ReleaseFish.FLY_BAIT_FISH.contains(ItemID.RAW_CAVE_EEL));
	}

	@Test
	public void cookedRoeShrimpAndBurntFishAreNotReleasable()
	{
		Assert.assertTrue(Collections.disjoint(
			ReleaseFish.enabledIds(true, true),
			Arrays.asList(
				ItemID.TROUT,
				ItemID.SALMON,
				ItemID.PIKE,
				ItemID.SARDINE,
				ItemID.HERRING,
				ItemID.HUNTING_FISH_SPECIAL,
				ItemID.HUNTING_RAW_FISH_SPECIAL_BURNT,
				ItemID.MORT_SLIMEY_EEL_COOKED,
				ItemID.CAVE_EEL,
				ItemID.BURNT_CAVE_EEL,
				ItemID.BRUT_ROE,
				ItemID.BRUT_CAVIAR,
				ItemID.RAW_SHRIMP,
				ItemID.SHRIMP,
				ItemID.RAW_LOBSTER,
				ItemID.BRUT_FISHING_ROD,
				ItemID.COINS
			)));
	}

	@Test
	public void categoriesDoNotOverlap()
	{
		Assert.assertTrue(Collections.disjoint(ReleaseFish.BARBARIAN_FISH, ReleaseFish.FLY_BAIT_FISH));
	}

	@Test
	public void enabledIdsFollowTheCategoryToggles()
	{
		Set<Integer> both = ReleaseFish.enabledIds(true, true);
		Set<Integer> barbarian = ReleaseFish.enabledIds(true, false);
		Set<Integer> flyBait = ReleaseFish.enabledIds(false, true);
		Set<Integer> none = ReleaseFish.enabledIds(false, false);

		Assert.assertTrue(both.contains(ItemID.RAW_TROUT));
		Assert.assertTrue(both.contains(ItemID.BRUT_STURGEON));
		Assert.assertTrue(both.contains(ItemID.MORT_SLIMEY_EEL));
		Assert.assertTrue(both.contains(ItemID.RAW_CAVE_EEL));

		Assert.assertTrue(flyBait.contains(ItemID.RAW_TROUT));
		Assert.assertFalse(flyBait.contains(ItemID.BRUT_SPAWNING_TROUT));
		Assert.assertTrue(barbarian.contains(ItemID.BRUT_SPAWNING_SALMON));
		Assert.assertFalse(barbarian.contains(ItemID.RAW_TROUT));
		Assert.assertFalse(barbarian.contains(ItemID.RAW_CAVE_EEL));

		Assert.assertTrue(none.isEmpty());
		Assert.assertTrue(Collections.disjoint(none, both));
	}

	@Test
	public void onlyDropOnAnEnabledFishIsRelease()
	{
		Set<Integer> ids = ReleaseFish.enabledIds(true, true);
		Assert.assertTrue(isRelease(ReleaseFish.DROP_OPTION, ItemID.RAW_TROUT, ids));
		Assert.assertTrue(isRelease(ReleaseFish.DROP_OPTION, ItemID.BRUT_SPAWNING_SALMON, ids));
		Assert.assertTrue(isRelease(ReleaseFish.DROP_OPTION, ItemID.RAW_CAVE_EEL, ReleaseFish.enabledIds(false, true)));

		Assert.assertFalse(isRelease("Use", ItemID.RAW_SALMON, ids));
		Assert.assertFalse(isRelease(ReleaseFish.DROP_OPTION, ItemID.TROUT, ids));
		Assert.assertFalse(isRelease(ReleaseFish.DROP_OPTION, ItemID.RAW_TROUT, ReleaseFish.enabledIds(true, false)));
		Assert.assertFalse(isRelease(ReleaseFish.DROP_OPTION, ItemID.BRUT_STURGEON, ReleaseFish.enabledIds(false, true)));
		Assert.assertFalse(isRelease(null, ItemID.RAW_TROUT, ids));
	}

	private static boolean isRelease(String option, int itemId, Set<Integer> enabledIds)
	{
		return ReleaseFish.DROP_OPTION.equals(option) && enabledIds.contains(itemId);
	}
}
