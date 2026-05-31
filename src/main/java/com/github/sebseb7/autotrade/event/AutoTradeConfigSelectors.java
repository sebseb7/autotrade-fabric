package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.util.AutotradeInfoUtils;
import com.github.sebseb7.autotrade.util.TradeItemSpec;
import fi.dy.masa.malilib.gui.Message.MessageType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

final class AutoTradeConfigSelectors {

	private AutoTradeConfigSelectors() {
	}

	static void tickItemFrameSelection(Minecraft mc) {
		if (!Configs.Generic.ITEM_FRAME.getBooleanValue() || mc.player == null || mc.level == null) {
			return;
		}
		AABB box = mc.player.getBoundingBox().inflate(3.0D);
		for (ItemFrame frame : mc.level.getEntitiesOfClass(ItemFrame.class, box, ItemFrame::isAlive)) {
			ItemStack stack = frame.getItem();
			if (stack.isEmpty()) {
				continue;
			}
			String marker = frameMarkerName(stack);
			if (marker == null) {
				continue;
			}
			if (isMarker(marker, "sell")) {
				handleItemFrameSell(stack);
			} else if (isMarker(marker, "buy")) {
				handleItemFrameBuy(stack);
			}
		}
	}

	/**
	 * Nametag on the item in the frame ({@link DataComponents#CUSTOM_NAME}), not the
	 * item type display name (e.g. "Enchanted Book").
	 */
	private static String frameMarkerName(ItemStack stack) {
		Component custom = stack.get(DataComponents.CUSTOM_NAME);
		if (custom != null) {
			return normalizeMarker(custom.getString());
		}
		return null;
	}

	private static String normalizeMarker(String raw) {
		String name = raw.trim();
		if (name.length() >= 2 && name.startsWith("\"") && name.endsWith("\"")) {
			name = name.substring(1, name.length() - 1).trim();
		}
		return name.isEmpty() ? null : name;
	}

	private static boolean isMarker(String name, String marker) {
		return marker.equalsIgnoreCase(name);
	}

	private static void handleItemFrameSell(ItemStack stack) {
		String sellItem = TradeItemSpec.encodeFromStack(stack);
		if (Configs.Generic.SELL_ITEM.getStringValue().equals(sellItem)) {
			return;
		}
		Configs.Generic.SELL_ITEM.setValueFromString(sellItem);
		Configs.saveToFile();
		AutotradeInfoUtils.showGuiOrInGameMessage(MessageType.INFO, "autotrade.message.sell_item_set", sellItem);
	}

	private static void handleItemFrameBuy(ItemStack stack) {
		String buyItem = TradeItemSpec.encodeFromStack(stack);
		if (Configs.Generic.BUY_ITEM.getStringValue().equals(buyItem)) {
			return;
		}
		Configs.Generic.BUY_ITEM.setValueFromString(buyItem);
		Configs.saveToFile();
		AutotradeInfoUtils.showGuiOrInGameMessage(MessageType.INFO, "autotrade.message.buy_item_set", buyItem);
	}
}
