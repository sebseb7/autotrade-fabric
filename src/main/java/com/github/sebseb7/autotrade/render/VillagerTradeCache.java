package com.github.sebseb7.autotrade.render;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Client-side cache of villager/wandering-trader trade offers, keyed by entity
 * UUID.
 *
 * <p>
 * Populated by {@code AutoTradeClientTick} when the mod opens a merchant
 * screen.
 */
public final class VillagerTradeCache {
	private static final Map<UUID, MerchantOffers> CACHE = new ConcurrentHashMap<>();

	private VillagerTradeCache() {
	}

	/** Store (or update) the offers we observed for a given entity. */
	public static void put(UUID entityUuid, MerchantOffers offers) {
		CACHE.put(entityUuid, offers);
	}

	/**
	 * Retrieve cached offers, or {@code null} if we haven't seen this entity trade
	 * yet.
	 */
	public static MerchantOffers get(UUID entityUuid) {
		return CACHE.get(entityUuid);
	}

	/** Remove a single entry (e.g. when the entity leaves render distance). */
	public static void remove(UUID entityUuid) {
		CACHE.remove(entityUuid);
	}

	/** Drop every cached entry (e.g. on world change). */
	public static void clear() {
		CACHE.clear();
	}
}
