package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

final class AutoTradeVoidDelay {

    static boolean handle(Minecraft mc, AutoTradeTickState state) {
        if (state.voidDelay <= 0) return false;

        if (Configs.Generic.VOID_TRADING_DELAY_AFTER_TELEPORT.getBooleanValue()) {
            if (!entityStillExists(mc, state.getVillagerActive())) {
                state.voidDelay--;
            }
        } else {
            state.voidDelay--;
        }
        return true;
    }

    private static boolean entityStillExists(Minecraft mc, int entityId) {
        if (mc.level == null) return false;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.getId() == entityId) return true;
        }
        return false;
    }
}
