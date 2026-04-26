package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.util.TradeItemSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

final class ContainerIoHelper {
	private ContainerIoHelper() {
	}

	static void quickMoveResultSlot(Minecraft mc, AbstractContainerMenu menu, int slotIndex) {
		Slot slot = menu.getSlot(slotIndex);
		mc.gameMode.handleContainerInput(menu.containerId, slot.index, 0, ContainerInput.QUICK_MOVE, mc.player);
	}

	static void processOutput(AbstractContainerMenu menu, Inventory playerInv) {
		String itemToPlace = "minecraft:emerald";
		if (Configs.Generic.ENABLE_BUY.getBooleanValue()) {
			itemToPlace = Configs.Generic.BUY_ITEM.getStringValue();
		}
		Minecraft mc = Minecraft.getInstance();
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container == playerInv) {
				if (TradeItemSpec.matches(s.getItem(), itemToPlace)) {
					try {
						quickMoveResultSlot(mc, menu, i);
					} catch (Exception e) {
						System.out.println("err " + e);
					}
				}
			}
		}
	}

	static void processInput(AbstractContainerMenu menu, Inventory playerInv) {
		String itemToTake = "minecraft:emerald";
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
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container == playerInv) {
				continue;
			}
			if (TradeItemSpec.matches(s.getItem(), itemToTake)) {
				if (inputCount < (Configs.Generic.MAX_INPUT_ITEMS.getIntegerValue() * 64)) {
					inputCount += s.getItem().getCount();
					try {
						quickMoveResultSlot(mc, menu, i);
					} catch (Exception e) {
						System.out.println("err " + e);
					}
				}
			}
		}
	}
}
