package com.github.sebseb7.autotrade.mixin;

import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the {@code trader} field of {@link MerchantMenu} so the mod can
 * determine the villager's profession (e.g. to show the "Roll" button only for
 * librarians).
 */
@Mixin(MerchantMenu.class)
public interface MerchantMenuTraderAccessor {

	@Accessor("trader")
	Merchant getTrader();
}
