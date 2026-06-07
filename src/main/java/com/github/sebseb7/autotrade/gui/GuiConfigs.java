package com.github.sebseb7.autotrade.gui;

import com.github.sebseb7.autotrade.Reference;
import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.config.Hotkeys;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.Collections;
import java.util.List;

import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import fi.dy.masa.malilib.config.IConfigValue;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
//? if npcSplit {
import fi.dy.masa.malilib.gui.wrappers.TextFieldType;
//?}

public class GuiConfigs extends GuiConfigsBase {
	private static ConfigGuiTab tab = ConfigGuiTab.GENERIC;

	public GuiConfigs() {
		super(10, 50, Reference.MOD_ID, null, "autotrade.gui.title.configs");
	}

	@Override
	public void initGui() {
		super.initGui();
		this.clearOptions();

		int x = 10;
		int y = 26;

		for (ConfigGuiTab tab : ConfigGuiTab.VALUES) {
			x += this.createButton(x, y, -1, tab);
		}
	}

	private int createButton(int x, int y, int width, ConfigGuiTab tab) {
		ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName());
		button.setEnabled(GuiConfigs.tab != tab);
		this.addButton(button, new ButtonListener(tab, this));

		return button.getWidth() + 2;
	}

	@Override
	protected int getConfigWidth() {
		ConfigGuiTab tab = GuiConfigs.tab;

		if (tab == ConfigGuiTab.GENERIC) {
			return 200;
		}

		return super.getConfigWidth();
	}

	@Override
	public List<ConfigOptionWrapper> getConfigs() {
		List<? extends IConfigBase> configs;
		ConfigGuiTab tab = GuiConfigs.tab;

		if (tab == ConfigGuiTab.GENERIC) {
			configs = Configs.Generic.OPTIONS;
		} else if (tab == ConfigGuiTab.HOTKEYS) {
			configs = Hotkeys.HOTKEY_LIST;
		} else {
			return Collections.emptyList();
		}

		return ConfigOptionWrapper.createFor(configs);
	}

	private static class ButtonListener implements IButtonActionListener {
		private final GuiConfigs parent;
		private final ConfigGuiTab tab;

		public ButtonListener(ConfigGuiTab tab, GuiConfigs parent) {
			this.tab = tab;
			this.parent = parent;
		}

		@Override
		public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
			GuiConfigs.tab = this.tab;

			this.parent.reCreateListWidget(); // apply the new config width
			this.parent.getListWidget().resetScrollbarPosition();
			this.parent.initGui();
		}
	}

	public enum ConfigGuiTab {
		GENERIC("autotrade.gui.button.config_gui.generic"), HOTKEYS("autotrade.gui.button.config_gui.hotkeys");

		private final String translationKey;

		public static final ImmutableList<ConfigGuiTab> VALUES = ImmutableList.copyOf(values());

		ConfigGuiTab(String translationKey) {
			this.translationKey = translationKey;
		}

		public String getDisplayName() {
			return StringUtils.translate(this.translationKey);
		}
	}

	@Override
	protected WidgetListConfigOptions createListWidget(int listX, int listY) {
		return new CustomWidgetListConfigOptions(listX, listY,
				this.getBrowserWidth(), this.getBrowserHeight(), this.getConfigWidth(), 0.f, this.useKeybindSearch(), this);
	}

	private static class CustomWidgetListConfigOptions extends WidgetListConfigOptions {
		public CustomWidgetListConfigOptions(int x, int y, int width, int height, int configWidth, float zLevel, boolean useKeybindSearch, GuiConfigsBase parent) {
			super(x, y, width, height, configWidth, zLevel, useKeybindSearch, parent);
		}

		@Override
		protected WidgetConfigOption createListEntryWidget(int x, int y, int listIndex, boolean isOdd, ConfigOptionWrapper wrapper) {
			return new CustomWidgetConfigOption(x, y, this.browserEntryWidth, this.browserEntryHeight,
					this.maxLabelWidth, this.configWidth, wrapper, listIndex, this.parent, this);
		}
	}

	private static class CustomWidgetConfigOption extends WidgetConfigOption {
		public CustomWidgetConfigOption(int x, int y, int width, int height, int labelWidth, int configWidth,
				ConfigOptionWrapper wrapper, int listIndex, IKeybindConfigGui host, WidgetListConfigOptionsBase<?, ?> parent) {
			super(x, y, width, height, labelWidth, configWidth, wrapper, listIndex, host, parent);
		}

		//? if npcSplit {
		@Override
		protected void addConfigTextFieldEntry(int x, int y, int resetX, int configWidth, int configHeight, IConfigValue config, TextFieldType type) {
			String name = config.getName();
			if (name.equals("executeMidnight") || name.equals("executeDawn") || name.equals("executeNoon") || name.equals("executeDusk")) {
				int walkBtnWidth = 90;
				int adjustedConfigWidth = configWidth - (walkBtnWidth + 4);
				int adjustedResetX = x + adjustedConfigWidth + 2;

				super.addConfigTextFieldEntry(x, y, adjustedResetX, adjustedConfigWidth, configHeight, config, type);

				ButtonBase resetButton = null;
				for (int i = this.subWidgets.size() - 1; i >= 0; i--) {
					if (this.subWidgets.get(i) instanceof ButtonBase btn) {
						resetButton = btn;
						break;
					}
				}

				int walkBtnX = resetButton != null ? adjustedResetX + resetButton.getWidth() + 2 : adjustedResetX + 30 + 2;
				ButtonGeneric walkToHereBtn = new ButtonGeneric(walkBtnX, y, walkBtnWidth, 20, StringUtils.translate("autotrade.gui.button.walk_to_here"));
				this.addButton(walkToHereBtn, (button, mouseButton) -> {
					Minecraft client = Minecraft.getInstance();
					if (client.player != null && this.textField != null) {
						BlockPos playerPos = client.player.blockPosition();
						String command = String.format("#goto %d %d %d", playerPos.getX(), playerPos.getY(), playerPos.getZ());
						this.textField.textField().setValue(command);

						ITextFieldListener listener = this.textField.listener();
						if (listener != null) {
							listener.onTextChange(this.textField.textField());
						}
					}
				});
			} else {
				super.addConfigTextFieldEntry(x, y, resetX, configWidth, configHeight, config, type);
			}
		}
		//?} else {
		@Override
		protected void addConfigTextFieldEntry(int x, int y, int resetX, int configWidth, int configHeight, IConfigValue config) {
			String name = config.getName();
			if (name.equals("executeMidnight") || name.equals("executeDawn") || name.equals("executeNoon") || name.equals("executeDusk")) {
				int walkBtnWidth = 90;
				int adjustedConfigWidth = configWidth - (walkBtnWidth + 4);
				int adjustedResetX = x + adjustedConfigWidth + 2;

				super.addConfigTextFieldEntry(x, y, adjustedResetX, adjustedConfigWidth, configHeight, config);

				ButtonBase resetButton = null;
				for (int i = this.subWidgets.size() - 1; i >= 0; i--) {
					if (this.subWidgets.get(i) instanceof ButtonBase btn) {
						resetButton = btn;
						break;
					}
				}

				int walkBtnX = resetButton != null ? adjustedResetX + resetButton.getWidth() + 2 : adjustedResetX + 30 + 2;
				ButtonGeneric walkToHereBtn = new ButtonGeneric(walkBtnX, y, walkBtnWidth, 20, StringUtils.translate("autotrade.gui.button.walk_to_here"));
				this.addButton(walkToHereBtn, (button, mouseButton) -> {
					Minecraft client = Minecraft.getInstance();
					if (client.player != null && this.textField != null) {
						BlockPos playerPos = client.player.blockPosition();
						String command = String.format("#goto %d %d %d", playerPos.getX(), playerPos.getY(), playerPos.getZ());
						this.textField.getTextField().setValue(command);

						ITextFieldListener listener = this.textField.getListener();
						if (listener != null) {
							listener.onTextChange(this.textField.getTextField());
						}
					}
				});
			} else {
				super.addConfigTextFieldEntry(x, y, resetX, configWidth, configHeight, config);
			}
		}
		//?}
	}
}
