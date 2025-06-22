package org.metadevs.buycraftapi.tasks;

import org.metadevs.buycraftapi.BuyCraftAPI;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import java.util.concurrent.TimeUnit;

public class Tasks {

    private final BuyCraftAPI buyCraftAPI;
    private final Plugin placeholderapi;

    public Tasks(BuyCraftAPI buyCraftAPI, Plugin placeholderapi) {
        this.buyCraftAPI = buyCraftAPI;
        this.placeholderapi = placeholderapi;
        loadAPITask();
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.FoliaGlobalRegionScheduler");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void loadAPITask() {
        buyCraftAPI.getLogger().info("Loading API Tasks...");
        
        Runnable task = () -> {
            if(!buyCraftAPI.isRegistered()) {
                return;
            }
            
            if (buyCraftAPI.getQuery() == null) {
                buyCraftAPI.getLogger().warning("Query not initialized yet, skipping payment load");
                return;
            }

            long start = System.currentTimeMillis();
            buyCraftAPI.getLogger().info("Loading payments...");
            buyCraftAPI.getQuery().loadPayments().thenAccept(success -> {
                if (success) {
                    long end = System.currentTimeMillis();
                    buyCraftAPI.getLogger().info("Successfully loaded payments in " + (end - start) + "ms");
                } else {
                    buyCraftAPI.getLogger().info("Failed to load payments");
                }
            }).exceptionally(throwable -> {
                buyCraftAPI.getLogger().log(java.util.logging.Level.SEVERE, "Failed to load payments", throwable);
                return null;
            });
        };

        if (isFolia()) {
            scheduleFoliaTask(task);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(placeholderapi, task, 0L, 20L * 60L * 60L);
        }
    }

    private void scheduleFoliaTask(Runnable task) {
        try {
            Object server = Bukkit.getServer();
            Object asyncScheduler = server.getClass().getMethod("getAsyncScheduler").invoke(server);
            java.util.function.Consumer<Object> taskConsumer = scheduledTask -> {
                if(!buyCraftAPI.isRegistered()) {
                    try {
                        scheduledTask.getClass().getMethod("cancel").invoke(scheduledTask);
                    } catch (Exception e) {
                        buyCraftAPI.getLogger().warning("Failed to cancel scheduled task: " + e.getMessage());
                    }
                    return;
                }
                task.run();
            };
            asyncScheduler.getClass()
                .getMethod("runAtFixedRate", Plugin.class, java.util.function.Consumer.class, long.class, long.class, TimeUnit.class)
                .invoke(asyncScheduler, placeholderapi, taskConsumer, 0L, 1L, TimeUnit.HOURS);
                
        } catch (Exception e) {
            buyCraftAPI.getLogger().severe("Failed to schedule task on Folia, falling back to legacy scheduler: " + e.getMessage());
            fallbackToLegacyScheduler(task);
        }
    }

    private void fallbackToLegacyScheduler(Runnable task) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(placeholderapi, task, 0L, 20L * 60L * 60L);
    }
}
