package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.AutoTrade;
import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.render.VillagerTradeCache;
import com.github.sebseb7.autotrade.util.TradeItemSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Villager GUI: one trade attempt per {@link #tick}, matching {@link MerchantOffers}
 * recipe stacks, then deferring a normal pickup (not shift-click) from the result slot when
 * it is an enchanted book (shift-click would chain other book trades); other results still use shift-click.
 */
final class AutoTradeMerchantScreenTick {
	/** Ticks after {@code SelectTrade} before moving the result (server must fill slot 2). */
	private static final int RESULT_QUICK_MOVE_DELAY_TICKS = 5;
	private static final int RESULT_EMPTY_RETRY_TICKS = 2;
	private static final int RESULT_EMPTY_MAX_WAITS = 15;

	private final AutoTradeTickState state;

	AutoTradeMerchantScreenTick(AutoTradeTickState state) {
		this.state = state;
	}

	void tickDeferredResultQuickMove(Minecraft mc) {
		if (state.merchantResultQuickMoveDelayTicks <= 0) {
			return;
		}
		state.merchantResultQuickMoveDelayTicks--;
		if (state.merchantResultQuickMoveDelayTicks > 0) {
			return;
		}
		//? if mc262 {
		if (!(mc.gui.screen() instanceof MerchantScreen screen)) {
		//?} else {
		/*if (!(mc.screen instanceof MerchantScreen screen)) {
		*///?}
			AutoTrade.logger.warn("[AutoTrade merchant] defer execute aborted: current screen is not MerchantScreen");
			state.clearMerchantQuickMoveDefer();
			return;
		}
		MerchantMenu menu = screen.getMenu();
		MerchantOffers offers = menu.getOffers();
		int idx = state.merchantResultQuickMoveOfferIndex;
		if (offers == null || idx < 0 || idx >= offers.size()) {
			AutoTrade.logger.warn(
					"[AutoTrade merchant] defer execute aborted: bad offer index idx={} offersSize={}",
					idx,
					offers == null ? -1 : offers.size());
			state.clearMerchantQuickMoveDefer();
			return;
		}
		menu.setSelectionHint(idx);
		var offer = offers.get(idx);
		var offerForNotice = offer.copy();
		var player = mc.player;
		String sellSpec = Configs.Generic.SELL_ITEM.getStringValue();
		String buySpec = Configs.Generic.BUY_ITEM.getStringValue();
		int emeraldBefore = TradeFormatHelper.countInInventory(player, "minecraft:emerald");
		int buyItemBefore = 0;
		if (state.merchantResultQuickMoveIsBuy) {
			buyItemBefore = TradeFormatHelper.countInInventory(player, buySpec);
		}
		menu.tryMoveItems(idx);
		var slot = menu.getSlot(2);
		ItemStack slot2 = slot.getItem();
		if (slot2.isEmpty()) {
			if (state.merchantResultEmptyWaits < RESULT_EMPTY_MAX_WAITS) {
				state.merchantResultEmptyWaits++;
				state.merchantResultQuickMoveDelayTicks = RESULT_EMPTY_RETRY_TICKS;
				return;
			}
			AutoTrade.logger.warn("[AutoTrade merchant] result slot still empty after {} waits", RESULT_EMPTY_MAX_WAITS);
			state.clearMerchantQuickMoveDefer();
			return;
		}
		state.merchantResultEmptyWaits = 0;
		int resultSlotCount = slot2.getCount();
		try {
			if (state.merchantResultQuickMoveIsBuy) {
				if (TradeItemSpec.matches(slot2, buySpec)) {
					ContainerIoHelper.quickMoveResultSlot(mc, menu, slot.index);
				} else {
					AutoTrade.logger.warn("[AutoTrade merchant] defer quickMove skipped: slot2 did not match buy spec");
				}
			} else if (!slot2.isEmpty()) {
				ContainerIoHelper.quickMoveResultSlot(mc, menu, slot.index);
			}
		} catch (Exception e) {
			AutoTrade.logger.warn("[AutoTrade merchant] defer quickMove exception", e);
		}
		state.clearMerchantQuickMoveDefer();
		ContainerIoHelper.syncPlayerInventoryAfterMerchant(mc);

		int emeraldAfter = TradeFormatHelper.countInInventory(player, "minecraft:emerald");
		int emeraldFallback = TradeFormatHelper.modifiedCostCount(offerForNotice, offerForNotice.getItemCostA());
		if (state.merchantResultQuickMoveIsBuy) {
			int buyItemAfter = TradeFormatHelper.countInInventory(player, buySpec);
			int emeraldPaid = TradeFormatHelper.inventoryDelta(emeraldBefore, emeraldAfter, emeraldFallback);
			int buyItemCount = TradeFormatHelper.inventoryDelta(buyItemBefore, buyItemAfter, resultSlotCount);
			TradeFormatHelper.showBuyTradeNotice(mc, offerForNotice, buyItemCount, emeraldPaid);
		} else {
			int sellCostFallback = offerForNotice.getCostA().getCount();
			if (sellCostFallback <= 0) {
				sellCostFallback = TradeFormatHelper.modifiedCostCount(offerForNotice,
						offerForNotice.getItemCostA());
			}
			int emeraldReceivedFallback = Math.max(resultSlotCount, offerForNotice.getResult().getCount());
			int ironPerEmerald = sellCostFallback;
			int emeraldReceived = TradeFormatHelper.inventoryIncrease(emeraldBefore, emeraldAfter,
					emeraldReceivedFallback);
			int totalIronPaid = ironPerEmerald * emeraldReceived;
			TradeFormatHelper.showSellTradeNotice(mc, offerForNotice, totalIronPaid, emeraldReceived, ironPerEmerald);
		}

		// Trade completed successfully — close the session so tick() won't
		// re-enter tryExecuteOneMerchantTrade and log "trades exhausted" for
		// the now-depleted offer.
		screen.onClose();
		state.postMerchantInventorySyncTicks = 15;
		state.startTraderGlow(mc, state.getVillagerActive());
		BaritoneHelper.resumeMovementGoal();
	}

	void tick(Minecraft mc, MerchantScreen screen) {
		MerchantMenu menu = screen.getMenu();
		MerchantOffers offers = menu.getOffers();
		int villagerActive = state.getVillagerActive();

		cacheTraderOffers(mc, villagerActive, offers);

		if (state.merchantResultQuickMoveDelayTicks > 0) {
			return;
		}

		if (!merchantResultSlotEmpty(menu)) {
			moveMerchantResultToInventory(mc, menu);
			return;
		}

		if (tryExecuteOneMerchantTrade(mc, menu, offers, screen)) {
			ContainerIoHelper.syncPlayerInventoryAfterMerchant(mc);
			return;
		}

		// tryExecuteOneMerchantTrade already handled cleanup when no matching
		// trade was found. When a matching trade was found but couldn't be
		// executed, the specific reason was already shown.
	}
	private static boolean merchantResultSlotEmpty(MerchantMenu menu) {
		return menu.getSlot(2).getItem().isEmpty();
	}

	private static void moveMerchantResultToInventory(Minecraft mc, MerchantMenu menu) {
		var slot = menu.getSlot(2);
		if (slot.getItem().isEmpty()) {
			return;
		}
		try {
			ContainerIoHelper.quickMoveResultSlot(mc, menu, slot.index);
		} catch (Exception e) {
			AutoTrade.logger.warn("[AutoTrade merchant] move result exception", e);
		}
		ContainerIoHelper.syncPlayerInventoryAfterMerchant(mc);
	}

	private static void cacheTraderOffers(Minecraft mc, int villagerActive, MerchantOffers offers) {
		Entity activeEntity = TraderInteractor.findEntityById(mc, villagerActive);
		if (activeEntity != null && offers != null && !offers.isEmpty()) {
			VillagerTradeCache.put(activeEntity.getUUID(), offers);
		}
	}

	private boolean tryExecuteOneMerchantTrade(Minecraft mc, MerchantMenu menu, MerchantOffers offers, MerchantScreen screen) {
		if (offers == null || offers.isEmpty() || !merchantResultSlotEmpty(menu)) {
			return false;
		}
		String sellItemStr = Configs.Generic.SELL_ITEM.getStringValue();
		String buyItemStr = Configs.Generic.BUY_ITEM.getStringValue();
		boolean buyOn = Configs.Generic.ENABLE_BUY.getBooleanValue();
		boolean sellOn = Configs.Generic.ENABLE_SELL.getBooleanValue();
		int buyLimit = Configs.Generic.BUY_LIMIT.getIntegerValue();
		int sellLimit = Configs.Generic.SELL_LIMIT.getIntegerValue();

		for (int i = 0; i < offers.size(); i++) {
			var offer = offers.get(i);
			int tradesLeft = offer.getMaxUses() - offer.getUses();
			boolean buyRecipe = buyOn && TradeItemSpec.matches(offer.getResult(), buyItemStr);
			boolean buyCountOk = offer.getResult().getCount() <= buyLimit;
			boolean costOk = TradeFormatHelper.playerHasMerchantCosts(mc.player, offer);

			boolean sellRecipe = sellOn && TradeItemSpec.matches(offer.getCostA(), sellItemStr);
			boolean sellCountOk = offer.getCostA().getCount() <= sellLimit;

			if (buyOn && buyRecipe) {
				if (buyCountOk && tradesLeft > 0 && costOk) {
					menu.setSelectionHint(i);
					mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSelectTradePacket(i));
					AutoTrade.bought += offer.getResult().getCount();
					state.merchantResultQuickMoveDelayTicks = RESULT_QUICK_MOVE_DELAY_TICKS;
					state.merchantResultQuickMoveOfferIndex = i;
					state.merchantResultQuickMoveIsBuy = true;
					state.merchantResultEmptyWaits = 0;
					return true;
				}
				// Buy recipe matched but can't execute — show why and close session
				String reason = !buyCountOk ? "autotrade.message.reason_price_too_high"
						: tradesLeft <= 0 ? "autotrade.message.reason_trades_exhausted"
						: "autotrade.message.reason_no_resources";
				finishMerchantSessionWithReason(mc, screen, reason);
				return false;
			}

			if (sellOn && sellRecipe) {
				if (sellCountOk && tradesLeft > 0 && costOk) {
					menu.setSelectionHint(i);
					mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSelectTradePacket(i));
					AutoTrade.sold += offer.getCostA().getCount();
					state.merchantResultQuickMoveDelayTicks = RESULT_QUICK_MOVE_DELAY_TICKS;
					state.merchantResultQuickMoveOfferIndex = i;
					state.merchantResultQuickMoveIsBuy = false;
					state.merchantResultEmptyWaits = 0;
					return true;
				}
				// Sell recipe matched but can't execute — show why and close session
				String reason = !sellCountOk ? "autotrade.message.reason_price_too_high"
						: tradesLeft <= 0 ? "autotrade.message.reason_trades_exhausted"
						: "autotrade.message.reason_no_resources";
				finishMerchantSessionWithReason(mc, screen, reason);
				return false;
			}
		}

		// No matching trade found at all
		finishMerchantSessionWithReason(mc, screen, "autotrade.message.reason_other");
		return false;
	}

	private void finishMerchantSessionWithReason(Minecraft mc, MerchantScreen screen, String reasonKey) {
		Entity villager = TraderInteractor.findEntityById(mc, state.getVillagerActive());
		TradeFormatHelper.showNoTradeNotice(mc, villager, reasonKey);
		state.clearMerchantQuickMoveDefer();
		ContainerIoHelper.syncPlayerInventoryAfterMerchant(mc);
		screen.onClose();
		ContainerIoHelper.syncPlayerInventoryAfterMerchant(mc);
		state.postMerchantInventorySyncTicks = 15;
		state.startTraderGlow(mc, state.getVillagerActive());
		BaritoneHelper.resumeMovementGoal();
	}
}
