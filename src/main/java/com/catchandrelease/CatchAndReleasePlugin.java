package com.catchandrelease;

import com.google.inject.Provides;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ItemLayer;
import net.runelite.api.Node;
import net.runelite.api.Scene;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Catch and Release Fishing",
	description = "Renames Drop to Release for barbarian and fly/bait fish, and hides those fish on the ground",
	tags = {"fishing", "barbarian", "fly", "bait", "drop", "release"}
)
public class CatchAndReleasePlugin extends Plugin
{
	@Inject
	private CatchAndReleaseConfig config;

	@Inject
	private RenderCallbackManager renderCallbackManager;

	private final RenderCallback renderCallback = new RenderCallback()
	{
		@Override
		public boolean drawObject(Scene scene, TileObject object)
		{
			return shouldDrawObject(object);
		}
	};

	private volatile boolean renameEnabled = true;
	private volatile boolean hideEnabled = true;
	private volatile Set<Integer> releaseIds = ReleaseFish.enabledIds(true, true);

	@Provides
	CatchAndReleaseConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CatchAndReleaseConfig.class);
	}

	@Override
	protected void startUp()
	{
		refreshConfig();
		renderCallbackManager.register(renderCallback);
	}

	@Override
	protected void shutDown()
	{
		renderCallbackManager.unregister(renderCallback);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CatchAndReleaseConfig.GROUP.equals(event.getGroup()))
		{
			refreshConfig();
		}
	}

	/**
	 * Shift-click drop promotes this same inventory Drop entry to left-click.
	 * The menu is built through {@code MenuEntryAdded}, so renaming the option
	 * here is what both the right-click menu and the shift-click tooltip show.
	 */
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!renameEnabled)
		{
			return;
		}
		if (!ReleaseFish.DROP_OPTION.equals(event.getOption()))
		{
			return;
		}
		if (!releaseIds.contains(event.getItemId()))
		{
			return;
		}
		event.getMenuEntry().setOption(ReleaseFish.RELEASE_OPTION);
	}

	private void refreshConfig()
	{
		renameEnabled = config.renameDropToRelease();
		hideEnabled = config.hideReleasedFish();
		releaseIds = ReleaseFish.enabledIds(config.barbarianFish(), config.flyBaitFish());
	}

	/**
	 * Hide the layer when it contains any selected fish. The client draws an
	 * item pile as one object. Walk from {@code getBottom()} through
	 * {@code getPrevious()}, which is the pile order {@code Tile#getGroundItems}
	 * uses.
	 */
	private boolean shouldDrawObject(TileObject object)
	{
		if (!hideEnabled || !(object instanceof ItemLayer))
		{
			return true;
		}
		Set<Integer> ids = releaseIds;
		Node node = ((ItemLayer) object).getBottom();
		while (node instanceof TileItem)
		{
			if (ids.contains(((TileItem) node).getId()))
			{
				return false;
			}
			node = node.getPrevious();
		}
		return true;
	}
}
