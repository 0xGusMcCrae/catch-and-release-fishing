package com.catchandrelease;

import java.util.Set;
import net.runelite.api.gameval.AnimationID;

/**
 * Decides whether the local player is fly fishing, bait fishing, or barbarian
 * rod fishing, including a short period after the animation stops.
 * <p>
 * The standard bait and fly rods share {@link AnimationID#HUMAN_FISHING_CASTING}
 * and {@link AnimationID#HUMAN_FISH_ONSPOT} (cast and loop). Pearl bait and pearl
 * fly rods have their own sequences and are treated as the same fly/bait category.
 * The heavy barbarian rod, including the pearl barbarian rod, uses the
 * {@code *_BRUT} sequences. Bare-handed barbarian fishing and the oily rod are
 * not included: those spots do not catch the fish this plugin releases.
 * <p>
 * While a matching animation is playing, that style stays active. After it
 * stops, it stays active for {@link #RECENT_TICKS} game ticks (one minute) so a
 * full inventory can still be released. Any other animation clears both styles.
 * Standing or walking uses animation {@code -1} and does not clear the timer.
 */
public final class FishingActivity
{
	public static final int IDLE_ANIMATION = -1;

	/**
	 * 100 game ticks is 60 seconds.
	 */
	public static final int RECENT_TICKS = 100;

	public static final Set<Integer> BARBARIAN_ANIMATIONS = Set.of(
		AnimationID.HUMAN_FISHING_CASTING_BRUT,
		AnimationID.HUMAN_FISHING_ONSPOT_BRUT,
		AnimationID.HUMAN_FISHING_CASTING_PEARL_BRUT,
		AnimationID.HUMAN_FISH_ONSPOT_PEARL_BRUT
	);

	public static final Set<Integer> FLY_BAIT_ANIMATIONS = Set.of(
		AnimationID.HUMAN_FISHING_CASTING,
		AnimationID.HUMAN_FISH_ONSPOT,
		AnimationID.HUMAN_FISHING_CASTING_PEARL,
		AnimationID.HUMAN_FISH_ONSPOT_PEARL,
		AnimationID.HUMAN_FISHING_CASTING_PEARL_FLY,
		AnimationID.HUMAN_FISH_ONSPOT_PEARL_FLY
	);

	private FishingActivity()
	{
	}

	public static FishCategory styleForAnimation(int animationId)
	{
		if (BARBARIAN_ANIMATIONS.contains(animationId))
		{
			return FishCategory.BARBARIAN;
		}
		if (FLY_BAIT_ANIMATIONS.contains(animationId))
		{
			return FishCategory.FLY_BAIT;
		}
		return null;
	}

	public static final class Session
	{
		private static final int NEVER = Integer.MIN_VALUE;

		private int barbarianTick = NEVER;
		private int flyBaitTick = NEVER;

		public void reset()
		{
			barbarianTick = NEVER;
			flyBaitTick = NEVER;
		}

		/**
		 * Update the session from the local player's current animation.
		 * Idle ({@code -1}) leaves the recent-fishing timer alone.
		 */
		public void observe(int animationId, int tick)
		{
			if (animationId == IDLE_ANIMATION)
			{
				return;
			}
			FishCategory style = styleForAnimation(animationId);
			if (style == FishCategory.BARBARIAN)
			{
				barbarianTick = tick;
			}
			else if (style == FishCategory.FLY_BAIT)
			{
				flyBaitTick = tick;
			}
			else
			{
				reset();
			}
		}

		public boolean isActive(FishCategory style, int tick)
		{
			int last = style == FishCategory.BARBARIAN ? barbarianTick : flyBaitTick;
			if (style != FishCategory.BARBARIAN && style != FishCategory.FLY_BAIT)
			{
				return false;
			}
			if (last == NEVER)
			{
				return false;
			}
			int elapsed = tick - last;
			return elapsed >= 0 && elapsed <= RECENT_TICKS;
		}
	}
}
