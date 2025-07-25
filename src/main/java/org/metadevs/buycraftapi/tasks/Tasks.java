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

        Bukkit.getAsyncScheduler().runAtFixedRate(placeholderapi, scheduledTask -> {
            if(!buyCraftAPI.isRegistered()) {
                scheduledTask.cancel();
                return;
            }
            task.run();
        }, 0L, 1L, TimeUnit.HOURS);
    }
}
