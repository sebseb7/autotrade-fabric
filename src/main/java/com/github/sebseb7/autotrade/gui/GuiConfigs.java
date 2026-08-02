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
		} else if (tab == ConfigGuiTab.ENCHANTMENTS) {
			// Open enchantment screen and switch back to GENERIC tab
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
			if (mc != null) {
				//? if mc262 {
				mc.gui.setScreen(new EnchantmentSelectionScreen(this));
				//?} else {
				/*mc.setScreen(new EnchantmentSelectionScreen(this));
				*///?}
			}
			GuiConfigs.tab = ConfigGuiTab.GENERIC;
			return Collections.emptyList();
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
		GENERIC("autotrade.gui.button.config_gui.generic"),
		HOTKEYS("autotrade.gui.button.config_gui.hotkeys"),
		ENCHANTMENTS("autotrade.gui.button.config_gui.enchantments");

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

		@Override
		protected void addConfigOption(int x, int y, int labelWidth, int configWidth, fi.dy.masa.malilib.config.IConfigBase config) {
			// Use custom button for timeWaypoints config
			if (config instanceof fi.dy.masa.malilib.config.IConfigStringList stringListConfig
					&& "timeWaypoints".equals(config.getName())) {
				y += 1;
				int configHeight = 20;
				String configName = config.getConfigGuiDisplayName();
				this.addLabel(x, y + 7, labelWidth, 8, 0xFFFFFFFF, configName);

				String comment = config.getComment();
				if (comment != null) {
					this.addConfigComment(x, y + 5, labelWidth, 12, comment);
				}

				x += labelWidth + 10;
				ConfigButtonTimeWaypoints optionButton = new ConfigButtonTimeWaypoints(x, y, configWidth, configHeight,
						stringListConfig, this.host, this.host.getDialogHandler());					optionButton.setEnabled(com.github.sebseb7.autotrade.event.BaritoneHelper.isPresent());				this.addConfigButtonEntry(x + configWidth + 2, y, (fi.dy.masa.malilib.config.IConfigResettable) config, optionButton);
			} else {
				super.addConfigOption(x, y, labelWidth, configWidth, config);
			}
		}

		//? if npcSplit {
		@Override
		protected void addConfigTextFieldEntry(int x, int y, int resetX, int configWidth, int configHeight, IConfigValue config, TextFieldType type) {
			super.addConfigTextFieldEntry(x, y, resetX, configWidth, configHeight, config, type);
		}
		//?} else {
		@Override
		protected void addConfigTextFieldEntry(int x, int y, int resetX, int configWidth, int configHeight, IConfigValue config) {
			super.addConfigTextFieldEntry(x, y, resetX, configWidth, configHeight, config);
		}
		//?}
	}
}
