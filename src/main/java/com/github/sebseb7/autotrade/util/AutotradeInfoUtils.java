package com.github.sebseb7.autotrade.util;

import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Wraps malilib status messages and mirrors them to the client chat with a colored prefix.
 */
public final class AutotradeInfoUtils {

	private AutotradeInfoUtils() {
	}

	public static void showGuiOrInGameMessage(Message.MessageType type, String translationKey, Object... args) {
		InfoUtils.showGuiOrInGameMessage(type, translationKey, args);
		postToChat(type, Component.translatable(translationKey, args));
	}

	public static void postToChat(Message.MessageType type, Component message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		Component line = Component.empty().append(prefix(type)).append(message);
		//? if mc26 {
		mc.player.sendSystemMessage(line);
		//?} else {
		mc.player.displayClientMessage(line, false);
		//?}
	}

	private static Component prefix(Message.MessageType type) {
		ChatFormatting color = switch (type) {
			case ERROR -> ChatFormatting.RED;
			case WARNING -> ChatFormatting.YELLOW;
			case SUCCESS -> ChatFormatting.GREEN;
			default -> ChatFormatting.AQUA;
		};
		return Component.literal("[AutoTrade] ").withStyle(color);
	}
}
