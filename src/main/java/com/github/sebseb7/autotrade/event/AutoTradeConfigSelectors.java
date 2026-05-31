package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.util.AutotradeInfoUtils;
import com.github.sebseb7.autotrade.util.TradeItemSpec;
import fi.dy.masa.malilib.gui.Message.MessageType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class AutoTradeConfigSelectors {

	private static final double ITEM_FRAME_RADIUS = 3.0D;

	private AutoTradeConfigSelectors() {
	}

	static void tickItemFrameSelection(Minecraft mc) {
		boolean selectItems = Configs.Generic.ITEM_FRAME.getBooleanValue();
		boolean selectContainers = Configs.Generic.SELECT_BY_NAMETAG.getBooleanValue();
		if ((!selectItems && !selectContainers) || mc.player == null || mc.level == null) {
			return;
		}

		Vec3 playerPos = mc.player.position();
		AABB box = mc.player.getBoundingBox().inflate(ITEM_FRAME_RADIUS);

		BlockPos nearestInput = null;
		double nearestInputDist = Double.MAX_VALUE;
		String nearestInputLabel = null;
		BlockPos nearestOutput = null;
		double nearestOutputDist = Double.MAX_VALUE;
		String nearestOutputLabel = null;

		for (ItemFrame frame : mc.level.getEntitiesOfClass(ItemFrame.class, box, ItemFrame::isAlive)) {
			ItemStack stack = frame.getItem();
			if (stack.isEmpty()) {
				continue;
			}
			String marker = frameMarkerName(stack);
			if (marker == null) {
				continue;
			}

			if (selectItems) {
				if (isMarker(marker, "sell")) {
					handleItemFrameSell(stack);
				} else if (isMarker(marker, "buy")) {
					handleItemFrameBuy(stack);
				}
			}

			if (!selectContainers || !isChestMarkerItem(stack)) {
				continue;
			}

			BlockPos containerPos = containerBehindFrame(frame);
			if (!isStorageContainerBlock(mc, containerPos)) {
				continue;
			}

			double dist = playerPos.distanceToSqr(Vec3.atCenterOf(containerPos));
			String label = containerLabel(mc, containerPos);
			if (isMarker(marker, "input") && dist < nearestInputDist) {
				nearestInput = containerPos.immutable();
				nearestInputDist = dist;
				nearestInputLabel = label;
			} else if (isMarker(marker, "output") && dist < nearestOutputDist) {
				nearestOutput = containerPos.immutable();
				nearestOutputDist = dist;
				nearestOutputLabel = label;
			}
		}

		if (selectContainers) {
			if (nearestInput != null) {
				setInputContainerIfChanged(nearestInput, nearestInputLabel);
			}
			if (nearestOutput != null) {
				setOutputContainerIfChanged(nearestOutput, nearestOutputLabel);
			}
		}
	}

	/** Nametagged chest item in the frame; the block the frame is on is the container. */
	private static boolean isChestMarkerItem(ItemStack stack) {
		return stack.is(Items.CHEST) || stack.is(Items.TRAPPED_CHEST);
	}

	/** Block the item frame is mounted on (chest / shulker / barrel face). */
	private static BlockPos containerBehindFrame(ItemFrame frame) {
		Direction out = frame.getDirection();
		return frame.blockPosition().relative(out.getOpposite());
	}

	private static boolean isStorageContainerBlock(Minecraft mc, BlockPos pos) {
		Block block = mc.level.getBlockState(pos).getBlock();
		return block instanceof ChestBlock || block instanceof ShulkerBoxBlock || block instanceof BarrelBlock;
	}

	private static String containerLabel(Minecraft mc, BlockPos pos) {
		return BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(pos).getBlock()).toString();
	}

	private static void setInputContainerIfChanged(BlockPos pos, String label) {
		if (pos.getX() == Configs.Generic.INPUT_CONTAINER_X.getIntegerValue()
				&& pos.getY() == Configs.Generic.INPUT_CONTAINER_Y.getIntegerValue()
				&& pos.getZ() == Configs.Generic.INPUT_CONTAINER_Z.getIntegerValue()) {
			return;
		}
		Configs.Generic.INPUT_CONTAINER_X.setIntegerValue(pos.getX());
		Configs.Generic.INPUT_CONTAINER_Y.setIntegerValue(pos.getY());
		Configs.Generic.INPUT_CONTAINER_Z.setIntegerValue(pos.getZ());
		Configs.saveToFile();
		AutotradeInfoUtils.showGuiOrInGameMessage(MessageType.INFO, "autotrade.message.input_container_set", label,
				pos.toShortString());
	}

	private static void setOutputContainerIfChanged(BlockPos pos, String label) {
		if (pos.getX() == Configs.Generic.OUTPUT_CONTAINER_X.getIntegerValue()
				&& pos.getY() == Configs.Generic.OUTPUT_CONTAINER_Y.getIntegerValue()
				&& pos.getZ() == Configs.Generic.OUTPUT_CONTAINER_Z.getIntegerValue()) {
			return;
		}
		Configs.Generic.OUTPUT_CONTAINER_X.setIntegerValue(pos.getX());
		Configs.Generic.OUTPUT_CONTAINER_Y.setIntegerValue(pos.getY());
		Configs.Generic.OUTPUT_CONTAINER_Z.setIntegerValue(pos.getZ());
		Configs.saveToFile();
		AutotradeInfoUtils.showGuiOrInGameMessage(MessageType.INFO, "autotrade.message.output_container_set", label,
				pos.toShortString());
	}

	private static String frameMarkerName(ItemStack stack) {
		Component custom = stack.get(DataComponents.CUSTOM_NAME);
		if (custom != null) {
			return normalizeMarker(custom.getString());
		}
		return null;
	}

	private static String normalizeMarker(String raw) {
		String name = ChatFormatting.stripFormatting(raw).trim();
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
