package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.config.Configs;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

final class AutoTradeConfigSelectors {

    private AutoTradeConfigSelectors() {}

    static void tickItemFrameSelection(Minecraft mc) {
        if (!Configs.Generic.ITEM_FRAME.getBooleanValue()) return;
        Vec3 pm = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        var box = new net.minecraft.world.phys.AABB(pm.subtract(3, 3, 3), pm.add(3, 3, 3));
        @SuppressWarnings("unchecked")
        var frames = (java.util.List<net.minecraft.world.entity.decoration.ItemFrame>)
                (java.util.List<?>) mc.level.getEntities((Entity) null, box,
                e -> e instanceof net.minecraft.world.entity.decoration.ItemFrame && e.isAlive());

        for (var entity : frames) {
            var stack = entity.getItem();
            String customName = stack.getHoverName().getString();
            handleItemFrameSell(stack, customName);
            handleItemFrameBuy(stack, customName);
        }
    }

    private static void handleItemFrameSell(net.minecraft.world.item.ItemStack stack, String customName) {
        if (!("sell".equalsIgnoreCase(customName) || "\"sell\"".equals(customName))) return;
        String sellItem = com.github.sebseb7.autotrade.util.TradeItemSpec.encodeFromStack(stack);
        if (!Configs.Generic.SELL_ITEM.getStringValue().equals(sellItem)) {
            InfoUtils.showGuiOrInGameMessage(MessageType.INFO,
                    "autotrade.message.sell_item_set", sellItem);
            Configs.Generic.SELL_ITEM.setValueFromString(sellItem);
        }
    }

    private static void handleItemFrameBuy(net.minecraft.world.item.ItemStack stack, String customName) {
        if (!("buy".equalsIgnoreCase(customName) || "\"buy\"".equals(customName))) return;
        String buyItem = com.github.sebseb7.autotrade.util.TradeItemSpec.encodeFromStack(stack);
        if (!Configs.Generic.BUY_ITEM.getStringValue().equals(buyItem)) {
            InfoUtils.showGuiOrInGameMessage(MessageType.INFO,
                    "autotrade.message.buy_item_set", buyItem);
            Configs.Generic.BUY_ITEM.setValueFromString(buyItem);
        }
    }
}
