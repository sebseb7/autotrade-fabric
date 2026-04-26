package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.AutoTrade;
import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.config.Hotkeys;
import com.github.sebseb7.autotrade.gui.GuiConfigs;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class KeybindCallbacks implements IHotkeyCallback, IClientTickHandler {
	private static final KeybindCallbacks INSTANCE = new KeybindCallbacks();

	private Vector<Entity> villagersInRange = new Vector<>();
	private int villagerActive = 0;

	private boolean state = false;
	private boolean inputInRange = false;
	private boolean inputOpened = false;
	private boolean outputInRange = false;
	private boolean outputOpened = false;
	private int tickCount = 0;
	private int voidDelay = 0;
	private int containerDelay = 0;

	public static KeybindCallbacks getInstance() {
		return INSTANCE;
	}

	private KeybindCallbacks() {
	}

	public void setCallbacks() {
		for (ConfigHotkey hotkey : Hotkeys.HOTKEY_LIST) {
			hotkey.getKeybind().setCallback(this);
		}
	}

	public boolean functionalityEnabled() {
		return Configs.Generic.ENABLED.getBooleanValue();
	}

	@Override
	public boolean onKeyAction(KeyAction action, IKeybind key) {
		return this.onKeyActionImpl(action, key);
	}

	private static String id(net.minecraft.world.item.Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	private void quickMoveResultSlot(Minecraft mc, AbstractContainerMenu menu, int slotIndex) {
		Slot slot = menu.getSlot(slotIndex);
		mc.gameMode.handleContainerInput(menu.containerId, slot.index, 0, ContainerInput.QUICK_MOVE, mc.player);
	}

	private void processOutput(AbstractContainerMenu menu, Inventory playerInv) {
		outputOpened = false;
		String itemToPlace = "minecraft:emerald";
		if (Configs.Generic.ENABLE_BUY.getBooleanValue()) {
			itemToPlace = Configs.Generic.BUY_ITEM.getStringValue();
		}
		Minecraft mc = Minecraft.getInstance();
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container == playerInv) {
				if (id(s.getItem().getItem()).equals(itemToPlace)) {
					try {
						quickMoveResultSlot(mc, menu, i);
					} catch (Exception e) {
						System.out.println("err " + e);
					}
				}
			}
		}
	}

	private void processInput(AbstractContainerMenu menu, Inventory playerInv) {
		inputOpened = false;
		HashMap<String, Integer> inventory = new HashMap<>();
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container == playerInv) {
				String k = id(s.getItem().getItem());
				inventory.put(k, s.getItem().getCount() + inventory.getOrDefault(k, 0));
			}
		}
		String itemToTake = "minecraft:emerald";
		if (Configs.Generic.ENABLE_SELL.getBooleanValue()) {
			itemToTake = Configs.Generic.SELL_ITEM.getStringValue();
		}
		int inputCount = inventory.getOrDefault(itemToTake, 0);
		Minecraft mc = Minecraft.getInstance();
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot s = menu.getSlot(i);
			if (s.container == playerInv) {
				continue;
			}
			if (id(s.getItem().getItem()).equals(itemToTake)) {
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

	private boolean onKeyActionImpl(KeyAction action, IKeybind key) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return false;
		}
		if (key == Hotkeys.TOGGLE_KEY.getKeybind()) {
			Configs.Generic.ENABLED.toggleBooleanValue();
			String msg = this.functionalityEnabled()
					? "autotrade.message.toggled_mod_on"
					: "autotrade.message.toggled_mod_off";
			InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, msg);
			if (this.functionalityEnabled()) {
				AutoTrade.sold = 0;
				AutoTrade.bought = 0;
				AutoTrade.sessionStart = System.currentTimeMillis() / 1000L;
			}
		} else if (key == Hotkeys.OPEN_GUI_SETTINGS.getKeybind()) {
			GuiBase.openGui(new GuiConfigs());
			return true;
		} else if (key == Hotkeys.SET_INPUT_KEY.getKeybind()) {
			HitResult result = mc.player.pick(20.0D, 0.0F, false);
			if (result.getType() == HitResult.Type.BLOCK) {
				BlockHitResult blockHit = (BlockHitResult) result;
				BlockPos p = blockHit.getBlockPos();
				Configs.Generic.INPUT_CONTAINER_X.setIntegerValue(p.getX());
				Configs.Generic.INPUT_CONTAINER_Y.setIntegerValue(p.getY());
				Configs.Generic.INPUT_CONTAINER_Z.setIntegerValue(p.getZ());
				InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.input_container_set",
						p.getX(), p.getY(), p.getZ());
			}
		} else if (key == Hotkeys.SET_OUTPUT_KEY.getKeybind()) {
			HitResult result = mc.player.pick(20.0D, 0.0F, false);
			if (result.getType() == HitResult.Type.BLOCK) {
				BlockHitResult blockHit = (BlockHitResult) result;
				BlockPos p = blockHit.getBlockPos();
				Configs.Generic.OUTPUT_CONTAINER_X.setIntegerValue(p.getX());
				Configs.Generic.OUTPUT_CONTAINER_Y.setIntegerValue(p.getY());
				Configs.Generic.OUTPUT_CONTAINER_Z.setIntegerValue(p.getZ());
				InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.output_container_set",
						p.getX(), p.getY(), p.getZ());
			}
		} else if (key == Hotkeys.SET_BUY_KEY.getKeybind()) {
			String buyItem = id(mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem());
			InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.buy_item_set", buyItem);
			Configs.Generic.BUY_ITEM.setValueFromString(buyItem);
		} else if (key == Hotkeys.SET_SELL_KEY.getKeybind()) {
			String sellItem = id(mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem());
			InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.sell_item_set", sellItem);
			Configs.Generic.SELL_ITEM.setValueFromString(sellItem);
		}
		return false;
	}

	@Override
	public void onClientTick(Minecraft mc) {
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
		if (this.functionalityEnabled() == false || mc.player == null) {
			return;
		}
		Inventory plInv = mc.player.getInventory();
		if (Configs.Generic.GLASS_BLOCK.getBooleanValue()) {
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
		if (Configs.Generic.ITEM_FRAME.getBooleanValue()) {
			Vec3 pm = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
			AABB box = new AABB(pm.subtract(3, 3, 3), pm.add(3, 3, 3));
			@SuppressWarnings("unchecked")
			List<ItemFrame> frames = (List<ItemFrame>) (List<?>) mc.level.getEntities((Entity) null, box,
					e -> e instanceof ItemFrame && e.isAlive());
			for (ItemFrame entity : frames) {
				ItemStack stack = entity.getItem();
				String customName = stack.getHoverName().getString();
				if ("sell".equalsIgnoreCase(customName) || "\"sell\"".equals(customName)) {
					String sellItem = id(stack.getItem());
					if (!Configs.Generic.SELL_ITEM.getStringValue().equals(sellItem)) {
						InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.sell_item_set",
								sellItem);
						Configs.Generic.SELL_ITEM.setValueFromString(sellItem);
						break;
					}
				}
				if ("buy".equalsIgnoreCase(customName) || "\"buy\"".equals(customName)) {
					String buyItem = id(stack.getItem());
					if (!Configs.Generic.BUY_ITEM.getStringValue().equals(buyItem)) {
						InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.buy_item_set",
								buyItem);
						Configs.Generic.BUY_ITEM.setValueFromString(buyItem);
						break;
					}
				}
			}
		}
		if (GuiUtils.getCurrentScreen() instanceof MerchantScreen screen) {
			if (!state) {
				String sellItemStr = Configs.Generic.SELL_ITEM.getStringValue();
				String buyItemStr = Configs.Generic.BUY_ITEM.getStringValue();
				state = true;
				MerchantMenu menu = screen.getMenu();
				MerchantOffers offers = menu.getOffers();
				for (int i = 0; i < offers.size(); i++) {
					MerchantOffer offer = offers.get(i);
					String costA = id(offer.getCostA().getItem());
					String resultI = id(offer.getResult().getItem());
					// buying from villager: configured buy item matches the trade result
					if (resultI.equals(buyItemStr) && Configs.Generic.ENABLE_BUY.getBooleanValue()
							&& offer.getResult().getCount() <= Configs.Generic.BUY_LIMIT.getIntegerValue()) {
						Slot slot = menu.getSlot(2);
						menu.setSelectionHint(i);
						mc.player.connection.send(new ServerboundSelectTradePacket(i));
						AutoTrade.bought += offer.getMaxUses();
						try {
							quickMoveResultSlot(mc, menu, slot.index);
						} catch (Exception e) {
							System.out.println("err " + e);
						}
					}
					// "sell" to villager: cost matches configured sell list
					if (costA.equals(sellItemStr) && Configs.Generic.ENABLE_SELL.getBooleanValue()
							&& offer.getCostA().getCount() <= Configs.Generic.SELL_LIMIT.getIntegerValue()) {
						Slot slot = menu.getSlot(2);
						menu.setSelectionHint(i);
						AutoTrade.sold += offer.getMaxUses();
						mc.player.connection.send(new ServerboundSelectTradePacket(i));
						try {
							quickMoveResultSlot(mc, menu, slot.index);
						} catch (Exception e) {
							System.out.println("err " + e);
						}
					}
				}
			}
			screen.onClose();
			inputInRange = false;
			outputInRange = false;
			return;
		}
		if (GuiUtils.getCurrentScreen() instanceof ShulkerBoxScreen sbs) {
			ShulkerBoxMenu m = sbs.getMenu();
			if ((containerDelay == 0) && inputOpened) {
				processInput(m, plInv);
				sbs.onClose();
			}
			if ((containerDelay == 0) && outputOpened) {
				processOutput(m, plInv);
				sbs.onClose();
			}
		} else if (GuiUtils.getCurrentScreen() instanceof ContainerScreen cs) {
			AbstractContainerMenu m = cs.getMenu();
			if ((containerDelay == 0) && inputOpened) {
				processInput(m, plInv);
				cs.onClose();
			}
			if ((containerDelay == 0) && outputOpened) {
				processOutput(m, plInv);
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
		villagersInRange = newVillagersInRange;
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
			villagersInRange = new Vector<>();
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
}
