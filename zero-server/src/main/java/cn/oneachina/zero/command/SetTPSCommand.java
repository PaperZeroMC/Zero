package cn.oneachina.zero.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

public class SetTPSCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("settps")
            .requires((listener) -> listener.hasPermission(Permissions.COMMANDS_GAMEMASTER, "bukkit.command.settps"))
            .then(Commands.argument("entityType", StringArgumentType.string())
                .suggests((context, builder) -> {
                    // Tab complete for entity types
                    List<String> entityTypes = java.util.Arrays.asList(
                        "zombie", "skeleton", "creeper", "spider", "enderman",
                        "cow", "pig", "sheep", "chicken", "rabbit"
                    );
                    return SharedSuggestionProvider.suggest(entityTypes, builder);
                })
                .then(Commands.argument("tps", FloatArgumentType.floatArg(0.1f, 20.0f))
                    .executes((context) -> {
                        String entityTypeStr = StringArgumentType.getString(context, "entityType");
                        float tps = FloatArgumentType.getFloat(context, "tps");
                        return execute(context.getSource(), entityTypeStr, tps);
                    })
                )
            )
        );

        // Also register under zero: namespace
        dispatcher.register(Commands.literal("zero")
            .then(Commands.literal("settps")
                .requires((listener) -> listener.hasPermission(Permissions.COMMANDS_GAMEMASTER, "bukkit.command.settps"))
                .then(Commands.argument("entityType", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        // Tab complete for entity types
                        List<String> entityTypes = java.util.Arrays.asList(
                            "zombie", "skeleton", "creeper", "spider", "enderman",
                            "cow", "pig", "sheep", "chicken", "rabbit"
                        );
                        return SharedSuggestionProvider.suggest(entityTypes, builder);
                    })
                    .then(Commands.argument("tps", FloatArgumentType.floatArg(0.1f, 20.0f))
                        .executes((context) -> {
                            String entityTypeStr = StringArgumentType.getString(context, "entityType");
                            float tps = FloatArgumentType.getFloat(context, "tps");
                            return execute(context.getSource(), entityTypeStr, tps);
                        })
                    )
                )
            )
        );
    }

    private static int execute(CommandSourceStack sender, String entityTypeStr, float tps) {
        ServerLevel level = sender.getLevel();

        // Zero start - Simplified entity count based on target TPS
        // Calculate entity count proportional to TPS: count = tps * 5, clamped between 1 and 100.
        int entityCount = (int) (tps * 5);
        entityCount = Math.max(1, Math.min(100, entityCount));
        // Zero end

        // Get entity type
        EntityType<?> entityType = getEntityType(entityTypeStr);

        if (entityType == null) {
            sender.sendFailure(Component.literal("Invalid entity type: " + entityTypeStr));
            return 0;
        }

        // Spawn entities
        spawnEntities(sender, level, entityType, entityCount);

        sender.sendSuccess(net.kyori.adventure.text.Component.text("Spawned " + entityCount + " " + entityTypeStr + " entities. Target TPS: " + String.format(Locale.ROOT, "%.1f", tps)), false);
        return entityCount;
    }

    private static EntityType<?> getEntityType(String entityTypeStr) {
        // Simple mapping of entity type names to EntityType instances
        return switch (entityTypeStr.toLowerCase()) {
            case "zombie" -> EntityType.ZOMBIE;
            case "skeleton" -> EntityType.SKELETON;
            case "creeper" -> EntityType.CREEPER;
            case "spider" -> EntityType.SPIDER;
            case "enderman" -> EntityType.ENDERMAN;
            case "cow" -> EntityType.COW;
            case "pig" -> EntityType.PIG;
            case "sheep" -> EntityType.SHEEP;
            case "chicken" -> EntityType.CHICKEN;
            case "rabbit" -> EntityType.RABBIT;
            default -> null;
        };
    }

    private static void spawnEntities(CommandSourceStack sender, ServerLevel level, EntityType<?> entityType, int count) {
        Vec3 spawnPos = sender.getPosition();

        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = Math.sqrt(Math.random()) * 10;
            double x = spawnPos.x() + Math.cos(angle) * distance;
            double z = spawnPos.z() + Math.sin(angle) * distance;
            double y = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, new net.minecraft.core.BlockPos((int)x, 0, (int)z)).getY() + 1;

            net.minecraft.core.BlockPos blockPos = new net.minecraft.core.BlockPos((int)x, (int)(y - 1), (int)z);
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.isSolid()) {
                // Zero start - Correct entity creation based on summon command
                // Check if position is within world bounds
                if (!net.minecraft.world.level.Level.isInSpawnableBounds(blockPos)) {
                    continue; // Skip this spawn
                }

                // Create the entity with command spawn reason
                Entity entity = entityType.create(level, EntitySpawnReason.COMMAND);
                if (entity == null) {
                    continue;
                }

                // Set position and rotation
                entity.snapTo(x, y, z, entity.getYRot(), entity.getXRot());

                // If it's a mob, perform finalization spawn (sets equipment, AI, etc.)
                if (entity instanceof Mob mob) {
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.COMMAND, null);
                }

                // Add the entity to the world
                level.addFreshEntity(entity);
                // Zero end
            }
        }
    }
}
