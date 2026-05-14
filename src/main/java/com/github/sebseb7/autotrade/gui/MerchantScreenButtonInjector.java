package com.github.sebseb7.autotrade.gui;

import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.mixin.MerchantMenuAccessor;
import com.github.sebseb7.autotrade.render.VillagerTradeCache;
import com.github.sebseb7.autotrade.util.TradeItemSpec;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import java.util.List;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
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
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
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

		// Position buttons safely to the right of the merchant GUI (276 px wide, centered).
		int x = scaledWidth / 2 + 140;
		int y = scaledHeight / 2 - 83;
		int bw = 160;
		int lw = 110;
		int h = 20;
		int gap = 2;
		int labelX = x + bw + 4;

		Button openSettings = Button
				.builder(Component.literal("Open Settings"), btn -> {
					// vanilla Screen.onClose -> setScreen(null) sends the container-close
					// packet; switching the screen out from under the merchant GUI via
					// GuiBase.openGui skips that path, so the server still thinks we are
					// trading with this villager and rejects the next interact packet.
					if (client.player != null) {
						client.player.closeContainer();
					}
					GuiBase.openGui(new GuiConfigs());
				})
				.bounds(x, y, bw, h).build();

		Button selectSell = Button
				.builder(sellButtonLabel(null), btn -> onSellButton(client, merchantScreen))
				.bounds(x, y + (h + gap), bw, h).build();
		StringWidget sellCurrent = new StringWidget(labelX, y + (h + gap), lw, h,
				currentSellLabel(), client.font);

		Button selectBuy = Button
				.builder(buyButtonLabel(null), btn -> onBuyButton(client, merchantScreen))
				.bounds(x, y + 2 * (h + gap), bw, h).build();
		StringWidget buyCurrent = new StringWidget(labelX, y + 2 * (h + gap), lw, h,
				currentBuyLabel(), client.font);

		Button enableAutotrade = Button
				.builder(autotradeButtonLabel(), btn -> toggleAutotrade(btn))
				.bounds(x, y + 3 * (h + gap), bw, h).build();

		Screen asScreen = merchantScreen;
		//? if mc26 {
		Screens.getWidgets(asScreen).add(openSettings);
		Screens.getWidgets(asScreen).add(selectSell);
		Screens.getWidgets(asScreen).add(sellCurrent);
		Screens.getWidgets(asScreen).add(selectBuy);
		Screens.getWidgets(asScreen).add(buyCurrent);
		Screens.getWidgets(asScreen).add(enableAutotrade);
		//?} else {
		Screens.getButtons(asScreen).add(openSettings);
		Screens.getButtons(asScreen).add(selectSell);
		Screens.getButtons(asScreen).add(sellCurrent);
		Screens.getButtons(asScreen).add(selectBuy);
		Screens.getButtons(asScreen).add(buyCurrent);
		Screens.getButtons(asScreen).add(enableAutotrade);
		//?}

		// Offers arrive via a server packet after the screen opens.
		// Register a per-screen tick handler to wait for offers, cache them,
		// and refresh dynamic button state (active/label).
		final boolean[] cached = {false};
		ScreenEvents.afterTick(merchantScreen).register(s -> {
			MerchantOffers offers = merchantScreen.getMenu().getOffers();
			if (!cached[0] && offers != null && !offers.isEmpty()) {
				cached[0] = true;
				cacheOffersForNearestTrader(client, offers);
			}

			MerchantOffer current = currentSelectedOffer(merchantScreen);
			MerchantOffer sellOffer = (current != null && isSellOffer(current)) ? current : null;
			MerchantOffer buyOffer = (current != null && isBuyOffer(current)) ? current : null;
			boolean sellOn = Configs.Generic.ENABLE_SELL.getBooleanValue();
			boolean buyOn = Configs.Generic.ENABLE_BUY.getBooleanValue();
			// Button doubles as a "Disable" toggle when no applicable trade is selected
			// but the corresponding ENABLE flag is currently on — so the user can turn
			// it off without leaving the merchant screen.
			selectSell.active = sellOffer != null || sellOn;
			selectBuy.active = buyOffer != null || buyOn;
			selectSell.setMessage(sellOffer != null ? sellButtonLabel(sellOffer)
					: (sellOn ? Component.literal("Disable sell") : sellButtonLabel(null)));
			selectBuy.setMessage(buyOffer != null ? buyButtonLabel(buyOffer)
					: (buyOn ? Component.literal("Disable buy") : buyButtonLabel(null)));
			sellCurrent.setMessage(currentSellLabel());
			buyCurrent.setMessage(currentBuyLabel());
			enableAutotrade.setMessage(autotradeButtonLabel());
		});
	}

	private static void onSellButton(Minecraft client, MerchantScreen screen) {
		MerchantOffer offer = currentSelectedOffer(screen);
		if (offer != null && isSellOffer(offer)) {
			applySelectedTradeAsSell(client, screen);
			return;
		}
		if (Configs.Generic.ENABLE_SELL.getBooleanValue()) {
			Configs.Generic.ENABLE_SELL.setBooleanValue(false);
			Configs.saveToFile();
		}
	}

	private static void onBuyButton(Minecraft client, MerchantScreen screen) {
		MerchantOffer offer = currentSelectedOffer(screen);
		if (offer != null && isBuyOffer(offer)) {
			applySelectedTradeAsBuy(client, screen);
			return;
		}
		if (Configs.Generic.ENABLE_BUY.getBooleanValue()) {
			Configs.Generic.ENABLE_BUY.setBooleanValue(false);
			Configs.saveToFile();
		}
	}

	private static Component autotradeButtonLabel() {
		boolean on = Configs.Generic.ENABLED.getBooleanValue();
		return Component.literal(on ? "Disable Autotrade" : "Enable Autotrade");
	}

	/** Button label previews the item that would be written if clicked. */
	private static Component sellButtonLabel(MerchantOffer offer) {
		String item = offer == null ? "-" : describeSpec(TradeItemSpec.encodeFromStack(offer.getCostA()));
		return Component.literal("Set " + item + " as sell item");
	}

	private static Component buyButtonLabel(MerchantOffer offer) {
		String item = offer == null ? "-" : describeSpec(TradeItemSpec.encodeFromStack(offer.getResult()));
		return Component.literal("Set " + item + " as buy item");
	}

	/** Side-label shows the currently-configured sell/buy item from config. */
	private static Component currentSellLabel() {
		String off = Configs.Generic.ENABLE_SELL.getBooleanValue() ? "" : " (off)";
		return Component.literal(describeSpec(Configs.Generic.SELL_ITEM.getStringValue()) + off);
	}

	private static Component currentBuyLabel() {
		String off = Configs.Generic.ENABLE_BUY.getBooleanValue() ? "" : " (off)";
		return Component.literal(describeSpec(Configs.Generic.BUY_ITEM.getStringValue()) + off);
	}

	/**
	 * Compact human-readable form of a {@link com.github.sebseb7.autotrade.util.TradeItemSpec}
	 * string. Strips the {@code minecraft:} namespace and renders enchant
	 * suffixes after a {@code +}, e.g. {@code minecraft:enchanted_book#minecraft:mending=1}
	 * → {@code enchanted_book +mending=1}.
	 */
	private static String describeSpec(String spec) {
		if (spec == null || spec.isEmpty()) {
			return "(none)";
		}
		int sep = spec.indexOf('#');
		String itemPart = sep < 0 ? spec : spec.substring(0, sep);
		String name = stripVanillaNs(itemPart);
		if (sep < 0) {
			return name;
		}
		String enchants = spec.substring(sep + 1).replace("minecraft:", "");
		return name + " +" + enchants;
	}

	private static String stripVanillaNs(String id) {
		return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
	}

	private static void toggleAutotrade(Button btn) {
		Configs.Generic.ENABLED.toggleBooleanValue();
		boolean enabled = Configs.Generic.ENABLED.getBooleanValue();
		btn.setMessage(autotradeButtonLabel());
		String msg = enabled ? "autotrade.message.toggled_mod_on" : "autotrade.message.toggled_mod_off";
		InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, msg);
		Configs.saveToFile();
	}

	private static MerchantOffer currentSelectedOffer(MerchantScreen screen) {
		MerchantMenu menu = screen.getMenu();
		MerchantOffers offers = menu.getOffers();
		if (offers == null || offers.isEmpty()) {
			return null;
		}
		int idx = ((MerchantMenuAccessor) (Object) screen).getShopItem();
		if (idx < 0 || idx >= offers.size()) {
			return null;
		}
		return offers.get(idx);
	}

	/** A "sell" trade pays the player in emeralds for an item. */
	private static boolean isSellOffer(MerchantOffer offer) {
		return offer.getResult().is(Items.EMERALD) && !offer.getCostA().isEmpty()
				&& !offer.getCostA().is(Items.EMERALD);
	}

	/** A "buy" trade gives the player an item in exchange for emeralds. */
	private static boolean isBuyOffer(MerchantOffer offer) {
		if (offer.getResult().isEmpty() || offer.getResult().is(Items.EMERALD)) {
			return false;
		}
		return offer.getCostA().is(Items.EMERALD) || offer.getCostB().is(Items.EMERALD);
	}

	private static void applySelectedTradeAsSell(Minecraft client, MerchantScreen screen) {
		MerchantOffer offer = currentSelectedOffer(screen);
		if (offer == null || !isSellOffer(offer)) {
			return;
		}
		ItemStack item = offer.getCostA();
		String spec = TradeItemSpec.encodeFromStack(item);
		Configs.Generic.SELL_ITEM.setValueFromString(spec);
		Configs.Generic.SELL_LIMIT.setIntegerValue(item.getCount());
		Configs.Generic.ENABLE_SELL.setBooleanValue(true);
		Configs.saveToFile();
		InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.sell_item_set", spec);
	}

	private static void applySelectedTradeAsBuy(Minecraft client, MerchantScreen screen) {
		MerchantOffer offer = currentSelectedOffer(screen);
		if (offer == null || !isBuyOffer(offer)) {
			return;
		}
		ItemStack item = offer.getResult();
		String spec = TradeItemSpec.encodeFromStack(item);
		Configs.Generic.BUY_ITEM.setValueFromString(spec);
		Configs.Generic.BUY_LIMIT.setIntegerValue(item.getCount());
		Configs.Generic.ENABLE_BUY.setBooleanValue(true);
		Configs.saveToFile();
		InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.buy_item_set", spec);
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
