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
import net.minecraft.world.entity.player.Player;
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

	/**
	 * 1 second at 20 TPS — client wireframe highlight (see
	 * {@code TraderHighlightRenderer}).
	 */
	private static final int TRADER_HIGHLIGHT_TICKS = 20;

	private int traderGlowTicksRemaining = 0;
	private int traderGlowEntityId = -1;

	private int inputContainerHighlightTicks = 0;
	private int outputContainerHighlightTicks = 0;

	/**
	 * Entity to draw in-world highlight for; {@code null} when inactive or unknown
	 * id.
	 */
	Entity getTraderGlowEntityForRender(Minecraft mc) {
		if (traderGlowTicksRemaining <= 0 || traderGlowEntityId < 0 || mc.level == null) {
			return null;
		}
		return findEntityById(mc, traderGlowEntityId);
	}

	void tick(Minecraft mc) {
		tickTraderGlow(mc);
		tickContainerHighlights(mc);
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
			inputContainerHighlightTicks = TRADER_HIGHLIGHT_TICKS;
			return;
		}
		if ((mc.player.distanceToSqr(oc) < 16.0D) && (outputInRange == false)) {
			outputInRange = true;
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
					new BlockHitResult(oc, Direction.UP, output, false));
			containerDelay = Configs.Generic.CONTAINER_CLOSE_DELAY.getIntegerValue();
			outputOpened = true;
			outputContainerHighlightTicks = TRADER_HIGHLIGHT_TICKS;
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
				int tradesLeft = offer.getMaxUses() - offer.getUses();
				if (TradeItemSpec.matches(offer.getResult(), buyItemStr) && Configs.Generic.ENABLE_BUY.getBooleanValue()
						&& offer.getResult().getCount() <= Configs.Generic.BUY_LIMIT.getIntegerValue()) {
					if (tradesLeft > 0 && playerHasMerchantCosts(mc.player, offer)) {
						Slot slot = menu.getSlot(2);
						menu.setSelectionHint(i);
						mc.player.connection.send(new ServerboundSelectTradePacket(i));
						AutoTrade.bought += offer.getMaxUses();
						InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.trade_bought",
								formatItemCountNameForTrades(offer.getResult(), tradesLeft),
								formatOfferPriceForTrades(offer, tradesLeft));
						try {
							ContainerIoHelper.quickMoveResultSlot(mc, menu, slot.index);
						} catch (Exception e) {
							System.out.println("err " + e);
						}
					}
				}
				if (TradeItemSpec.matches(offer.getCostA(), sellItemStr)
						&& Configs.Generic.ENABLE_SELL.getBooleanValue()
						&& offer.getCostA().getCount() <= Configs.Generic.SELL_LIMIT.getIntegerValue()) {
					if (tradesLeft > 0 && playerHasMerchantCosts(mc.player, offer)) {
						Slot slot = menu.getSlot(2);
						menu.setSelectionHint(i);
						AutoTrade.sold += offer.getMaxUses();
						mc.player.connection.send(new ServerboundSelectTradePacket(i));
						InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.trade_sold",
								formatItemCountNameForTrades(offer.getCostA(), tradesLeft)
										+ formatOptionalSecondCostForTrades(offer, tradesLeft),
								formatItemCountNameForTrades(offer.getResult(), tradesLeft));
						try {
							ContainerIoHelper.quickMoveResultSlot(mc, menu, slot.index);
						} catch (Exception e) {
							System.out.println("err " + e);
						}
					}
				}
			}
		}
		screen.onClose();
		startTraderGlow(mc, villagerActive);
	}

	private void tickTraderGlow(Minecraft mc) {
		if (mc.level == null || traderGlowTicksRemaining <= 0) {
			return;
		}
		traderGlowTicksRemaining--;
		if (traderGlowTicksRemaining == 0) {
			traderGlowEntityId = -1;
		}
	}

	private void tickContainerHighlights(Minecraft mc) {
		if (mc.level == null) {
			return;
		}
		if (inputContainerHighlightTicks > 0) {
			inputContainerHighlightTicks--;
		}
		if (outputContainerHighlightTicks > 0) {
			outputContainerHighlightTicks--;
		}
	}

	int getInputContainerHighlightTicks() {
		return inputContainerHighlightTicks;
	}

	int getOutputContainerHighlightTicks() {
		return outputContainerHighlightTicks;
	}

	private void startTraderGlow(Minecraft mc, int entityId) {
		if (mc.level == null) {
			return;
		}
		if (findEntityById(mc, entityId) == null) {
			traderGlowTicksRemaining = 0;
			traderGlowEntityId = -1;
			return;
		}
		traderGlowEntityId = entityId;
		traderGlowTicksRemaining = TRADER_HIGHLIGHT_TICKS;
	}

	private static Entity findEntityById(Minecraft mc, int entityId) {
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e.getId() == entityId) {
				return e;
			}
		}
		return null;
	}

	/**
	 * Same stack rules as the merchant menu: player must have enough of each
	 * non-empty cost before we fire packets or show a trade toast.
	 */
	private static boolean playerHasMerchantCosts(Player player, MerchantOffer offer) {
		if (!costRequirementMet(player.getInventory(), offer.getCostA())) {
			return false;
		}
		return costRequirementMet(player.getInventory(), offer.getCostB());
	}

	private static boolean costRequirementMet(Inventory inv, ItemStack required) {
		if (required.isEmpty()) {
			return true;
		}
		int need = required.getCount();
		int have = 0;
		for (int s = 0; s < inv.getContainerSize(); s++) {
			ItemStack st = inv.getItem(s);
			if (ItemStack.isSameItemSameComponents(st, required)) {
				have += st.getCount();
				if (have >= need) {
					return true;
				}
			}
		}
		return false;
	}

	/** e.g. "3× Book" (one villager use). */
	private static String formatItemCountAndName(ItemStack stack) {
		return stack.getCount() + "× " + stack.getHoverName().getString();
	}

	/**
	 * Per-trade count × how many of this offer remain before the trade, e.g. 1
	 * iron/trade × 12 runs → "12× …".
	 */
	private static String formatItemCountNameForTrades(ItemStack perTrade, int remainingOfferUses) {
		if (remainingOfferUses <= 0) {
			return formatItemCountAndName(perTrade);
		}
		return (perTrade.getCount() * remainingOfferUses) + "× " + perTrade.getHoverName().getString();
	}

	/** For buying: the stacks you pay, scaled to how many of this offer remain. */
	private static String formatOfferPriceForTrades(MerchantOffer offer, int t) {
		if (t <= 0) {
			String a = offer.getCostA().isEmpty() ? null : formatItemCountAndName(offer.getCostA());
			if (offer.getCostB().isEmpty()) {
				return a != null ? a : "—";
			}
			String b = formatItemCountAndName(offer.getCostB());
			return a == null ? b : a + " + " + b;
		}
		String a = offer.getCostA().isEmpty()
				? null
				: (offer.getCostA().getCount() * t) + "× " + offer.getCostA().getHoverName().getString();
		if (offer.getCostB().isEmpty()) {
			return a != null ? a : "—";
		}
		String b = (offer.getCostB().getCount() * t) + "× " + offer.getCostB().getHoverName().getString();
		return a == null ? b : a + " + " + b;
	}

	/**
	 * If the trade has a second cost item, " + 2× …" scaled to remaining offer
	 * uses.
	 */
	private static String formatOptionalSecondCostForTrades(MerchantOffer offer, int t) {
		if (offer.getCostB().isEmpty()) {
			return "";
		}
		if (t <= 0) {
			return " + " + formatItemCountAndName(offer.getCostB());
		}
		return " + " + (offer.getCostB().getCount() * t) + "× " + offer.getCostB().getHoverName().getString();
	}
}
