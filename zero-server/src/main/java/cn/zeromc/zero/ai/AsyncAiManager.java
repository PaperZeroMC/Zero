package cn.zeromc.zero.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.server.level.ServerLevel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncAiManager {
    private static final ExecutorService AI_EXECUTOR = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2)
    );
    private static final Set<Integer> SUBMITTED_ENTITIES = ConcurrentHashMap.newKeySet();

    public static void submitSensingTask(Mob mob) {
        if (!SUBMITTED_ENTITIES.add(mob.getId())) {
            return; // Already being processed
        }

        AI_EXECUTOR.submit(() -> {
            try {
                // Sensing tick usually reads world but doesn't modify it in a thread-unsafe way
                mob.getSensing().tick();
                
                // Brain sensors also do similar sensing tasks
                Brain<?> brain = mob.getBrain();
                if (brain != null) {
                    tickBrainSensors(brain, (ServerLevel) mob.level(), mob);
                }
            } catch (Exception e) {
                // Silently catch errors in async thread to avoid crashing the server
            } finally {
                SUBMITTED_ENTITIES.remove(mob.getId());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <E extends net.minecraft.world.entity.LivingEntity> void tickBrainSensors(Brain<E> brain, ServerLevel level, net.minecraft.world.entity.LivingEntity entity) {
        brain.tickSensors(level, (E) entity);
    }

    public static void shutdown() {
        AI_EXECUTOR.shutdown();
        try {
            if (!AI_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                AI_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            AI_EXECUTOR.shutdownNow();
        }
    }
}
