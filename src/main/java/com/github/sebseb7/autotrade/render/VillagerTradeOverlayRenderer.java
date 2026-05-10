package com.github.sebseb7.autotrade.render;

import com.github.sebseb7.autotrade.config.Configs;
import com.mojang.blaze3d.vertex.PoseStack;
//? if mc26 {
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
//? if mc26 {
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
//?}
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a compact summary of each villager's known trades above their head.
 *
 * <p>
 * Trade data comes from {@link VillagerTradeCache}, which is populated when the
 * mod opens a merchant screen. Villagers whose trades haven't been seen yet
 * show nothing.
 */
public final class VillagerTradeOverlayRenderer {

	/** World-space scale of the text (vanilla name-tags use ~0.025). */
	private static final float TEXT_SCALE = 0.02F;

	/** Text background colour (semi-transparent dark). */
	private static final int BG_COLOR = 0x80000000;

	/** Normal trade text colour (white). */
	private static final int TEXT_COLOR = 0xFFFFFFFF;

	/** Depleted trade text colour (grey/red). */
	private static final int DEPLETED_COLOR = 0xFFFF6666;

	private VillagerTradeOverlayRenderer() {
	}

	public static void register() {
		//? if mc26 {
		LevelRenderEvents.COLLECT_SUBMITS.register(VillagerTradeOverlayRenderer::renderLevel);
		//?}
	}

	//? if mc26 {
	private static void renderLevel(LevelRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			return;
		}
		if (!Configs.Generic.SHOW_TRADES.getBooleanValue()) {
			return;
		}

		Font font = mc.font;
		Vec3 camera = mc.gameRenderer.getMainCamera().position();
		float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader)) {
				continue;
			}
			if (entity.distanceToSqr(mc.player) > 64.0 * 64.0) {
				continue;
			}

			MerchantOffers offers = VillagerTradeCache.get(entity.getUUID());
			if (offers == null || offers.isEmpty()) {
				continue;
			}

			List<TradeLineEntry> lines = buildTradeLines(offers);
			if (lines.isEmpty()) {
				continue;
			}

			double x = Mth.lerp(tickDelta, entity.xOld, entity.getX()) - camera.x;
			double y = Mth.lerp(tickDelta, entity.yOld, entity.getY()) - camera.y;
			double z = Mth.lerp(tickDelta, entity.zOld, entity.getZ()) - camera.z;

			float baseY = entity.getBbHeight() + 0.6F;

			PoseStack poseStack = new PoseStack();
			poseStack.pushPose();
			poseStack.translate(x, y + baseY, z);

			poseStack.mulPose(mc.gameRenderer.getMainCamera().rotation());
			poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

			for (int i = 0; i < lines.size(); i++) {
				TradeLineEntry entry = lines.get(i);
				float lineOffsetY = -(lines.size() - 1 - i) * (font.lineHeight + 2);

				Matrix4f matrix = poseStack.last().pose();
				matrix = new Matrix4f(matrix);
				matrix.translate(0, lineOffsetY, 0);

				int textWidth = font.width(entry.text);
				float textX = -textWidth / 2.0F;

				context.submitNodeCollector().submitText(poseStack, textX, 0,
						net.minecraft.network.chat.Component.literal(entry.text).getVisualOrderText(), false,
						Font.DisplayMode.NORMAL, entry.color, 0xF000F0, BG_COLOR, 0);
			}

			poseStack.popPose();
		}
	}

	//?}

	private static List<TradeLineEntry> buildTradeLines(MerchantOffers offers) {
		List<TradeLineEntry> lines = new ArrayList<>();
		for (int i = 0; i < offers.size(); i++) {
			MerchantOffer offer = offers.get(i);
			StringBuilder sb = new StringBuilder();

			if (!offer.getCostA().isEmpty()) {
				sb.append(offer.getCostA().getCount()).append("× ").append(offer.getCostA().getHoverName().getString());
			}

			if (!offer.getCostB().isEmpty()) {
				if (sb.length() > 0) {
					sb.append(" + ");
				}
				sb.append(offer.getCostB().getCount()).append("× ").append(offer.getCostB().getHoverName().getString());
			}

			sb.append(" → ");

			sb.append(offer.getResult().getCount()).append("× ").append(offer.getResult().getHoverName().getString());

			int remaining = offer.getMaxUses() - offer.getUses();
			sb.append("  (").append(remaining).append("/").append(offer.getMaxUses()).append(")");

			boolean depleted = remaining <= 0;
			lines.add(new TradeLineEntry(sb.toString(), depleted ? DEPLETED_COLOR : TEXT_COLOR));
		}
		return lines;
	}

	private record TradeLineEntry(String text, int color) {
	}
}
