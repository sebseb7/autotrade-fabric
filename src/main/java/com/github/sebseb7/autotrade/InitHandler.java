package com.github.sebseb7.autotrade;

import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.event.InputHandler;
import com.github.sebseb7.autotrade.event.KeybindCallbacks;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.interfaces.IValueChangeCallback;

public class InitHandler implements IInitializationHandler {
	@Override
	public void registerModHandlers() {
		ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());

		InputHandler handler = new InputHandler();
		InputEventHandler.getKeybindManager().registerKeybindProvider(handler);

		TickHandler.getInstance().registerClientTickHandler(KeybindCallbacks.getInstance());

		KeybindCallbacks.getInstance().setCallbacks();

		ValueChangeCallback valueChangeCallback = new ValueChangeCallback();
		Configs.Generic.SELL_ITEM.setValueChangeCallback(valueChangeCallback);
		Configs.Generic.BUY_ITEM.setValueChangeCallback(valueChangeCallback);

	}

	private static class ValueChangeCallback implements IValueChangeCallback<ConfigString> {
		@Override
		public void onValueChanged(ConfigString config) {
			if (config == Configs.Generic.SELL_ITEM) {
				if (Configs.Generic.SELL_ITEM.getStringValue().equals("minecraft:emerald")) {
					Configs.Generic.SELL_ITEM.setValueFromString("");
				} ;
			}
			if (config == Configs.Generic.BUY_ITEM) {
				if (Configs.Generic.BUY_ITEM.getStringValue().equals("minecraft:emerald")) {
					Configs.Generic.BUY_ITEM.setValueFromString("");
				} ;
			}
		}
	}
}
