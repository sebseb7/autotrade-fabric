package com.github.sebseb7.autotrade.config;

import com.github.sebseb7.autotrade.Reference;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.IConfigValue;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import java.io.File;

public class Configs implements IConfigHandler {
	private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";

	public static class Generic {
		public static final ConfigBoolean ENABLED = new ConfigBoolean("enabled", false,
				"Do auto trading with villagers in range");
		public static final ConfigBoolean ENABLE_SELL = new ConfigBoolean("enableSell", false,
				"Enable selling (if disabled emeralds are taken from the input container)");
		public static final ConfigString SELL_ITEM = new ConfigString("sellItem", "minecraft:gold_ingot",
				"The item to sell for emerald.");
		public static final ConfigInteger SELL_LIMIT = new ConfigInteger("sellLimit", 64, 1, 64,
				"max price to sell for");
		public static final ConfigBoolean ENABLE_BUY = new ConfigBoolean("enableBuy", false,
				"Enable buying (if disabled emeralds are placed in the output container)");
		public static final ConfigString BUY_ITEM = new ConfigString("buyItem", "minecraft:redstone",
				"The item to buy using emerald.");
		public static final ConfigInteger BUY_LIMIT = new ConfigInteger("buyLimit", 64, 1, 64, "max price to buy for");
		public static final ConfigInteger MAX_INPUT_ITEMS = new ConfigInteger("maxInputStacks", 9, 1, 35,
				"stacks to take from input container (or emerald container in buy-only mode)");
		public static final ConfigInteger INPUT_CONTAINER_X = new ConfigInteger("inputContainerX", 0, -30000000,
				30000000, "Input container X (not used when sell disabled)");
		public static final ConfigInteger INPUT_CONTAINER_Y = new ConfigInteger("inputContainerY", 0, -64, 320,
				"Input container Y (not used when sell disabled)");
		public static final ConfigInteger INPUT_CONTAINER_Z = new ConfigInteger("inputContainerZ", 0, -30000000,
				30000000, "Input container Z (not used when sell disabled)");
		public static final ConfigInteger OUTPUT_CONTAINER_X = new ConfigInteger("outputContainerX", 0, -30000000,
				30000000, "Output container X (not used when buy disabled)");
		public static final ConfigInteger OUTPUT_CONTAINER_Y = new ConfigInteger("outputContainerY", 0, -64, 320,
				"Output container Y (not used when buy disabled)");
		public static final ConfigInteger OUTPUT_CONTAINER_Z = new ConfigInteger("outputContainerZ", 0, -30000000,
				30000000, "Output container Z (not used when buy disabled)");
		public static final ConfigInteger VOID_TRADING_DELAY = new ConfigInteger("voidTradingDelay", 0, 0, 30000000,
				"delay in ticks for void trading");
		public static final ConfigBoolean VOID_TRADING_DELAY_AFTER_TELEPORT = new ConfigBoolean("delayAfterTeleport",
				false, "Start the delay after th villager is gone");

		public static final ImmutableList<IConfigValue> OPTIONS = ImmutableList.of(ENABLED, ENABLE_SELL, SELL_ITEM,
				SELL_LIMIT, ENABLE_BUY, BUY_ITEM, BUY_LIMIT, MAX_INPUT_ITEMS, INPUT_CONTAINER_X, INPUT_CONTAINER_Y,
				INPUT_CONTAINER_Z, OUTPUT_CONTAINER_X, OUTPUT_CONTAINER_Y, OUTPUT_CONTAINER_Z, VOID_TRADING_DELAY,
				VOID_TRADING_DELAY_AFTER_TELEPORT);
	}

	public static void loadFromFile() {
		File configFile = new File(FileUtils.getConfigDirectory(), CONFIG_FILE_NAME);

		if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
			JsonElement element = JsonUtils.parseJsonFile(configFile);

			if (element != null && element.isJsonObject()) {
				JsonObject root = element.getAsJsonObject();

				ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
				ConfigUtils.readConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
			}
		}
	}

	public static void saveToFile() {
		File dir = FileUtils.getConfigDirectory();

		if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
			JsonObject root = new JsonObject();

			ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
			ConfigUtils.writeConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);

			JsonUtils.writeJsonToFile(root, new File(dir, CONFIG_FILE_NAME));
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
}
