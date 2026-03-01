package cn.zeromc.zero.entity.ai.navigation;

import cn.zeromc.zero.PurpurConfig;
import org.bukkit.Server;

public class AsyncPathfindingInitializer {
    
    public static void init(Server server) {
        if (PurpurConfig.asyncPathfindingEnabled) {
            AsyncPathfindingManager.init();
        }
    }
    
    public static void shutdown() {
        AsyncPathfindingManager.shutdown();
    }
}
