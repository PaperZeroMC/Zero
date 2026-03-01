package cn.zeromc.zero.chunk;

import cn.zeromc.zero.PurpurConfig;
import ca.spottedleaf.concurrentutil.util.Priority;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.concurrent.*;
import java.util.function.Consumer;

public class AsyncChunkGenerationManager {
    private static final int DEFAULT_THREAD_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    private static final int MAX_THREAD_COUNT = 16;
    private static final int DEFAULT_QUEUE_CAPACITY = 1000;
    
    private ExecutorService executorService;
    private BlockingQueue<ChunkGenerationTask> taskQueue;
    private volatile boolean running;
    
    public static AsyncChunkGenerationManager INSTANCE;
    
    public static void init() {
        if (INSTANCE == null) {
            INSTANCE = new AsyncChunkGenerationManager();
        }
    }
    
    public static void shutdown() {
        if (INSTANCE != null) {
            INSTANCE.stop();
        }
    }
    
    private AsyncChunkGenerationManager() {
        int threadCount = Math.min(Math.max(1, PurpurConfig.asyncChunkGenerationThreadCount), MAX_THREAD_COUNT);
        int queueCapacity = Math.max(100, PurpurConfig.asyncChunkGenerationQueueCapacity);
        
        this.taskQueue = new PriorityBlockingQueue<>(queueCapacity, (a, b) -> b.priority.ordinal() - a.priority.ordinal());
        this.executorService = new ThreadPoolExecutor(
            threadCount,
            threadCount,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("AsyncChunkGen-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        this.running = true;
        
        // Start task processing threads
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(this::processTasks);
        }
    }
    
    public void submitTask(int chunkX, int chunkZ, ChunkStatus targetStatus, Priority priority, Consumer<ChunkAccess> callback) {
        if (!running) {
            return;
        }
        
        ChunkGenerationTask task = new ChunkGenerationTask(chunkX, chunkZ, targetStatus, priority, callback);
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void processTasks() {
        while (running) {
            try {
                ChunkGenerationTask task = taskQueue.take();
                executeTask(task);
            } catch (InterruptedException e) {
                if (!running) break;
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void executeTask(ChunkGenerationTask task) {
        try {
            // Get the server level for the chunk
            // Note: This is a simplification - in reality, we need to get the correct world for the chunk
            net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
            for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                // Try to generate the chunk in this level
                ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler scheduler =
                    ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel) level).moonrise$getChunkTaskScheduler();

                // Schedule the chunk load/generation
                scheduler.scheduleChunkLoad(task.chunkX, task.chunkZ, task.targetStatus, true, task.priority, (chunk) -> {
                    if (task.callback != null) {
                        task.callback.accept(chunk);
                    }
                });

                // For now, we'll just process one world
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (task.callback != null) {
                task.callback.accept(null);
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
        
        // Clear task queue
        taskQueue.clear();
    }

    private record ChunkGenerationTask(int chunkX, int chunkZ, ChunkStatus targetStatus, Priority priority,
                                       Consumer<ChunkAccess> callback) {
    }
}
