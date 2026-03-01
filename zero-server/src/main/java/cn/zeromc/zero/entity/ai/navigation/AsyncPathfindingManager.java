package cn.zeromc.zero.entity.ai.navigation;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import cn.zeromc.zero.PurpurConfig;

public class AsyncPathfindingManager {
    private static final int DEFAULT_THREAD_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    private static final int MAX_THREAD_COUNT = 8;
    private static final int DEFAULT_QUEUE_CAPACITY = 100;
    
    private ExecutorService executorService;
    private BlockingQueue<Runnable> mainThreadQueue;
    private volatile boolean running;
    
    public static AsyncPathfindingManager INSTANCE;
    
    public static void init() {
        if (INSTANCE == null) {
            INSTANCE = new AsyncPathfindingManager();
        }
    }
    
    public static void shutdown() {
        if (INSTANCE != null) {
            INSTANCE.stop();
        }
    }
    
    private AsyncPathfindingManager() {
        int threadCount = Math.min(Math.max(1, PurpurConfig.asyncPathfindingThreadCount), MAX_THREAD_COUNT);
        int queueCapacity = Math.max(10, PurpurConfig.asyncPathfindingQueueCapacity);
        
        this.executorService = new ThreadPoolExecutor(
            threadCount,
            threadCount,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new NamedThreadFactory("AsyncPathfinding-"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        this.mainThreadQueue = new LinkedBlockingQueue<>();
        this.running = true;
        
        // Start main thread processor
        Thread mainThreadProcessor = new Thread(this::processMainThreadQueue, "AsyncPathfinding-MainThreadProcessor");
        mainThreadProcessor.setDaemon(true);
        mainThreadProcessor.start();
    }
    
    public <T> CompletableFuture<T> submit(Callable<T> task) {
        if (!running) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, executorService);
    }
    
    public void runOnMainThread(Runnable task) {
        if (!running) return;
        
        try {
            mainThreadQueue.put(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void processMainThreadQueue() {
        while (running) {
            try {
                Runnable task = mainThreadQueue.take();
                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Zero"), task);
            } catch (InterruptedException e) {
                if (!running) break;
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void stop() {
        running = false;
        
        // Shutdown executor service
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Clear main thread queue
        mainThreadQueue.clear();
    }
    
    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(0);
        
        public NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, prefix + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
