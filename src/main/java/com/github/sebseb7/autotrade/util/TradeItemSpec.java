package com.github.sebseb7.autotrade.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Buy/sell config strings: {@code namespace:item_id} matches any stack of that
 * item. For enchanted books (and other enchanted items), holding the item when
 * binding the hotkey stores
 * {@code minecraft:enchanted_book#minecraft:sharpness=4&minecraft:unbreaking=3}
 * so only that exact enchantment set is matched.
 */
public final class TradeItemSpec {
	private static final char SPEC_SEP = '#';

	private TradeItemSpec() {
	}

	public static String encodeFromStack(ItemStack stack) {
		String base = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		ItemEnchantments enchants = enchantmentsForSpec(stack);
		if (enchants == null || enchants.isEmpty()) {
			return base;
		}
		List<String> parts = new ArrayList<>();
		for (var e : enchants.entrySet()) {
			String name = e.getKey().getRegisteredName();
			parts.add(name + "=" + e.getIntValue());
		}
		Collections.sort(parts);
		StringBuilder sb = new StringBuilder(base);
		sb.append(SPEC_SEP);
		for (int i = 0; i < parts.size(); i++) {
			if (i > 0) {
				sb.append('&');
			}
			sb.append(parts.get(i));
		}
		return sb.toString();
	}

	public static boolean matches(ItemStack stack, String spec) {
		if (stack.isEmpty()) {
			return false;
		}
		int sep = spec.indexOf(SPEC_SEP);
		String itemPart = sep < 0 ? spec : spec.substring(0, sep);
		if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemPart)) {
			return false;
		}
		if (sep < 0) {
			return true;
		}
		Map<String, Integer> expected = parseEnchantSection(spec.substring(sep + 1));
		if (expected == null) {
			return false;
		}
		ItemEnchantments actual = enchantmentsForSpec(stack);
		if (actual == null || actual.isEmpty()) {
			return expected.isEmpty();
		}
		if (actual.size() != expected.size()) {
			return false;
		}
		for (var e : actual.entrySet()) {
			String name = e.getKey().getRegisteredName();
			int level = e.getIntValue();
			Integer want = expected.get(name);
			if (want == null || want != level) {
				return false;
			}
		}
		return true;
	}

	private static ItemEnchantments enchantmentsForSpec(ItemStack stack) {
		ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		if (!stored.isEmpty()) {
			return stored;
		}
		return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
	}

	private static Map<String, Integer> parseEnchantSection(String section) {
		if (section.isEmpty()) {
			return Map.of();
		}
		Map<String, Integer> out = new HashMap<>();
		for (String piece : section.split("&")) {
			if (piece.isEmpty()) {
				return null;
			}
			int eq = piece.lastIndexOf('=');
			if (eq <= 0 || eq == piece.length() - 1) {
				return null;
			}
			String enchantId = piece.substring(0, eq);
			String levelStr = piece.substring(eq + 1);
			int level;
			try {
				level = Integer.parseInt(levelStr);
			} catch (NumberFormatException e) {
				return null;
			}
			out.put(enchantId, level);
		}
		return out;
	}
}
