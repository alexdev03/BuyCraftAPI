package org.metadevs.buycraftapi.tasks;

import org.metadevs.buycraftapi.BuyCraftAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Tasks {

    private final BuyCraftAPI buyCraftAPI;
    private final Plugin placeholderapi;
    private final PlaceholderAPIPlugin papi;


    public Tasks(BuyCraftAPI buyCraftAPI, Plugin placeholderapi) {
        this.buyCraftAPI = buyCraftAPI;
        this.placeholderapi = placeholderapi;
        papi = (PlaceholderAPIPlugin) placeholderapi;
        loadAPITask();
    }


    public void loadAPITask() {
        buyCraftAPI.getLogger().info("Loading API Tasks...");
        new BukkitRunnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                buyCraftAPI.getQuery().loadPayments().thenAccept(success -> {
                    if (success) {
                        long end = System.currentTimeMillis();
                        buyCraftAPI.getLogger().info("Successfully loaded payments in " + (end - start) + "ms");
                    } else {
                        buyCraftAPI.getLogger().info("Failed to load payments");
                        cancel();
                    }
                }).exceptionally(throwable -> {
                    throwable.printStackTrace();
                    cancel();
                    return null;
                });
            }
        }.runTaskTimerAsynchronously(placeholderapi, 0L, 20 * 60 * 60);
    }


}
