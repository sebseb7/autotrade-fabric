[![Build](https://github.com/sebseb7/autotrade-fabric/actions/workflows/build.yml/badge.svg)](https://github.com/sebseb7/autotrade-fabric/actions/workflows/build.yml)

# AutoTrade-fabric

## Table of contents
1. [Description](#description)
2. [Build](#build)
3. [Known Issues](#known-issues)
4. [Possible Setup](#possible-setup)
5. [Void Trading Example & Settings](#void-trading-example--settings)
6. [WDL](#wdl)

Feel free to ask questions: https://github.com/sebseb7/autotrade-fabric/discussions

### Description

Allows you to AFK trade with villagers.

Player movement is not part of this mod. You have to move between villagers and input and output containers using minecart, water stream or other methods. Use a light sensor to trade only once per day per villager to keep prices from climbing.

Default keybind for options GUI: Right-Shift+T

Beginning with version v0.0.10 you can select the sell/buy items using nametagged items in items frames and the input/output containers by placing colored glass blocks nearby.

From v0.0.13, those selections (and the set-buy / set-sell hotkeys) store exact enchantments when the stack is enchanted, so you can target a specific book or tool; a plain item id in config still matches any enchantment variant of that item.

If you can't access settings via the keybind, try Mod Menu https://modrinth.com/mod/modmenu

#### Supported versions (this branch)

Fabric targets maintained in-tree ([Stonecutter](https://stonecutter.kikugie.dev/) + [Modstitch](https://github.com/modunion/modstitch)):

- **Minecraft 1.21.10** (`:1.21.10-fabric`)
- **Minecraft 1.21.11** (`:1.21.11-fabric`)
- **Minecraft 26.1.2** (`:26.1.2-fabric`)

Older Minecraft releases (for example 1.19.x–1.20.x) are not built from this multi-target setup; use an older release tag or branch if you need those versions.

### Build

Requirements: a JDK suitable for the target (the build uses Java **21** for 1.21.x and **25** for 26.x via Gradle toolchains).

Build **every** configured game version (recommended before you commit):

```bash
./gradlew chiseledBuild
```

If the parallel build is flaky, try:

```bash
./gradlew chiseledBuild --max-workers=1
```

Build **one** Fabric target (jar ends up under that version subproject, e.g. `versions/1.21.11-fabric/build/libs/`):

```bash
./gradlew :1.21.10-fabric:build
./gradlew :1.21.11-fabric:build
./gradlew :26.1.2-fabric:build
```

When using Stonecutter’s active-project workflow, reset it before committing:

```bash
./gradlew resetActiveProject
```

#### Requires

- malilib

### Known Issues

- itemscroller trade favorites break trading

### Possible Setup

- shulker unloader presenter for input
- shulker loader presenter for output
- input, output, villagers connected by circular water stream to move the player

![b8e3f8d519109677b7eb24cd034440253de8a313](https://github.com/sebseb7/autotrade-fabric/assets/677956/f48c2d3e-e839-40ee-8d20-c0140eca06d2)

- block the water stream like this, to have the player trade only once per day to keep prices low:

<img width="1254" alt="image" src="https://github.com/sebseb7/autotrade-fabric/assets/677956/974bc9af-e5aa-40be-b980-c5721434e130">

- on 2b2t water stream for player movement doesn't work somehow, minecart circuit works, or this EssentialClient Script https://gist.github.com/sebseb7/6477190dd531d05991741ccb031c0684

#### Demo Video

https://youtu.be/ZbxkZqb-VsU

### Void Trading Example & Settings

- selectUsingGlassBlock: true
- selectionOffset: -3
- voidTradingDelay: 50
- delayAfterTeleport: true

https://github.com/sebseb7/autotrade-fabric/releases/download/v0.0.10/void_trader_outer_island.litematic
https://github.com/sebseb7/autotrade-fabric/releases/download/v0.0.10/void_trader_central_island.litematic

### WDL

https://github.com/sebseb7/autotrade-fabric/releases/download/v0.0.10/AutoTradeDemoWDL.zip
