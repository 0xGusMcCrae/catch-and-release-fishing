package com.catchandrelease;

import net.runelite.api.gameval.AnimationID;
import org.junit.Assert;
import org.junit.Test;

public class FishingActivityTest
{
	@Test
	public void rodAnimationsAreSplitBetweenBarbarianAndFlyBait()
	{
		for (int animationId : FishingActivity.BARBARIAN_ANIMATIONS)
		{
			Assert.assertEquals(FishCategory.BARBARIAN, FishingActivity.styleForAnimation(animationId));
		}
		for (int animationId : FishingActivity.FLY_BAIT_ANIMATIONS)
		{
			Assert.assertEquals(FishCategory.FLY_BAIT, FishingActivity.styleForAnimation(animationId));
		}
		Assert.assertTrue(CollectionsDisjoint.areDisjoint(
			FishingActivity.BARBARIAN_ANIMATIONS,
			FishingActivity.FLY_BAIT_ANIMATIONS));
	}

	@Test
	public void otherFishingAnimationsDoNotCount()
	{
		Assert.assertNull(FishingActivity.styleForAnimation(AnimationID.HUMAN_FISHING_CASTING_PEARL_OILY));
		Assert.assertNull(FishingActivity.styleForAnimation(AnimationID.HUMAN_FISH_ONSPOT_PEARL_OILY));
		Assert.assertNull(FishingActivity.styleForAnimation(AnimationID.BRUT_PLAYER_HAND_FISHING_READY));
		Assert.assertNull(FishingActivity.styleForAnimation(AnimationID.BRUT_PLAYER_HAND_FISHING_START));
		Assert.assertNull(FishingActivity.styleForAnimation(AnimationID.SKILLCAPES_FISHING_PLAYER_ANIM));
		Assert.assertNull(FishingActivity.styleForAnimation(FishingActivity.IDLE_ANIMATION));
	}

	@Test
	public void activeAnimationAndRecentWindow()
	{
		FishingActivity.Session session = new FishingActivity.Session();
		session.observe(AnimationID.HUMAN_FISH_ONSPOT, 10);

		Assert.assertTrue(session.isActive(FishCategory.FLY_BAIT, 10));
		Assert.assertTrue(session.isActive(FishCategory.FLY_BAIT, 10 + FishingActivity.RECENT_TICKS));
		Assert.assertFalse(session.isActive(FishCategory.FLY_BAIT, 11 + FishingActivity.RECENT_TICKS));
		Assert.assertFalse(session.isActive(FishCategory.BARBARIAN, 10));
	}

	@Test
	public void idleDoesNotRefreshOrClearTheWindow()
	{
		FishingActivity.Session session = new FishingActivity.Session();
		session.observe(AnimationID.HUMAN_FISHING_ONSPOT_BRUT, 0);
		session.observe(FishingActivity.IDLE_ANIMATION, 40);

		Assert.assertTrue(session.isActive(FishCategory.BARBARIAN, FishingActivity.RECENT_TICKS));
		Assert.assertFalse(session.isActive(FishCategory.BARBARIAN, FishingActivity.RECENT_TICKS + 1));
	}

	@Test
	public void continuingToFishRefreshesTheWindow()
	{
		FishingActivity.Session session = new FishingActivity.Session();
		session.observe(AnimationID.HUMAN_FISHING_CASTING_PEARL_FLY, 0);
		session.observe(AnimationID.HUMAN_FISH_ONSPOT_PEARL_FLY, 90);

		Assert.assertTrue(session.isActive(FishCategory.FLY_BAIT, 90 + FishingActivity.RECENT_TICKS));
		Assert.assertFalse(session.isActive(FishCategory.FLY_BAIT, 91 + FishingActivity.RECENT_TICKS));
	}

	@Test
	public void aDifferentAnimationClearsBothStyles()
	{
		FishingActivity.Session session = new FishingActivity.Session();
		session.observe(AnimationID.HUMAN_FISHING_CASTING_BRUT, 5);
		session.observe(AnimationID.HUMAN_FISHING_CASTING, 6);
		Assert.assertTrue(session.isActive(FishCategory.BARBARIAN, 6));
		Assert.assertTrue(session.isActive(FishCategory.FLY_BAIT, 6));

		session.observe(AnimationID.SKILLCAPES_FISHING_PLAYER_ANIM, 7);
		Assert.assertFalse(session.isActive(FishCategory.BARBARIAN, 7));
		Assert.assertFalse(session.isActive(FishCategory.FLY_BAIT, 7));
	}

	private static final class CollectionsDisjoint
	{
		private static boolean areDisjoint(java.util.Set<Integer> left, java.util.Set<Integer> right)
		{
			for (Integer id : left)
			{
				if (right.contains(id))
				{
					return false;
				}
			}
			return true;
		}
	}
}
