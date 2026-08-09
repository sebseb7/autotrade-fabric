package com.github.sebseb7.autotrade;

import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.event.InputHandler;
import com.github.sebseb7.autotrade.event.KeybindCallbacks;
import com.github.sebseb7.autotrade.gui.MerchantScreenButtonInjector;
import com.github.sebseb7.autotrade.render.TraderHighlightRenderer;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.interfaces.IValueChangeCallback;
import com.github.sebseb7.autotrade.util.AutotradeInfoUtils;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
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
					&& (entity instanceof Villager || entity instanceof WanderingTrader)) {
				// Remember which villager the player is opening a trade screen with.
				AutoTrade.lastInteractedVillagerId = entity.getId();
				if (Configs.Generic.ENABLED.getBooleanValue()) {
					Configs.Generic.ENABLED.setBooleanValue(false);
					AutotradeInfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.toggled_mod_off");
					Configs.saveToFile();
				}
			}
			return InteractionResult.PASS;
		});

		// Left or right click stops the villager roller cycle.
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (world.isClientSide()) {
				com.github.sebseb7.autotrade.event.VillagerRoller.getInstance().stop(net.minecraft.client.Minecraft.getInstance());
			}
			return net.minecraft.world.InteractionResult.PASS;
		});
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClientSide()) {
				com.github.sebseb7.autotrade.event.VillagerRoller.getInstance().stop(net.minecraft.client.Minecraft.getInstance());
			}
			return net.minecraft.world.InteractionResult.PASS;
		});
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (world.isClientSide()) {
				com.github.sebseb7.autotrade.event.VillagerRoller.getInstance().stop(net.minecraft.client.Minecraft.getInstance());
			}
			return net.minecraft.world.InteractionResult.PASS;
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
