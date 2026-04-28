package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.config.Hotkeys;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public class KeybindCallbacks implements IHotkeyCallback, IClientTickHandler {
	private static final KeybindCallbacks INSTANCE = new KeybindCallbacks();

	private final AutoTradeClientTick clientTick = new AutoTradeClientTick();

	public static KeybindCallbacks getInstance() {
		return INSTANCE;
	}

	private KeybindCallbacks() {
	}

	public void setCallbacks() {
		for (ConfigHotkey hotkey : Hotkeys.HOTKEY_LIST) {
			hotkey.getKeybind().setCallback(this);
		}
	}

	public boolean functionalityEnabled() {
		return Configs.Generic.ENABLED.getBooleanValue();
	}

	public Entity getTraderHighlightEntity(Minecraft mc) {
		return clientTick.getTraderGlowEntityForRender(mc);
	}

	public int getInputContainerHighlightTicks() {
		return clientTick.getInputContainerHighlightTicks();
	}

	public int getOutputContainerHighlightTicks() {
		return clientTick.getOutputContainerHighlightTicks();
	}

	@Override
	public boolean onKeyAction(KeyAction action, IKeybind key) {
		return HotkeyActions.handle(Minecraft.getInstance(), key);
	}

	@Override
	public void onClientTick(Minecraft mc) {
		clientTick.tick(mc);
	}
}
