# Catch and Release Fishing

RuneLite plugin that renames inventory **Drop** to **Release** on selected fish, and hides those fish as ground items. Release still drops the item. Shift-click drop uses that same Drop entry, so the left-click tooltip says Release as well.

## Fish

Barbarian: leaping trout, leaping salmon, leaping sturgeon.

Fly and bait: raw trout, raw salmon, raw pike, raw sardine, raw herring, raw rainbow fish, raw slimy eel, raw cave eel.

Cooked fish, burnt fish, roe, and caviar are not included. A ground pile that contains one of these fish is not drawn, on any tile, including fish other players dropped.

## Config

- Release instead of Drop
- Hide fish on the ground
- Barbarian fish
- Fly and bait fish

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
