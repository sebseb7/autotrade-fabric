package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.config.Configs;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;

public final class BaritoneHelper {
	private static final boolean PRESENT = hasClass("baritone.api.BaritoneAPI");
	private static final boolean HAS_GOAL_API = PRESENT
			&& hasClass("baritone.api.pathing.goals.GoalXZ")
			&& hasClass("baritone.api.pathing.goals.GoalBlock")
			&& hasClass("baritone.api.pathing.goals.GoalNear");
	private static final boolean HAS_ELYTRA_API = PRESENT && hasClass("baritone.api.process.IElytraProcess");
	private static final Deque<MovementGoal> GOAL_STACK = new ArrayDeque<>();
	private static boolean MOVEMENT_PAUSED = false;
	/** True when baritone was paused with an active goal that should be resumed. */
	private static boolean HAS_GOAL_TO_RESUME = false;

	private BaritoneHelper() {
	}

	public static boolean isPresent() {
		return PRESENT;
	}

	static String describeStatus() {
		return PRESENT ? (HAS_GOAL_API ? "present+goal-api" : "present-no-goal-api") : "absent";
	}

	static boolean hasGoalApi() {
		return HAS_GOAL_API;
	}

	static boolean hasElytraApi() {
		return HAS_ELYTRA_API;
	}

	/**
	 * Whether baritone is actually actively pursuing a path right now.
	 * We only want to resume after an interaction if baritone was genuinely
	 * navigating before we paused it, so we don't start walking the player to
	 * a long-forgotten goal when they traded manually.
	 */
	private static boolean hasActiveGoal() {
		// Any purpose fit: everything except idle is "actively doing something".
		try {
			Object baritone = getPrimaryBaritone();
			Object processManager = invokeInstance(baritone, "getPathingBehavior");
			Object purpose = invokeInstance(processManager, "purpose");
			if (purpose != null) {
				String repr = purpose.toString();
				if (repr != null && !repr.isEmpty()) {
					return true;
				}
			}
		} catch (Throwable t) {
			System.out.println("[AutoTrade] Baritone hasActiveGoal purpose query failed: " + t);
			// Fall through to the goal-stack check below.
		}
		return hasBaritoneGoal();
	}

	/**
	 * Whether baritone currently has an actual (non-null) goal set.
	 */
	private static boolean hasBaritoneGoal() {
		try {
			Object baritone = getPrimaryBaritone();
			Object customGoalProcess = invokeInstance(baritone, "getCustomGoalProcess");
			Object goal = invokeInstance(customGoalProcess, "getGoal");
			return goal != null;
		} catch (Throwable t) {
			System.out.println("[AutoTrade] Baritone hasBaritoneGoal failed: " + t);
			return false;
		}
	}

	static void setMovementGoal(int x, int y, int z) {
		if (!Configs.Generic.USE_BARITONE.getBooleanValue()) {
			return;
		}
		if (!PRESENT) {			System.out.println("[AutoTrade] Baritone not present, skipping setMovementGoal");			return;
		}
		System.out.println("[AutoTrade] Baritone setMovementGoal x=" + x + " y=" + y + " z=" + z + " status=" + describeStatus());
		MOVEMENT_PAUSED = false;
		HAS_GOAL_TO_RESUME = false;
		MovementGoal goal = new MovementGoal(x, y, z);
		GOAL_STACK.clear();
		GOAL_STACK.push(goal);
		applyGoal(goal);
	}

	/**
	 * Set a new navigation goal while preserving any existing goal on the stack.
	 * This is used when we want to navigate to a new location but be able to
	 * resume the previous navigation after an interaction.
	 */
	static void pushNewMovementGoal(int x, int y, int z) {
		if (!Configs.Generic.USE_BARITONE.getBooleanValue()) {
			return;
		}
		if (!PRESENT) {			System.out.println("[AutoTrade] Baritone not present, skipping pushNewMovementGoal");			return;
		}
		System.out.println("[AutoTrade] Baritone pushNewMovementGoal x=" + x + " y=" + y + " z=" + z + " status=" + describeStatus());
		MOVEMENT_PAUSED = false;
		HAS_GOAL_TO_RESUME = false;
		MovementGoal goal = new MovementGoal(x, y, z);
		GOAL_STACK.push(goal);
		applyGoal(goal);
	}

	static void pauseMovement() {
		if (!Configs.Generic.USE_BARITONE.getBooleanValue()) {
			return;
		}
		if (!PRESENT) {
			System.out.println("[AutoTrade] Baritone not present, skipping pauseMovement");
			return;
		}
		// Only resume after the interaction if baritone was actually actively
		// navigating a goal right now (not merely because a stale goal lingers in
		// our stack). If the user/path wasn't moving before the trade, don't
		// start walking again afterwards.
		MOVEMENT_PAUSED = true;
		HAS_GOAL_TO_RESUME = hasActiveGoal();
		System.out.println("[AutoTrade] Baritone pauseMovement status=" + describeStatus());
		try {
			Object primaryBaritone = getPrimaryBaritone();
			Object customGoalProcess = invokeInstance(primaryBaritone, "getCustomGoalProcess");
			try {
				invokeInstance(customGoalProcess, "setGoal", new Object[] {null});
			} catch (Throwable ignored) {
				invokeInstance(customGoalProcess, "setGoalAndPath", new Object[] {null});
			}
			Object pathingControlManager = invokeInstance(primaryBaritone, "getPathingControlManager");
			try {
				invokeInstance(pathingControlManager, "cancelEverything");
			} catch (NoSuchMethodException ignored) {
				// Method may not exist in this Baritone version; movement goal cancellation above is sufficient
			}
		} catch (Throwable t) {
			System.out.println("[AutoTrade] Baritone pauseMovement failed: " + t);
		}
	}

	static void stopMovement() {
		if (!Configs.Generic.USE_BARITONE.getBooleanValue()) {
			return;
		}
		if (!PRESENT) {
			return;
		}
		try {
			Object primaryBaritone = getPrimaryBaritone();
			Object pathingControlManager = invokeInstance(primaryBaritone, "getPathingControlManager");
			try {
				invokeInstance(pathingControlManager, "cancelEverything");
			} catch (NoSuchMethodException ignored) {
				// Method may not exist in this Baritone version
			}
		} catch (Throwable ignored) {
		}
	}

	static void resumeMovementGoal() {
		if (!Configs.Generic.USE_BARITONE.getBooleanValue()) {
			return;
		}
		if (!PRESENT) {
			System.out.println("[AutoTrade] Baritone not present, skipping resumeMovementGoal");
			return;
		}
		if (!HAS_GOAL_TO_RESUME) {
			System.out.println("[AutoTrade] Baritone resumeMovementGoal skipped (was not paused with an active goal)");
			MOVEMENT_PAUSED = false;
			return;
		}
		MOVEMENT_PAUSED = false;
		HAS_GOAL_TO_RESUME = false;
		System.out.println("[AutoTrade] Baritone resumeMovementGoal status=" + describeStatus());
		MovementGoal goal = GOAL_STACK.peek();
		if (goal != null) {
			applyGoal(goal);
		}
	}

	/**
	 * Pop the current goal from the stack and resume the previous goal.
	 * Used when we've completed an interaction and want to go back to the previous navigation target.
	 */
	static void popAndResumeMovementGoal() {
		if (!Configs.Generic.USE_BARITONE.getBooleanValue()) {
			return;
		}
		if (!PRESENT) {
			System.out.println("[AutoTrade] Baritone not present, skipping popAndResumeMovementGoal");
			return;
		}
		System.out.println("[AutoTrade] Baritone popAndResumeMovementGoal status=" + describeStatus());
		MOVEMENT_PAUSED = false;
		if (!GOAL_STACK.isEmpty()) {
			GOAL_STACK.pop();
		}
		MovementGoal goal = GOAL_STACK.peek();
		if (goal != null) {
			System.out.println("[AutoTrade] Resuming previous goal x=" + goal.x + " z=" + goal.z);
			applyGoal(goal);
		} else {
			System.out.println("[AutoTrade] No previous goal to resume");
		}
	}

	static void clearMovementGoals() {
		if (!Configs.Generic.USE_BARITONE.getBooleanValue()) {
			return;
		}
		GOAL_STACK.clear();
		HAS_GOAL_TO_RESUME = false;
	}

	/**
	 * Clear all goals and stop any current navigation.
	 */
	static void stopAndClearGoals() {
		if (!Configs.Generic.USE_BARITONE.getBooleanValue()) {
			return;
		}
		if (!PRESENT) {
			return;
		}
		GOAL_STACK.clear();
		HAS_GOAL_TO_RESUME = false;
		stopMovement();
	}

	private static void applyGoal(MovementGoal goal) {
		if (MOVEMENT_PAUSED) {
			return;
		}
		try {
			Object settings = invokeStatic("baritone.api.BaritoneAPI", "getSettings");
			setSettingValue(settings, "allowBreak", false);
			setSettingValue(settings, "allowPlace", false);
			setSettingValue(settings, "allowSprint", false);
			setSettingValue(settings, "primaryTimeoutMS", 2000L);

			Object primaryBaritone = getPrimaryBaritone();
			Object customGoalProcess = invokeInstance(primaryBaritone, "getCustomGoalProcess");
			Object baritoneGoal = newInstance("baritone.api.pathing.goals.GoalBlock", new Class<?>[] {int.class, int.class, int.class}, goal.x, goal.y, goal.z);
			System.out.println("[AutoTrade] Applying Baritone goal via setGoalAndPath to x=" + goal.x + " y=" + goal.y + " z=" + goal.z);
			try {
				invokeInstance(customGoalProcess, "setGoalAndPath", baritoneGoal);
			} catch (Throwable firstFailure) {
				System.out.println("[AutoTrade] setGoalAndPath failed, trying setGoal: " + firstFailure);
				invokeInstance(customGoalProcess, "setGoal", baritoneGoal);
			}
		} catch (Throwable t) {
			System.out.println("[AutoTrade] Failed to apply Baritone goal: " + t);
		}
	}

	private static Object getPrimaryBaritone() throws ReflectiveOperationException {
		Object provider = invokeStatic("baritone.api.BaritoneAPI", "getProvider");
		return invokeInstance(provider, "getPrimaryBaritone");
	}

	private static boolean hasClass(String name) {
		try {
			Class.forName(name);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	private static Object invokeStatic(String className, String methodName) throws ReflectiveOperationException {
		Class<?> clazz = Class.forName(className);
		return clazz.getMethod(methodName).invoke(null);
	}

	private static Object invokeInstance(Object target, String methodName, Object... args) throws ReflectiveOperationException {
		for (java.lang.reflect.Method method : target.getClass().getMethods()) {
			if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
				continue;
			}
			return method.invoke(target, args);
		}
		throw new NoSuchMethodException(methodName);
	}

	private static Object newInstance(String className, Class<?>[] types, Object... args) throws ReflectiveOperationException {
		return Class.forName(className).getConstructor(types).newInstance(args);
	}

	private static void setSettingValue(Object settings, String fieldName, Object value) throws ReflectiveOperationException {
		java.lang.reflect.Field field = settings.getClass().getField(fieldName);
		Object setting = field.get(settings);
		java.lang.reflect.Method setter = null;
		for (java.lang.reflect.Method method : setting.getClass().getMethods()) {
			if (method.getName().equals("setValue") && method.getParameterCount() == 1) {
				setter = method;
				break;
			}
		}
		if (setter != null) {
			setter.invoke(setting, value);
			return;
		}
		java.lang.reflect.Field valueField = setting.getClass().getField("value");
		valueField.set(setting, value);
	}

	private static final class MovementGoal {
		private final int x;
		private final int y;
		private final int z;

		private MovementGoal(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
}
