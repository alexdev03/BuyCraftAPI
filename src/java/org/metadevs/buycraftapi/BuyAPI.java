package org.metadevs.buycraftapi;

import lombok.Getter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.buycraft.plugin.bukkit.BuycraftPlugin;
import net.milkbowl.vault.Vault;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.metadevs.buycraftapi.Metrics.Metrics;
import org.metadevs.buycraftapi.Payments.Query;
import org.metadevs.buycraftapi.Placeholders.Placeholders;
import org.metadevs.buycraftapi.Tasks.Tasks;
import org.metadevs.buycraftapi.data.Request;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


@Getter
public class BuyAPI extends PlaceholderExpansion {
    private final Vault vault;
    private Permission perms = null;

    public Request request;
    private final Placeholders placeholdersClass;
    private final Query query;
    private final Logger logger;

    public BuyAPI() {
        BuycraftPlugin plugin = BuycraftPlugin.getPlugin(BuycraftPlugin.class);



        request = new Request(plugin.getConfiguration().getServerKey());

        query = new Query(this);

        Plugin placeholderAPI = Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI");

        int pluginId = 10173;
        Metrics metrics = new Metrics(placeholderAPI, pluginId);

        logger = Logger.getLogger("BuycraftAPI");


        if (metrics.isEnabled())
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

        placeholdersClass = new Placeholders(this);

    }


    @Override
    public boolean canRegister() {
        BuycraftPlugin plugin = BuycraftPlugin.getPlugin(BuycraftPlugin.class);

        if(plugin.getConfiguration().getServerKey()==null || plugin.getConfiguration().getServerKey().isEmpty() || plugin.getConfiguration().getServerKey().equals("INVALID")) {
            logger.severe("Server key is not set. Please set it in the BuyCraft config.yml");
            return false;
        }

        Plugin placeholderAPI = Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI");


        new Tasks(this, placeholderAPI);


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
        return "4.0";
    }

    @Override
    public String onPlaceholderRequest(Player p, @NotNull String identifier) {
        return placeholdersClass.onPlaceholderRequest(p, identifier);
    }



    private void vaultHook() {
        if (vault != null) {
            if (setupPermissions()) {
                getLogger().log(Level.INFO, "Successfully hooked into Vault for BuyCraftAPI v" + getVersion());
            }
        }
    }


    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    


}
