package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.mixin.MultiPlayerGameModeInvoker;
import com.github.sebseb7.autotrade.util.TradeItemSpec;
import fi.dy.masa.malilib.gui.Message;
import com.github.sebseb7.autotrade.util.AutotradeInfoUtils;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
//? if mc26 {
import net.minecraft.world.inventory.ContainerInput;
//?} else {
import net.minecraft.world.inventory.ClickType;
//?}
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class ContainerIoHelper {
	/** Logs to latest.log; set true while diagnosing input cap. */
	private static final boolean DEBUG_INPUT_COUNT = true;

	private ContainerIoHelper() {
	}

	static void quickMoveResultSlot(Minecraft mc, AbstractContainerMenu menu, int slotIndex) {
		Slot slot = menu.getSlot(slotIndex);
		ItemStack stack = slot.getItem();
		if (menu instanceof MerchantMenu && stack.is(Items.ENCHANTED_BOOK)) {
			moveMerchantResultWithPickup(mc, menu, slot);
			return;
		}
		//? if mc26 {
		mc.gameMode.handleContainerInput(menu.containerId, slot.index, 0, ContainerInput.QUICK_MOVE, mc.player);
		//?} else {
		mc.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, 0, ClickType.QUICK_MOVE, mc.player);
		//?}
	}

	/**
	 * For {@link MerchantMenu} result stacks that are {@linkplain Items#ENCHANTED_BOOK enchanted books} only:
	 * vanilla shift-click can chain every book row; use two {@code PICKUP} clicks instead.
	 * Other merchant results (e.g. emeralds) still use {@code QUICK_MOVE}.
	 */
	private static void moveMerchantResultWithPickup(Minecraft mc, AbstractContainerMenu menu, Slot resultSlot) {
		if (mc.player == null || mc.gameMode == null) {
			return;
		}
		if (resultSlot.getItem().isEmpty()) {
			return;
		}
		((MultiPlayerGameModeInvoker) mc.gameMode).invokeEnsureHasSentCarriedItem();
		containerPickupClick(mc, menu, resultSlot.index, 0);
		ItemStack carried = menu.getCarried();
		if (carried.isEmpty()) {
			return;
		}
		int depositSlotId = findMerchantDepositSlot(menu, mc.player, carried);
		if (depositSlotId < 0) {
			return;
		}
		containerPickupClick(mc, menu, depositSlotId, 0);
	}

	private static void containerPickupClick(Minecraft mc, AbstractContainerMenu menu, int slotId, int button) {
		//? if mc26 {
		mc.gameMode.handleContainerInput(menu.containerId, slotId, button, ContainerInput.PICKUP, mc.player);
		//?} else {
		mc.gameMode.handleInventoryMouseClick(menu.containerId, slotId, button, ClickType.PICKUP, mc.player);
		//?}
	}

	/** First player inventory slot in this menu that can accept {@code carried} (merge or empty). */
	private static int findMerchantDepositSlot(AbstractContainerMenu menu, Player player, ItemStack carried) {
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container != player.getInventory()) {
				continue;
			}
			ItemStack inSlot = s.getItem();
			if (inSlot.isEmpty()) {
				return s.index;
			}
			if (ItemStack.isSameItemSameComponents(inSlot, carried)
					&& inSlot.getCount() < maxStackCount(inSlot)) {
				return s.index;
			}
		}
		return -1;
	}

	/**
	 * After automated merchant packets, flush cursor/carried prediction so it matches the server.
	 * Implemented via {@link MultiPlayerGameModeInvoker} because {@code ensureHasSentCarriedItem()} is private.
	 */
	static void syncPlayerInventoryAfterMerchant(Minecraft mc) {
		if (mc.player == null || mc.gameMode == null) {
			return;
		}
		((MultiPlayerGameModeInvoker) mc.gameMode).invokeEnsureHasSentCarriedItem();
		mc.execute(() -> {
			if (mc.player != null && mc.gameMode != null) {
				((MultiPlayerGameModeInvoker) mc.gameMode).invokeEnsureHasSentCarriedItem();
			}
		});
	}

	private static final String EMERALD_SPEC = "minecraft:emerald";

	static void processOutput(AbstractContainerMenu menu, Inventory playerInv) {
		Minecraft mc = Minecraft.getInstance();
		int maxKeepStacks = Configs.Generic.MAX_INPUT_ITEMS.getIntegerValue();
		if (Configs.Generic.ENABLE_BUY.getBooleanValue()) {
			String buySpec = Configs.Generic.BUY_ITEM.getStringValue();
			MoveTotals movedBought = new MoveTotals();
			for (int i = 0; i < menu.slots.size(); i++) {
				Slot s = menu.getSlot(i);
				if (s.container == playerInv && TradeItemSpec.matches(s.getItem(), buySpec)) {
					if (s.getItem().isEmpty()) {
						continue;
					}
					ItemStack beforeMove = s.getItem().copy();
					try {
						quickMoveResultSlot(mc, menu, i);
						movedBought.add(beforeMove);
					} catch (Exception e) {
						System.out.println("err " + e);
					}
				}
			}
			movedBought.flush("autotrade.message.moved_bought_to_output");
		}
		if (!Configs.Generic.ENABLE_BUY.getBooleanValue()) {
			quickMovePlayerExcessOverCap(mc, menu, playerInv, EMERALD_SPEC, 0, true);
		}
		if (Configs.Generic.ENABLE_SELL.getBooleanValue()) {
			quickMovePlayerExcessOverCap(mc, menu, playerInv, Configs.Generic.SELL_ITEM.getStringValue(), maxKeepStacks,
					true);
		}
	}

	static void processInput(AbstractContainerMenu menu, Inventory playerInv) {
		String spec = EMERALD_SPEC;
		if (Configs.Generic.ENABLE_SELL.getBooleanValue()) {
			spec = Configs.Generic.SELL_ITEM.getStringValue();
		}
		int maxStacks = Configs.Generic.MAX_INPUT_ITEMS.getIntegerValue();
		Minecraft mc = Minecraft.getInstance();
		if (DEBUG_INPUT_COUNT) {
			debugInputCount(menu, playerInv, spec, maxStacks, "START");
		}
		MoveTotals movedFromInput = new MoveTotals();
		int itemsAtStart = countMatchingItemsOnPlayer(menu, playerInv, spec);

		// Shift-click only while below stack cap (shift-click at cap spills into extra stacks).
		for (int guard = 0; guard < maxStacks; guard++) {
			int stacks = countMatchingStacksOnPlayer(menu, playerInv, spec);
			if (stacks >= maxStacks) {
				if (DEBUG_INPUT_COUNT) {
					logInputStop("shift phase", "already at maxInputStacks=" + maxStacks + " (counted " + stacks + ")");
				}
				break;
			}
			int chestSlot = findInputChestSlot(menu, playerInv, spec);
			if (chestSlot < 0) {
				if (DEBUG_INPUT_COUNT) {
					logInputStop("shift phase", "no matching item in input chest");
				}
				break;
			}
			int stacksBefore = stacks;
			int itemsBefore = countMatchingItemsOnPlayer(menu, playerInv, spec);
			ItemStack beforeMove = menu.getSlot(chestSlot).getItem().copy();
			if (DEBUG_INPUT_COUNT) {
				System.out.println("[AutoTrade input] shift guard=" + guard + " maxInputStacks=" + maxStacks
						+ " before=" + stacksBefore + " stacks, " + itemsBefore + " items | chest slot " + chestSlot
						+ " has " + beforeMove.getCount() + " items");
			}
			try {
				quickMoveResultSlot(mc, menu, chestSlot);
				movedFromInput.add(beforeMove);
			} catch (Exception e) {
				System.out.println("[AutoTrade input] err " + e);
				break;
			}
			int stacksAfter = countMatchingStacksOnPlayer(menu, playerInv, spec);
			int itemsAfter = countMatchingItemsOnPlayer(menu, playerInv, spec);
			if (DEBUG_INPUT_COUNT) {
				System.out.println("[AutoTrade input] shift guard=" + guard + " after=" + stacksAfter + " stacks ("
						+ "limit " + maxStacks + "), " + itemsAfter + " items");
			}
			if (stacksAfter > maxStacks) {
				if (DEBUG_INPUT_COUNT) {
					logInputStop("shift phase", "OVER maxInputStacks: " + stacksAfter + " > " + maxStacks);
				}
				break;
			}
			if (stacksAfter <= stacksBefore && itemsAfter <= itemsBefore) {
				if (DEBUG_INPUT_COUNT) {
					logInputStop("shift phase", "no progress");
				}
				break;
			}
		}

		// At stack cap: pickup into partial slots only (never opens a new stack).
		for (int guard = 0; guard < 64; guard++) {
			if (remainingMatchingMergeItems(menu, playerInv, spec) <= 0) {
				break;
			}
			if (findInputChestSlot(menu, playerInv, spec) < 0) {
				break;
			}
			int itemsBefore = countMatchingItemsOnPlayer(menu, playerInv, spec);
			if (DEBUG_INPUT_COUNT) {
				System.out.println("[AutoTrade input] pickup guard=" + guard + " maxInputStacks=" + maxStacks
						+ " | " + formatInputTotals(menu, playerInv, spec, maxStacks));
			}
			if (!mergePartialFromInputChest(mc, menu, playerInv, spec, movedFromInput)) {
				if (DEBUG_INPUT_COUNT) {
					logInputStop("pickup phase", "merge failed or no partial slot");
				}
				break;
			}
			if (countMatchingItemsOnPlayer(menu, playerInv, spec) <= itemsBefore) {
				if (DEBUG_INPUT_COUNT) {
					logInputStop("pickup phase", "no item increase");
				}
				break;
			}
		}

		returnExcessInputStacksToChest(mc, menu, playerInv, spec, maxStacks);
		syncPlayerInventoryAfterMerchant(mc);
		if (DEBUG_INPUT_COUNT) {
			debugInputCount(menu, playerInv, spec, maxStacks, "END");
		}
		int itemsMovedTotal = countMatchingItemsOnPlayer(menu, playerInv, spec) - itemsAtStart;
		if (itemsMovedTotal > 0 && movedFromInput.isEmpty()) {
			ItemStack sample = firstMatchingStackOnPlayer(menu, playerInv, spec);
			if (!sample.isEmpty()) {
				movedFromInput.add(sample.copyWithCount(itemsMovedTotal));
			}
		}
		movedFromInput.flush("autotrade.message.moved_from_input");
	}

	/** Shift-click player stacks over {@code maxStacks} back into the open input chest. */
	private static void returnExcessInputStacksToChest(Minecraft mc, AbstractContainerMenu menu, Inventory playerInv,
			String spec, int maxStacks) {
		int stacks = countMatchingStacksOnPlayer(menu, playerInv, spec);
		if (stacks <= maxStacks) {
			return;
		}
		if (DEBUG_INPUT_COUNT) {
			System.out.println("[AutoTrade input] returning excess to chest: counted " + stacks
					+ " stacks > maxInputStacks=" + maxStacks);
		}
		quickMovePlayerExcessOverCap(mc, menu, playerInv, spec, maxStacks, false);
	}

	private static void logInputStop(String phase, String reason) {
		System.out.println("[AutoTrade input] " + phase + " stop: " + reason);
	}

	/**
	 * Pickup from input chest and deposit only into a partial matching player stack
	 * (never into an empty slot).
	 */
	private static boolean mergePartialFromInputChest(Minecraft mc, AbstractContainerMenu menu, Inventory playerInv,
			String spec, MoveTotals moved) {
		if (mc.player == null || mc.gameMode == null) {
			return false;
		}
		((MultiPlayerGameModeInvoker) mc.gameMode).invokeEnsureHasSentCarriedItem();

		ItemStack carried = menu.getCarried();
		if (!carried.isEmpty()) {
			return depositCarriedToPartialOnly(mc, menu, playerInv, spec, moved);
		}

		int chestSlot = findInputChestSlot(menu, playerInv, spec);
		if (chestSlot < 0) {
			return false;
		}
		Slot chest = menu.getSlot(chestSlot);
		ItemStack chestStack = chest.getItem();
		if (chestStack.isEmpty()) {
			return false;
		}
		int partialSlot = findPartialDepositSlot(menu, playerInv, chestStack, spec);
		if (partialSlot < 0) {
			return false;
		}

		int itemsBefore = countMatchingItemsOnPlayer(menu, playerInv, spec);
		containerPickupClick(mc, menu, chest.index, 0);
		if (menu.getCarried().isEmpty()) {
			return false;
		}

		partialSlot = findPartialDepositSlot(menu, playerInv, menu.getCarried(), spec);
		if (partialSlot < 0) {
			containerPickupClick(mc, menu, chest.index, 0);
			return false;
		}
		containerPickupClick(mc, menu, partialSlot, 0);

		if (!menu.getCarried().isEmpty()) {
			containerPickupClick(mc, menu, chest.index, 0);
		}

		int itemsAfter = countMatchingItemsOnPlayer(menu, playerInv, spec);
		int movedCount = itemsAfter - itemsBefore;
		if (movedCount <= 0) {
			return false;
		}
		moved.add(chestStack.copyWithCount(movedCount));
		return true;
	}

	private static boolean depositCarriedToPartialOnly(Minecraft mc, AbstractContainerMenu menu, Inventory playerInv,
			String spec, MoveTotals moved) {
		ItemStack carried = menu.getCarried();
		if (!TradeItemSpec.matches(carried, spec)) {
			return false;
		}
		int partialSlot = findPartialDepositSlot(menu, playerInv, carried, spec);
		if (partialSlot < 0) {
			return false;
		}
		int itemsBefore = countMatchingItemsOnPlayer(menu, playerInv, spec);
		containerPickupClick(mc, menu, partialSlot, 0);
		int movedCount = countMatchingItemsOnPlayer(menu, playerInv, spec) - itemsBefore;
		if (movedCount > 0) {
			moved.add(carried.copyWithCount(movedCount));
		}
		return movedCount > 0;
	}

	private static int findPartialDepositSlot(AbstractContainerMenu menu, Inventory playerInv, ItemStack stack,
			String spec) {
		if (stack.isEmpty() || !TradeItemSpec.matches(stack, spec)) {
			return -1;
		}
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container != playerInv) {
				continue;
			}
			ItemStack inSlot = s.getItem();
			if (inSlot.isEmpty()) {
				continue;
			}
			if (TradeItemSpec.matches(inSlot, spec) && ItemStack.isSameItemSameComponents(inSlot, stack)
					&& inSlot.getCount() < maxStackCount(stack)) {
				return s.index;
			}
		}
		return -1;
	}

	/** Item stack limit (e.g. 64 for iron). Not {@link Slot#getMaxStackSize()} which is often 99 for player inv. */
	private static int maxStackCount(ItemStack stack) {
		return stack.getMaxStackSize();
	}

	private static String formatInputTotals(AbstractContainerMenu menu, Inventory playerInv, String spec,
			int maxStacks) {
		int stacks = countMatchingStacksOnPlayer(menu, playerInv, spec);
		int items = countMatchingItemsOnPlayer(menu, playerInv, spec);
		int maxItems = maxStacks * 64;
		int mergeRoom = remainingMatchingMergeItems(menu, playerInv, spec);
		String cap = stacks > maxStacks ? " OVER maxInputStacks!" : (stacks == maxStacks ? " at cap" : " under cap");
		return "maxInputStacks(config)=" + maxStacks + " countedStacks=" + stacks + cap + ", items=" + items
				+ "/" + maxItems + ", mergeRoom=" + mergeRoom;
	}

	private static int remainingMatchingMergeItems(AbstractContainerMenu menu, Inventory playerInv, String spec) {
		int room = 0;
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container != playerInv) {
				continue;
			}
			ItemStack stack = s.getItem();
			if (!stack.isEmpty() && TradeItemSpec.matches(stack, spec)) {
				room += maxStackCount(stack) - stack.getCount();
			}
		}
		return room;
	}

	private static void debugInputCount(AbstractContainerMenu menu, Inventory playerInv, String spec, int maxStacks,
			String phase) {
		int stacks = countMatchingStacksOnPlayer(menu, playerInv, spec);
		ItemStack carried = menu.getCarried();
		System.out.println("[AutoTrade input] === " + phase + " === " + formatInputTotals(menu, playerInv, spec, maxStacks));
		if (stacks > maxStacks) {
			System.out.println("[AutoTrade input] *** BUG: " + stacks + " matching stacks on player, maxInputStacks="
					+ maxStacks + " ***");
		}
		int sumListed = 0;
		int slotNum = 0;
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container != playerInv) {
				continue;
			}
			ItemStack stack = s.getItem();
			if (stack.isEmpty() || !TradeItemSpec.matches(stack, spec)) {
				continue;
			}
			slotNum++;
			int limit = maxStackCount(stack);
			sumListed += stack.getCount();
			System.out.println("[AutoTrade input]   matching stack #" + slotNum + " (slot id " + s.index + "): "
					+ stack.getCount() + "/" + limit + " items");
		}
		System.out.println("[AutoTrade input]   verify: " + slotNum + " matching stacks, " + sumListed
				+ " items (limit " + maxStacks + " stacks)");
	}

	private static int findInputChestSlot(AbstractContainerMenu menu, Inventory playerInv, String spec) {
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container == playerInv || s.getItem().isEmpty()) {
				continue;
			}
			if (TradeItemSpec.matches(s.getItem(), spec)) {
				return i;
			}
		}
		return -1;
	}

	/** Non-empty player slots holding items that match {@code spec}. */
	private static int countMatchingStacksOnPlayer(AbstractContainerMenu menu, Inventory playerInv, String spec) {
		int stacks = 0;
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container == playerInv && !s.getItem().isEmpty() && TradeItemSpec.matches(s.getItem(), spec)) {
				stacks++;
			}
		}
		return stacks;
	}

	private static int countMatchingItemsOnPlayer(AbstractContainerMenu menu, Inventory playerInv, String spec) {
		int items = 0;
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container == playerInv && TradeItemSpec.matches(s.getItem(), spec)) {
				items += s.getItem().getCount();
			}
		}
		return items;
	}

	private static ItemStack firstMatchingStackOnPlayer(AbstractContainerMenu menu, Inventory playerInv, String spec) {
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container == playerInv && TradeItemSpec.matches(s.getItem(), spec) && !s.getItem().isEmpty()) {
				return s.getItem();
			}
		}
		return ItemStack.EMPTY;
	}

	/**
	 * Quick-move player stacks until at most {@code maxKeepStacks} matching stacks remain
	 * (same cap as processInput — {@link Configs.Generic#MAX_INPUT_ITEMS} is stack count).
	 */
	private static void quickMovePlayerExcessOverCap(Minecraft mc, AbstractContainerMenu menu, Inventory playerInv,
			String spec, int maxKeepStacks, boolean notify) {
		MoveTotals movedExcess = new MoveTotals();
		while (countMatchingStacksOnPlayer(menu, playerInv, spec) > maxKeepStacks) {
			boolean moved = false;
			for (int i = 0; i < menu.slots.size(); i++) {
				Slot s = menu.getSlot(i);
				if (s.container != playerInv || !TradeItemSpec.matches(s.getItem(), spec)) {
					continue;
				}
				if (s.getItem().isEmpty()) {
					continue;
				}
				int stacksBefore = countMatchingStacksOnPlayer(menu, playerInv, spec);
				ItemStack beforeMove = s.getItem().copy();
				try {
					quickMoveResultSlot(mc, menu, i);
					movedExcess.add(beforeMove);
				} catch (Exception e) {
					System.out.println("err " + e);
				}
				moved = true;
				if (countMatchingStacksOnPlayer(menu, playerInv, spec) >= stacksBefore) {
					break;
				}
				break;
			}
			if (!moved) {
				break;
			}
		}
		if (notify) {
			movedExcess.flush("autotrade.message.moved_excess_to_output");
		}
	}

	private static String formatStackForMessage(ItemStack stack) {
		return Component.translatable("autotrade.format.item_stack", stack.getCount(), stack.getHoverName())
				.getString();
	}

	/** Merges moved amounts by {@link Item} for one batched toast line per type. */
	private static final class MoveTotals {
		private final Map<Item, Integer> counts = new HashMap<>();

		void add(ItemStack stack) {
			if (stack.isEmpty()) {
				return;
			}
			counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}

		boolean isEmpty() {
			return counts.isEmpty();
		}

		void flush(String translationKey) {
			if (counts.isEmpty()) {
				return;
			}
			for (Map.Entry<Item, Integer> e : counts.entrySet()) {
				ItemStack line = new ItemStack(e.getKey(), e.getValue());
				AutotradeInfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, translationKey, formatStackForMessage(line));
			}
			counts.clear();
		}
	}
}
