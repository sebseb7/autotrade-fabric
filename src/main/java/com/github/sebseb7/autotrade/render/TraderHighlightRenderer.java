package com.github.sebseb7.autotrade.render;

//? if traderWireframeRender {
import com.github.sebseb7.autotrade.config.Configs;
import com.github.sebseb7.autotrade.event.KeybindCallbacks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
//?}
//? if traderWireframe121 {
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
//?}
//? if traderWireframeMc26 {
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
//?}
//? if traderWireframe121 || traderWireframeMc26Old {
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
//?}
//? if traderWireframe12110 {
import net.minecraft.client.renderer.RenderType;
//?}
//? if traderWireframe12111 || traderWireframeMc26Old {
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?}
//? if mc262 {
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
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
	private static final int TRADER_OUTLINE_COLOR = 0xFF66FF66;
	private static final int INPUT_CONTAINER_COLOR = 0xFFFF6666;
	private static final int OUTPUT_CONTAINER_COLOR = 0xFF6666FF;

	//? if traderWireframe12111 || traderWireframeMc26Old {
	private static final float LINE_WIDTH = 2.5F;
	//?}

	//? if mc262 {
	private static final float LINE_WIDTH_262 = 2.5F;
	//?}

	//? if traderWireframe121 || traderWireframeMc26Old {
	private static final ShapeRenderer SHAPE_RENDERER = new ShapeRenderer();
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

		PoseStack drawPose = new PoseStack();
		//? if mc262 {
		Vec3 camera = mc.gameRenderer.mainCamera().position();
		//?} else {
		/*Vec3 camera = mc.gameRenderer.getMainCamera().position();
		*///?}
		float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

	//? if mc262 {
	submitLevelOutline(context, drawPose, camera, tickDelta, trader, inTicks, outTicks);
	//?}
	//? if traderWireframeMc26Old {
	/*MultiBufferSource.BufferSource bufferSource = context.bufferSource();
	VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.lines());
	renderBoxes(drawPose, consumer, camera, tickDelta, trader, inTicks, outTicks);
	*///?}
	}
	//?}

	//? if mc262 {
	private static void submitLevelOutline(LevelRenderContext context, PoseStack drawPose,
			Vec3 camera, float tickDelta, Entity trader, int inTicks, int outTicks) {
		SubmitNodeCollector collector = context.submitNodeCollector();
		OrderedSubmitNodeCollector ordered = collector.order(0);

		// submitShapeOutline expects camera-relative coordinates, so translate
		// the pose stack by the negative camera position (same as the 1.21.x path).
		drawPose.pushPose();
		drawPose.translate(-camera.x, -camera.y, -camera.z);

		if (trader != null) {
			double offX = Mth.lerp(tickDelta, trader.xOld, trader.getX()) - trader.getX();
			double offY = Mth.lerp(tickDelta, trader.yOld, trader.getY()) - trader.getY();
			double offZ = Mth.lerp(tickDelta, trader.zOld, trader.getZ()) - trader.getZ();
			AABB worldBox = trader.getBoundingBox().move(offX, offY, offZ);
			submitBoxOutline(ordered, drawPose, worldBox, KeybindCallbacks.getInstance().getTraderGlowColor());
		}

		if (inTicks > 0) {
			BlockPos in = new BlockPos(Configs.Generic.INPUT_CONTAINER_X.getIntegerValue(),
					Configs.Generic.INPUT_CONTAINER_Y.getIntegerValue(),
					Configs.Generic.INPUT_CONTAINER_Z.getIntegerValue());
			AABB world = AABB.encapsulatingFullBlocks(in, in);
			submitBoxOutline(ordered, drawPose, world, INPUT_CONTAINER_COLOR);
		}

		if (outTicks > 0) {
			BlockPos out = new BlockPos(Configs.Generic.OUTPUT_CONTAINER_X.getIntegerValue(),
					Configs.Generic.OUTPUT_CONTAINER_Y.getIntegerValue(),
					Configs.Generic.OUTPUT_CONTAINER_Z.getIntegerValue());
			AABB world = AABB.encapsulatingFullBlocks(out, out);
			submitBoxOutline(ordered, drawPose, world, OUTPUT_CONTAINER_COLOR);
		}

		drawPose.popPose();
	}

	private static void submitBoxOutline(OrderedSubmitNodeCollector ordered, PoseStack drawPose,
			AABB box, int color) {
		ordered.submitShapeOutline(drawPose, Shapes.create(box),
				net.minecraft.client.renderer.rendertype.RenderTypes.lines(),
				color, LINE_WIDTH_262, true);
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
		/*consumer = vertexConsumers.getBuffer(RenderTypes.lines());
		*///?}

		renderBoxes(drawPose, consumer, camera, tickDelta, trader, inTicks, outTicks);
	}
	//?}

	//? if traderWireframe121 || traderWireframeMc26Old {
	private static void renderBoxes(PoseStack drawPose, VertexConsumer consumer, Vec3 camera,
			float tickDelta, Entity trader, int inTicks, int outTicks) {
		if (trader != null) {
			double offX = Mth.lerp(tickDelta, trader.xOld, trader.getX()) - trader.getX();
			double offY = Mth.lerp(tickDelta, trader.yOld, trader.getY()) - trader.getY();
			double offZ = Mth.lerp(tickDelta, trader.zOld, trader.getZ()) - trader.getZ();
			AABB worldBox = trader.getBoundingBox().move(offX, offY, offZ);
			AABB cameraRelative = worldBox.move(-camera.x, -camera.y, -camera.z);
			renderShape(drawPose, consumer, cameraRelative, KeybindCallbacks.getInstance().getTraderGlowColor());
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
		//? if traderWireframe12111 || traderWireframeMc26 {
		SHAPE_RENDERER.renderShape(drawPose, consumer, Shapes.create(cameraRelative), 0.0D, 0.0D, 0.0D, color,
				LINE_WIDTH);
		//?}
	}
	//?}
	//?}
}
