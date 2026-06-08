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
	private long lastTimeOfDay = -1;
	private int pendingAfkOnDelayTicks = -1;
	private String pendingAfkOnCommand = "";

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
		if (mc.level == null) {
			this.lastTimeOfDay = -1;
			this.pendingAfkOnDelayTicks = -1;
			this.pendingAfkOnCommand = "";
		} else {
			boolean enabled = Configs.Generic.ENABLED.getBooleanValue();
			long timeOfDay = getTimeOfDayReflect(mc.level) % 24000L;
			if (enabled && this.lastTimeOfDay != -1 && this.lastTimeOfDay != timeOfDay) {
				this.checkAndTimeTrigger(mc, this.lastTimeOfDay, timeOfDay);
			}
			this.lastTimeOfDay = timeOfDay;

			if (this.pendingAfkOnDelayTicks > 0) {
				this.pendingAfkOnDelayTicks--;
				if (this.pendingAfkOnDelayTicks == 0) {
					if (enabled) {
						this.executeCommand(mc, this.pendingAfkOnCommand);
					}
					this.pendingAfkOnDelayTicks = -1;
					this.pendingAfkOnCommand = "";
				}
			}
		}

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

	private void checkAndTimeTrigger(Minecraft mc, long lastTime, long currTime) {
		long diff = currTime - lastTime;
		if (diff < 0) {
			diff += 24000;
		}
		if (diff > 0 && diff <= 100) {
			if (this.isTimeCrossed(lastTime, currTime, 18000)) {
				this.triggerTimeCommand(mc, Configs.Generic.EXECUTE_MIDNIGHT.getStringValue());
			}
			if (this.isTimeCrossed(lastTime, currTime, 0)) {
				this.triggerTimeCommand(mc, Configs.Generic.EXECUTE_DAWN.getStringValue());
			}
			if (this.isTimeCrossed(lastTime, currTime, 6000)) {
				this.triggerTimeCommand(mc, Configs.Generic.EXECUTE_NOON.getStringValue());
			}
			if (this.isTimeCrossed(lastTime, currTime, 12000)) {
				this.triggerTimeCommand(mc, Configs.Generic.EXECUTE_DUSK.getStringValue());
			}
		}
	}

	private boolean isTimeCrossed(long lastTime, long currTime, long target) {
		if (lastTime == currTime) return false;
		if (lastTime < currTime) {
			return lastTime < target && currTime >= target;
		} else {
			return lastTime < target || currTime >= target;
		}
	}

	private void triggerTimeCommand(Minecraft mc, String cmdToRun) {
		if (cmdToRun == null || cmdToRun.isEmpty()) {
			return;
		}

		String offCmd = Configs.Generic.AFK_OFF_COMMAND.getStringValue();
		if (!offCmd.isEmpty()) {
			this.executeCommand(mc, offCmd);
		}

		this.executeCommand(mc, cmdToRun);

		String onCmd = Configs.Generic.AFK_ON_COMMAND.getStringValue();
		if (!onCmd.isEmpty()) {
			this.pendingAfkOnCommand = onCmd;
			this.pendingAfkOnDelayTicks = 1200; // 60 seconds * 20 ticks
		}
	}

	private void executeCommand(Minecraft mc, String command) {
		if (command == null || command.isEmpty()) {
			return;
		}
		if (mc.player != null && mc.player.connection != null) {
			if (command.startsWith("/")) {
				mc.player.connection.sendCommand(command.substring(1));
			} else {
				mc.player.connection.sendChat(command);
			}
		}
	}

	private static java.lang.reflect.Method timeMethod = null;
	private static boolean searchedTimeMethod = false;

	private static long getTimeOfDayReflect(net.minecraft.world.level.Level level) {
		if (!searchedTimeMethod) {
			searchedTimeMethod = true;
			String[] candidates = {"getDayTime", "dayTime", "getTimeOfDay"};
			for (String name : candidates) {
				try {
					java.lang.reflect.Method m = level.getClass().getMethod(name);
					m.setAccessible(true);
					timeMethod = m;
					break;
				} catch (Exception ignored) {
				}
			}
		}
		if (timeMethod != null) {
			try {
				return (Long) timeMethod.invoke(level);
			} catch (Exception ignored) {
			}
		}
		return level.getGameTime();
	}
}
