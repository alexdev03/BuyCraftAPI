package alexdev_.buycraftapi;

import alexdev_.buycraftapi.FileManager.FileManager;
import alexdev_.buycraftapi.Metrics.Metrics;
import alexdev_.buycraftapi.Payments.PaymentsManager;
import alexdev_.buycraftapi.Payments.Query;
import alexdev_.buycraftapi.Placeholders.Placeholders;
import alexdev_.buycraftapi.Tasks.Tasks;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.buycraft.plugin.data.RecentPayment;
import net.milkbowl.vault.Vault;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
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
    public boolean useUUID = false;


    public static Main getInstance() {
        return main;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    @Override
    public boolean canRegister() {
        main = this;
        fileManager = new FileManager();
        Query query = new Query(fileManager);
        paymentsManager = new PaymentsManager(100);

        recentPayments = new ArrayList<>();

        Plugin placeholderAPI = Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI");

        int pluginId = 10173;
        Metrics metrics = new Metrics(placeholderAPI, pluginId);


        if (metrics.isEnabled())
            placeholderAPI.getLogger().log(Level.INFO, "[BuyCraftAPI] Successfully connected to bstats");
        else
            placeholderAPI.getLogger().log(Level.WARNING, "[BuyCraftAPI] Could not connect to bstats! Enable it in bstats folder in plugins folder.");

        metrics.addCustomChart(new Metrics.MultiLineChart("players_and_servers", () -> {
            HashMap<String, Integer> valueMap = new HashMap<>();
            valueMap.put("servers", 1);
            valueMap.put("players", Bukkit.getOnlinePlayers().size());
            return valueMap;
        }));


        File file = new File("plugins/PlaceholderAPI/expansions/BuyCraftAPI/config.yml");
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (!config.isConfigurationSection("UseUUIDS")) {
            config.set("UseUUIDS", true);
            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            useUUID = config.getBoolean("UseUUIDS");
        }catch (NullPointerException e){
            useUUID = true;
        }

        if(useUUID) placeholderAPI.getLogger().log(Level.INFO, "[BuyCraftAPI] Using UUIDS for payments");
        else placeholderAPI.getLogger().log(Level.INFO, "[BuyCraftAPI] Using Players Names for payments");

        vault = (Vault) Bukkit.getServer().getPluginManager().getPlugin("Vault");
        PlaceholderAPIPlugin placeholderAPI1 = (PlaceholderAPIPlugin) placeholderAPI;

        vaultHook(placeholderAPI1);



        placeholders = new Placeholders(query);


        List<Integer> tasksId = new ArrayList<>();


        if (!paymentsManager.loadPayments() || recentPayments.size() == 0) {
            placeholderAPI.getLogger().log(Level.SEVERE, "[BuyCraftAPI] Could not load expansion. There are no payments yet.");
            unregister();
            return false;
        } else {
            Tasks tasks = new Tasks(placeholderAPI);
            countPayments();
            placeholderAPI.getLogger().log(Level.INFO, "[BuyCraftAPI] Loading tasks");
            tasksId.add(tasks.loadPaymentsTask());
            tasksId.add(tasks.loadCalcTotTask());
            tasksId.add(tasks.loadCalcMontlhyTask());
            tasksId.add(tasks.loadSavePaymentsTask());
            tasksId.add(tasks.loadCalcCurrentMonthTask());
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!placeholderAPI1.getLocalExpansionManager().getExpansions().contains(main)) {
                    for (Integer task : tasksId) {
                        Bukkit.getScheduler().cancelTask(task);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(placeholderAPI1, 1, 20);


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
        return "2.8";
    }

    @Override
    public String onPlaceholderRequest(Player p, String identifier) {
        return placeholders.onPlaceholderRequest(p, identifier);
    }

    private void vaultHook(PlaceholderAPIPlugin placeholderAPI) {
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
