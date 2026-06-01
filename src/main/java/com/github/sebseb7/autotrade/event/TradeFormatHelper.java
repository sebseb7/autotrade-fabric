package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.util.AutotradeInfoUtils;
import com.github.sebseb7.autotrade.util.TradeItemSpec;
import fi.dy.masa.malilib.gui.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
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

	static int countInInventory(Player player, String itemSpec) {
		if (player == null || itemSpec == null || itemSpec.isBlank()) {
			return 0;
		}
		int total = 0;
		Inventory inv = player.getInventory();
		for (int s = 0; s < inv.getContainerSize(); s++) {
			ItemStack stack = inv.getItem(s);
			if (TradeItemSpec.matches(stack, itemSpec)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	static void showTradeNotice(Minecraft mc, String translationKey, Component arg1, Component arg2) {
		Component notice = Component.translatable(translationKey, arg1, arg2);
		if (mc.gui != null) {
			mc.gui.setOverlayMessage(notice, false);
		}
		AutotradeInfoUtils.postToChat(Message.MessageType.INFO, notice);
	}

	static void showTradeNotice(Minecraft mc, String translationKey, Component arg1, Component arg2,
			Component arg3) {
		Component notice = Component.translatable(translationKey, arg1, arg2, arg3);
		if (mc.gui != null) {
			mc.gui.setOverlayMessage(notice, false);
		}
		AutotradeInfoUtils.postToChat(Message.MessageType.INFO, notice);
	}

	private static Component formatItemStack(ItemStack stack, int count) {
		return Component.translatable("autotrade.format.item_stack", count, stack.getHoverName());
	}

	private static Component formatStack(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		return formatItemStack(stack, stack.getCount());
	}

	private static Component formatMerchantPayment(ItemStack costA, ItemStack costB) {
		Component a = formatStack(costA);
		if (costB.isEmpty()) {
			return a != null ? a : Component.translatable("autotrade.format.cost_empty");
		}
		Component b = formatStack(costB);
		if (a == null) {
			return b;
		}
		return Component.translatable("autotrade.format.cost_pair", a, b);
	}

	private static ItemStack withCount(ItemStack template, int count) {
		if (template.isEmpty() || count <= 0) {
			return ItemStack.EMPTY;
		}
		ItemStack copy = template.copy();
		copy.setCount(count);
		return copy;
	}

	/** Same formula as {@link MerchantOffer#getCostA()} assembly (demand + reputation). */
	static int modifiedCostCount(MerchantOffer offer, ItemCost itemCost) {
		int base = itemCost.count();
		int demandAdd = Math.max(0, Mth.floor(base * offer.getDemand() * offer.getPriceMultiplier()));
		return Mth.clamp(base + demandAdd + offer.getSpecialPriceDiff(), 1, itemCost.itemStack().getMaxStackSize());
	}

	private static ItemStack labelStack(ItemStack preferred, ItemStack fallback) {
		return preferred.isEmpty() ? fallback.copy() : preferred.copy();
	}

	/**
	 * @param totalSellItemPaid total items paid (e.g. all iron spent this trade)
	 * @param emeraldReceived total emeralds received
	 * @param sellItemPerEmerald offer rate (input items per emerald)
	 */
	static void showSellTradeNotice(Minecraft mc, MerchantOffer offer, int totalSellItemPaid, int emeraldReceived,
			int sellItemPerEmerald) {
		ItemStack paymentTemplate = labelStack(offer.getCostA(), offer.getItemCostA().itemStack());
		Component totalPaidLine = formatStack(withCount(paymentTemplate, totalSellItemPaid));
		Component emeraldsLine = formatStack(withCount(offer.getResult(), emeraldReceived));
		Component rateLine = formatStack(withCount(paymentTemplate, sellItemPerEmerald));
		if (totalPaidLine == null) {
			totalPaidLine = Component.translatable("autotrade.format.cost_empty");
		}
		if (emeraldsLine == null) {
			emeraldsLine = Component.translatable("autotrade.format.cost_empty");
		}
		if (rateLine == null) {
			rateLine = Component.translatable("autotrade.format.cost_empty");
		}
		showTradeNotice(mc, "autotrade.message.trade_sold", totalPaidLine, emeraldsLine, rateLine);
	}

	static void showBuyTradeNotice(Minecraft mc, MerchantOffer offer, int acquiredCount, int emeraldCount) {
		ItemStack acquiredItem = withCount(offer.getResult(), acquiredCount);
		ItemStack priceItem = withCount(labelStack(offer.getCostA(), offer.getItemCostA().itemStack()), emeraldCount);
		Component acquiredLine = formatStack(acquiredItem);
		if (acquiredLine == null) {
			acquiredLine = Component.translatable("autotrade.format.cost_empty");
		}
		Component priceLine = formatStack(priceItem);
		if (priceLine == null) {
			priceLine = Component.translatable("autotrade.format.cost_empty");
		}
		showTradeNotice(mc, "autotrade.message.trade_bought", acquiredLine, priceLine);
	}

	static int inventoryDelta(int before, int after, int offerFallback) {
		int delta = Math.abs(before - after);
		return delta > 0 ? delta : Math.max(offerFallback, 1);
	}

	static int inventoryDecrease(int before, int after, int fallback) {
		int delta = before - after;
		return delta > 0 ? delta : Math.max(fallback, 1);
	}

	static int inventoryIncrease(int before, int after, int fallback) {
		int delta = after - before;
		return delta > 0 ? delta : Math.max(fallback, 1);
	}

	static Component formatItemCountNameForTrades(ItemStack perTrade, int remainingOfferUses) {
		int count = remainingOfferUses <= 0 ? perTrade.getCount() : perTrade.getCount() * remainingOfferUses;
		return formatItemStack(perTrade, count);
	}

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

	static Component formatSellCostForTrades(MerchantOffer offer, int tradesRemaining) {
		Component cost = formatItemCountNameForTrades(offer.getCostA(), tradesRemaining);
		if (offer.getCostB().isEmpty()) {
			return cost;
		}
		return Component.empty().append(cost).append(formatOptionalSecondCostForTrades(offer, tradesRemaining));
	}

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
