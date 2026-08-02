package com.github.sebseb7.autotrade.event;

import com.github.sebseb7.autotrade.AutoTrade;
import com.github.sebseb7.autotrade.config.Configs;
import java.util.Vector;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
//? if npcSplit {
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
//?}
//? if npcFlat {
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
//?}
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Finds nearby villagers/wandering traders and performs the synthetic interact used to open trades.
 */
final class TraderInteractor {
	private static final int LOS_TIMEOUT_TICKS = 60;

	private final AutoTradeTickState state;
	private final Vector<Entity> villagersInRange = new Vector<>();
	private int rotatingTargetId = -1;
	private int rotatingTargetTicks = 0;

	TraderInteractor(AutoTradeTickState state) {
		this.state = state;
	}

	int getVillagerActive() {
		return state.villagerActive;
	}

	boolean findAndInteract(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			return false;
		}
		boolean found = false;
		Vector<Entity> newVillagersInRange = new Vector<>(villagersInRange);
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity instanceof Villager || entity instanceof WanderingTrader) {
				double interactDist = Configs.Generic.INTERACT_DISTANCE.getDoubleValue();
				if (entity.distanceToSqr(mc.player) < (interactDist * interactDist)) {
					if (!found) {
						if (!newVillagersInRange.contains(entity)) {
							found = true;
							Vec3 aimPoint = firstVisiblePoint(mc, entity);
							if (rotatingTargetId != entity.getId()) {
								rotatingTargetId = entity.getId();
								rotatingTargetTicks = 0;
								if (Configs.Generic.TURN_HEAD_BEFORE_INTERACT.getBooleanValue()) {
									Vec3 lookAt = aimPoint != null ? aimPoint : entity.getEyePosition();
									Vec3 eyePos = mc.player.getEyePosition();
									Vec3 delta = lookAt.subtract(eyePos);
									double horiz = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
									float yaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
									float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, horiz));
									mc.player.setYRot(yaw);
									mc.player.setXRot(pitch);
									mc.player.yHeadRot = yaw;
								}
							} else {
								rotatingTargetTicks++;
							}
							if (aimPoint == null) {
								if (rotatingTargetTicks > LOS_TIMEOUT_TICKS) {
									newVillagersInRange.add(entity);
									rotatingTargetId = -1;
								}
								break;
							}
							newVillagersInRange.add(entity);
							rotatingTargetId = -1;
							EntityHitResult ehr = new EntityHitResult(entity, aimPoint);
							BaritoneHelper.pauseMovement();
							AutoTrade.autoInteracting = true;
							try {
								//? if mc26 {
								mc.gameMode.interact(mc.player, entity, ehr, InteractionHand.MAIN_HAND);
								//?} else {
								mc.gameMode.interactAt(mc.player, entity, ehr, InteractionHand.MAIN_HAND);
								mc.gameMode.interact(mc.player, entity, InteractionHand.MAIN_HAND);
								//?}
							} finally {
								AutoTrade.autoInteracting = false;
							}
							mc.player.swing(InteractionHand.MAIN_HAND);
							state.postMerchantInventorySyncTicks = 0;
							state.voidDelay = Configs.Generic.VOID_TRADING_DELAY.getIntegerValue();
							state.villagerActive = entity.getId();
							break;
						}
					}
				}
			}
		}
		for (Entity entity : villagersInRange) {
			double removeDist = Configs.Generic.REMOVE_DISTANCE.getDoubleValue();
			if (entity.distanceToSqr(mc.player) >= (removeDist * removeDist)) {
				newVillagersInRange.remove(entity);
			}
		}
		villagersInRange.clear();
		villagersInRange.addAll(newVillagersInRange);
		return found;
	}

	static Entity findEntityById(Minecraft mc, int entityId) {
		if (mc.level == null) {
			return null;
		}
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e.getId() == entityId) {
				return e;
			}
		}
		return null;
	}

	/**
	 * Returns the first point on {@code target}'s bounding box (eyes, head,
	 * chest, feet, or one of the four upper corners) that has an unobstructed
	 * block ray-cast from the player's eyes; {@code null} when the entire body
	 * is occluded.
	 */
	private static Vec3 firstVisiblePoint(Minecraft mc, Entity target) {
		Vec3 eye = mc.player.getEyePosition();
		AABB box = target.getBoundingBox();
		double cx = (box.minX + box.maxX) * 0.5;
		double cz = (box.minZ + box.maxZ) * 0.5;
		double buffer = Configs.Generic.VISIBLE_POINT_BUFFER.getDoubleValue();
		Vec3[] candidates = {
				target.getEyePosition(),
				new Vec3(cx, (box.minY + box.maxY) * 0.5, cz),
				new Vec3(cx, box.maxY - 0.05, cz),
				new Vec3(cx, box.minY + 0.1, cz),
				new Vec3(box.minX + 0.05, box.maxY - 0.2, box.minZ + 0.05),
				new Vec3(box.maxX - 0.05, box.maxY - 0.2, box.minZ + 0.05),
				new Vec3(box.minX + 0.05, box.maxY - 0.2, box.maxZ - 0.05),
				new Vec3(box.maxX - 0.05, box.maxY - 0.2, box.maxZ - 0.05),
		};
		for (Vec3 p : candidates) {
			BlockHitResult hit = mc.level.clip(new ClipContext(eye, p, ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE, mc.player));
			if (hit.getType() == HitResult.Type.MISS
					|| eye.distanceToSqr(hit.getLocation()) >= eye.distanceToSqr(p) - buffer) {
				return p;
			}
		}
		return null;
	}
}
