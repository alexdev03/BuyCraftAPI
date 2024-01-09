package org.metadevs.buycraftapi;

import lombok.Getter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import net.milkbowl.vault.Vault;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.metadevs.buycraftapi.data.Request;
import org.metadevs.buycraftapi.metrics.Metrics;
import org.metadevs.buycraftapi.payments.Query;
import org.metadevs.buycraftapi.placeholders.Placeholders;
import org.metadevs.buycraftapi.providers.BuyCraftXProvider;
import org.metadevs.buycraftapi.providers.Provider;
import org.metadevs.buycraftapi.providers.TebexProvider;
import org.metadevs.buycraftapi.tasks.Tasks;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


@Getter
public class BuyCraftAPI extends PlaceholderExpansion implements Taskable {

    private Vault vault;
    private Permission perms = null;
    private Request request;
    private Placeholders placeholdersIstance;
    private Query query;
    private Logger logger;
    private Provider provider;

    private Provider getProvider() {
        if (Bukkit.getPluginManager().isPluginEnabled("BuycraftX")) {
            getLogger().log(Level.INFO, "BuycraftX found! Using it...");
            return new BuyCraftXProvider();
        } else if (Bukkit.getPluginManager().isPluginEnabled("Tebex")) {
            getLogger().log(Level.INFO, "Tebex found! Using it...");
            return new TebexProvider();
        } else {
            throw new IllegalStateException("No supported plugin found");
        }
    }


    @Override
    public boolean canRegister() {
        logger = Logger.getLogger("BuycraftAPI");
        provider = getProvider();

        final String key = provider.getKey();
        final JavaPlugin placeholderAPI = (JavaPlugin) Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI");

        if (placeholderAPI == null) {
            throw new IllegalStateException("Could not find PlaceholderAPI!");
        }

        if (key == null || key.isEmpty() || key.equals("INVALID")) {
            logger.severe("Server key is not set. Please set it in the BuyCraft/Tebex config.yml");
            return false;
        }

        return true;
    }

    public @NotNull
    String getAuthor() {
        return "AlexDev_";
    }


    public @NotNull String getIdentifier() {
        return "buycraftAPI";
    }


    public @NotNull String getVersion() {
        return "4.6";
    }

    @Override
    public String onPlaceholderRequest(Player p, @NotNull String identifier) {
        return placeholdersIstance.onPlaceholderRequest(p, identifier);
    }


    private void vaultHook() {
        if (vault != null) {
            if (setupPermissions()) {
                getLogger().log(Level.INFO, "Successfully hooked into Vault for BuyCraftAPI v" + getVersion());
            }
        }
    }


    private boolean setupPermissions() {
        try {
            RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
            if (rsp == null) return false;
            perms = rsp.getProvider();
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    public void start() {
        final JavaPlugin placeholderAPI = (JavaPlugin) Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI");

        if (placeholderAPI == null) {
            throw new IllegalStateException("Could not find PlaceholderAPI!");
        }

        new Tasks(this, placeholderAPI);

        request = new Request(provider.getKey(), this);

        query = new Query(this);

        int pluginId = 10173;
        final Metrics metrics = new Metrics(placeholderAPI, pluginId);



        if (metrics.getMetricsBase().isEnabled())
            getLogger().log(Level.INFO, "Successfully connected to bstats");
        else
            getLogger().log(Level.WARNING, "Could not connect to bstats! Enable it in bstats folder in plugins folder.");

        metrics.addCustomChart(new Metrics.MultiLineChart("players_and_servers", () -> {
            HashMap<String, Integer> valueMap = new HashMap<>();
            valueMap.put("servers", 1);
            valueMap.put("players", Bukkit.getOnlinePlayers().size());
            return valueMap;
        }));


        vault = (Vault) Bukkit.getServer().getPluginManager().getPlugin("Vault");

        vaultHook();

        placeholdersIstance = new Placeholders(this);
    }

    @Override
    public void stop() {
        query.close();
    }
}
