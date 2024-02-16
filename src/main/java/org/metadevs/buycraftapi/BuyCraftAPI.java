package org.metadevs.buycraftapi;

import lombok.Getter;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import me.clip.placeholderapi.metrics.bukkit.Metrics;
import me.clip.placeholderapi.metrics.charts.MultiLineChart;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.metadevs.buycraftapi.config.ConfigManager;
import org.metadevs.buycraftapi.data.Request;
import org.metadevs.buycraftapi.payments.Query;
import org.metadevs.buycraftapi.placeholders.Placeholders;
import org.metadevs.buycraftapi.providers.BuyCraftXProvider;
import org.metadevs.buycraftapi.providers.Provider;
import org.metadevs.buycraftapi.providers.TebexProvider;
import org.metadevs.buycraftapi.tasks.Tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


@Getter
public class BuyCraftAPI extends PlaceholderExpansion implements Taskable, Configurable {

    private Permission perms = null;
    private Request request;
    private Placeholders placeholdersManager;
    private Query query;
    private Logger logger;
    private Provider provider;
    private ConfigManager configManager;

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
        configManager = new ConfigManager(this);

        final String key = provider.getKey();

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
        return "buycraftapi";
    }


    public @NotNull String getVersion() {
        try {
            return getClass().getPackage().getImplementationVersion();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error while getting version", e);
        }
        return "0.0.0";
    }

    @Override
    public String onPlaceholderRequest(Player p, @NotNull String identifier) {
        return placeholdersManager.onPlaceholderRequest(p, identifier);
    }


    private void vaultHook() {
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
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
        new Tasks(this, getPlaceholderAPI());

        request = new Request(provider.getKey(), this);

        query = new Query(this);

        int pluginId = 10173;
        final Metrics metrics = new Metrics(getPlaceholderAPI(), pluginId);

        metrics.addCustomChart(new MultiLineChart("players_and_servers", () -> {
            HashMap<String, Integer> valueMap = new HashMap<>();
            valueMap.put("servers", 1);
            valueMap.put("players", Bukkit.getOnlinePlayers().size());
            return valueMap;
        }));

        vaultHook();
        placeholdersManager = new Placeholders(this);
    }

    @Override
    public void stop() {
        query.close();
    }

    @Override
    public Map<String, Object> getDefaults() {
        return configManager.getDefaults();
    }
}
