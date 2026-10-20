package com.github.sebseb7.autotrade.gui;

import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.gui.interfaces.IDialogHandler;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Custom button for editing the time-triggered command list that opens the
 * custom {@link GuiTimeCommandEdit} editor.
 */
public class ConfigButtonTimeCommands extends ButtonGeneric {
	private final IConfigStringList config;
	private final IConfigGui configGui;
	private final IDialogHandler dialogHandler;

	public ConfigButtonTimeCommands(int x, int y, int width, int height,
			IConfigStringList config, IConfigGui configGui, IDialogHandler dialogHandler) {
		super(x, y, width, height, "");

		this.config = config;
		this.configGui = configGui;
		this.dialogHandler = dialogHandler;

		this.updateDisplayString();
	}

	@Override
	protected boolean onMouseClickedImpl(MouseButtonEvent click, boolean doubleClick) {
		super.onMouseClickedImpl(click, doubleClick);

		GuiBase.openGui(new GuiTimeCommandEdit(this.config, this.dialogHandler, GuiUtils.getCurrentScreen()));

		return true;
	}

	@Override
	public void updateDisplayString() {
		this.displayString = StringUtils.getClampedDisplayStringRenderlen(this.config.getStrings(), this.width - 10, "[ ", " ]");
	}
}
