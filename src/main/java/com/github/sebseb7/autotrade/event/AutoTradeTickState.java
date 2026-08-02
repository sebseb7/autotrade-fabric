package com.github.sebseb7.autotrade.event;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * Mutable per-session tick state for {@link AutoTradeClientTick}.
 */
final class AutoTradeTickState {
	static final int TRADER_HIGHLIGHT_TICKS = 20;

	int voidDelay;
	int containerDelay;
	int postMerchantInventorySyncTicks;

	/** Last villager/wandering trader we opened trades with (entity id). */
	int villagerActive;

	boolean inputInRange;
	boolean inputOpened;
	boolean outputInRange;
	boolean outputOpened;

	int traderGlowTicksRemaining;
	int traderGlowEntityId = -1;

	int inputContainerHighlightTicks;
	int outputContainerHighlightTicks;

	/** "Moved N items" reports waiting for server inventory sync before being shown. */
	final List<ContainerIoHelper.PendingMoveReport> pendingMoveReports = new ArrayList<>();

	/**
	 * After selecting a trade on the server, wait this many client ticks before
	 * shift-moving the result so slot contents match the server.
	 */
	int merchantResultQuickMoveDelayTicks;
	int merchantResultQuickMoveOfferIndex = -1;
	boolean merchantResultQuickMoveIsBuy;
	/** Waits for the server to put the trade result in slot 2 before quick-moving. */
	int merchantResultEmptyWaits;

	void clearMerchantQuickMoveDefer() {
		merchantResultQuickMoveDelayTicks = 0;
		merchantResultQuickMoveOfferIndex = -1;
		merchantResultEmptyWaits = 0;
	}

	int getVillagerActive() {
		return villagerActive;
	}

	Entity getTraderGlowEntityForRender(Minecraft mc) {
		if (traderGlowTicksRemaining <= 0 || traderGlowEntityId < 0 || mc.level == null) return null;
		return TraderInteractor.findEntityById(mc, traderGlowEntityId);
	}

	int getInputContainerHighlightTicks() {
		return inputContainerHighlightTicks;
	}

	int getOutputContainerHighlightTicks() {
		return outputContainerHighlightTicks;
	}

	void tickTraderGlow(Minecraft mc) {
		if (mc.level == null || traderGlowTicksRemaining <= 0) return;
		traderGlowTicksRemaining--;
		if (traderGlowTicksRemaining == 0) traderGlowEntityId = -1;
	}

	void startTraderGlow(Minecraft mc, int entityId) {
		if (mc.level == null) {
			traderGlowTicksRemaining = 0;
			traderGlowEntityId = -1;
			return;
		}
		Entity active = TraderInteractor.findEntityById(mc, entityId);
		if (active == null) {
			traderGlowTicksRemaining = 0;
			traderGlowEntityId = -1;
			return;
		}
		traderGlowEntityId = entityId;
		traderGlowTicksRemaining = TRADER_HIGHLIGHT_TICKS;
	}

	void tickContainerHighlights(Minecraft mc) {
		if (mc.level == null) return;
		if (inputContainerHighlightTicks > 0) inputContainerHighlightTicks--;
		if (outputContainerHighlightTicks > 0) outputContainerHighlightTicks--;
	}
}
