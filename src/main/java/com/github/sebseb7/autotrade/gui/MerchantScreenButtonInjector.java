package com.github.sebseb7.autotrade.gui;

import com.github.sebseb7.autotrade.render.VillagerTradeCache;
import java.util.List;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
//? if npcSplit {
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
//?}
//? if npcFlat {
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
//?}
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;

public final class MerchantScreenButtonInjector {

	private MerchantScreenButtonInjector() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register(MerchantScreenButtonInjector::onScreenInit);
	}

	private static void onScreenInit(Minecraft client, Screen screen, int scaledWidth, int scaledHeight) {
		if (!(screen instanceof MerchantScreen merchantScreen)) {
			return;
		}

		// Add button during AFTER_INIT so it is properly registered as renderable.
		// We position it safely to the right of the merchant GUI.
		// The merchant GUI is 276 pixels wide and centered.
		Button button = Button
				.builder(Component.literal("Select Enchantments"),
						btn -> client.setScreen(new EnchantmentSelectionScreen(merchantScreen)))
				.bounds(scaledWidth / 2 + 140, scaledHeight / 2 - 83, 120, 20).build();
		Screen asScreen = merchantScreen;
		//? if mc26 {
		Screens.getWidgets(asScreen).add(button);
		//?} else {
		Screens.getButtons(asScreen).add(button);
		//?}

		// Offers arrive via a server packet after the screen opens.
		// Register a per-screen tick handler to wait for offers and cache them.
		final boolean[] handled = {false};
		ScreenEvents.afterTick(merchantScreen).register(s -> {
			if (handled[0]) {
				return;
			}
			MerchantOffers offers = merchantScreen.getMenu().getOffers();
			if (offers == null || offers.isEmpty()) {
				return; // not yet synced
			}
			handled[0] = true;

			cacheOffersForNearestTrader(client, offers);
		});
	}

	private static void cacheOffersForNearestTrader(Minecraft mc, MerchantOffers offers) {
		if (mc.player == null || mc.level == null) {
			return;
		}

		AABB searchBox = mc.player.getBoundingBox().inflate(10.0);
		List<Entity> nearby = mc.level.getEntitiesOfClass(Entity.class, searchBox);

		Entity closest = null;
		double closestDist = Double.MAX_VALUE;

		for (Entity entity : nearby) {
			if (entity instanceof Villager || entity instanceof WanderingTrader) {
				double dist = entity.distanceToSqr(mc.player);
				if (dist < closestDist) {
					closestDist = dist;
					closest = entity;
				}
			}
		}
		if (closest != null) {
			VillagerTradeCache.put(closest.getUUID(), offers);
		}
	}
}
