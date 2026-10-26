package com.catchandrelease;

import com.google.inject.Provides;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemLayer;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Node;
import net.runelite.api.Scene;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Catch and Release Fishing",
	description = "Renames Drop to Release for barbarian and fly/bait fish, and hides those fish on the ground",
	tags = {"fishing", "barbarian", "fly", "bait", "drop", "release"}
)
public class CatchAndReleasePlugin extends Plugin
{
	@Inject
	private Client client;

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
	private volatile boolean leftClickEnabled = true;
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
	 * Remove Take, Examine, and the other ground options for these fish as the
	 * entry is added, so the hover text never offers them. Other entries on the
	 * menu stay in place.
	 */
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!hideEnabled)
		{
			return;
		}
		MenuAction type = MenuAction.of(event.getType());
		if (!ReleaseFish.isGroundMenuAction(type))
		{
			return;
		}
		if (!releaseIds.contains(event.getIdentifier()))
		{
			return;
		}
		client.getMenu().removeMenuEntry(event.getMenuEntry());
	}

	/**
	 * Runs after the usual left-click swaps, which still see the option as Drop.
	 * The label is changed here, and Release is moved to the top only when it is
	 * not already the left-click option.
	 */
	@Subscribe(priority = -1)
	public void onPostMenuSort(PostMenuSort event)
	{
		if (!renameEnabled && !leftClickEnabled)
		{
			return;
		}

		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		int dropIndex = -1;
		for (int i = 0; i < entries.length; i++)
		{
			MenuEntry entry = entries[i];
			if (!isInventoryFishDrop(entry))
			{
				continue;
			}
			entry.setOption(ReleaseFish.RELEASE_OPTION);
			dropIndex = i;
		}
		if (!leftClickEnabled || dropIndex == -1)
		{
			return;
		}

		MenuEntry drop = entries[dropIndex];
		boolean typeChanged = false;
		if (drop.getType() == MenuAction.CC_OP_LOW_PRIORITY)
		{
			// Low priority ops are sorted under Use and are right-click only.
			drop.setType(MenuAction.CC_OP);
			typeChanged = true;
		}

		int top = entries.length - 1;
		if (dropIndex == top)
		{
			if (typeChanged)
			{
				menu.setMenuEntries(entries);
			}
			return;
		}

		entries[dropIndex] = entries[top];
		entries[top] = drop;
		menu.setMenuEntries(entries);
	}

	private void refreshConfig()
	{
		renameEnabled = config.renameDropToRelease();
		leftClickEnabled = config.leftClickRelease();
		hideEnabled = config.hideReleasedFish();
		releaseIds = ReleaseFish.enabledIds(config.barbarianFish(), config.flyBaitFish());
	}

	private boolean isInventoryFishDrop(MenuEntry entry)
	{
		String option = entry.getOption();
		if (option == null)
		{
			return false;
		}
		String stripped = Text.removeTags(option);
		if (!ReleaseFish.DROP_OPTION.equals(stripped) && !ReleaseFish.RELEASE_OPTION.equals(stripped))
		{
			return false;
		}
		int itemId = entry.getItemId();
		Widget widget = entry.getWidget();
		if (itemId <= 0 && widget != null)
		{
			itemId = widget.getItemId();
		}
		if (!releaseIds.contains(itemId))
		{
			return false;
		}
		int widgetId = widget != null ? widget.getId() : entry.getParam1();
		return WidgetUtil.componentToInterface(widgetId) == InterfaceID.INVENTORY;
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
