package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.util.TradeItemSpec;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class ContainerIoHelper {
	private ContainerIoHelper() {
	}

	static void quickMoveResultSlot(Minecraft mc, AbstractContainerMenu menu, int slotIndex) {
		Slot slot = menu.getSlot(slotIndex);
		mc.gameMode.handleContainerInput(menu.containerId, slot.index, 0, ContainerInput.QUICK_MOVE, mc.player);
	}

	private static final String EMERALD_SPEC = "minecraft:emerald";

	static void processOutput(AbstractContainerMenu menu, Inventory playerInv) {
		Minecraft mc = Minecraft.getInstance();
		int maxKeep = Configs.Generic.MAX_INPUT_ITEMS.getIntegerValue() * 64;
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
		quickMovePlayerExcessOverCap(mc, menu, playerInv, EMERALD_SPEC, maxKeep);
		if (Configs.Generic.ENABLE_SELL.getBooleanValue()) {
			quickMovePlayerExcessOverCap(mc, menu, playerInv, Configs.Generic.SELL_ITEM.getStringValue(), maxKeep);
		}
	}

	static void processInput(AbstractContainerMenu menu, Inventory playerInv) {
		String itemToTake = EMERALD_SPEC;
		if (Configs.Generic.ENABLE_SELL.getBooleanValue()) {
			itemToTake = Configs.Generic.SELL_ITEM.getStringValue();
		}
		int inputCount = 0;
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container == playerInv && TradeItemSpec.matches(s.getItem(), itemToTake)) {
				inputCount += s.getItem().getCount();
			}
		}
		Minecraft mc = Minecraft.getInstance();
		MoveTotals movedFromInput = new MoveTotals();
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container == playerInv) {
				continue;
			}
			if (TradeItemSpec.matches(s.getItem(), itemToTake)) {
				if (inputCount < (Configs.Generic.MAX_INPUT_ITEMS.getIntegerValue() * 64)) {
					inputCount += s.getItem().getCount();
					if (s.getItem().isEmpty()) {
						continue;
					}
					ItemStack beforeMove = s.getItem().copy();
					try {
						quickMoveResultSlot(mc, menu, i);
						movedFromInput.add(beforeMove);
					} catch (Exception e) {
						System.out.println("err " + e);
					}
				}
			}
		}
		movedFromInput.flush("autotrade.message.moved_from_input");
	}

	private static int countMatchingOnPlayer(AbstractContainerMenu menu, Inventory playerInv, String spec) {
		int n = 0;
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container == playerInv && TradeItemSpec.matches(s.getItem(), spec)) {
				n += s.getItem().getCount();
			}
		}
		return n;
	}

	/**
	 * Quick-move player stacks until at most {@code maxKeep} matching items remain
	 * (same cap as processInput).
	 */
	private static void quickMovePlayerExcessOverCap(Minecraft mc, AbstractContainerMenu menu, Inventory playerInv,
			String spec, int maxKeep) {
		MoveTotals movedExcess = new MoveTotals();
		while (true) {
			int before = countMatchingOnPlayer(menu, playerInv, spec);
			if (before <= maxKeep) {
				break;
			}
			boolean moved = false;
			for (int i = 0; i < menu.slots.size(); i++) {
				Slot s = menu.getSlot(i);
				if (s.container != playerInv || !TradeItemSpec.matches(s.getItem(), spec)) {
					continue;
				}
				if (s.getItem().isEmpty()) {
					continue;
				}
				ItemStack beforeMove = s.getItem().copy();
				try {
					quickMoveResultSlot(mc, menu, i);
					movedExcess.add(beforeMove);
				} catch (Exception e) {
					System.out.println("err " + e);
				}
				moved = true;
				break;
			}
			if (!moved) {
				break;
			}
			int after = countMatchingOnPlayer(menu, playerInv, spec);
			if (after >= before) {
				break;
			}
		}
		movedExcess.flush("autotrade.message.moved_excess_to_output");
	}

	private static String formatStackForMessage(ItemStack stack) {
		return stack.getCount() + "× " + stack.getHoverName().getString();
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

		void flush(String translationKey) {
			if (counts.isEmpty()) {
				return;
			}
			for (Map.Entry<Item, Integer> e : counts.entrySet()) {
				ItemStack line = new ItemStack(e.getKey(), e.getValue());
				InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, translationKey, formatStackForMessage(line));
			}
			counts.clear();
		}
	}
}
