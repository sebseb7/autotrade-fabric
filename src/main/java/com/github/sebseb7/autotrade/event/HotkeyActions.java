package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.AutoTrade;
import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.config.Hotkeys;
import com.github.sebseb7.autotrade.gui.GuiConfigs;
import com.github.sebseb7.autotrade.util.TradeItemSpec;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

final class HotkeyActions {
	private HotkeyActions() {
	}

	static boolean handle(Minecraft mc, IKeybind key) {
		if (mc.player == null || mc.level == null) {
			return false;
		}
		if (key == Hotkeys.TOGGLE_KEY.getKeybind()) {
			Configs.Generic.ENABLED.toggleBooleanValue();
			boolean enabled = Configs.Generic.ENABLED.getBooleanValue();
			String msg = enabled ? "autotrade.message.toggled_mod_on" : "autotrade.message.toggled_mod_off";
			InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, msg);
			if (enabled) {
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
			String buyItem = TradeItemSpec.encodeFromStack(mc.player.getItemInHand(InteractionHand.MAIN_HAND));
			InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.buy_item_set", buyItem);
			Configs.Generic.BUY_ITEM.setValueFromString(buyItem);
		} else if (key == Hotkeys.SET_SELL_KEY.getKeybind()) {
			String sellItem = TradeItemSpec.encodeFromStack(mc.player.getItemInHand(InteractionHand.MAIN_HAND));
			InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, "autotrade.message.sell_item_set", sellItem);
			Configs.Generic.SELL_ITEM.setValueFromString(sellItem);
		}
		return false;
	}
}
