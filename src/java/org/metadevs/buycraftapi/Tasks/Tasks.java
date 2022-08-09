package org.metadevs.buycraftapi.Tasks;

import org.metadevs.buycraftapi.BuyAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Tasks {

    private final BuyAPI buyAPI;
    private final Plugin placeholderapi;
    private final PlaceholderAPIPlugin papi;


    public Tasks(BuyAPI buyAPI, Plugin placeholderapi) {
        this.buyAPI = buyAPI;
        this.placeholderapi = placeholderapi;
        papi = (PlaceholderAPIPlugin) placeholderapi;
        loadAPITask();
    }


    public void loadAPITask(){
        buyAPI.getLogger().info("Loading API Tasks...");
        new BukkitRunnable() {
            @Override
            public void run() {
                buyAPI.getQuery().loadPayments().whenComplete((success, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace();
                        cancel();
                    }

                    if (success) {
                        buyAPI.getLogger().info("Successfully loaded payments");
                    } else {
                        buyAPI.getLogger().info("Failed to load payments");
                        cancel();
                    }
                });
            }
        }.runTaskTimer(placeholderapi, 0L, 20*60*10);
    }




}
