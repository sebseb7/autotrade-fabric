package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.config.Configs;
import fi.dy.masa.malilib.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.entity.Entity;

/**
 * Main tick handler for AutoTrade.
 * Orchestrates trader location, merchant trading, and container management.
 */
final class AutoTradeClientTick {
	private final AutoTradeTickState state = new AutoTradeTickState();
	private final TraderInteractor traderInteractor = new TraderInteractor(state);
	private final AutoTradeContainerFlow containerFlow = new AutoTradeContainerFlow(state);
	private final AutoTradeMerchantScreenTick merchantScreenTick = new AutoTradeMerchantScreenTick(state);

	Entity getTraderGlowEntityForRender(Minecraft mc) {
		return state.getTraderGlowEntityForRender(mc);
	}

	int getInputContainerHighlightTicks() {
		return state.getInputContainerHighlightTicks();
	}

	int getOutputContainerHighlightTicks() {
		return state.getOutputContainerHighlightTicks();
	}

	void tick(Minecraft mc) {
		state.tickTraderGlow(mc);
		state.tickContainerHighlights(mc);

		if (AutoTradeVoidDelay.handle(mc, state)) return;

		if (state.containerDelay > 0) state.containerDelay--;
		if (mc.player == null) return;

		tickPostMerchantSync(mc);
		AutoTradeConfigSelectors.tickItemFrameSelection(mc);

		if (!Configs.Generic.ENABLED.getBooleanValue()) {
			state.clearMerchantQuickMoveDefer();
			return;
		}

		merchantScreenTick.tickDeferredResultQuickMove(mc);

		if (GuiUtils.getCurrentScreen() instanceof MerchantScreen screen) {
			merchantScreenTick.tick(mc, screen);
			containerFlow.resetContainerFlags();
			return;
		}

		containerFlow.processOpenContainers(mc, mc.player.getInventory());

		if (traderInteractor.findAndInteract(mc)) return;

		containerFlow.handleContainerProximity(mc);
	}

	private void tickPostMerchantSync(Minecraft mc) {
		if (state.postMerchantInventorySyncTicks > 0) {
			state.postMerchantInventorySyncTicks--;
			ContainerIoHelper.syncPlayerInventoryAfterMerchant(mc);
		}
	}
}
