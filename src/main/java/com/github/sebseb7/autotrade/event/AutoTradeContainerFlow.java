package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.config.Configs;
import fi.dy.masa.malilib.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class AutoTradeContainerFlow {
    private final AutoTradeTickState state;

    AutoTradeContainerFlow(AutoTradeTickState state) {
        this.state = state;
    }

    void processOpenContainers(Minecraft mc, Inventory plInv) {
        if (!state.inputOpened && !state.outputOpened) {
            return;
        }

        Screen screen = GuiUtils.getCurrentScreen();
        if (screen instanceof ShulkerBoxScreen sbs) {
            processContainerIo(mc, sbs, sbs.getMenu(), plInv);
        } else if (screen instanceof ContainerScreen cs) {
            processContainerIo(mc, cs, cs.getMenu(), plInv);
        } else if (state.containerDelay <= 0) {
            state.inputOpened = false;
            state.outputOpened = false;
        }
    }

    void resetContainerFlags() {
        state.inputInRange = false;
        state.outputInRange = false;
    }

    boolean handleContainerProximity(Minecraft mc) {
        BlockPos input = new BlockPos(
                Configs.Generic.INPUT_CONTAINER_X.getIntegerValue(),
                Configs.Generic.INPUT_CONTAINER_Y.getIntegerValue(),
                Configs.Generic.INPUT_CONTAINER_Z.getIntegerValue());
        BlockPos output = new BlockPos(
                Configs.Generic.OUTPUT_CONTAINER_X.getIntegerValue(),
                Configs.Generic.OUTPUT_CONTAINER_Y.getIntegerValue(),
                Configs.Generic.OUTPUT_CONTAINER_Z.getIntegerValue());
        Vec3 ic = input.getCenter();
        Vec3 oc = output.getCenter();

        if (mc.player.distanceToSqr(ic) < 16.0D && !state.inputInRange) {
            state.inputInRange = true;
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(ic, net.minecraft.core.Direction.UP, input, false));
            state.containerDelay = Configs.Generic.CONTAINER_CLOSE_DELAY.getIntegerValue();
            state.inputOpened = true;
            state.pendingContainerPos = input;
            state.inputContainerHighlightTicks = AutoTradeTickState.TRADER_HIGHLIGHT_TICKS;
            return true;
        }

        if (mc.player.distanceToSqr(oc) < 16.0D && !state.outputInRange) {
            state.outputInRange = true;
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(oc, net.minecraft.core.Direction.UP, output, false));
            state.containerDelay = Configs.Generic.CONTAINER_CLOSE_DELAY.getIntegerValue();
            state.outputOpened = true;
            state.pendingContainerPos = output;
            state.outputContainerHighlightTicks = AutoTradeTickState.TRADER_HIGHLIGHT_TICKS;
            return true;
        }

        if (mc.player.distanceToSqr(ic) > 25.0D) {
            state.inputOpened = false;
            state.inputInRange = false;
        }
        if (mc.player.distanceToSqr(oc) > 25.0D) {
            state.outputOpened = false;
            state.outputInRange = false;
        }
        if (!state.inputOpened && !state.outputOpened) {
            state.pendingContainerPos = null;
        }

        return false;
    }

    private void processContainerIo(Minecraft mc, AbstractContainerScreen<?> screen, AbstractContainerMenu menu,
            Inventory plInv) {
        if (state.containerDelay > 0) {
            return;
        }

        BlockPos openPos = containerBlockPos(mc, screen);
        if (state.pendingContainerPos != null && openPos != null
                && !state.pendingContainerPos.equals(openPos)) {
            return;
        }

        boolean didIo = false;
        if (state.inputOpened) {
            state.inputOpened = false;
            ContainerIoHelper.processInput(menu, plInv);
            didIo = true;
        }
        if (state.outputOpened) {
            state.outputOpened = false;
            ContainerIoHelper.processOutput(menu, plInv);
            didIo = true;
        }
        if (didIo) {
            state.pendingContainerPos = null;
            screen.onClose();
        }
    }

    private static BlockPos containerBlockPos(Minecraft mc, AbstractContainerScreen<?> screen) {
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) mc.hitResult).getBlockPos();
        }
        return null;
    }
}
