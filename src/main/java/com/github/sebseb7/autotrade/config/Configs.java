package com.github.sebseb7.autotrade.config;

import com.github.sebseb7.autotrade.Reference;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.IConfigValue;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;

public class Configs implements IConfigHandler {
	private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";

	public static class Generic {
		private static final String GENERIC_KEY = Reference.MOD_ID + ".config.generic";

		private static ConfigBoolean i18n(ConfigBoolean config) {
			return config.apply(GENERIC_KEY)
					.translatedName(GENERIC_KEY + ".prettyName." + config.getName());
		}

		private static ConfigInteger i18n(ConfigInteger config) {
			return config.apply(GENERIC_KEY)
					.translatedName(GENERIC_KEY + ".prettyName." + config.getName());
		}

		private static ConfigString i18n(ConfigString config) {
			return config.apply(GENERIC_KEY)
					.translatedName(GENERIC_KEY + ".prettyName." + config.getName());
		}

		private static ConfigDouble i18n(ConfigDouble config) {
			return config.apply(GENERIC_KEY)
					.translatedName(GENERIC_KEY + ".prettyName." + config.getName());
		}

		private static ConfigStringList i18n(ConfigStringList config) {
			return config.apply(GENERIC_KEY)
					.translatedName(GENERIC_KEY + ".prettyName." + config.getName());
		}

		public static final ConfigBoolean ENABLED = i18n(new ConfigBoolean("enabled", false,
				"Do auto trading with villagers in range"));
		public static final ConfigBoolean ITEM_FRAME = i18n(new ConfigBoolean("selectUsingItemFrame", true,
				"Select buy/sell items with item frames (max. distance 3) with items nametagged \"buy\" or \"sell\""));
		public static final ConfigBoolean SELECT_BY_NAMETAG = i18n(new ConfigBoolean("selectByNameTag", true,
				"Select input/output containers via item frame on the block: put a nametagged chest item (\"input\" / \"output\") in the frame (max. distance 3)"));
		public static final ConfigBoolean TURN_HEAD_BEFORE_INTERACT = i18n(new ConfigBoolean("turnHeadBeforeInteract", true,
				"Turn the player's head toward the villager before interacting (some servers e.g. GrimAC check head direction, most servers don't need this)"));

		public static final ConfigBoolean ENABLE_SELL = i18n(new ConfigBoolean("enableSell", false,
				"Enable selling (if disabled emeralds are taken from the input container)"));
		public static final ConfigString SELL_ITEM = i18n(new ConfigString("sellItem", "minecraft:gold_ingot",
				"The item to sell for emerald. Optional suffix #enc1=lv&enc2=lv (enchantment registry ids) matches exact enchantments, e.g. enchanted books."));
		public static final ConfigInteger SELL_LIMIT = i18n(new ConfigInteger("sellLimit", 64, 1, 64,
				"max price to sell for"));
		public static final ConfigBoolean ENABLE_BUY = i18n(new ConfigBoolean("enableBuy", false,
				"Enable buying (if disabled emeralds are placed in the output container)"));
		public static final ConfigString BUY_ITEM = i18n(new ConfigString("buyItem", "minecraft:redstone",
				"The item to buy using emerald. Optional suffix #enc1=lv&enc2=lv matches exact enchantments (use set-buy hotkey with the book in hand)."));
		public static final ConfigInteger BUY_LIMIT = i18n(new ConfigInteger("buyLimit", 64, 1, 64, "max price to buy for"));
		public static final ConfigInteger MAX_INPUT_ITEMS = i18n(new ConfigInteger("maxInputStacks", 9, 1, 35,
				"stacks to take from input container (or emerald container in buy-only mode), also the max amount of input and emerald kept."));
		public static final ConfigInteger INPUT_CONTAINER_X = i18n(new ConfigInteger("inputContainerX", 0, -30000000,
				30000000, "Input container X (not used when sell disabled)"));
		public static final ConfigInteger INPUT_CONTAINER_Y = i18n(new ConfigInteger("inputContainerY", 0, -64, 320,
				"Input container Y (not used when sell disabled)"));
		public static final ConfigInteger INPUT_CONTAINER_Z = i18n(new ConfigInteger("inputContainerZ", 0, -30000000,
				30000000, "Input container Z (not used when sell disabled)"));
		public static final ConfigInteger OUTPUT_CONTAINER_X = i18n(new ConfigInteger("outputContainerX", 0, -30000000,
				30000000, "Output container X (not used when buy disabled)"));
		public static final ConfigInteger OUTPUT_CONTAINER_Y = i18n(new ConfigInteger("outputContainerY", 0, -64, 320,
				"Output container Y (not used when buy disabled)"));
		public static final ConfigInteger OUTPUT_CONTAINER_Z = i18n(new ConfigInteger("outputContainerZ", 0, -30000000,
				30000000, "Output container Z (not used when buy disabled)"));
		public static final ConfigInteger VOID_TRADING_DELAY = i18n(new ConfigInteger("voidTradingDelay", 0, 0, 30000000,
				"delay in ticks for void trading"));
		public static final ConfigBoolean VOID_TRADING_DELAY_AFTER_TELEPORT = i18n(new ConfigBoolean("delayAfterTeleport",
				true,
				"true: Start the delay after the villager was unloaded; false: Start the delay after the trade has been initiated"));
		public static final ConfigInteger CONTAINER_CLOSE_DELAY = i18n(new ConfigInteger("containerCloseDelay", 0, 0,
				30000000, "delay in ticks; to get signal from trapped chest"));

		public static final ConfigDouble INTERACT_DISTANCE = i18n(new ConfigDouble("interactDistance", 2.5, 1.0, 10.0,
				"Maximum distance to interact with a villager (in blocks)"));
		public static final ConfigDouble REMOVE_DISTANCE = i18n(new ConfigDouble("removeDistance", 4.0, 1.0, 20.0,
				"Distance at which a villager is removed from the tracked range list (in blocks)"));
		public static final ConfigDouble ITEM_FRAME_RADIUS = i18n(new ConfigDouble("itemFrameRadius", 3.0, 1.0, 10.0,
				"Maximum distance to detect item frames for buy/sell/container selection (in blocks)"));
		public static final ConfigDouble CONTAINER_INTERACTION_RANGE = i18n(new ConfigDouble("containerInteractionRange", 4.0, 1.0, 32.0,
				"Maximum distance to interact with input/output containers (in blocks)"));
		public static final ConfigDouble CONTAINER_FORGET_RANGE = i18n(new ConfigDouble("containerForgetRange", 9.0, 1.0, 64.0,
				"Distance at which a container is forgotten after interaction (must be > interaction range)"));
		public static final ConfigDouble VISIBLE_POINT_BUFFER = i18n(new ConfigDouble("visiblePointBuffer", 0.0001, 0.0, 0.1,
				"Buffer tolerance for line-of-sight checks (higher = more lenient)"));

		public static final ConfigString SELECTED_ENCHANTMENTS = i18n(new ConfigString("selectedEnchantments", "",
				"Comma-separated list of selected enchantment IDs (set via the \"Select Enchantments\" button on a librarian's trade screen)"));

		public static final ConfigString AFK_ON_COMMAND = i18n(new ConfigString("afkOnCommand", "",
				"Command to execute to turn AFK on"));
		public static final ConfigString AFK_OFF_COMMAND = i18n(new ConfigString("afkOffCommand", "",
				"Command to execute to turn AFK off"));
		public static final ConfigStringList TIME_WAYPOINTS = i18n(new ConfigStringList("timeWaypoints",
				com.google.common.collect.ImmutableList.of(),
				"Time waypoints for Baritone walking. Format per entry: tick:x,y,z"));

		/**
		 * Parse time waypoints from the config list.
		 * Each entry format: "tick:x,y,z" (e.g. "0:100,64,200")
		 * @return list of TimeWaypoint objects sorted by tick
		 */
		public static java.util.List<TimeWaypoint> parseTimeWaypoints() {
			java.util.List<TimeWaypoint> waypoints = new java.util.ArrayList<>();
			for (String entry : TIME_WAYPOINTS.getStrings()) {
				entry = entry.trim();
				if (entry.isEmpty()) continue;
				String[] parts = entry.split(":");
				if (parts.length != 2) continue;
				try {
					long tick = Long.parseLong(parts[0].trim());
					String[] coords = parts[1].split(",");
					if (coords.length == 3) {
						int x = Integer.parseInt(coords[0].trim());
						int y = Integer.parseInt(coords[1].trim());
						int z = Integer.parseInt(coords[2].trim());
						waypoints.add(new TimeWaypoint(tick, x, y, z));
					}
				} catch (NumberFormatException ignored) {
				}
			}
			waypoints.sort(java.util.Comparator.comparingLong(w -> w.tick));
			return waypoints;
		}

		public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(ENABLED, ITEM_FRAME, SELECT_BY_NAMETAG,
			TURN_HEAD_BEFORE_INTERACT,
			ENABLE_SELL, SELL_ITEM, SELL_LIMIT, ENABLE_BUY, BUY_ITEM, BUY_LIMIT, MAX_INPUT_ITEMS,
				INPUT_CONTAINER_X, INPUT_CONTAINER_Y, INPUT_CONTAINER_Z, OUTPUT_CONTAINER_X, OUTPUT_CONTAINER_Y,
				OUTPUT_CONTAINER_Z, VOID_TRADING_DELAY, VOID_TRADING_DELAY_AFTER_TELEPORT, CONTAINER_CLOSE_DELAY,
				INTERACT_DISTANCE, REMOVE_DISTANCE, ITEM_FRAME_RADIUS, CONTAINER_INTERACTION_RANGE, CONTAINER_FORGET_RANGE, VISIBLE_POINT_BUFFER,
			SELECTED_ENCHANTMENTS,
			AFK_ON_COMMAND, AFK_OFF_COMMAND, TIME_WAYPOINTS);
	}

	public record TimeWaypoint(long tick, int x, int y, int z) {}

	public static void loadFromFile() {
		File configFile = new File(getConfigDirectory(), CONFIG_FILE_NAME);

		if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
			try {
				String json = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
				JsonElement element = JsonParser.parseString(json);

				if (element != null && element.isJsonObject()) {
					JsonObject root = element.getAsJsonObject();

					ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
					ConfigUtils.readConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
				}
			} catch (IOException ignored) {
				// Malformed or unreadable config; defaults stay active.
			}
		}
	}

	public static void saveToFile() {
		File dir = getConfigDirectory();

		if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
			try {
				JsonObject root = new JsonObject();

				ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
				ConfigUtils.writeConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);

				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				Files.writeString(new File(dir, CONFIG_FILE_NAME).toPath(), gson.toJson(root), StandardCharsets.UTF_8);
			} catch (IOException ignored) {
			}
		}
	}

	@Override
	public void load() {
		loadFromFile();
	}

	@Override
	public void save() {
		saveToFile();
	}

	private static File getConfigDirectory() {
		return FabricLoader.getInstance().getConfigDir().toFile();
	}
}
