package org.metadevs.buycraftapi.tasks;

import org.metadevs.buycraftapi.BuyCraftAPI;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
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

    private static boolean checkFolia() {
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

        boolean isFolia = checkFolia();
        
        if (isFolia) {
            try {
                Class<?> foliaAsyncScheduler = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
                Object asyncScheduler = Bukkit.getServer().getClass().getMethod("getAsyncScheduler").invoke(Bukkit.getServer());
                Class<?> scheduledTaskClass = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
                Class<?> consumerClass = Class.forName("java.util.function.Consumer");
                Object taskConsumer = java.lang.reflect.Proxy.newProxyInstance(
                    consumerClass.getClassLoader(),
                    new Class[]{consumerClass},
                    (proxy, method, args) -> {
                        if (method.getName().equals("accept")) {
                            Object scheduledTask = args[0];
                            if(!buyCraftAPI.isRegistered()) {
                                scheduledTask.getClass().getMethod("cancel").invoke(scheduledTask);
                                return null;
                            }
                            task.run();
                        }
                        return null;
                    }
                );
                
                foliaAsyncScheduler.getMethod("runAtFixedRate", Plugin.class, consumerClass, long.class, long.class, TimeUnit.class)
                    .invoke(asyncScheduler, placeholderapi, taskConsumer, 0L, 1L, TimeUnit.HOURS);
                    
            } catch (Exception e) {
                buyCraftAPI.getLogger().severe("Failed to schedule task on Folia, falling back to legacy scheduler: " + e.getMessage());
                fallbackToLegacyScheduler(task);
            }
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if(!buyCraftAPI.isRegistered()) {
                        cancel();
                        return;
                    }
                    task.run();
                }
            }.runTaskTimerAsynchronously(placeholderapi, 0L, 20L * 60L * 60L);
        }
    }

    private void fallbackToLegacyScheduler(Runnable task) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if(!buyCraftAPI.isRegistered()) {
                    cancel();
                    return;
                }
                task.run();
            }
        }.runTaskTimerAsynchronously(placeholderapi, 0L, 20L * 60L * 60L);
    }
}
