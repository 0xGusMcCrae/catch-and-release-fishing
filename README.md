# Catch and Release Fishing

RuneLite plugin that renames inventory **Drop** to **Release** on selected fish, and hides those fish as ground items. Release still drops the item. Left-click on a configured fish is Release. Shift-click drop uses that same entry.

## Fish

Barbarian: leaping trout, leaping salmon, leaping sturgeon.

Fly and bait: raw trout, raw salmon, raw pike, raw sardine, raw herring, raw rainbow fish, raw slimy eel, raw cave eel.

Leechfin: leechfin (big fishing net, Vampyrium). Item id `LEECHFIN` (33621).

Cooked fish, burnt fish, roe, caviar, and blood sacs are not included. A ground pile that contains one of these fish is not drawn, and that fish has no Take, Examine, or mouseover option, on any tile, including fish other players dropped.

The client draws a pile from three slots (bottom, middle, top). The first item on an empty tile is stored in the top slot, with the other two empty, and that is the model that used to flash. Hiding reads those three slots in the draw callback. Menu entries for those fish are removed as they are added, so the hover line never offers them.

The three-slot idea came from reading [Hide Ground Items](https://github.com/vahnx/hide-ground-items) by vahnx (BSD 2-Clause, Plugin Hub commit `990adbe43bc725587eebcdd4cc4678f06fdc0b44`). That plugin was a reference for the approach only. None of its source was copied or adapted into this repository.

## Config

- Release instead of Drop
- Left-click Release
- Hide fish on the ground
- Barbarian fish
- Fly and bait fish
- Leechfin

## Build and run

Java 11 or newer.

```sh
./gradlew build
./gradlew run
```

`build` compiles against the current RuneLite release and runs the tests. `run` starts a dev client with this plugin loaded (`--developer-mode --debug`). Log in with a Jagex account using the [RuneLite Jagex account guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Bolt

```sh
./gradlew shadowJar
```

Point Bolt's custom RuneLite JAR at `build/libs/catch-and-release-fishing-1.0-SNAPSHOT-all.jar`. Bolt runs `java -Duser.home=<Bolt data> -jar <jar> -J-Duser.home=<Bolt data>`. The dev client drops that `-J` argument (and other launcher-only options such as `--configure`) and, when assertions are off, starts itself again with `-ea`. You do not need `JAVA_TOOL_OPTIONS=-ea`. `--developer-mode` and `--debug` are still passed through.
