package com.github.sebseb7.autotrade.gui;

import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.GuiStringListEdit;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IDialogHandler;
import fi.dy.masa.malilib.gui.widgets.WidgetListStringListEdit;
import fi.dy.masa.malilib.gui.widgets.WidgetStringListEditEntry;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Editor for the time-triggered command list, based on malilib's
 * {@link GuiStringListEdit}.
 *
 * <p>
 * Each row holds a single entry in the format {@code tick:command}, where
 * {@code tick} is the in-game day time in ticks. New rows are pre-filled with
 * the current in-game time so they can be edited right away.
 */
public class GuiTimeCommandEdit extends GuiStringListEdit {
	private boolean saved = false;
	private final List<String> originalStrings;

	public GuiTimeCommandEdit(IConfigStringList config, IDialogHandler dialogHandler, Screen parent) {
		super(config, null, dialogHandler, parent);
		this.title = StringUtils.translate("autotrade.gui.title.time_commands");
		this.originalStrings = new ArrayList<>(config.getStrings());
	}

	@Override
	public void initGui() {
		super.initGui();

		int buttonWidth = 60;
		int spacing = 10;
		int totalWidth = buttonWidth * 2 + spacing;
		int startX = this.dialogLeft + this.dialogWidth / 2 - totalWidth / 2;
		int buttonY = this.dialogTop + this.dialogHeight - 26;

		ButtonGeneric saveButton = new ButtonGeneric(startX, buttonY, buttonWidth, 20,
				StringUtils.translate("autotrade.gui.button.save"));
		saveButton.setActionListener(new SaveButtonListener());
		this.addButton(saveButton, new SaveButtonListener());

		ButtonGeneric cancelButton = new ButtonGeneric(startX + buttonWidth + spacing, buttonY, buttonWidth, 20,
				StringUtils.translate("autotrade.gui.button.cancel"));
		cancelButton.setActionListener(new CancelButtonListener());
		this.addButton(cancelButton, new CancelButtonListener());
	}

	@Override
	public void removed() {
		if (!this.saved) {
			// Discard the changes: restore the list as it was when the editor opened.
			this.config.getStrings().clear();
			this.config.getStrings().addAll(this.originalStrings);
			this.config.markDirty();
			this.config.setModified();
		} else {
			if (this.getListWidget() != null && this.getListWidget().wereConfigsModified()) {
				this.getListWidget().applyPendingModifications();
			}
		}
	}

	@Override
	protected WidgetListStringListEdit createListWidget(int listX, int listY) {
		return new TimeCommandListWidget(this.dialogLeft + 10, this.dialogTop + 20,
				this.getBrowserWidth(), this.getBrowserHeight(),
				this.dialogWidth - 100, this);
	}

	/**
	 * The default value used for a freshly added row: the current in-game day
	 * time followed by a placeholder command.
	 */
	static String getDefaultEntryValue() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.player != null) {
			//? if mc26 {
			long tick = mc.level.getOverworldClockTime() % 24000;
			//?} else {
			/*long tick = mc.level.getDayTime() % 24000;
			*///?}
			return tick + ":";
		}
		return "0:";
	}

	private static class TimeCommandListWidget extends WidgetListStringListEdit {
		TimeCommandListWidget(int x, int y, int width, int height, int configWidth, GuiTimeCommandEdit parent) {
			super(x, y, width, height, configWidth, parent);
		}

		@Override
		protected WidgetStringListEditEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, String entry) {
			IConfigStringList config = this.getConfig();

			if (listIndex >= 0 && listIndex < config.getStrings().size()) {
				String actualValue = config.getStrings().get(listIndex);
				String defaultValue = config.getDefaultStrings().size() > listIndex
						? config.getDefaultStrings().get(listIndex) : "";

				// A newly added row is empty; pre-fill it with the current time.
				if (actualValue.isEmpty() || actualValue.isBlank()) {
					String defaultVal = getDefaultEntryValue();
					config.getStrings().set(listIndex, defaultVal);
					config.markDirty();
					config.setModified();
					actualValue = defaultVal;
				}

				return new TimeCommandEntryWidget(x, y, this.browserEntryWidth, this.browserEntryHeight,
						listIndex, isOdd, actualValue, defaultValue, this);
			}

			// Dummy entry (listIndex == -1), used when the list is empty.
			String defaultValue = getDefaultEntryValue();
			return new TimeCommandEntryWidget(x, y, this.browserEntryWidth, this.browserEntryHeight,
					listIndex, isOdd, defaultValue, defaultValue, this);
		}
	}

	private static class TimeCommandEntryWidget extends WidgetStringListEditEntry {
		TimeCommandEntryWidget(int x, int y, int width, int height,
				int listIndex, boolean isOdd, String initialValue, String defaultValue,
				WidgetListStringListEdit parent) {
			super(x, y, width, height, listIndex, isOdd, initialValue, defaultValue, parent);
		}
	}

	private class SaveButtonListener implements IButtonActionListener {
		@Override
		public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
			saved = true;
			closeEditor();
		}
	}

	private class CancelButtonListener implements IButtonActionListener {
		@Override
		public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
			saved = false;
			closeEditor();
		}
	}

	private void closeEditor() {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null) {
			return;
		}

		if (this.dialogHandler != null) {
			// Opened as a dialog (within the config GUI), use the dialog handler.
			this.dialogHandler.closeDialog();
		} else {
			// Opened standalone, go back to the parent screen.
			Screen parent = this.getParent();
			if (parent != null) {
				//? if mc262 {
				mc.gui.setScreen(parent);
				//?} else {
				/*mc.setScreen(parent);
				*///?}
			} else {
				//? if mc262 {
				mc.gui.setScreen(null);
				//?} else {
				/*mc.setScreen(null);
				*///?}
			}
		}
	}
}
