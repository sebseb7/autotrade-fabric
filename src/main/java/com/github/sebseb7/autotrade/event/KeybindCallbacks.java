package com.github.sebseb7.autotrade.event;

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
import java.util.Vector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Hand;

public class KeybindCallbacks implements IHotkeyCallback, IClientTickHandler {
	private static final KeybindCallbacks INSTANCE = new KeybindCallbacks();

	private Vector<Entity> villagersInRange = new Vector<Entity>();

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
		boolean cancel = this.onKeyActionImpl(action, key);
		return cancel;
	}

	private boolean onKeyActionImpl(KeyAction action, IKeybind key) {
		MinecraftClient mc = MinecraftClient.getInstance();

		if (mc.player == null || mc.world == null) {
			return false;
		}

		if (key == Hotkeys.TOGGLE_KEY.getKeybind()) {
			Configs.Generic.ENABLED.toggleBooleanValue();
			String msg = this.functionalityEnabled()
					? "autotrade.message.toggled_mod_on"
					: "autotrade.message.toggled_mod_off";
			InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, msg);
			return true;
		} else if (key == Hotkeys.OPEN_GUI_SETTINGS.getKeybind()) {
			GuiBase.openGui(new GuiConfigs());
			return true;
		}

		if (this.functionalityEnabled() == false || (GuiUtils.getCurrentScreen() instanceof HandledScreen) == false) {
			return false;
		}

		return false;
	}

	@Override
	public void onClientTick(MinecraftClient mc) {
		if (this.functionalityEnabled() == false || mc.player == null) {
			return;
		}
		mc.inGameHud.getChatHud().addToMessageHistory("here");

		if (GuiUtils.getCurrentScreen() instanceof HandledScreen) {
			return;
		}

		boolean found = false;

		Vector<Entity> newVillagersInRange = new Vector<Entity>(villagersInRange);

		for (Entity entity : mc.player.clientWorld.getEntities()) {
			if (entity instanceof VillagerEntity) {
				if (entity.getPos().distanceTo(mc.player.getPos()) < 3) {
					if (found == false) {
						if (newVillagersInRange.contains(entity) == false) {
							found = true;
							newVillagersInRange.add(entity);
							mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND);
						}
					}
				}
			}
		}
		for (Entity entity : villagersInRange) {
			if ((entity.getPos().distanceTo(mc.player.getPos()) < 4) == false) {
				newVillagersInRange.remove(entity);
			}
		}
		villagersInRange = newVillagersInRange;
	}
}
