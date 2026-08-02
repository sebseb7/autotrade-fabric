package com.github.sebseb7.autotrade.gui;

import com.github.sebseb7.autotrade.config.Configs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
//? if mc26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Full-screen GUI that lists every registered enchantment at every possible
 * level with a checkbox. For example, Sharpness (max level 5) produces five
 * rows: {@code minecraft:sharpness=1} through {@code minecraft:sharpness=5}.
 *
 * <p>
 * Ticked entries are persisted into
 * {@link Configs.Generic#SELECTED_ENCHANTMENTS} as a comma-separated list (e.g.
 * {@code minecraft:sharpness=5,minecraft:mending=1}).
 */
public class EnchantmentSelectionScreen extends Screen {
	private final Screen parent;

	/**
	 * Every (enchantment, level) pair in alphabetical order. Each entry's
	 * {@link EnchantmentEntry#key} is the string that gets saved to config (e.g.
	 * {@code minecraft:sharpness=3}).
	 */
	private List<EnchantmentEntry> entries = List.of();

	/** Currently selected set (mutable copy while the screen is open). */
	private final Set<String> selected = new LinkedHashSet<>();

	private static final int ROW_HEIGHT = 24;
	private static final int LEFT_PADDING = 10;

	/** Current scroll offset (in pixels). */
	private int scrollOffset = 0;

	/** Total content height (number of rows × ROW_HEIGHT). */
	private int contentHeight = 0;

	/** Checkboxes created for the current scroll window. */
	private final List<CheckboxEntry> checkboxEntries = new ArrayList<>();

	public EnchantmentSelectionScreen(Screen parent) {
		super(Component.translatable("autotrade.gui.title.select_enchantments"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();

		// Load currently-selected enchantments from config.
		selected.clear();
		String cfg = Configs.Generic.SELECTED_ENCHANTMENTS.getStringValue().trim();
		if (!cfg.isEmpty()) {
			Arrays.stream(cfg.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(selected::add);
		}

		// Build (enchantment, level) pairs from the dynamic registry.
		entries = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			Registry<Enchantment> registry = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
			var ids = new ArrayList<>(registry.keySet());
			ids.sort((a, b) -> a.toString().compareTo(b.toString()));
			for (var id : ids) {
				Enchantment ench = registry.getValue(id);
				if (ench == null) {
					continue;
				}
				int maxLevel = ench.getMaxLevel();
				for (int level = 1; level <= maxLevel; level++) {
					String key = id.toString() + "=" + level;
					Component label = Component.translatable("autotrade.gui.enchantment_row", id.toString(), level);
					entries.add(new EnchantmentEntry(key, label));
				}
			}
		}

		contentHeight = entries.size() * ROW_HEIGHT;

		// "Done" button at the bottom.
		this.addRenderableWidget(Button.builder(Component.translatable("autotrade.gui.button.done"), btn -> onDone())
				.bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());

		rebuildCheckboxes();
	}

	/** Rebuild the checkbox widgets for the current scroll offset. */
	private void rebuildCheckboxes() {
		// Remove old checkboxes.
		for (CheckboxEntry entry : checkboxEntries) {
			this.removeWidget(entry.checkbox);
		}
		checkboxEntries.clear();

		for (int i = 0; i < entries.size(); i++) {
			int rowY = 30 + i * ROW_HEIGHT - scrollOffset;
			if (rowY + ROW_HEIGHT < 30 || rowY > this.height - 36) {
				continue; // off-screen
			}
			EnchantmentEntry e = entries.get(i);
			boolean checked = selected.contains(e.key);

			Checkbox cb = Checkbox.builder(e.label, this.font).pos(LEFT_PADDING, rowY)
					.selected(checked).build();
			this.addRenderableWidget(cb);
			checkboxEntries.add(new CheckboxEntry(i, cb));
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int maxScroll = Math.max(0, contentHeight - (this.height - 60));
		scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * ROW_HEIGHT));
		syncCheckboxSelections();
		rebuildCheckboxes();
		return true;
	}

	/** Before rebuilding, read back checkbox state into our selected set. */
	private void syncCheckboxSelections() {
		for (CheckboxEntry entry : checkboxEntries) {
			String key = entries.get(entry.index).key;
			if (entry.checkbox.selected()) {
				selected.add(key);
			} else {
				selected.remove(key);
			}
		}
	}

	//? if mc26 {
	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
	}
	//?} else {
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
	}
	//?}

	private void onDone() {
		syncCheckboxSelections();
		String value = selected.stream().sorted().collect(Collectors.joining(","));
		Configs.Generic.SELECTED_ENCHANTMENTS.setValueFromString(value);
		Configs.saveToFile();
		//? if mc262 {
		this.minecraft.gui.setScreen(parent);
		//?} else {
		/*this.minecraft.setScreen(parent);
		*///?}
	}

	@Override
	public void onClose() {
		onDone();
	}

	/** An enchantment at a specific level. */
	private record EnchantmentEntry(String key, Component label) {
	}

	/** Pairs a checkbox widget with its index in {@link #entries}. */
	private record CheckboxEntry(int index, Checkbox checkbox) {
	}
}
