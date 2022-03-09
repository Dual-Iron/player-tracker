package tracker;

import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.TypeFilter;

public class Utils {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("player-tracker");

    private static final TargetPredicate isValidTarget = TargetPredicate.createAttackable().ignoreVisibility();

    private static final Predicate<Entity> isTrackable(PlayerEntity p) {
        return e -> {
            if (p != e) {
                if (EntityType.getId(e.getType()).toString().equals("requiem:player_shell")) {
                    var compound = e.writeNbt(new NbtCompound());
                    var playername = compound.getCompound("automatone:display_profile").getString("Name");
                    if (playername.isEmpty()) {
                        return false;
                    }
                    var team = p.getServer().getScoreboard().getPlayerTeam(playername);
                    return !p.isTeamPlayer(team);
                }
                if (e instanceof PlayerEntity) {
                    return !p.isTeammate(e);
                }
            }
            return false;
        };
    }

    private static LivingEntity getRandomTrackable(PlayerEntity plr, MinecraftServer server) {
        for (var world : server.getWorlds()) {
            for (var entity : world.getEntitiesByType(TypeFilter.instanceOf(LivingEntity.class), isTrackable(plr))) {
                if (isValidTarget.test(plr, entity)) {
                    return entity;
                }
            }
        }
        return null;
    }

    private static LivingEntity getClosestTrackable(PlayerEntity plr, ServerWorld serverWorld) {
        final var entities = serverWorld.getEntitiesByType(TypeFilter.instanceOf(LivingEntity.class), isTrackable(plr));

        return serverWorld.getClosestEntity(entities, isValidTarget, plr, plr.getX(), plr.getY(), plr.getZ());
    }

    private static void setTrackerData(LivingEntity target, ItemStack tracker) {
        tracker.setCustomName(Text.of("Player Tracker").shallowCopy().setStyle(Style.EMPTY.withColor(0xFFFF55)));

        var nbt = tracker.getNbt();
        nbt.put(CompassItem.LODESTONE_POS_KEY, NbtHelper.fromBlockPos(target.getBlockPos()));
        nbt.putString(CompassItem.LODESTONE_DIMENSION_KEY, target.world.getRegistryKey().getValue().toString());
        nbt.putBoolean(CompassItem.LODESTONE_TRACKED_KEY, false);
    }

    public static LivingEntity findTrackable(PlayerEntity plr, ServerWorld world) {
        var closest = getClosestTrackable(plr, world);
        if (closest == null) {
            return getRandomTrackable(plr, world.getServer());
        }
        return closest;
    }

    public static void giveTracker(PlayerEntity plr, LivingEntity target, ItemStack compass, ItemStack lapis) {
        // Decrement stacks accordingly.
        if (!plr.isCreative()) {
            lapis.decrement(1);
            if (compass.getCount() > 1) {
                compass.decrement(1);
            }
        }

        // Overwrite single compasses instead of giving a new single compass.
        if (!plr.isCreative() && compass.getCount() == 1) {
            setTrackerData(target, compass);
        } else {
            var tracker = new ItemStack(Items.COMPASS, 1);
            setTrackerData(target, tracker);
            if (!plr.getInventory().insertStack(tracker)) {
                plr.dropItem(tracker, false);
            }
        }
    }
}
