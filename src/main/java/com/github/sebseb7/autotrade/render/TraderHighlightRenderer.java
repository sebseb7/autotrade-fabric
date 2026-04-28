package com.github.sebseb7.autotrade.render;

import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.event.KeybindCallbacks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Client wireframe highlights: last-traded villager, and input/output container
 * blocks for one second after the mod opens them (same idea as Meteor-style ESP
 * boxes).
 */
public final class TraderHighlightRenderer {
	private static final ShapeRenderer SHAPE_RENDERER = new ShapeRenderer();

	private static final int TRADER_OUTLINE_COLOR = 0xFF66FF66;
	private static final int INPUT_CONTAINER_COLOR = 0xFFFF6666;
	private static final int OUTPUT_CONTAINER_COLOR = 0xFF6666FF;

	private static final float LINE_WIDTH = 2.5F;

	private TraderHighlightRenderer() {
	}

	public static void register() {
		LevelRenderEvents.AFTER_SOLID_FEATURES.register(TraderHighlightRenderer::render);
	}

	private static void render(LevelRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		KeybindCallbacks kb = KeybindCallbacks.getInstance();
		Entity trader = kb.getTraderHighlightEntity(mc);
		int inTicks = kb.getInputContainerHighlightTicks();
		int outTicks = kb.getOutputContainerHighlightTicks();
		if (trader == null && inTicks <= 0 && outTicks <= 0) {
			return;
		}

		MultiBufferSource.BufferSource bufferSource = context.bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.lines());
		PoseStack drawPose = new PoseStack();
		Vec3 camera = mc.gameRenderer.getMainCamera().position();
		float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

		if (trader != null) {
			double offX = Mth.lerp(tickDelta, trader.xOld, trader.getX()) - trader.getX();
			double offY = Mth.lerp(tickDelta, trader.yOld, trader.getY()) - trader.getY();
			double offZ = Mth.lerp(tickDelta, trader.zOld, trader.getZ()) - trader.getZ();
			AABB worldBox = trader.getBoundingBox().move(offX, offY, offZ);
			AABB cameraRelative = worldBox.move(-camera.x, -camera.y, -camera.z);
			SHAPE_RENDERER.renderShape(drawPose, consumer, Shapes.create(cameraRelative), 0.0D, 0.0D, 0.0D,
					TRADER_OUTLINE_COLOR, LINE_WIDTH);
		}

		if (inTicks > 0) {
			BlockPos in = new BlockPos(Configs.Generic.INPUT_CONTAINER_X.getIntegerValue(),
					Configs.Generic.INPUT_CONTAINER_Y.getIntegerValue(),
					Configs.Generic.INPUT_CONTAINER_Z.getIntegerValue());
			drawBlockBox(drawPose, consumer, camera, in, INPUT_CONTAINER_COLOR);
		}

		if (outTicks > 0) {
			BlockPos out = new BlockPos(Configs.Generic.OUTPUT_CONTAINER_X.getIntegerValue(),
					Configs.Generic.OUTPUT_CONTAINER_Y.getIntegerValue(),
					Configs.Generic.OUTPUT_CONTAINER_Z.getIntegerValue());
			drawBlockBox(drawPose, consumer, camera, out, OUTPUT_CONTAINER_COLOR);
		}
	}

	private static void drawBlockBox(PoseStack drawPose, VertexConsumer consumer, Vec3 camera, BlockPos pos,
			int color) {
		AABB world = AABB.encapsulatingFullBlocks(pos, pos);
		AABB cameraRelative = world.move(-camera.x, -camera.y, -camera.z);
		SHAPE_RENDERER.renderShape(drawPose, consumer, Shapes.create(cameraRelative), 0.0D, 0.0D, 0.0D, color,
				LINE_WIDTH);
	}
}
