package com.catchandrelease;

import com.google.inject.Provides;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemLayer;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.Node;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

@Slf4j
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
	private ItemManager itemManager;

	@Inject
	private RenderCallbackManager renderCallbackManager;

	private final FishingActivity.Session session = new FishingActivity.Session();

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
	private volatile boolean barbarianEnabled = true;
	private volatile boolean flyBaitEnabled = true;
	private volatile Map<Integer, FishCategory> notedCategories = Collections.emptyMap();

	private boolean notedIdsReady;

	@Provides
	CatchAndReleaseConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CatchAndReleaseConfig.class);
	}

	@Override
	protected void startUp()
	{
		refreshConfig();
		notedIdsReady = false;
		renderCallbackManager.register(renderCallback);
		log.debug("Catch and Release Fishing started");
	}

	@Override
	protected void shutDown()
	{
		renderCallbackManager.unregister(renderCallback);
		session.reset();
		notedCategories = Collections.emptyMap();
		notedIdsReady = false;
		log.debug("Catch and Release Fishing stopped");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CatchAndReleaseConfig.GROUP.equals(event.getGroup()))
		{
			refreshConfig();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING || state == GameState.CONNECTION_LOST)
		{
			session.reset();
			notedCategories = Collections.emptyMap();
			notedIdsReady = false;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		ensureNotedIds();
		observeLocalPlayer();
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (event.getActor() != client.getLocalPlayer())
		{
			return;
		}
		session.observe(event.getActor().getAnimation(), client.getTickCount());
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		observeLocalPlayer();
		applyRelease(event.getMenuEntry());
	}

	/**
	 * Shift-click drop reuses the inventory Drop entry as the left-click action.
	 * Walking the built menu catches that entry when it is assembled or swapped
	 * without another {@link MenuEntryAdded}.
	 */
	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (!renameEnabled || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		observeLocalPlayer();
		int tick = client.getTickCount();
		if (!session.isActive(FishCategory.BARBARIAN, tick) && !session.isActive(FishCategory.FLY_BAIT, tick))
		{
			return;
		}

		Menu menu = client.getMenu();
		if (menu == null)
		{
			return;
		}
		MenuEntry[] entries = menu.getMenuEntries();
		boolean changed = false;
		for (MenuEntry entry : entries)
		{
			if (applyRelease(entry))
			{
				changed = true;
			}
		}
		if (changed)
		{
			menu.setMenuEntries(entries);
		}
	}

	private void refreshConfig()
	{
		renameEnabled = config.renameDropToRelease();
		hideEnabled = config.hideReleasedFish();
		barbarianEnabled = config.barbarianFish();
		flyBaitEnabled = config.flyBaitFish();
	}

	private void observeLocalPlayer()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		session.observe(player.getAnimation(), client.getTickCount());
	}

	/**
	 * @return true when the entry's option text was changed to Release
	 */
	private boolean applyRelease(MenuEntry entry)
	{
		if (!renameEnabled || entry == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return false;
		}
		if (!isInventoryEntry(entry))
		{
			return false;
		}

		String option = entry.getOption();
		if (option == null)
		{
			return false;
		}
		int itemId = entry.getItemId();
		Widget widget = entry.getWidget();
		if (itemId <= 0 && widget != null)
		{
			itemId = widget.getItemId();
		}

		int tick = client.getTickCount();
		String replacement = ReleaseFish.releaseOption(
			Text.removeTags(option),
			itemId,
			true,
			barbarianEnabled,
			flyBaitEnabled,
			session.isActive(FishCategory.BARBARIAN, tick),
			session.isActive(FishCategory.FLY_BAIT, tick),
			notedCategories);
		if (replacement == null || replacement.equals(option))
		{
			return false;
		}
		entry.setOption(replacement);
		return true;
	}

	private static boolean isInventoryEntry(MenuEntry entry)
	{
		Widget widget = entry.getWidget();
		if (widget == null)
		{
			return false;
		}
		return WidgetUtil.componentToInterface(widget.getId()) == InterfaceID.INVENTORY;
	}

	private boolean shouldDrawObject(TileObject object)
	{
		if (!hideEnabled || !(object instanceof ItemLayer))
		{
			return true;
		}
		ItemLayer layer = (ItemLayer) object;
		Node node = layer.getTop();
		if (!(node instanceof TileItem))
		{
			node = layer.getBottom();
		}

		boolean sawFish = false;
		Map<Integer, FishCategory> noted = notedCategories;
		while (node instanceof TileItem)
		{
			int itemId = ((TileItem) node).getId();
			if (!ReleaseFish.shouldHide(itemId, true, barbarianEnabled, flyBaitEnabled, noted))
			{
				return true;
			}
			sawFish = true;
			node = node.getNext();
		}
		return !sawFish;
	}

	private void ensureNotedIds()
	{
		if (notedIdsReady || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		ItemComposition sample = itemManager.getItemComposition(ItemID.RAW_TROUT);
		String sampleName = sample.getName();
		int sampleNoted = sample.getLinkedNoteId();
		if (sampleName == null || sampleName.isEmpty() || "null".equalsIgnoreCase(sampleName) || sampleNoted <= 0)
		{
			return;
		}

		Map<Integer, FishCategory> found = new HashMap<>();
		for (int itemId : ReleaseFish.allItemIds())
		{
			ItemComposition composition = itemManager.getItemComposition(itemId);
			ReleaseFish.rememberNoted(found, itemId, composition.getLinkedNoteId(), composition.getNote() != -1);
		}
		notedCategories = Collections.unmodifiableMap(found);
		notedIdsReady = true;
	}
}
