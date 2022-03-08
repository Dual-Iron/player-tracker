package tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Utils {
    private static final Box BOX_INF = Box.of(Vec3d.ZERO, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY);

    private static final Predicate<Entity> isTrackable(PlayerEntity p) {
        return e -> {
            if (e instanceof PlayerEntity && !p.isTeammate(e)) {
                return true;
            }
            if (EntityType.getId(e.getType()).toString() == "requiem:player_shell") {
                for (var entry : e.getDataTracker().getAllEntries()) {
                    TrackerMod.LOGGER.info(entry.toString());
                }
            }
            return false;
        };
    }

    public static LivingEntity findTrackable(PlayerEntity plr, World world) {
        var closest = getClosestTrackable(plr, world);
        if (closest == null) {
            return getRandomTrackable(plr, world.getServer());
        }
        return closest;
    }

    private static LivingEntity getRandomTrackable(PlayerEntity plr, MinecraftServer server) {
        for (var world : server.getWorlds()) {
            for (var entity : world.getEntitiesByClass(LivingEntity.class, Utils.BOX_INF, Utils.isTrackable(plr))) {
                return entity;
            }
        }
        return null;
    }

    private static LivingEntity getClosestTrackable(PlayerEntity plr, World world) {
        final var entities = from(world.getOtherEntities(plr, Utils.BOX_INF, Utils.isTrackable(plr)));
        final var predicate = TargetPredicate.createAttackable().ignoreVisibility();
        return world.getClosestEntity(entities, predicate, plr, plr.getX(), plr.getY(), plr.getZ());
    }

    private static List<LivingEntity> from(List<Entity> entities) {
        ArrayList<LivingEntity> livingEntities = new ArrayList<>();
        for (var entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                livingEntities.add(livingEntity);
            }
        }
        return livingEntities;
    }
}
