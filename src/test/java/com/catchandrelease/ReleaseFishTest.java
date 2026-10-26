package com.catchandrelease;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import net.runelite.api.MenuAction;
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
	public void leechfinUsesTheGamevalItemId()
	{
		Assert.assertTrue(ReleaseFish.LEECHFIN_FISH.contains(ItemID.LEECHFIN));
		Assert.assertEquals(33621, ItemID.LEECHFIN);
		Assert.assertFalse(ReleaseFish.LEECHFIN_FISH.contains(ItemID.BLOOD_SAC));
	}

	@Test
	public void cookedRoeShrimpAndBurntFishAreNotReleasable()
	{
		Assert.assertTrue(Collections.disjoint(
			ReleaseFish.enabledIds(true, true, true),
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
				ItemID.COINS,
				ItemID.BLOOD_SAC
			)));
	}

	@Test
	public void categoriesDoNotOverlap()
	{
		Assert.assertTrue(Collections.disjoint(ReleaseFish.BARBARIAN_FISH, ReleaseFish.FLY_BAIT_FISH));
		Assert.assertTrue(Collections.disjoint(ReleaseFish.BARBARIAN_FISH, ReleaseFish.LEECHFIN_FISH));
		Assert.assertTrue(Collections.disjoint(ReleaseFish.FLY_BAIT_FISH, ReleaseFish.LEECHFIN_FISH));
	}

	@Test
	public void enabledIdsFollowTheCategoryToggles()
	{
		Set<Integer> all = ReleaseFish.enabledIds(true, true, true);
		Set<Integer> barbarian = ReleaseFish.enabledIds(true, false, false);
		Set<Integer> flyBait = ReleaseFish.enabledIds(false, true, false);
		Set<Integer> leechfin = ReleaseFish.enabledIds(false, false, true);
		Set<Integer> none = ReleaseFish.enabledIds(false, false, false);

		Assert.assertTrue(all.contains(ItemID.RAW_TROUT));
		Assert.assertTrue(all.contains(ItemID.BRUT_STURGEON));
		Assert.assertTrue(all.contains(ItemID.MORT_SLIMEY_EEL));
		Assert.assertTrue(all.contains(ItemID.RAW_CAVE_EEL));
		Assert.assertTrue(all.contains(ItemID.LEECHFIN));

		Assert.assertTrue(flyBait.contains(ItemID.RAW_TROUT));
		Assert.assertFalse(flyBait.contains(ItemID.BRUT_SPAWNING_TROUT));
		Assert.assertFalse(flyBait.contains(ItemID.LEECHFIN));
		Assert.assertTrue(barbarian.contains(ItemID.BRUT_SPAWNING_SALMON));
		Assert.assertFalse(barbarian.contains(ItemID.RAW_TROUT));
		Assert.assertFalse(barbarian.contains(ItemID.RAW_CAVE_EEL));
		Assert.assertFalse(barbarian.contains(ItemID.LEECHFIN));
		Assert.assertTrue(leechfin.contains(ItemID.LEECHFIN));
		Assert.assertFalse(leechfin.contains(ItemID.RAW_TROUT));
		Assert.assertFalse(leechfin.contains(ItemID.BRUT_STURGEON));

		Assert.assertTrue(none.isEmpty());
		Assert.assertTrue(Collections.disjoint(none, all));
	}

	@Test
	public void onlyDropOnAnEnabledFishIsRelease()
	{
		Set<Integer> ids = ReleaseFish.enabledIds(true, true, true);
		Assert.assertTrue(isRelease(ReleaseFish.DROP_OPTION, ItemID.RAW_TROUT, ids));
		Assert.assertTrue(isRelease(ReleaseFish.DROP_OPTION, ItemID.BRUT_SPAWNING_SALMON, ids));
		Assert.assertTrue(isRelease(ReleaseFish.DROP_OPTION, ItemID.RAW_CAVE_EEL, ReleaseFish.enabledIds(false, true, false)));
		Assert.assertTrue(isRelease(ReleaseFish.DROP_OPTION, ItemID.LEECHFIN, ReleaseFish.enabledIds(false, false, true)));

		Assert.assertFalse(isRelease("Use", ItemID.RAW_SALMON, ids));
		Assert.assertFalse(isRelease(ReleaseFish.DROP_OPTION, ItemID.TROUT, ids));
		Assert.assertFalse(isRelease(ReleaseFish.DROP_OPTION, ItemID.RAW_TROUT, ReleaseFish.enabledIds(true, false, false)));
		Assert.assertFalse(isRelease(ReleaseFish.DROP_OPTION, ItemID.BRUT_STURGEON, ReleaseFish.enabledIds(false, true, false)));
		Assert.assertFalse(isRelease(ReleaseFish.DROP_OPTION, ItemID.LEECHFIN, ReleaseFish.enabledIds(true, true, false)));
		Assert.assertFalse(isRelease(null, ItemID.RAW_TROUT, ids));
	}

	@Test
	public void aNewPileWithOnlyTheTopSlotFilledIsHidden()
	{
		Set<Integer> ids = ReleaseFish.enabledIds(true, true, true);
		Assert.assertTrue(ReleaseFish.pileContainsEnabledFish(ids, null, null, ItemID.RAW_TROUT));
		Assert.assertTrue(ReleaseFish.pileContainsEnabledFish(ids, null, null, ItemID.LEECHFIN));
		Assert.assertTrue(ReleaseFish.pileContainsEnabledFish(ids, ItemID.BRUT_STURGEON, null, null));
		Assert.assertTrue(ReleaseFish.pileContainsEnabledFish(ids, null, ItemID.RAW_SALMON, ItemID.COINS));

		Assert.assertFalse(ReleaseFish.pileContainsEnabledFish(ids, null, null, null));
		Assert.assertFalse(ReleaseFish.pileContainsEnabledFish(ids, ItemID.COINS, ItemID.TROUT, ItemID.BLOOD_SAC));
		Assert.assertFalse(ReleaseFish.pileContainsEnabledFish(ReleaseFish.enabledIds(false, false, false), null, null, ItemID.RAW_TROUT));
		Assert.assertFalse(ReleaseFish.pileContainsEnabledFish(ReleaseFish.enabledIds(true, true, false), null, null, ItemID.LEECHFIN));
	}

	@Test
	public void leftClickDoesNotRenameWhenReleaseToggleIsOff()
	{
		Assert.assertEquals(ReleaseFish.DROP_OPTION, ReleaseFish.inventoryDropLabel(false, ReleaseFish.DROP_OPTION));
		Assert.assertEquals(ReleaseFish.DROP_OPTION, ReleaseFish.inventoryDropLabel(false, ReleaseFish.RELEASE_OPTION));
		Assert.assertEquals(ReleaseFish.RELEASE_OPTION, ReleaseFish.inventoryDropLabel(true, ReleaseFish.DROP_OPTION));
		Assert.assertEquals(ReleaseFish.RELEASE_OPTION, ReleaseFish.inventoryDropLabel(true, ReleaseFish.RELEASE_OPTION));
		Assert.assertEquals("Use", ReleaseFish.inventoryDropLabel(false, "Use"));
	}

	private static boolean isRelease(String option, int itemId, Set<Integer> enabledIds)
	{
		return ReleaseFish.DROP_OPTION.equals(option) && enabledIds.contains(itemId);
	}

	@Test
	public void groundMenusForTheseFishAreTheOnesThatGetHidden()
	{
		Assert.assertTrue(ReleaseFish.isGroundMenuAction(MenuAction.GROUND_ITEM_FIRST_OPTION));
		Assert.assertTrue(ReleaseFish.isGroundMenuAction(MenuAction.GROUND_ITEM_FIFTH_OPTION));
		Assert.assertTrue(ReleaseFish.isGroundMenuAction(MenuAction.EXAMINE_ITEM_GROUND));
		Assert.assertTrue(ReleaseFish.isGroundMenuAction(MenuAction.WIDGET_TARGET_ON_GROUND_ITEM));
		Assert.assertFalse(ReleaseFish.isGroundMenuAction(MenuAction.WALK));
		Assert.assertFalse(ReleaseFish.isGroundMenuAction(MenuAction.CC_OP));
		Assert.assertFalse(ReleaseFish.isGroundMenuAction(MenuAction.EXAMINE_NPC));
		Assert.assertFalse(ReleaseFish.isGroundMenuAction(MenuAction.CANCEL));
	}
}
