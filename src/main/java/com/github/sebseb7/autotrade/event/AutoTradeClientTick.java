package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.AutoTrade;
import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.util.TradeItemSpec;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import java.util.List;
import java.util.Vector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

final class AutoTradeClientTick {
	private final Vector<Entity> villagersInRange = new Vector<>();
	private int villagerActive = 0;

	private boolean state = false;
	private boolean inputInRange = false;
	private boolean inputOpened = false;
	private boolean outputInRange = false;
	private boolean outputOpened = false;
	private int tickCount = 0;
	private int voidDelay = 0;
	private int containerDelay = 0;

	void tick(Minecraft mc) {
		if (voidDelay > 0) {
			if (Configs.Generic.VOID_TRADING_DELAY_AFTER_TELEPORT.getBooleanValue()) {
				boolean found = false;
				for (Entity entity : mc.level.entitiesForRendering()) {
					if (entity.getId() == villagerActive) {
						found = true;
					}
				}
				if (!found) {
					voidDelay--;
				}
			} else {
				voidDelay--;
			}
			return;
		}
		if (containerDelay > 0) {
			containerDelay--;
		}
		if (!Configs.Generic.ENABLED.getBooleanValue() || mc.player == null) {
			return;
		}
		Inventory plInv = mc.player.getInventory();
		if (Configs.Generic.GLASS_BLOCK.getBooleanValue()) {
			tickGlassBlockSelection(mc);
		}
		if (Configs.Generic.ITEM_FRAME.getBooleanValue()) {
			tickItemFrameSelection(mc);
		}
		if (GuiUtils.getCurrentScreen() instanceof MerchantScreen screen) {
			tickMerchantScreen(mc, screen);
			inputInRange = false;
			outputInRange = false;
			return;
		}
		if (GuiUtils.getCurrentScreen() instanceof ShulkerBoxScreen sbs) {
			ShulkerBoxMenu m = sbs.getMenu();
			if ((containerDelay == 0) && inputOpened) {
				inputOpened = false;
				ContainerIoHelper.processInput(m, plInv);
				sbs.onClose();
			}
			if ((containerDelay == 0) && outputOpened) {
				outputOpened = false;
				ContainerIoHelper.processOutput(m, plInv);
				sbs.onClose();
			}
		} else if (GuiUtils.getCurrentScreen() instanceof ContainerScreen cs) {
			AbstractContainerMenu m = cs.getMenu();
			if ((containerDelay == 0) && inputOpened) {
				inputOpened = false;
				ContainerIoHelper.processInput(m, plInv);
				cs.onClose();
			}
			if ((containerDelay == 0) && outputOpened) {
				outputOpened = false;
				ContainerIoHelper.processOutput(m, plInv);
				cs.onClose();
			}
		}
		boolean found = false;
		Vector<Entity> newVillagersInRange = new Vector<>(villagersInRange);
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity instanceof Villager || entity instanceof WanderingTrader) {
				if (entity.distanceToSqr(mc.player) < (2.5f * 2.5f)) {
					if (!found) {
						if (!newVillagersInRange.contains(entity)) {
							found = true;
							newVillagersInRange.add(entity);
							EntityHitResult ehr = new EntityHitResult(entity, entity.position());
							mc.gameMode.interact(mc.player, entity, ehr, InteractionHand.MAIN_HAND);
							voidDelay = Configs.Generic.VOID_TRADING_DELAY.getIntegerValue();
							villagerActive = entity.getId();
							state = false;
							break;
						}
					}
				}
			}
		}
		for (Entity entity : villagersInRange) {
			if (entity.distanceToSqr(mc.player) >= 16.0D) {
				newVillagersInRange.remove(entity);
			}
		}
		villagersInRange.clear();
		villagersInRange.addAll(newVillagersInRange);
		if (found) {
			return;
		}
		BlockPos input = new BlockPos(Configs.Generic.INPUT_CONTAINER_X.getIntegerValue(),
				Configs.Generic.INPUT_CONTAINER_Y.getIntegerValue(),
				Configs.Generic.INPUT_CONTAINER_Z.getIntegerValue());
		BlockPos output = new BlockPos(Configs.Generic.OUTPUT_CONTAINER_X.getIntegerValue(),
				Configs.Generic.OUTPUT_CONTAINER_Y.getIntegerValue(),
				Configs.Generic.OUTPUT_CONTAINER_Z.getIntegerValue());
		Vec3 ic = input.getCenter();
		Vec3 oc = output.getCenter();
		if ((mc.player.distanceToSqr(ic) < 16.0D) && (inputInRange == false)) {
			inputInRange = true;
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
					new BlockHitResult(ic, Direction.UP, input, false));
			containerDelay = Configs.Generic.CONTAINER_CLOSE_DELAY.getIntegerValue();
			inputOpened = true;
			return;
		}
		if ((mc.player.distanceToSqr(oc) < 16.0D) && (outputInRange == false)) {
			outputInRange = true;
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
					new BlockHitResult(oc, Direction.UP, output, false));
			containerDelay = Configs.Generic.CONTAINER_CLOSE_DELAY.getIntegerValue();
			outputOpened = true;
			return;
		}
		if (mc.player.distanceToSqr(ic) > 25.0D) {
			inputOpened = false;
			inputInRange = false;
		}
		if (mc.player.distanceToSqr(oc) > 25.0D) {
			outputOpened = false;
			outputInRange = false;
		}
		tickCount++;
		if (tickCount > 200) {
			tickCount = 0;
			villagersInRange.clear();
			inputInRange = false;
			outputInRange = false;
			var cur = GuiUtils.getCurrentScreen();
			if (cur != null) {
				if (cur instanceof MerchantScreen || cur instanceof ShulkerBoxScreen
						|| cur instanceof ContainerScreen) {
					cur.onClose();
				}
			}
		}
	}

	private void tickGlassBlockSelection(Minecraft mc) {
		int playerX = (int) mc.player.getX();
		int playerZ = (int) mc.player.getZ();
		int playerY = (int) mc.player.getY();
		int selectorOffset = Configs.Generic.SELECTOR_OFFSET.getIntegerValue();
		int absSelectorOffset = Math.abs(selectorOffset);
		for (int x = playerX - (absSelectorOffset + 3); x < playerX + (absSelectorOffset + 3); x += 1) {
			for (int z = playerZ - (absSelectorOffset + 3); z < playerZ + (absSelectorOffset + 3); z += 1) {
				for (int y = playerY - (absSelectorOffset + 3); y < playerY + (absSelectorOffset + 3); y += 1) {
					BlockPos pos = new BlockPos(x, y, z);
					if (mc.level.getBlockState(pos).getBlock() == Blocks.RED_STAINED_GLASS) {
						if ((x != Configs.Generic.INPUT_CONTAINER_X.getIntegerValue())
								|| ((y - selectorOffset) != Configs.Generic.INPUT_CONTAINER_Y.getIntegerValue())
								|| (z != Configs.Generic.INPUT_CONTAINER_Z.getIntegerValue())) {
							Configs.Generic.INPUT_CONTAINER_X.setIntegerValue(x);
							Configs.Generic.INPUT_CONTAINER_Y.setIntegerValue(y - selectorOffset);
							Configs.Generic.INPUT_CONTAINER_Z.setIntegerValue(z);
							InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO,
									"autotrade.message.input_container_set", x, y - selectorOffset, z);
						}
						break;
					}
					if (mc.level.getBlockState(pos).getBlock() == Blocks.BLUE_STAINED_GLASS) {
						if ((x != Configs.Generic.OUTPUT_CONTAINER_X.getIntegerValue())
								|| ((y - selectorOffset) != Configs.Generic.OUTPUT_CONTAINER_Y.getIntegerValue())
								|| (z != Configs.Generic.OUTPUT_CONTAINER_Z.getIntegerValue())) {
							Configs.Generic.OUTPUT_CONTAINER_X.setIntegerValue(x);
							Configs.Generic.OUTPUT_CONTAINER_Y.setIntegerValue(y - selectorOffset);
							Configs.Generic.OUTPUT_CONTAINER_Z.setIntegerValue(z);
							InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO,
									"autotrade.message.output_container_set", x, y - selectorOffset, z);
						}
						break;
					}
				}
			}
		}
	}

	private void tickItemFrameSelection(Minecraft mc) {
		Vec3 pm = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
		AABB box = new AABB(pm.subtract(3, 3, 3), pm.add(3, 3, 3));
		@SuppressWarnings("unchecked")
		List<ItemFrame> frames = (List<ItemFrame>) (List<?>) mc.level.getEntities((Entity) null, box,
				e -> e instanceof ItemFrame && e.isAlive());
		for (ItemFrame entity : frames) {
			ItemStack stack = entity.getItem();
			String customName = stack.getHoverName().getString();
			if ("sell".equalsIgnoreCase(customName) || "\"sell\"".equals(customName)) {
				String sellItem = TradeItemSpec.encodeFromStack(stack);
				if (!Configs.Generic.SELL_ITEM.getStringValue().equals(sellItem)) {
					InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.sell_item_set",
							sellItem);
					Configs.Generic.SELL_ITEM.setValueFromString(sellItem);
					break;
				}
			}
			if ("buy".equalsIgnoreCase(customName) || "\"buy\"".equals(customName)) {
				String buyItem = TradeItemSpec.encodeFromStack(stack);
				if (!Configs.Generic.BUY_ITEM.getStringValue().equals(buyItem)) {
					InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.buy_item_set",
							buyItem);
					Configs.Generic.BUY_ITEM.setValueFromString(buyItem);
					break;
				}
			}
		}
	}

	private void tickMerchantScreen(Minecraft mc, MerchantScreen screen) {
		if (!state) {
			String sellItemStr = Configs.Generic.SELL_ITEM.getStringValue();
			String buyItemStr = Configs.Generic.BUY_ITEM.getStringValue();
			state = true;
			MerchantMenu menu = screen.getMenu();
			MerchantOffers offers = menu.getOffers();
			for (int i = 0; i < offers.size(); i++) {
				MerchantOffer offer = offers.get(i);
				if (TradeItemSpec.matches(offer.getResult(), buyItemStr) && Configs.Generic.ENABLE_BUY.getBooleanValue()
						&& offer.getResult().getCount() <= Configs.Generic.BUY_LIMIT.getIntegerValue()) {
					Slot slot = menu.getSlot(2);
					menu.setSelectionHint(i);
					mc.player.connection.send(new ServerboundSelectTradePacket(i));
					AutoTrade.bought += offer.getMaxUses();
					try {
						ContainerIoHelper.quickMoveResultSlot(mc, menu, slot.index);
					} catch (Exception e) {
						System.out.println("err " + e);
					}
				}
				if (TradeItemSpec.matches(offer.getCostA(), sellItemStr)
						&& Configs.Generic.ENABLE_SELL.getBooleanValue()
						&& offer.getCostA().getCount() <= Configs.Generic.SELL_LIMIT.getIntegerValue()) {
					Slot slot = menu.getSlot(2);
					menu.setSelectionHint(i);
					AutoTrade.sold += offer.getMaxUses();
					mc.player.connection.send(new ServerboundSelectTradePacket(i));
					try {
						ContainerIoHelper.quickMoveResultSlot(mc, menu, slot.index);
					} catch (Exception e) {
						System.out.println("err " + e);
					}
				}
			}
		}
		screen.onClose();
	}
}
