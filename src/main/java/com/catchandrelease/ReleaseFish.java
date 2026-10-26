package com.catchandrelease;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/**
 * Which item ids count as releasable fish.
 * <p>
 * Ids are the un-noted cache items. Display names do not always match the
 * gameval constant: {@code BRUT_SPAWNING_TROUT} is "Leaping trout",
 * {@code BRUT_STURGEON} is "Leaping sturgeon", and
 * {@code HUNTING_RAW_FISH_SPECIAL} is "Raw rainbow fish".
 * Noted variants are not separate constants; {@link #rememberNoted} records
 * them once the item definition has been read.
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
		ItemID.HUNTING_RAW_FISH_SPECIAL
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

	public static List<Integer> allItemIds()
	{
		return new ArrayList<>(ALL_FISH);
	}

	public static FishCategory categoryOf(int itemId)
	{
		if (BARBARIAN_FISH.contains(itemId))
		{
			return FishCategory.BARBARIAN;
		}
		if (FLY_BAIT_FISH.contains(itemId))
		{
			return FishCategory.FLY_BAIT;
		}
		return null;
	}

	public static FishCategory categoryOf(int itemId, Map<Integer, FishCategory> notedIds)
	{
		FishCategory category = categoryOf(itemId);
		if (category != null || notedIds == null)
		{
			return category;
		}
		return notedIds.get(itemId);
	}

	/**
	 * Record the noted form of an un-noted fish. {@code linkedNoteId} comes from
	 * {@code ItemComposition#getLinkedNoteId()} and {@code noted} from
	 * {@code getNote() != -1}.
	 */
	public static void rememberNoted(Map<Integer, FishCategory> notedIds, int itemId, int linkedNoteId, boolean noted)
	{
		if (notedIds == null || noted || linkedNoteId <= 0 || linkedNoteId == itemId)
		{
			return;
		}
		FishCategory category = categoryOf(itemId);
		if (category == null || categoryOf(linkedNoteId) != null)
		{
			return;
		}
		notedIds.put(linkedNoteId, category);
	}

	public static boolean categoryEnabled(FishCategory category, boolean barbarianEnabled, boolean flyBaitEnabled)
	{
		if (category == FishCategory.BARBARIAN)
		{
			return barbarianEnabled;
		}
		if (category == FishCategory.FLY_BAIT)
		{
			return flyBaitEnabled;
		}
		return false;
	}

	public static boolean shouldHide(int itemId, boolean hideEnabled, boolean barbarianEnabled, boolean flyBaitEnabled, Map<Integer, FishCategory> notedIds)
	{
		if (!hideEnabled)
		{
			return false;
		}
		FishCategory category = categoryOf(itemId, notedIds);
		return category != null && categoryEnabled(category, barbarianEnabled, flyBaitEnabled);
	}

	/**
	 * Rename Drop to Release only for an enabled category the player is currently
	 * fishing, or was fishing recently. Hiding does not use this check.
	 */
	public static String releaseOption(
		String strippedOption,
		int itemId,
		boolean renameEnabled,
		boolean barbarianEnabled,
		boolean flyBaitEnabled,
		boolean doingBarbarian,
		boolean doingFlyBait,
		Map<Integer, FishCategory> notedIds)
	{
		if (!renameEnabled || strippedOption == null || !DROP_OPTION.equals(strippedOption))
		{
			return null;
		}
		FishCategory category = categoryOf(itemId, notedIds);
		if (category == null || !categoryEnabled(category, barbarianEnabled, flyBaitEnabled))
		{
			return null;
		}
		boolean doing = category == FishCategory.BARBARIAN ? doingBarbarian : doingFlyBait;
		return doing ? RELEASE_OPTION : null;
	}

	/**
	 * @return false when the pile should not be drawn. A pile that also contains
	 * something else is still drawn: the client renders an item layer as one object,
	 * and suppressing it would hide the other item too.
	 */
	public static boolean shouldDrawItemLayer(
		Collection<Integer> itemIds,
		boolean hideEnabled,
		boolean barbarianEnabled,
		boolean flyBaitEnabled,
		Map<Integer, FishCategory> notedIds)
	{
		if (!hideEnabled || itemIds == null || itemIds.isEmpty())
		{
			return true;
		}
		for (Integer itemId : itemIds)
		{
			if (itemId == null || !shouldHide(itemId, true, barbarianEnabled, flyBaitEnabled, notedIds))
			{
				return true;
			}
		}
		return false;
	}
}
