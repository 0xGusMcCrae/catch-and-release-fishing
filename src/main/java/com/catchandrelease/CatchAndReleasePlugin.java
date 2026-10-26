package com.catchandrelease;

import com.google.inject.Provides;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemLayer;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Renderable;
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
	description = "Renames Drop to Release for barbarian, fly/bait, and leechfin, and hides those fish on the ground",
	tags = {"fishing", "barbarian", "fly", "bait", "leechfin", "drop", "release"}
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
		public boolean addEntity(Renderable renderable, boolean drawingUi)
		{
			return shouldDrawRenderable(renderable);
		}

		@Override
		public boolean drawObject(Scene scene, TileObject object)
		{
			return shouldDrawObject(object);
		}
	};

	private volatile boolean renameEnabled = true;
	private volatile boolean leftClickEnabled = true;
	private volatile boolean hideEnabled = true;
	private volatile Set<Integer> releaseIds = ReleaseFish.enabledIds(true, true, true);

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
	 * The label changes only when Release is enabled. Left-click promotion is
	 * separate, so Drop can stay the left-click word when the rename is off.
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
			String shown = ReleaseFish.inventoryDropLabel(renameEnabled, Text.removeTags(entry.getOption()));
			if (!shown.equals(Text.removeTags(entry.getOption())))
			{
				entry.setOption(shown);
			}
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
		releaseIds = ReleaseFish.enabledIds(config.barbarianFish(), config.flyBaitFish(), config.leechfin());
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
	 * The client draws a ground pile as one object, from whichever of the three
	 * slots are filled. A brand-new pile puts its only item in {@code getTop()}
	 * and leaves bottom and middle empty, so this draw has to read all three.
	 * The README names the plugin that was read for that approach.
	 */
	private boolean shouldDrawObject(TileObject object)
	{
		if (!hideEnabled || !(object instanceof ItemLayer))
		{
			return true;
		}
		ItemLayer layer = (ItemLayer) object;
		return !ReleaseFish.pileContainsEnabledFish(
			releaseIds,
			itemId(layer.getBottom()),
			itemId(layer.getMiddle()),
			itemId(layer.getTop()));
	}

	/**
	 * Tile items that are submitted as entities, rather than as a pile, use the
	 * same id check. Inventory icons are not tile items.
	 */
	private boolean shouldDrawRenderable(Renderable renderable)
	{
		if (!hideEnabled || !(renderable instanceof TileItem))
		{
			return true;
		}
		return !releaseIds.contains(((TileItem) renderable).getId());
	}

	private static Integer itemId(Renderable renderable)
	{
		if (!(renderable instanceof TileItem))
		{
			return null;
		}
		return ((TileItem) renderable).getId();
	}
}
