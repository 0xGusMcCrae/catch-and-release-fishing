package com.catchandrelease;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Assert;
import org.junit.Test;

public class ReleaseFishTest
{
	private static final Map<Integer, FishCategory> NO_NOTES = Collections.emptyMap();

	@Test
	public void barbarianCatchesUseBrutItemIds()
	{
		Assert.assertEquals(FishCategory.BARBARIAN, ReleaseFish.categoryOf(ItemID.BRUT_SPAWNING_TROUT));
		Assert.assertEquals(FishCategory.BARBARIAN, ReleaseFish.categoryOf(ItemID.BRUT_SPAWNING_SALMON));
		Assert.assertEquals(FishCategory.BARBARIAN, ReleaseFish.categoryOf(ItemID.BRUT_STURGEON));
	}

	@Test
	public void flyAndBaitCatchesUseRawItemIds()
	{
		Assert.assertEquals(FishCategory.FLY_BAIT, ReleaseFish.categoryOf(ItemID.RAW_TROUT));
		Assert.assertEquals(FishCategory.FLY_BAIT, ReleaseFish.categoryOf(ItemID.RAW_SALMON));
		Assert.assertEquals(FishCategory.FLY_BAIT, ReleaseFish.categoryOf(ItemID.RAW_PIKE));
		Assert.assertEquals(FishCategory.FLY_BAIT, ReleaseFish.categoryOf(ItemID.RAW_SARDINE));
		Assert.assertEquals(FishCategory.FLY_BAIT, ReleaseFish.categoryOf(ItemID.RAW_HERRING));
		Assert.assertEquals(FishCategory.FLY_BAIT, ReleaseFish.categoryOf(ItemID.HUNTING_RAW_FISH_SPECIAL));
	}

	@Test
	public void cookedRoeShrimpAndBurntFishAreNotReleasable()
	{
		Assert.assertNull(ReleaseFish.categoryOf(ItemID.TROUT));
		Assert.assertNull(ReleaseFish.categoryOf(ItemID.SALMON));
		Assert.assertNull(ReleaseFish.categoryOf(ItemID.PIKE));
		Assert.assertNull(ReleaseFish.categoryOf(ItemID.SARDINE));
		Assert.assertNull(ReleaseFish.categoryOf(ItemID.HERRING));
		Assert.assertNull(ReleaseFish.categoryOf(ItemID.HUNTING_FISH_SPECIAL));
		Assert.assertNull(ReleaseFish.categoryOf(ItemID.HUNTING_RAW_FISH_SPECIAL_BURNT));
		Assert.assertNull(ReleaseFish.categoryOf(ItemID.BRUT_ROE));
		Assert.assertNull(ReleaseFish.categoryOf(ItemID.BRUT_CAVIAR));
		Assert.assertNull(ReleaseFish.categoryOf(ItemID.RAW_SHRIMP));
		Assert.assertNull(ReleaseFish.categoryOf(ItemID.RAW_LOBSTER));
		Assert.assertNull(ReleaseFish.categoryOf(ItemID.BRUT_FISHING_ROD));
	}

	@Test
	public void notedIdsMapBackToTheUnnotedCategory()
	{
		Map<Integer, FishCategory> noted = new HashMap<>();
		ReleaseFish.rememberNoted(noted, ItemID.RAW_PIKE, 900_001, false);
		ReleaseFish.rememberNoted(noted, ItemID.BRUT_STURGEON, 900_002, false);

		Assert.assertEquals(FishCategory.FLY_BAIT, ReleaseFish.categoryOf(900_001, noted));
		Assert.assertEquals(FishCategory.BARBARIAN, ReleaseFish.categoryOf(900_002, noted));
		Assert.assertEquals(FishCategory.FLY_BAIT, ReleaseFish.categoryOf(ItemID.RAW_PIKE, noted));
	}

	@Test
	public void rememberNotedIgnoresNotedDefinitionsAndKnownIds()
	{
		Map<Integer, FishCategory> noted = new HashMap<>();
		ReleaseFish.rememberNoted(noted, ItemID.RAW_TROUT, 900_010, true);
		ReleaseFish.rememberNoted(noted, ItemID.RAW_TROUT, ItemID.RAW_TROUT, false);
		ReleaseFish.rememberNoted(noted, ItemID.RAW_TROUT, ItemID.RAW_SALMON, false);
		ReleaseFish.rememberNoted(noted, ItemID.COINS, 900_011, false);

		Assert.assertTrue(noted.isEmpty());
	}

	@Test
	public void releaseRequiresTheMatchingActivityAndCategoryToggle()
	{
		Assert.assertEquals(ReleaseFish.RELEASE_OPTION, ReleaseFish.releaseOption(
			"Drop", ItemID.RAW_TROUT, true, true, true, false, true, NO_NOTES));
		Assert.assertEquals(ReleaseFish.RELEASE_OPTION, ReleaseFish.releaseOption(
			"Drop", ItemID.BRUT_SPAWNING_SALMON, true, true, false, true, false, NO_NOTES));

		Assert.assertNull(ReleaseFish.releaseOption(
			"Drop", ItemID.RAW_TROUT, true, true, true, false, false, NO_NOTES));
		Assert.assertNull(ReleaseFish.releaseOption(
			"Drop", ItemID.RAW_TROUT, true, true, false, false, true, NO_NOTES));
		Assert.assertNull(ReleaseFish.releaseOption(
			"Drop", ItemID.BRUT_STURGEON, true, false, true, true, true, NO_NOTES));
		Assert.assertNull(ReleaseFish.releaseOption(
			"Drop", ItemID.RAW_SALMON, false, true, true, true, true, NO_NOTES));
		Assert.assertNull(ReleaseFish.releaseOption(
			"Use", ItemID.RAW_SALMON, true, true, true, true, true, NO_NOTES));
		Assert.assertNull(ReleaseFish.releaseOption(
			"Drop", ItemID.TROUT, true, true, true, true, true, NO_NOTES));
		Assert.assertNull(ReleaseFish.releaseOption(
			null, ItemID.RAW_TROUT, true, true, true, true, true, NO_NOTES));
	}

	@Test
	public void hidingFollowsCategoryTogglesAndNotFishingActivity()
	{
		Assert.assertTrue(ReleaseFish.shouldHide(ItemID.RAW_HERRING, true, false, true, NO_NOTES));
		Assert.assertTrue(ReleaseFish.shouldHide(ItemID.BRUT_SPAWNING_TROUT, true, true, false, NO_NOTES));
		Assert.assertFalse(ReleaseFish.shouldHide(ItemID.RAW_HERRING, false, true, true, NO_NOTES));
		Assert.assertFalse(ReleaseFish.shouldHide(ItemID.RAW_HERRING, true, true, false, NO_NOTES));
		Assert.assertFalse(ReleaseFish.shouldHide(ItemID.BRUT_STURGEON, true, false, true, NO_NOTES));
		Assert.assertFalse(ReleaseFish.shouldHide(ItemID.SHRIMP, true, true, true, NO_NOTES));
	}

	@Test
	public void itemLayersDrawUnlessEveryItemIsAHiddenFish()
	{
		Assert.assertFalse(ReleaseFish.shouldDrawItemLayer(
			Arrays.asList(ItemID.RAW_TROUT, ItemID.RAW_SALMON, ItemID.HUNTING_RAW_FISH_SPECIAL),
			true, false, true, NO_NOTES));
		Assert.assertFalse(ReleaseFish.shouldDrawItemLayer(
			Collections.singletonList(ItemID.BRUT_STURGEON),
			true, true, false, NO_NOTES));

		Assert.assertTrue(ReleaseFish.shouldDrawItemLayer(
			Arrays.asList(ItemID.RAW_TROUT, ItemID.TROUT),
			true, true, true, NO_NOTES));
		Assert.assertTrue(ReleaseFish.shouldDrawItemLayer(
			Collections.singletonList(ItemID.RAW_TROUT),
			false, true, true, NO_NOTES));
		Assert.assertTrue(ReleaseFish.shouldDrawItemLayer(
			Collections.singletonList(ItemID.BRUT_STURGEON),
			true, false, true, NO_NOTES));
		Assert.assertTrue(ReleaseFish.shouldDrawItemLayer(
			Collections.emptyList(),
			true, true, true, NO_NOTES));
	}
}
