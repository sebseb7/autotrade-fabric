package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.util.AutotradeInfoUtils;
import fi.dy.masa.malilib.gui.Message;
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
	 * Action-bar overlay plus the same line in chat (see {@link AutotradeInfoUtils}).
	 */
	static void showTradeNotice(Minecraft mc, String translationKey, Component arg1, Component arg2) {
		Component notice = Component.translatable(translationKey, arg1, arg2);
		if (mc.gui != null) {
			mc.gui.setOverlayMessage(notice, false);
		}
		AutotradeInfoUtils.postToChat(Message.MessageType.INFO, notice);
	}

	private static Component formatItemStack(ItemStack stack, int count) {
		return Component.translatable("autotrade.format.item_stack", count, stack.getHoverName());
	}

	/**
	 * Per-trade count × how many of this offer remain before the trade, e.g. 1
	 * iron/trade × 12 runs → "12× …".
	 */
	static Component formatItemCountNameForTrades(ItemStack perTrade, int remainingOfferUses) {
		int count = remainingOfferUses <= 0 ? perTrade.getCount() : perTrade.getCount() * remainingOfferUses;
		return formatItemStack(perTrade, count);
	}

	/** For buying: the stacks you pay, scaled to how many of this offer remain. */
	static Component formatOfferPriceForTrades(MerchantOffer offer, int tradesRemaining) {
		Component costA = formatScaledCost(offer.getCostA(), tradesRemaining);
		if (offer.getCostB().isEmpty()) {
			return costA != null ? costA : Component.translatable("autotrade.format.cost_empty");
		}
		Component costB = formatScaledCost(offer.getCostB(), tradesRemaining);
		if (costA == null) {
			return costB;
		}
		return Component.translatable("autotrade.format.cost_pair", costA, costB);
	}

	/**
	 * Sell-side cost: primary cost plus optional second cost, scaled to remaining offer uses.
	 */
	static Component formatSellCostForTrades(MerchantOffer offer, int tradesRemaining) {
		Component cost = formatItemCountNameForTrades(offer.getCostA(), tradesRemaining);
		if (offer.getCostB().isEmpty()) {
			return cost;
		}
		return Component.empty().append(cost).append(formatOptionalSecondCostForTrades(offer, tradesRemaining));
	}

	/**
	 * If the trade has a second cost item, " + 2× …" scaled to remaining offer uses.
	 */
	static Component formatOptionalSecondCostForTrades(MerchantOffer offer, int tradesRemaining) {
		if (offer.getCostB().isEmpty()) {
			return Component.empty();
		}
		Component costB = formatScaledCost(offer.getCostB(), tradesRemaining);
		return Component.translatable("autotrade.format.cost_second", costB);
	}

	private static Component formatScaledCost(ItemStack stack, int tradesRemaining) {
		if (stack.isEmpty()) {
			return null;
		}
		if (tradesRemaining <= 0) {
			return formatItemStack(stack, stack.getCount());
		}
		return formatItemStack(stack, stack.getCount() * tradesRemaining);
	}
}
