package com.github.sebseb7.autotrade.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import java.util.List;

public class Hotkeys {
	public static final ConfigHotkey TOGGLE_KEY = new ConfigHotkey("toggleTrading", "",
			"Enables / disables auto trading");
	public static final ConfigHotkey SET_SELL_KEY = new ConfigHotkey("setSellItem", "",
			"Sets the item to sell from hotbar");
	public static final ConfigHotkey SET_BUY_KEY = new ConfigHotkey("setBuyItem", "",
			"Sets the item to buy from hotbar");
	public static final ConfigHotkey SET_INPUT_KEY = new ConfigHotkey("setInputContainer", "",
			"Sets the input (item to sell) container");
	public static final ConfigHotkey SET_OUTPUT_KEY = new ConfigHotkey("setOutputContainer", "",
			"Sets the output (item to buy) container");
	public static final ConfigHotkey OPEN_GUI_SETTINGS = new ConfigHotkey("openGuiSettings", "RIGHT_SHIFT,T",
			"Open the Config GUI");

	public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(TOGGLE_KEY, SET_SELL_KEY, SET_BUY_KEY,
			SET_INPUT_KEY, SET_OUTPUT_KEY, OPEN_GUI_SETTINGS);
}
