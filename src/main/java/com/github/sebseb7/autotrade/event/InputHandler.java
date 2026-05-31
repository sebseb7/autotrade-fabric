package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.config.Hotkeys;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;

public class InputHandler implements IKeybindProvider {
	private final KeybindCallbacks callbacks;

	public InputHandler() {
		this.callbacks = KeybindCallbacks.getInstance();
	}

	@Override
	public void addKeysToMap(IKeybindManager manager) {
		for (IHotkey hotkey : Hotkeys.HOTKEY_LIST) {
			manager.addKeybindToMap(hotkey.getKeybind());
		}
	}

	@Override
	public void addHotkeys(IKeybindManager manager) {
		manager.addHotkeysForCategory(StringUtils.translate("autotrade.mod.name"),
				"autotrade.hotkeys.category.hotkeys", Hotkeys.HOTKEY_LIST);
	}
}
