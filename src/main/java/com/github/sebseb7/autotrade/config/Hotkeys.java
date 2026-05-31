package com.github.sebseb7.autotrade.config;

import com.github.sebseb7.autotrade.Reference;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import java.util.List;

public class Hotkeys {
	private static final String HOTKEYS_KEY = Reference.MOD_ID + ".config.hotkeys";

	private static ConfigHotkey i18n(ConfigHotkey config) {
		config.apply(HOTKEYS_KEY);
		return config.translatedName(HOTKEYS_KEY + ".prettyName." + config.getName());
	}

	public static final ConfigHotkey TOGGLE_KEY = i18n(new ConfigHotkey("toggleTrading", "",
			"Enables / disables auto trading"));
	public static final ConfigHotkey SET_SELL_KEY = i18n(new ConfigHotkey("setSellItem", "",
			"Sets the item to sell from hotbar"));
	public static final ConfigHotkey SET_BUY_KEY = i18n(new ConfigHotkey("setBuyItem", "",
			"Sets the item to buy from hotbar"));
	public static final ConfigHotkey SET_INPUT_KEY = i18n(new ConfigHotkey("setInputContainer", "",
			"Sets the input (item to sell) container"));
	public static final ConfigHotkey SET_OUTPUT_KEY = i18n(new ConfigHotkey("setOutputContainer", "",
			"Sets the output (item to buy) container"));
	public static final ConfigHotkey OPEN_GUI_SETTINGS = i18n(new ConfigHotkey("openGuiSettings", "RIGHT_SHIFT,T",
			"Open the Config GUI"));

	public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(TOGGLE_KEY, SET_SELL_KEY, SET_BUY_KEY,
			SET_INPUT_KEY, SET_OUTPUT_KEY, OPEN_GUI_SETTINGS);
}
