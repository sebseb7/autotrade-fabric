package com.github.sebseb7.autotrade.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
 * so only that exact enchantment set is matched (same ids and levels as on the stack).
 */
public final class TradeItemSpec {
	private static final char SPEC_SEP = '#';

	private TradeItemSpec() {
	}

	private static String normalizeEnchantId(String id) {
		String t = id.trim();
		if (t.isEmpty()) {
			return t;
		}
		if (t.indexOf(':') < 0) {
			return "minecraft:" + t;
		}
		return t;
	}

	public static String encodeFromStack(ItemStack stack) {
		String base = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		ItemEnchantments enchants = enchantmentsForSpec(stack);
		if (enchants == null || enchants.isEmpty()) {
			return base;
		}
		ArrayList<String> parts = new ArrayList<>();
		for (var e : enchants.entrySet()) {
			String name = normalizeEnchantId(e.getKey().getRegisteredName());
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
		String trimmed = spec.trim();
		int sep = trimmed.indexOf(SPEC_SEP);
		String itemPart = (sep < 0 ? trimmed : trimmed.substring(0, sep)).trim();
		if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemPart)) {
			return false;
		}
		if (sep < 0) {
			return true;
		}
		Map<String, Integer> expected = parseEnchantSection(trimmed.substring(sep + 1));
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
			String name = normalizeEnchantId(e.getKey().getRegisteredName());
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
		String s = section.trim();
		if (s.isEmpty()) {
			return Map.of();
		}
		Map<String, Integer> out = new HashMap<>();
		for (String piece : s.split("&")) {
			String p = piece.trim();
			if (p.isEmpty()) {
				return null;
			}
			int eq = p.lastIndexOf('=');
			if (eq <= 0 || eq == p.length() - 1) {
				return null;
			}
			String enchantId = p.substring(0, eq).trim();
			String levelStr = p.substring(eq + 1).trim();
			int level;
			try {
				level = Integer.parseInt(levelStr);
			} catch (NumberFormatException e) {
				return null;
			}
			out.put(normalizeEnchantId(enchantId), level);
		}
		return out;
	}
}
