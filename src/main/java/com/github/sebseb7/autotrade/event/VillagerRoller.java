package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.util.AutotradeInfoUtils;
import fi.dy.masa.malilib.gui.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
//? if npcSplit {
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
//?}
//? if npcFlat {
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
//?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Villager "roller": repeatedly breaks and re-places a lectern above the player
 * to re-roll a librarian's trades until one of the desired enchantments appears.
 *
 * <p>Requirements before starting:
 * <ul>
 *   <li>At least 3 lecterns in the offhand</li>
 *   <li>An axe in the main hand</li>
 *   <li>A lectern above the player, with a solid block behind it</li>
 *   <li>At least one enchantment in {@link Configs.Generic#SELECTED_ENCHANTMENTS}</li>
 * </ul>
 *
 * <p>Cycle: if the villager has a desired enchantment, remove it from the list
 * and stop. Otherwise break the lectern above with the axe, re-place it from the
 * offhand, and wait for the villager to become a librarian again. Left or right
 * click stops the cycle.
 */
public final class VillagerRoller {
	private static final VillagerRoller INSTANCE = new VillagerRoller();

	private boolean active = false;
	private int villagerId = -1;
	private BlockPos lecternPos = null;
	private int waitTicks = 0;

	private VillagerRoller() {
	}

	public static VillagerRoller getInstance() {
		return INSTANCE;
	}

	public boolean isActive() {
		return active;
	}

	/** Start the roller for the given villager (must be a librarian). */
	public void start(Minecraft mc, int villagerId) {
		if (active) {
			return;
		}
		if (!hasEnchantments()) {
			AutotradeInfoUtils.showGuiOrInGameMessage(Message.MessageType.ERROR,
					"autotrade.message.roller_no_enchantments");
			return;
		}
		if (!hasRequirements(mc)) {
			return;
		}
		this.lecternPos = findLecternAbove(mc);
		if (this.lecternPos == null) {
			AutotradeInfoUtils.showGuiOrInGameMessage(Message.MessageType.ERROR,
					"autotrade.message.roller_no_lectern");
			return;
		}
		this.villagerId = villagerId;
		this.active = true;
		this.waitTicks = 0;
		// Close the merchant screen so we can break/place blocks.
		if (mc.player != null) {
			mc.player.closeContainer();
		}
		AutotradeInfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO,
				"autotrade.message.roller_started");
	}

	/** Stop the roller cycle (called on left/right click). */
	public void stop(Minecraft mc) {
		if (!active) {
			return;
		}
		active = false;
		villagerId = -1;
		lecternPos = null;
		waitTicks = 0;
		AutotradeInfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO,
				"autotrade.message.roller_stopped");
	}

	/** Called every client tick while the roller is active. */
	public void tick(Minecraft mc) {
		if (!active || mc.level == null || mc.player == null) {
			return;
		}
		Villager villager = findVillager(mc);
		if (villager == null) {
			// Villager may be temporarily gone while re-rolling; keep waiting.
			return;
		}

		// If a merchant screen is open, read the offers the server just sent us
		// (the mod reads offers from the menu, never from the client entity).
		if (fi.dy.masa.malilib.util.GuiUtils.getCurrentScreen()
				instanceof net.minecraft.client.gui.screens.inventory.MerchantScreen merchantScreen) {
			MerchantOffers offers = merchantScreen.getMenu().getOffers();
			if (offers != null && !offers.isEmpty()) {
				String found = findDesiredEnchantment(offers);
				if (found != null) {
					// Remove the found enchantment from the list and stop.
					removeEnchantmentFromList(found);
					AutotradeInfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO,
							"autotrade.message.roller_found", found);
					stop(mc);
					return;
				}
				// No desired enchantment — close the screen and re-roll.
				if (mc.player != null) {
					mc.player.closeContainer();
				}
				waitTicks = 10;
			}
		}

		// If the villager is a librarian, we need to break the lectern to re-roll.
		if (isLibrarian(villager)) {
			// If no trade screen is open yet, interact with the villager so the
			// server sends us its current offers.
			if (!(fi.dy.masa.malilib.util.GuiUtils.getCurrentScreen()
					instanceof net.minecraft.client.gui.screens.inventory.MerchantScreen)) {
				interactWithVillager(mc, villager);
				return;
			}
			breakLectern(mc);
			return;
		}

		// Villager is not a librarian (profession cleared) — place the lectern again.
		if (waitTicks > 0) {
			waitTicks--;
			return;
		}
		placeLectern(mc);
	}

	/** Interact with the villager to open its trade screen (server sends offers). */
	private void interactWithVillager(Minecraft mc, Villager villager) {
		if (mc.player == null) {
			return;
		}
		Vec3 eye = mc.player.getEyePosition();
		Vec3 target = villager.getEyePosition();
		EntityHitResult ehr = new EntityHitResult(villager, target);
		com.github.sebseb7.autotrade.AutoTrade.autoInteracting = true;
		try {
			//? if mc26 {
			mc.gameMode.interact(mc.player, villager, ehr, InteractionHand.MAIN_HAND);
			//?} else {
			mc.gameMode.interactAt(mc.player, villager, ehr, InteractionHand.MAIN_HAND);
			mc.gameMode.interact(mc.player, villager, InteractionHand.MAIN_HAND);
			//?}
		} finally {
			com.github.sebseb7.autotrade.AutoTrade.autoInteracting = false;
		}
		mc.player.swing(InteractionHand.MAIN_HAND);
		waitTicks = 10;
	}

	private boolean hasEnchantments() {
		return !Configs.Generic.SELECTED_ENCHANTMENTS.getStringValue().trim().isEmpty();
	}

	private boolean hasRequirements(Minecraft mc) {
		if (mc.player == null) {
			return false;
		}
		// At least 3 lecterns in the offhand.
		ItemStack offhand = mc.player.getOffhandItem();
		if (!offhand.is(Items.LECTERN) || offhand.getCount() < 3) {
			AutotradeInfoUtils.showGuiOrInGameMessage(Message.MessageType.ERROR,
					"autotrade.message.roller_need_lecterns");
			return false;
		}
		// An axe in the main hand.
		ItemStack mainhand = mc.player.getMainHandItem();
		if (mainhand.isEmpty()
				|| !BuiltInRegistries.ITEM.getKey(mainhand.getItem()).toString().endsWith("_axe")) {
			AutotradeInfoUtils.showGuiOrInGameMessage(Message.MessageType.ERROR,
					"autotrade.message.roller_need_axe");
			return false;
		}
		return true;
	}

	/** Find a lectern directly above the player. */
	private BlockPos findLecternAbove(Minecraft mc) {
		if (mc.player == null) {
			return null;
		}
		BlockPos playerPos = mc.player.blockPosition();
		// Check a few blocks directly above the player.
		for (int dy = 1; dy <= 3; dy++) {
			BlockPos pos = playerPos.above(dy);
			if (mc.level.getBlockState(pos).is(Blocks.LECTERN)) {
				return pos;
			}
		}
		return null;
	}

	private Villager findVillager(Minecraft mc) {
		if (mc.level == null) {
			return null;
		}
		Entity e = TraderInteractor.findEntityById(mc, villagerId);
		return e instanceof Villager v ? v : null;
	}

	private boolean isLibrarian(Villager villager) {
		return villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN);
	}

	/** Break the lectern above the player using the main-hand axe. */
	private void breakLectern(Minecraft mc) {
		if (lecternPos == null || mc.level == null) {
			return;
		}
		if (!mc.level.getBlockState(lecternPos).is(Blocks.LECTERN)) {
			// Already broken; wait for profession to clear.
			waitTicks = 20;
			return;
		}
		// Send start + stop destroy to break the block instantly (like CivBreak).
		mc.getConnection().send(new ServerboundPlayerActionPacket(
				ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, lecternPos, Direction.UP));
		mc.getConnection().send(new ServerboundPlayerActionPacket(
				ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, lecternPos, Direction.UP));
		mc.player.swing(InteractionHand.MAIN_HAND);
		waitTicks = 20; // wait for the block to break and profession to clear
	}

	/** Place a lectern from the offhand at the position above the player. */
	private void placeLectern(Minecraft mc) {
		if (lecternPos == null || mc.player == null) {
			return;
		}
		ItemStack offhand = mc.player.getOffhandItem();
		if (!offhand.is(Items.LECTERN) || offhand.getCount() < 1) {
			AutotradeInfoUtils.showGuiOrInGameMessage(Message.MessageType.ERROR,
					"autotrade.message.roller_out_of_lecterns");
			stop(mc);
			return;
		}
		// Place the lectern at the position above the player.
		Vec3 target = new Vec3(lecternPos.getX() + 0.5, lecternPos.getY() + 0.5, lecternPos.getZ() + 0.5);
		BlockHitResult hit = new BlockHitResult(target, Direction.DOWN, lecternPos.below(), false);
		mc.gameMode.useItemOn(mc.player, InteractionHand.OFF_HAND, hit);
		mc.player.swing(InteractionHand.OFF_HAND);
		waitTicks = 40; // wait for the villager to take the profession
	}

	/** Check if any offer's result is an enchanted book with a desired enchantment. */
	private String findDesiredEnchantment(MerchantOffers offers) {
		List<String> desired = parseEnchantmentList();
		for (MerchantOffer offer : offers) {
			ItemStack result = offer.getResult();
			if (!result.is(Items.ENCHANTED_BOOK)) {
				continue;
			}
			ItemEnchantments enchants = result.get(DataComponents.STORED_ENCHANTMENTS);
			if (enchants == null) {
				continue;
			}
			for (var e : enchants.entrySet()) {
				String id = e.getKey().getRegisteredName();
				int level = e.getIntValue();
				String key = id + "=" + level;
				for (String d : desired) {
					if (d.equals(key) || d.equals(id)) {
						return key;
					}
				}
			}
		}
		return null;
	}

	private List<String> parseEnchantmentList() {
		List<String> result = new ArrayList<>();
		String cfg = Configs.Generic.SELECTED_ENCHANTMENTS.getStringValue().trim();
		if (cfg.isEmpty()) {
			return result;
		}
		for (String s : cfg.split(",")) {
			String t = s.trim();
			if (!t.isEmpty()) {
				result.add(t);
			}
		}
		return result;
	}

	private void removeEnchantmentFromList(String found) {
		List<String> remaining = new ArrayList<>();
		for (String s : parseEnchantmentList()) {
			if (!s.equals(found)) {
				remaining.add(s);
			}
		}
		Configs.Generic.SELECTED_ENCHANTMENTS.setValueFromString(String.join(",", remaining));
		Configs.saveToFile();
	}
}
