[![Build](https://github.com/sebseb7/autotrade-fabric/actions/workflows/build.yml/badge.svg)](https://github.com/sebseb7/autotrade-fabric/actions/workflows/build.yml)

# AutoTrade-fabric

Allows you to AFK trade with villagers.

Player movement is not part of this mod. You have to move between villagers and input and output containers using minecart, water stream or other methods. Use a light sensor to trade only once per day per villager to keep prices from climbing.

Default keybind for options GUI: Right-Shift+T

Beginning with version v0.0.10 you can select the sell/buy items using nametagged items in items frames and the input/output containers by placing colored glass blocks nearby.

if you can't access settings via the keybind, try modmenu https://modrinth.com/mod/modmenu

# Supported Version:

- Minecraft 1.19.4 - 1.20.4

# Build for 1.20.3 / 1.20.4

```
./gradlew build
```

# Build for older minecraft versions:

```
./gradlew build -Pminecraft_version_out=1.20.2 -Pminecraft_version=1.20.2 -Pminecraft_version_min=1.20.2 -Pmalilib_version=0.17.0 -Pmod_menu_version=8.0.1 -Pmappings_version=1.20.2+build.4
./gradlew build -Pminecraft_version_out=1.20.1 -Pminecraft_version=1.20.1 -Pminecraft_version_min=1.20 -Pmalilib_version=0.16.1 -Pmod_menu_version=7.0.1 -Pmappings_version=1.20.1+build.10
./gradlew build -Pminecraft_version_out=1.19.4 -Pminecraft_version=1.19.4 -Pminecraft_version_min=1.19.4 -Pmalilib_version=0.15.2 -Pmod_menu_version=6.1.0 -Pmappings_version=1.19.4+build.2
```

# Requires:

- malilib 

# Known Issues:

- itemscroller trade favorites break trading

# Demo World Download

- https://github.com/sebseb7/autotrade-fabric/releases/download/v0.0.2/AutoTradeDemo_WDL.zip
- Setup required for the Demo World Download:

# Settings Screen

<img width="570" alt="image" src="https://github.com/sebseb7/autotrade-fabric/assets/677956/4e6f8311-96a8-4952-bad7-3eab5e812828">

# Possible Setup

- shulker unloader presenter for input
- shulker loader presenter for output
- input, output, villagers connected by circular water stream to move the player

![b8e3f8d519109677b7eb24cd034440253de8a313](https://github.com/sebseb7/autotrade-fabric/assets/677956/f48c2d3e-e839-40ee-8d20-c0140eca06d2)

- block the water stream like this, to have the player trade only once per day to keep prices low:

<img width="1254" alt="image" src="https://github.com/sebseb7/autotrade-fabric/assets/677956/974bc9af-e5aa-40be-b980-c5721434e130">

- on 2b2t water stream for player movement doesn't work somehow, minecart circuit works.

# Demo Video

https://youtu.be/ZbxkZqb-VsU

# Void Trading

For void trading support see: https://github.com/sebseb7/autotrade-fabric/issues/1
