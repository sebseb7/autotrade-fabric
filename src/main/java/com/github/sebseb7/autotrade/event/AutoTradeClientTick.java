package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.AutoTrade;
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
	/**
	 * Maximum forward clock advance that is considered a normal tick. The
	 * server re-syncs the client clock periodically, which can jump the clock
	 * by more than a few ticks; such a jump must not be treated as the clock
	 * having swept across every target in between.
	 */
	private static final long MAX_NORMAL_CLOCK_ADVANCE = 100L;

	private final AutoTradeTickState state = new AutoTradeTickState();
	private final TraderInteractor traderInteractor = new TraderInteractor(state);
	private final AutoTradeContainerFlow containerFlow = new AutoTradeContainerFlow(state);
	private final AutoTradeMerchantScreenTick merchantScreenTick = new AutoTradeMerchantScreenTick(state);
	/** Last seen in-game day time, used to detect when a configured tick was crossed. */
	private long lastTimeOfDay = -1;
	/**
	 * Indices of the time command entries that already fired in the current day.
	 * The server periodically corrects the client clock, which can move it
	 * backwards across a target; without this the command would fire again.
	 */
	private final java.util.Set<Integer> firedTimeCommands = new java.util.HashSet<>();

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
		// The clock is always tracked (so the day rollover is detected and the
		// commands are re-armed), but the commands only run while auto trading
		// is enabled.
		tickTimeCommands(mc, Configs.Generic.ENABLED.getBooleanValue());

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

	/**
	 * Run the configured time-triggered commands when the in-game day time
	 * crosses their configured tick. The in-game day time wraps at 24000 ticks,
	 * so a tick near 0 can be crossed both when the clock rolls over and when
	 * it reaches it from the previous day.
	 *
	 * @param enabled whether auto trading is currently enabled; the commands
	 *                are only executed while this is true
	 */
	private void tickTimeCommands(Minecraft mc, boolean enabled) {
		if (mc.level == null) {
			this.lastTimeOfDay = -1;
			this.firedTimeCommands.clear();
			return;
		}

		long timeOfDay = Math.floorMod(getDayClockTime(mc.level), 24000L);
		long lastTime = this.lastTimeOfDay;
		this.lastTimeOfDay = timeOfDay;

		// First tick after joining / after the world was (re)loaded: only record
		// the baseline, don't fire anything.
		if (lastTime == -1) {
			return;
		}

		if (lastTime == timeOfDay) {
			return;
		}

		// The clock wrapped past 24000: a new in-game day started, so re-arm
		// the commands for the new day. A real wrap means the clock jumped from
		// the end of the day back to the start, so the backwards distance is
		// large. A small backwards distance is a server correction, not a wrap.
		if (lastTime > timeOfDay && (lastTime - timeOfDay) > MAX_NORMAL_CLOCK_ADVANCE) {
			this.firedTimeCommands.clear();
		}

		java.util.List<Configs.TimeCommand> commands = Configs.Generic.parseTimeCommands();
		for (int i = 0; i < commands.size(); i++) {
			Configs.TimeCommand entry = commands.get(i);
			long target = Math.floorMod(entry.tick(), 24000L);
			// Skip entries that already fired today, and entries whose target
			// was already passed before the clock was first seen.
			if (!isTimeCrossed(lastTime, timeOfDay, target) || this.firedTimeCommands.contains(i)) {
				continue;
			}
			// Mark it as fired even while disabled, so a command that came due
			// while auto trading was off is not fired retroactively once it is
			// turned back on.
			this.firedTimeCommands.add(i);
			if (!enabled) {
				AutoTrade.logger.info("[AutoTrade] time command skipped (auto trade disabled): tick={} command=\"{}\"",
						entry.tick(), entry.command());
				continue;
			}
			AutoTrade.logger.info("[AutoTrade] time command at tick {}: \"{}\"", entry.tick(), entry.command());
			this.executeCommand(mc, entry.command());
		}
	}

	/**
	 * True when the in-game clock moved from {@code lastTime} to {@code currTime}
	 * and passed (or landed on) {@code target}.
	 *
	 * <p>
	 * The server periodically re-syncs the client clock, which can move it
	 * <em>backwards</em> by a tick (the client counts the day time locally, the
	 * server is authoritative). Such a backwards correction must never re-fire a
	 * target that was already passed, so only a normal forward advance counts.
	 */
	private static boolean isTimeCrossed(long lastTime, long currTime, long target) {
		// Forward distance of the clock in ticks, modulo the 24000 tick day.
		// A backwards correction yields a huge value, which the guard below
		// rejects.
		long diff = Math.floorMod(currTime - lastTime, 24000L);
		if (diff == 0 || diff > MAX_NORMAL_CLOCK_ADVANCE) {
			return false;
		}
		target = Math.floorMod(target, 24000L);
		// Forward distance from lastTime to the target. A target of 0 means the
		// target sits exactly on lastTime, i.e. it was already handled in the
		// previous tick, so it must not fire again.
		long toTarget = Math.floorMod(target - lastTime, 24000L);
		return toTarget != 0 && toTarget <= diff;
	}

	private static long getDayClockTime(net.minecraft.world.level.Level level) {
		//? if mc26 {
		return level.getOverworldClockTime();
		//?} else {
		/*return level.getDayTime();
		*///?}
	}

	/**
	 * Send a chat or command message. Messages starting with {@code /} are sent
	 * as a command, everything else as a chat message.
	 */
	private static void executeCommand(Minecraft mc, String command) {
		if (command == null || command.isEmpty()) {
			return;
		}
		if (mc.player == null || mc.player.connection == null) {
			return;
		}
		if (command.startsWith("/")) {
			mc.player.connection.sendCommand(command.substring(1));
		} else {
			mc.player.connection.sendChat(command);
		}
	}
}
