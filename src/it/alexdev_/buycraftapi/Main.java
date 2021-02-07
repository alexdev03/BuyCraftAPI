package it.alexdev_.buycraftapi;

import it.alexdev_.buycraftapi.FileManager.FileManager;
import it.alexdev_.buycraftapi.Metrics.Metrics;
import it.alexdev_.buycraftapi.Payments.PaymentsManager;
import it.alexdev_.buycraftapi.Payments.Query;
import it.alexdev_.buycraftapi.Placeholders.Placeholders;
import it.alexdev_.buycraftapi.Tasks.Tasks;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.buycraft.plugin.data.RecentPayment;
import net.milkbowl.vault.Vault;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;


public class Main extends PlaceholderExpansion {


    public static List<RecentPayment> recentPayments = null;

    private int maxPayments = 100;

    private Vault vault;
    public static Permission perms = null;

    public FileManager fileManager;
    public PaymentsManager paymentsManager;
    private static Main main;
    private Placeholders placeholders;





    public static Main getInstance() {
        return main;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public Placeholders getPlaceholdersInstance() {
        return placeholders;
    }

    @Override
    public boolean canRegister(){
        main = this;

        fileManager = new FileManager();
        Query query = new Query(fileManager);
        paymentsManager = new PaymentsManager(100);

        recentPayments = new ArrayList<>();

        Plugin placeholderAPI = Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI");

        int pluginId = 	10173;
        Metrics metrics = new Metrics(placeholderAPI, pluginId);


        if(metrics.isEnabled()) placeholderAPI.getLogger().log(Level.INFO, "[BuyCraftAPI] Successfully connected to bstats");
        else placeholderAPI.getLogger().log(Level.WARNING, "[BuyCraftAPI] Could not connect to bstats! Enable it in bstats folder in plugins folder.");

        metrics.addCustomChart(new Metrics.MultiLineChart("players_and_servers", () -> {
            HashMap<String, Integer> valueMap = new HashMap<>();
            valueMap.put("servers", 1);
            valueMap.put("players", Bukkit.getOnlinePlayers().size());
            return valueMap;
        }));



        vault = (Vault) Bukkit.getServer().getPluginManager().getPlugin("Vault");
        PlaceholderAPIPlugin placeholderAPI1 = (PlaceholderAPIPlugin) placeholderAPI;

        vaultHook(placeholderAPI1);



        //Placeholders class
        placeholders = new Placeholders(query);






        paymentsManager.loadPayments();
        if (!paymentsManager.loadPayments() || recentPayments.size() == 0) {
            placeholderAPI.getLogger().log(Level.INFO, "[BuyCraftAPI] Could not load expansion. There are no payments yet.");
            unregister();
            return false;
        } else {

            Tasks tasks = new Tasks(placeholderAPI);
            countPayments();
            tasks.loadPaymentsTask();
            tasks.loadCalcTotTask();
            tasks.loadCalcMontlhyTask();
            tasks.loadSavePaymentsTask();
        }



        return true;
    }

    public @NotNull String getAuthor() {
        return "AlexDev_";
    }


    public @NotNull String getIdentifier() {
        return "buycraftAPI";
    }



    public @NotNull String getVersion() {
        return "2.5";
    }

    @Override
    public String onPlaceholderRequest(Player p, @NotNull String identifier) {
        return placeholders.onPlaceholderRequest(p, identifier);
    }

    private void  vaultHook(PlaceholderAPIPlugin placeholderAPI){
        if (vault != null) {
            if (setupPermissions()) {
                placeholderAPI.getLogger().log(Level.INFO, "[BuyCraftAPI] Successfully hooked into Vault for BuyCraftAPI v" + getVersion());
            }
        }
    }

    public int getMaxPayments() {
        return maxPayments;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }









    public void countPayments() {
        if (recentPayments == null) {
            paymentsManager.loadPayments();
        }
        maxPayments = recentPayments.size();
    }






}
