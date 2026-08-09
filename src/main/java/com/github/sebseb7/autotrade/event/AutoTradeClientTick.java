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
	/** Last parsed waypoint list, used to log only when the config changes. */
	private java.util.List<Configs.TimeWaypoint> lastLoggedWaypoints = null;

	Entity getTraderGlowEntityForRender(Minecraft mc) {
		return state.getTraderGlowEntityForRender(mc);
	}

	Entity getActiveVillagerEntity(Minecraft mc) {
		return state.getActiveVillagerEntity(mc);
	}

	int getTraderGlowColor() {
		return state.getTraderGlowColor();
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
			long timeOfDay = Math.floorMod(getDayClockTime(mc.level), 24000L);
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

		// Villager roller runs independently of the global autotrade toggle.
		VillagerRoller.getInstance().tick(mc);

		if (AutoTradeVoidDelay.handle(mc, state)) return;

		if (state.containerDelay > 0) state.containerDelay--;
		if (mc.player == null) return;

		tickPostMerchantSync(mc);
		ContainerIoHelper.tickPendingMoveReports(mc, state);
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
			java.util.List<Configs.TimeWaypoint> waypoints = Configs.Generic.parseTimeWaypoints();
			// Log the configured waypoints only when they change, so the log isn't flooded.
			if (!waypoints.equals(this.lastLoggedWaypoints)) {
				this.lastLoggedWaypoints = waypoints;
				System.out.println("[AutoTrade time] " + waypoints.size() + " configured waypoint(s):");
				for (Configs.TimeWaypoint wp : waypoints) {
					System.out.println("[AutoTrade time]   tick=" + wp.tick() + " -> " + wp.x() + "," + wp.y()
							+ "," + wp.z());
				}
			}
			for (Configs.TimeWaypoint wp : waypoints) {
				if (this.isTimeCrossed(lastTime, currTime, wp.tick())) {
					this.triggerTimeMovement(mc, wp.x(), wp.y(), wp.z());
				}
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

	private static long getDayClockTime(net.minecraft.world.level.Level level) {
		//? if mc26 {
		return level.getOverworldClockTime();
		//?} else {
		/*return level.getDayTime();
		*///?}
	}

	private void triggerTimeMovement(Minecraft mc, int x, int y, int z) {
		System.out.println("[AutoTrade] time trigger -> target x=" + x + " y=" + y + " z=" + z + " baritone=" + BaritoneHelper.describeStatus());
		if (BaritoneHelper.isPresent()) {
			BaritoneHelper.pauseMovement();
			BaritoneHelper.setMovementGoal(x, y, z);
		} else {
			System.out.println("[AutoTrade] Baritone not present; time-trigger movement skipped");
		}

		String offCmd = Configs.Generic.AFK_OFF_COMMAND.getStringValue();
		if (!offCmd.isEmpty()) {
			this.executeCommand(mc, offCmd);
		}

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
}
