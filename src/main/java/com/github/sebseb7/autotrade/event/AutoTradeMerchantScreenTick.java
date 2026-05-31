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
	/** One trade per packet; chat/counters use this, not remaining offer uses. */
	private static final int TRADES_PER_EXECUTION = 1;

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
		if (!(mc.screen instanceof MerchantScreen screen)) {
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
		String buySpec = Configs.Generic.BUY_ITEM.getStringValue();
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

		if (tryExecuteOneMerchantTrade(mc, menu, offers)) {
			ContainerIoHelper.syncPlayerInventoryAfterMerchant(mc);
			return;
		}

		finishMerchantSession(mc, screen);
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

	private boolean tryExecuteOneMerchantTrade(Minecraft mc, MerchantMenu menu, MerchantOffers offers) {
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

			if (buyOn && buyRecipe && buyCountOk) {
				if (tradesLeft > 0 && costOk) {
					menu.setSelectionHint(i);
					mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSelectTradePacket(i));
					AutoTrade.bought += offer.getResult().getCount();
					TradeFormatHelper.showTradeNotice(mc, "autotrade.message.trade_bought",
							TradeFormatHelper.formatItemCountNameForTrades(offer.getResult(), TRADES_PER_EXECUTION),
							TradeFormatHelper.formatOfferPriceForTrades(offer, TRADES_PER_EXECUTION));
					state.merchantResultQuickMoveDelayTicks = RESULT_QUICK_MOVE_DELAY_TICKS;
					state.merchantResultQuickMoveOfferIndex = i;
					state.merchantResultQuickMoveIsBuy = true;
					state.merchantResultEmptyWaits = 0;
					return true;
				}
			}

			if (sellOn && sellRecipe && sellCountOk) {
				if (tradesLeft > 0 && costOk) {
					menu.setSelectionHint(i);
					mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSelectTradePacket(i));
					AutoTrade.sold += offer.getCostA().getCount();
					TradeFormatHelper.showTradeNotice(mc, "autotrade.message.trade_sold",
							TradeFormatHelper.formatSellCostForTrades(offer, TRADES_PER_EXECUTION),
							TradeFormatHelper.formatItemCountNameForTrades(offer.getResult(), TRADES_PER_EXECUTION));
					state.merchantResultQuickMoveDelayTicks = RESULT_QUICK_MOVE_DELAY_TICKS;
					state.merchantResultQuickMoveOfferIndex = i;
					state.merchantResultQuickMoveIsBuy = false;
					state.merchantResultEmptyWaits = 0;
					return true;
				}
			}
		}
		return false;
	}

	private void finishMerchantSession(Minecraft mc, MerchantScreen screen) {
		state.clearMerchantQuickMoveDefer();
		ContainerIoHelper.syncPlayerInventoryAfterMerchant(mc);
		screen.onClose();
		ContainerIoHelper.syncPlayerInventoryAfterMerchant(mc);
		state.postMerchantInventorySyncTicks = 15;
		state.startTraderGlow(mc, state.getVillagerActive());
	}
}
