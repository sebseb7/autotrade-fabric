package com.github.sebseb7.autotrade.render;

//? if traderWireframeRender {
import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.event.KeybindCallbacks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
//?}
//? if traderWireframeMc26 {
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
//?}
//? if traderWireframe121 {
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
//?}
//? if traderWireframe12110 {
import net.minecraft.client.renderer.RenderType;
//?}
//? if traderWireframeMc26 || traderWireframe12111 {
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?}

/**
 * Client wireframe highlights: last-traded villager, and input/output container
 * blocks for one second after the mod opens them (same idea as Meteor-style ESP
 * boxes).
 */
public final class TraderHighlightRenderer {

	private TraderHighlightRenderer() {
	}

	public static void register() {
		//? if traderWireframeMc26 {
		LevelRenderEvents.AFTER_SOLID_FEATURES.register(TraderHighlightRenderer::renderLevelMc26);
		//?}
		//? if traderWireframe121 {
		WorldRenderEvents.END_MAIN.register(TraderHighlightRenderer::renderWorld121);
		//?}
	}

	//? if traderWireframeRender {
	private static final ShapeRenderer SHAPE_RENDERER = new ShapeRenderer();

	private static final int TRADER_OUTLINE_COLOR = 0xFF66FF66;
	private static final int INPUT_CONTAINER_COLOR = 0xFFFF6666;
	private static final int OUTPUT_CONTAINER_COLOR = 0xFF6666FF;

	//? if traderWireframeMc26 || traderWireframe12111 {
	private static final float LINE_WIDTH = 2.5F;
	//?}

	//? if traderWireframeMc26 {
	private static void renderLevelMc26(LevelRenderContext context) {
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

		renderBoxes(mc, drawPose, consumer, camera, tickDelta, trader, inTicks, outTicks);
	}
	//?}

	//? if traderWireframe121 {
	private static void renderWorld121(WorldRenderContext context) {
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

		var vertexConsumers = context.consumers();
		PoseStack drawPose = new PoseStack();
		Vec3 camera = mc.gameRenderer.getMainCamera().position();
		float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

		final VertexConsumer consumer;
		//? if traderWireframe12110 {
		consumer = vertexConsumers.getBuffer(RenderType.lines());
		//?}
		//? if traderWireframe12111 {
		consumer = vertexConsumers.getBuffer(RenderTypes.lines());
		//?}

		renderBoxes(mc, drawPose, consumer, camera, tickDelta, trader, inTicks, outTicks);
	}
	//?}

	private static void renderBoxes(Minecraft mc, PoseStack drawPose, VertexConsumer consumer, Vec3 camera,
			float tickDelta, Entity trader, int inTicks, int outTicks) {
		if (trader != null) {
			double offX = Mth.lerp(tickDelta, trader.xOld, trader.getX()) - trader.getX();
			double offY = Mth.lerp(tickDelta, trader.yOld, trader.getY()) - trader.getY();
			double offZ = Mth.lerp(tickDelta, trader.zOld, trader.getZ()) - trader.getZ();
			AABB worldBox = trader.getBoundingBox().move(offX, offY, offZ);
			AABB cameraRelative = worldBox.move(-camera.x, -camera.y, -camera.z);
			renderShape(drawPose, consumer, cameraRelative, TRADER_OUTLINE_COLOR);
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
		renderShape(drawPose, consumer, cameraRelative, color);
	}

	private static void renderShape(PoseStack drawPose, VertexConsumer consumer, AABB cameraRelative, int color) {
		//? if traderWireframe12110 {
		SHAPE_RENDERER.renderShape(drawPose, consumer, Shapes.create(cameraRelative), 0.0D, 0.0D, 0.0D, color);
		//?}
		//? if traderWireframeMc26 || traderWireframe12111 {
		SHAPE_RENDERER.renderShape(drawPose, consumer, Shapes.create(cameraRelative), 0.0D, 0.0D, 0.0D, color,
				LINE_WIDTH);
		//?}
	}
	//?}
}
