package cn.zeromc.zero.task;

import cn.zeromc.zero.PurpurConfig;
import cn.zeromc.zero.util.MinecraftInternalPlugin;
import io.papermc.paper.FeatureHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class DynamicSimulationDistanceTask extends BukkitRunnable {
    private static DynamicSimulationDistanceTask instance;
    private int currentSimulationDistance = -1;
    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = 100; // Check every 5 seconds (100 ticks)

    public static DynamicSimulationDistanceTask instance() {
        if (instance == null) {
            instance = new DynamicSimulationDistanceTask();
        }
        return instance;
    }

    @Override
    public void run() {
        if (!PurpurConfig.dynamicSimulationDistanceEnabled) {
            return;
        }

        if (++tickCounter < CHECK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        double mspt = Bukkit.getAverageTickTime();
        int newDistance = currentSimulationDistance;

        if (currentSimulationDistance == -1) {
            currentSimulationDistance = PurpurConfig.dynamicSimulationDistanceMax;
            newDistance = currentSimulationDistance;
        }

        if (mspt > PurpurConfig.dynamicSimulationDistanceThresholdMSPT) {
            if (currentSimulationDistance > PurpurConfig.dynamicSimulationDistanceMin) {
                newDistance = currentSimulationDistance - 1;
            }
        } else if (mspt < PurpurConfig.dynamicSimulationDistanceRecoveryMSPT) {
            if (currentSimulationDistance < PurpurConfig.dynamicSimulationDistanceMax) {
                newDistance = currentSimulationDistance + 1;
            }
        }

        if (newDistance != currentSimulationDistance) {
            Bukkit.getLogger().info("[Zero] Dynamic simulation distance: " + currentSimulationDistance + " -> " + newDistance);
            currentSimulationDistance = newDistance;
            updateSimulationDistance(newDistance);
        }
    }

    private void updateSimulationDistance(int distance) {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            ServerLevel nmsLevel = ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
            // Update world-wide simulation distance
            FeatureHooks.setSimulationDistance(nmsLevel, distance);
            
            // Update each player's simulation distance
            List<ServerPlayer> players = nmsLevel.players();
            for (ServerPlayer player : players) {
                FeatureHooks.setSimulationDistance(player, distance);
            }
        }
    }

    public void start() {
        this.runTaskTimer(new MinecraftInternalPlugin(), 1, 1);
    }
}
