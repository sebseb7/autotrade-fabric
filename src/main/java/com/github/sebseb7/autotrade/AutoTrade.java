package com.github.sebseb7.autotrade;

import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AutoTrade implements ModInitializer {
	public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);
	public static long sessionStart = 0;
	public static int sold = 0;
	public static int bought = 0;

	/**
	 * Set to {@code true} while the auto-trader is firing its synthetic
	 * villager interact packets so {@link InitHandler}'s {@code UseEntityCallback}
	 * can tell automated clicks apart from a real player right-click.
	 */
	public static boolean autoInteracting = false;

	/**
	 * Entity id of the last villager/wandering trader the player right-clicked
	 * to open a trade screen. Captured in {@link InitHandler}'s
	 * {@code UseEntityCallback} so the merchant screen knows which villager it
	 * belongs to (the menu's {@code trader} is only a {@code ClientSideMerchant}
	 * wrapper, not the entity).
	 */
	public static int lastInteractedVillagerId = -1;

	@Override
	public void onInitialize() {
		InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
		logger.info("AutoTrade mod initialized successfully");
	}
}
