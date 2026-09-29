package dev.stormbaton;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class StormBatonMod implements ModInitializer {
    public static final String MOD_ID = "storm_baton";
    public static final Item STORM_BATON = registerItem();

    private static final List<PendingStrike> PENDING_STRIKES = new ArrayList<>();

    @Override
    public void onInitialize() {
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (level.isClientSide()
                    || hand != InteractionHand.MAIN_HAND
                    || !player.getMainHandItem().is(STORM_BATON)
                    || !(target instanceof LivingEntity victim)
                    || target instanceof Player) {
                return InteractionResult.PASS;
            }

            if (player.isCrouching()) {
                pullTogether(player, victim);
                scheduleStrike(victim, level.dimension(), 2);
            } else {
                launch(victim, player.getLookAngle());
                if (player.isSprinting()) {
                    pullNearbyMobs(player, victim, (ServerLevel) level);
                }
                scheduleStrike(victim, level.dimension(), 4);
            }

            return InteractionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<PendingStrike> strikes = PENDING_STRIKES.iterator();
            while (strikes.hasNext()) {
                PendingStrike strike = strikes.next();
                if (--strike.ticksRemaining > 0) {
                    continue;
                }

                ServerLevel level = server.getLevel(strike.dimension);
                Entity entity = level == null ? null : level.getEntity(strike.targetId);
                if (entity instanceof LivingEntity target && target.isAlive()) {
                        Entity bolt = BuiltInRegistries.ENTITY_TYPE
                            .getValue(Identifier.withDefaultNamespace("lightning_bolt"))
                            .create(level, EntitySpawnReason.TRIGGERED);
                    if (bolt != null) {
                        bolt.setPos(target.getX(), target.getY(), target.getZ());
                        level.addFreshEntity(bolt);
                    }
                }
                strikes.remove();
            }
        });
    }

    private static Item registerItem() {
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "storm_baton");
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, id,
            new Item(new Item.Properties().setId(itemKey).stacksTo(1)));
    }

    private static void launch(LivingEntity target, Vec3 lookAngle) {
        Vec3 launchVelocity = lookAngle.scale(1.5).add(0.0, 1.0, 0.0);
        target.setDeltaMovement(launchVelocity.x, Math.max(0.85, launchVelocity.y), launchVelocity.z);
    }

    private static void pullNearbyMobs(Player attacker, LivingEntity target, ServerLevel level) {
        AABB radius = target.getBoundingBox().inflate(9.0, 5.0, 9.0);
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, radius,
                entity -> entity != attacker && entity != target && !(entity instanceof Player) && entity.isAlive())) {
            Vec3 towardTarget = target.position().subtract(nearby.position());
            if (towardTarget.lengthSqr() < 0.0001) {
                continue;
            }
            nearby.setDeltaMovement(towardTarget.normalize().scale(1.8).add(0.0, 0.25, 0.0));
        }
    }

    private static void pullTogether(Player player, LivingEntity target) {
        Vec3 towardTarget = target.position().subtract(player.position());
        if (towardTarget.lengthSqr() < 0.0001) {
            return;
        }
        Vec3 direction = towardTarget.normalize();
        player.setDeltaMovement(direction.scale(1.7).add(0.0, 0.18, 0.0));
        target.setDeltaMovement(direction.scale(-1.7).add(0.0, 0.18, 0.0));
    }

    private static void scheduleStrike(LivingEntity target, ResourceKey<Level> dimension, int ticks) {
        PENDING_STRIKES.add(new PendingStrike(target.getUUID(), dimension, ticks));
    }

    private static final class PendingStrike {
        private final UUID targetId;
        private final ResourceKey<Level> dimension;
        private int ticksRemaining;

        private PendingStrike(UUID targetId, ResourceKey<Level> dimension, int ticksRemaining) {
            this.targetId = targetId;
            this.dimension = dimension;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
