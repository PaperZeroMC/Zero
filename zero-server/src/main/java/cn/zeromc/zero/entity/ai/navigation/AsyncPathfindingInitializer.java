package cn.zeromc.zero.entity.ai.navigation;

import cn.zeromc.zero.PurpurConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.Server;

public class AsyncPathfindingInitializer implements Listener {
    
    public static void init(Server server) {
        server.getPluginManager().registerEvents(new AsyncPathfindingInitializer(), server.getPluginManager().getPlugin("Zero"));
    }
    
    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (PurpurConfig.asyncPathfindingEnabled) {
            AsyncPathfindingManager.init();
        }
    }
    
    public static void shutdown() {
        AsyncPathfindingManager.shutdown();
    }
}
