package com.github.sebseb7.autotrade.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Merchant trade overlay strings and cost checks (extracted from the client tick flow).
 */
final class TradeFormatHelper {

	private TradeFormatHelper() {
	}

	static boolean playerHasMerchantCosts(Player player, MerchantOffer offer) {
		return costRequirementMet(player.getInventory(), offer.getCostA())
				&& costRequirementMet(player.getInventory(), offer.getCostB());
	}

	private static boolean costRequirementMet(Inventory inv, ItemStack required) {
		if (required.isEmpty()) {
			return true;
		}
		int need = required.getCount();
		int have = 0;
		for (int s = 0; s < inv.getContainerSize(); s++) {
			ItemStack st = inv.getItem(s);
			if (ItemStack.isSameItemSameComponents(st, required)) {
				have += st.getCount();
				if (have >= need) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Malilib's {@code showGuiOrInGameMessage} routes to multiple HUD targets; trade spam looked like 3× duplication.
	 * Vanilla overlay is a single on-screen line (same idea as vanilla toast-adjacent hints).
	 */
	static void showTradeNotice(Minecraft mc, String translationKey, Component arg1, Component arg2) {
		if (mc.gui == null) {
			return;
		}
		mc.gui.setOverlayMessage(Component.translatable(translationKey, arg1, arg2), false);
	}

	/** e.g. "3× Book" (one villager use). */
	private static String formatItemCountAndName(ItemStack stack) {
		return stack.getCount() + "× " + stack.getHoverName().getString();
	}

	/**
	 * Per-trade count × how many of this offer remain before the trade, e.g. 1
	 * iron/trade × 12 runs → "12× …".
	 */
	static String formatItemCountNameForTrades(ItemStack perTrade, int remainingOfferUses) {
		if (remainingOfferUses <= 0) {
			return formatItemCountAndName(perTrade);
		}
		return (perTrade.getCount() * remainingOfferUses) + "× " + perTrade.getHoverName().getString();
	}

	/** For buying: the stacks you pay, scaled to how many of this offer remain. */
	static String formatOfferPriceForTrades(MerchantOffer offer, int t) {
		if (t <= 0) {
			String a = offer.getCostA().isEmpty() ? null : formatItemCountAndName(offer.getCostA());
			if (offer.getCostB().isEmpty()) {
				return a != null ? a : "—";
			}
			String b = formatItemCountAndName(offer.getCostB());
			return a == null ? b : a + " + " + b;
		}
		String a = offer.getCostA().isEmpty()
				? null
				: (offer.getCostA().getCount() * t) + "× " + offer.getCostA().getHoverName().getString();
		if (offer.getCostB().isEmpty()) {
			return a != null ? a : "—";
		}
		String b = (offer.getCostB().getCount() * t) + "× " + offer.getCostB().getHoverName().getString();
		return a == null ? b : a + " + " + b;
	}

	/**
	 * If the trade has a second cost item, " + 2× …" scaled to remaining offer
	 * uses.
	 */
	static String formatOptionalSecondCostForTrades(MerchantOffer offer, int t) {
		if (offer.getCostB().isEmpty()) {
			return "";
		}
		if (t <= 0) {
			return " + " + formatItemCountAndName(offer.getCostB());
		}
		return " + " + (offer.getCostB().getCount() * t) + "× " + offer.getCostB().getHoverName().getString();
	}
}
