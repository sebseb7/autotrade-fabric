package com.github.sebseb7.autotrade.mixin;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the currently-selected trade index from {@link MerchantScreen}.
 *
 * <p>{@code MerchantMenu#selectionHint} is server-side hint state and is not
 * persisted on the client in newer Minecraft versions, so the only reliable
 * client source of truth is the screen's {@code shopItem} field which is
 * updated by the trade buttons.
 */
@Mixin(MerchantScreen.class)
public interface MerchantMenuAccessor {

	@Accessor("shopItem")
	int getShopItem();
}
