package com.github.sebseb7.autotrade;

import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.event.InputHandler;
import com.github.sebseb7.autotrade.event.KeybindCallbacks;
import com.github.sebseb7.autotrade.gui.MerchantScreenButtonInjector;
import com.github.sebseb7.autotrade.render.TraderHighlightRenderer;
import com.github.sebseb7.autotrade.render.VillagerTradeOverlayRenderer;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.interfaces.IValueChangeCallback;
import fi.dy.masa.malilib.util.InfoUtils;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
//? if npcSplit {
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
//?}
//? if npcFlat {
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
//?}
import net.minecraft.world.InteractionResult;

public class InitHandler implements IInitializationHandler {
	@Override
	public void registerModHandlers() {
		ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());

		TraderHighlightRenderer.register();
		VillagerTradeOverlayRenderer.register();
		MerchantScreenButtonInjector.register();

		InputHandler handler = new InputHandler();
		InputEventHandler.getKeybindManager().registerKeybindProvider(handler);

		TickHandler.getInstance().registerClientTickHandler(KeybindCallbacks.getInstance());

		KeybindCallbacks.getInstance().setCallbacks();

		// A real right-click on a villager/wandering trader cancels the global
		// auto-trade switch — but skip the synthetic interact packets the mod
		// itself emits in AutoTradeClientTick (guarded by AutoTrade.autoInteracting).
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!AutoTrade.autoInteracting && player.level().isClientSide()
					&& (entity instanceof Villager || entity instanceof WanderingTrader)
					&& Configs.Generic.ENABLED.getBooleanValue()) {
				Configs.Generic.ENABLED.setBooleanValue(false);
				InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.toggled_mod_off");
				Configs.saveToFile();
			}
			return InteractionResult.PASS;
		});

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
				}
			}
			if (config == Configs.Generic.BUY_ITEM) {
				if (Configs.Generic.BUY_ITEM.getStringValue().equals("minecraft:emerald")) {
					Configs.Generic.BUY_ITEM.setValueFromString("");
				}
			}
		}
	}
}
