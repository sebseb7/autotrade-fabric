[![Build](https://github.com/sebseb7/autotrade-fabric/actions/workflows/build.yml/badge.svg)](https://github.com/sebseb7/autotrade-fabric/actions/workflows/build.yml)

# AutoTrade-fabric

## Table of contents
1. [Description](#description)
2. [Build](#build-for-1203--1204)
3. [Known Issues](#known-issues)
4. [Possible Setup](#possible-setup)
5. [Void Trading Example & Settings](#void-trading-example--settings)
6. [WDL](#wdl)

feel free to ask questions: https://github.com/sebseb7/autotrade-fabric/discussions

### Description

Allows you to AFK trade with villagers.

Player movement is not part of this mod. You have to move between villagers and input and output containers using minecart, water stream or other methods. Use a light sensor to trade only once per day per villager to keep prices from climbing.

Default keybind for options GUI: Right-Shift+T

Beginning with version v0.0.10 you can select the sell/buy items using nametagged items in items frames and the input/output containers by placing colored glass blocks nearby.

if you can't access settings via the keybind, try modmenu https://modrinth.com/mod/modmenu

#### Supported Version:

- Minecraft 1.19.4 - 1.20.4

### Build for 1.20.3 / 1.20.4

```
./gradlew build
```

#### Build for older minecraft versions:

```
./gradlew build -Pminecraft_version_out=1.20.2 -Pminecraft_version=1.20.2 -Pminecraft_version_min=1.20.2 -Pmalilib_version=0.17.0 -Pmod_menu_version=8.0.1 -Pmappings_version=1.20.2+build.4
./gradlew build -Pminecraft_version_out=1.20.1 -Pminecraft_version=1.20.1 -Pminecraft_version_min=1.20 -Pmalilib_version=0.16.1 -Pmod_menu_version=7.0.1 -Pmappings_version=1.20.1+build.10
./gradlew build -Pminecraft_version_out=1.19.4 -Pminecraft_version=1.19.4 -Pminecraft_version_min=1.19.4 -Pmalilib_version=0.15.2 -Pmod_menu_version=6.1.0 -Pmappings_version=1.19.4+build.2
```

#### Requires:

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

- on 2b2t water stream for player movement doesn't work somehow, minecart circuit works.

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
