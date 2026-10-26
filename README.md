# Catch and Release Fishing

RuneLite external plugin that treats barbarian and fly/bait catches as catch-and-release.

While you are fishing those methods, the inventory **Drop** option on the matching fish is renamed to **Release**. The click still performs the normal drop, including shift-click drop. Piles on the ground that contain only those fish are not drawn.

The project follows the [RuneLite example plugin](https://github.com/runelite/example-plugin) layout (`build.gradle`, Java 11, `runelite-plugin.properties`) so it can be submitted to the Plugin Hub with `build=standard`.

## What it changes

- **Release instead of Drop.** `MenuEntryAdded` renames the inventory `Drop` entry to `Release` and leaves the opcode, item id, and slot alone. A `ClientTick` pass repeats that rename so shift-click drop, which reuses the same Drop entry as the left-click action, shows Release as well.
- **Hidden ground fish.** A render callback skips an item layer when every item in the pile is a selected fish. That is the same hook the Hub's ground-item hider uses: the client draws a pile as one object, so one fish inside a mixed pile cannot be removed on its own. Mixed piles stay visible. Ground Items name labels are drawn by that other plugin and are unchanged.

## Which fish

Ids are `net.runelite.api.gameval.ItemID` constants. The cache names below are not always the name you see in game.

| Category | In game | Constant |
| --- | --- | --- |
| Barbarian | Leaping trout | `BRUT_SPAWNING_TROUT` |
| Barbarian | Leaping salmon | `BRUT_SPAWNING_SALMON` |
| Barbarian | Leaping sturgeon | `BRUT_STURGEON` |
| Fly / bait | Raw trout | `RAW_TROUT` |
| Fly / bait | Raw salmon | `RAW_SALMON` |
| Fly / bait | Raw pike | `RAW_PIKE` |
| Fly / bait | Raw sardine | `RAW_SARDINE` |
| Fly / bait | Raw herring | `RAW_HERRING` |
| Fly / bait | Raw rainbow fish | `HUNTING_RAW_FISH_SPECIAL` |

Cooked fish, burnt rainbow fish, roe, caviar, shrimp, and other net or harpoon catches are left alone. Noted forms are picked up from the item definition after login, because notes do not have their own constants.

## When Drop becomes Release

Hiding follows the category toggles whenever the plugin is on, so released fish do not pop back in after you stop. The rename is narrower:

- Fly and bait rods, including pearl bait and pearl fly rods, use `HUMAN_FISHING_CASTING`, `HUMAN_FISH_ONSPOT`, and the matching pearl sequences. During those animations, enabled fly/bait fish can be released.
- The heavy barbarian rod, including the pearl barbarian rod, uses `HUMAN_FISHING_CASTING_BRUT`, `HUMAN_FISHING_ONSPOT_BRUT`, and the pearl brut sequences. During those animations, enabled leaping fish can be released.
- After the animation stops, that style stays active for 100 game ticks (60 seconds) so you can empty a full inventory. Walking or standing does not cancel the timer. Any other animation does, including bare-handed barbarian fishing and the oily rod.

## Config

- **Release instead of Drop**
- **Hide fish on the ground**
- **Barbarian fish**
- **Fly and bait fish**

## Build and test

Java 11 or newer. From this directory:

```sh
./gradlew build
```

`build` compiles against the current RuneLite release and runs the unit tests for item matching, noted ids, pile hiding, and the fishing-animation window.

## Run it in a development client

```sh
./gradlew run
```

That loads this plugin into a dev RuneLite client (`--developer-mode --debug`). Log in with a Jagex account using the steps in the [RuneLite Jagex account guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). The plugin is injected with `ExternalPluginManager.loadBuiltin`, so it shows up in the plugin list without a Plugin Hub install.

In game, check:

1. Fly or bait fish a river or sea spot. Right-click raw trout, salmon, pike, sardine, herring, or raw rainbow fish and confirm the option says **Release**, then that the fish leaves the inventory.
2. Hold shift and click one of those fish. The left-click tooltip says **Release** and the fish is dropped.
3. The dropped fish model is gone when the tile holds only those fish. Drop coins on the same tile and the pile is drawn again.
4. Cooked trout, shrimp, and roe still say **Drop** and still appear on the ground.
5. At Otto's Grotto, leaping trout, salmon, and sturgeon say **Release** while the heavy rod animation is playing.
6. Turn a category off. That category goes back to **Drop** and shows on the ground. Turn **Hide fish on the ground** off and the models return without affecting the menu text.
7. Stop fishing and release the rest of the inventory within a minute. After that, or after you start another skilling animation, the option says **Drop** again.
