package it.alexdev_.buycraftapi;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.buycraft.plugin.bukkit.BuycraftPlugin;
import net.buycraft.plugin.data.RecentPayment;
import net.buycraft.plugin.internal.retrofit2.Call;
import net.milkbowl.vault.Vault;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.logging.Level;


public class Main extends PlaceholderExpansion {

    private static Call<List<RecentPayment>> recentPaymentsCall;
    public static List<RecentPayment> recentPayments = null;
    private BuycraftPlugin plugin;
    private int maxPayments = 100;
    private static boolean times = true;
    private Vault vault;
    private static Permission perms = null;

    private FileManager fileManager;





    private final PlaceholderExpansion placeholderExpansion = this;



    @Override
    public @NotNull String getAuthor() {
        return "AlexDev_";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "buycraftAPI";
    }


    @Override
    public @NotNull String getVersion() {
        return "2.3";
    }


    @Override
    public boolean canRegister() {
        fileManager = new FileManager();

        Plugin placeholderAPI = Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI");

        int pluginId = 	10173;
        Metrics metrics = new Metrics(placeholderAPI, pluginId);


        if(metrics.isEnabled()) placeholderAPI.getLogger().log(Level.INFO, "[BuyCraftAPI] Successfully connected to bstats");
        else placeholderAPI.getLogger().log(Level.WARNING, "[BuyCraftAPI] Could not connect to bstats! Enable it in bstats folder in plugins folder.");

        metrics.addCustomChart(new Metrics.MultiLineChart("players_and_servers", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            valueMap.put("servers", 1);
            valueMap.put("players", Bukkit.getOnlinePlayers().size());
            return valueMap;
        }));



        plugin = (BuycraftPlugin) Bukkit.getServer().getPluginManager().getPlugin("BuycraftX");
        vault = (Vault) Bukkit.getServer().getPluginManager().getPlugin("Vault");



        PlaceholderAPIPlugin placeholderAPI1 = (PlaceholderAPIPlugin) placeholderAPI;

        if (!loadPayments() || recentPayments.size() == 0) {
            placeholderAPI.getLogger().log(Level.INFO, "[BuyCraftAPI] Could not load expansion. There are no payments yet.");
            unregister();
            return false;
        }
        countPayments();
        if (times) {
            times = false;
            new BukkitRunnable() {
                @Override
                public void run() {

                    if (placeholderAPI1.getLocalExpansionManager().getExpansions().contains(placeholderExpansion)) loadPayments();
                    else {
                        placeholderAPI.getLogger().log(Level.INFO, "Task loadPayments cancelled");
                        cancel();
                    }
                }
            }.runTaskTimerAsynchronously(placeholderAPI, 0L, 20*3600+200);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (placeholderAPI1.getLocalExpansionManager().getExpansions().contains(placeholderExpansion)) fileManager.savePaymentsInFile();
                    else {
                        placeholderAPI.getLogger().log(Level.INFO, "Task savePaymentsInFile cancelled");
                        cancel();
                    }
                }
            }.runTaskTimerAsynchronously(placeholderAPI, 0L, 20*3600+400);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (placeholderAPI1.getLocalExpansionManager().getExpansions().contains(placeholderExpansion)) fileManager.calcTot();
                    else {
                        placeholderAPI.getLogger().log(Level.INFO, "Task calcTot cancelled");
                        cancel();
                    }
                }
            }.runTaskTimerAsynchronously(placeholderAPI, 0L, 20*3600+600);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (placeholderAPI1.getLocalExpansionManager().getExpansions().contains(placeholderExpansion)) fileManager.calcMonthly();
                    else {
                        placeholderAPI.getLogger().log(Level.INFO, "Task calcMonthly cancelled");
                        cancel();
                    }
                }
            }.runTaskTimerAsynchronously(placeholderAPI, 0L, 20*3600+800);

        }


        if (plugin == null) return false;
        if (vault != null) {
            if (setupPermissions()) {
                placeholderAPI.getLogger().log(Level.INFO, "[BuyCraftAPI] Successfully hooked into Vault for BuyCraftAPI v" + getVersion());
            }
        }
        return true;
    }

    public String getPlayerFromTop(String top, int position) {
        switch (top) {
            case "Global": {
                if (fileManager.getFileConfiguration().isConfigurationSection("Global." + position)) {
                    return fileManager.getFileConfiguration().getString("Global." + position + ".Name");
                } else {
                    return null;
                }
            }
            case "Monthly": {
                if (fileManager.getFileConfiguration().isConfigurationSection("Monthly." + position)) {
                    return fileManager.getFileConfiguration().getString("Monthly." + position + ".Name");
                } else {
                    return null;
                }
            }
            default:
                return null;
        }
    }



    public double getValueFromTop(String top, int position) {
        switch (top) {
            case "Global": {
                if (fileManager.getFileConfiguration().isConfigurationSection("Global." + position)) {
                    return fileManager.getFileConfiguration().getDouble("Global." + position + ".Value");
                } else {
                    return -1;
                }
            }
            case "Monthly": {
                if (fileManager.getFileConfiguration().isConfigurationSection("Monthly." + position)) {
                    return fileManager.getFileConfiguration().getDouble("Monthly." + position + ".Value");
                } else {
                    return -1;
                }
            }
            default:
                return -1;
        }
    }

    public double getAllMoneySpent(String type){
        double amount = 0D;
        switch (type){
            case "Global":{
                for(String id : fileManager.getFileConfiguration().getConfigurationSection("Global").getKeys(false)){
                    amount += fileManager.getFileConfiguration().getDouble("Global." + id + ".Value");
                }
                break;
            }
            case "Monthly":{
                for(String id : fileManager.getFileConfiguration().getConfigurationSection("Monthly").getKeys(false)){
                    amount += fileManager.getFileConfiguration().getDouble("Monthly." + id + ".Value");
                }
                break;
            }
            default:
                amount = -1D;
        }
        return amount;

    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }




    @Override
    @SuppressWarnings("deprecation")
    public String onPlaceholderRequest(Player p, String identifier) {

        if (identifier.contains("vault_recent_name_")) {
            int num;
            try {
                num = Integer.parseInt(identifier.replace("vault_recent_name_", ""));
            } catch (NumberFormatException e) {
                return "Invalid payment number. Put a number from 0 to " + (maxPayments - 1);
            }
            if (maxPayments == 0) return "Payments could not be found";

            if (num > maxPayments - 1 || num < 0)
                return "Invalid payment number. Put a number from 0 to " + (maxPayments - 1);


            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(getRecentPayment(num)[0]);
            return perms.getPrimaryGroup(null, offlinePlayer);
        }

        if (identifier.contains("recent_name_")) {
            int num;
            try {
                num = Integer.parseInt(identifier.replace("recent_name_", ""));
            } catch (NumberFormatException e) {
                return "Invalid payment number. Put a number from 0 to " + (maxPayments - 1);
            }

            if (maxPayments == 0) return "Payments could not be found";

            if (num > maxPayments - 1 || num < 0)
                return "Invalid payment number. Put a number from 0 to " + (maxPayments - 1);


            return getRecentPayment(num)[0];
        }


        if (identifier.contains("recent_currency_")) {
            String replace = identifier.replace("recent_currency_", "");
            if (!checkNumExeption(replace)) return "Invalid number";
            int num = Integer.parseInt(replace);

            if (maxPayments == 0) return "Payments could not be found";
            if (num > maxPayments - 1 || num < 0)
                return "Error, Invalid number! You can put a number from 0 to " + (maxPayments - 1);


            return getRecentPayment(num)[1];
        }


        if (identifier.contains("recent_price_")) {
            String replace = identifier.replace("recent_price_", "");
            if (!checkNumExeption(replace)) return "Invalid number";
            int num = Integer.parseInt(replace);
            if (maxPayments == 0) return "Payments could not be found";
            if (num > maxPayments - 1 || num < 0)
                return "Error, Invalid number! You can put a number from 0 to " + (maxPayments - 1);


            return String.valueOf(round(Double.parseDouble(getRecentPayment(num)[2]), 2));
        }

        if (identifier.contains("vault_top_donor_global_name_")) {
            String replace = identifier.replace("vault_top_donor_global_name_", "");
            if (!checkNumExeption(replace)) return "Error, Invalid number";
            int num = Integer.parseInt(replace);
            String player = getPlayerFromTop("Global", num);
            if (player == null) return "Error";
            else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
                return perms.getPrimaryGroup(null, offlinePlayer);
            }
        }
        if (identifier.contains("vault_top_donor_monthly_name_")) {
            String replace = identifier.replace("vault_top_donor_monthly_name_", "");
            if (!checkNumExeption(replace)) return "Error, Invalid number";
            int num = Integer.parseInt(replace);
            String player = getPlayerFromTop("Monthly", num);
            if (player == null) return "Error";
            else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
                return perms.getPrimaryGroup(null, offlinePlayer);
            }
        }

        if (identifier.contains("top_donor_global_name_")) {
            String replace = identifier.replace("top_donor_global_name_", "");
            if (!checkNumExeption(replace)) return "Error, Invalid number";
            int num = Integer.parseInt(replace);
            String player = getPlayerFromTop("Global", num);
            if (player == null) return "Error";
            else {
                return player;
            }
        }

        if (identifier.contains("top_donor_monthly_name_")) {
            String replace = identifier.replace("top_donor_monthly_name_", "");
            if (!checkNumExeption(replace)) return "Error, Invalid number";
            int num = Integer.parseInt(replace);
            String player = getPlayerFromTop("Monthly", num);
            if (player == null) return "Error";
            else {
                return player;
            }
        }

        if (identifier.contains("top_donor_global_price_")) {
            String replace = identifier.replace("top_donor_global_price_", "");
            if (!checkNumExeption(replace)) return "Error, Invalid number";
            int num = Integer.parseInt(replace);
            double player = getValueFromTop("Global", num);
            if (player == -1) return "Error";
            else {
                return player + "";
            }
        }

        if (identifier.contains("top_donor_monthly_price_")) {
            String replace = identifier.replace("top_donor_monthly_price_", "");
            if (!checkNumExeption(replace)) return "Error, Invalid number";
            int num = Integer.parseInt(replace);
            double player = getValueFromTop("Monthly", num);
            if (player == -1) return "Error";
            else {
                return player + "";
            }
        }

        if (identifier.equalsIgnoreCase("total_earnings_global")) {
            double data = getAllMoneySpent("Global");
            if (data == -1) return "Error";
            else {
                return data + "";
            }
        }

        if (identifier.equalsIgnoreCase("total_earnings_monthly")) {
            double data = getAllMoneySpent("Monthly");
            if (data == -1) return "Error";
            else {
                return data + "";
            }
        }


        if (identifier.contains("top_donor_global_currency_")) {
            return fileManager.getDefaultCurrency();
        }

        if (identifier.contains("top_donor_monthly_currency_")) {
            return fileManager.getDefaultCurrency();
        }

        if (identifier.equalsIgnoreCase("info")) {
            HashMap<String, Double> values = loadValues();
            for (String player : values.keySet()) {
                if (p.getName().equalsIgnoreCase(player)) {
                    return String.valueOf(values.get(player));
                }
            }
        }


        if (identifier.equalsIgnoreCase("all")) { //only for test
            HashMap<String, Double> values = loadValues();
            HashMap<String, String> currency = loadCurrency();
            if (maxPayments == 0) return "Payments could not be found";
            for (int i = 0; i < sortMap(values).size(); i++) {
                p.sendMessage(getNameWanted(values, i) + " " + round(Double.parseDouble(getPriceWanted(values, i)), 2) + " " + getCurrencyWanted(currency, values, i) + " " + perms.getPrimaryGroup(null, Bukkit.getOfflinePlayer(getNameWanted(values, i))));
            }
            return "";
        }


        return null;
    }


    public List<Double> sortMap(HashMap<String, Double> values) {
        List<Double> test1 = new ArrayList<>(values.values());
        Collections.sort(test1);
        Collections.reverse(test1);
        List<Double> croce = new ArrayList<>();
        for (Double data : test1) {
            for (String data2 : values.keySet()) {
                if (data.equals(values.get(data2)) && !croce.contains(data)) {
                    croce.add(data);
                }
            }
        }


        return croce;

    }

    public void countPayments() {
        if (recentPayments == null) {
            loadPayments();
        }
        maxPayments = recentPayments.size();
    }


    public String getNameWanted(HashMap<String, Double> values, int num) {
        List<Double> test = sortMap(values);
        Double doubleval = test.get(num);
        return getNameFromValue(values, doubleval);
    }

    public String getCurrencyWanted(HashMap<String, String> currency, HashMap<String, Double> values, int num) {
        return getCurrencyFromName(currency, getNameWanted(values, num));
    }

    public String getPriceWanted(HashMap<String, Double> values, int num) {
        return getTotalPrice(values, getNameWanted(values, num));
    }

    public String getNameFromValue(HashMap<String, Double> values, Double val) {
        for (String name : values.keySet()) {
            if (val.equals(values.get(name))) {
                return name;
            }
        }
        return null;
    }

    public String getCurrencyFromName(HashMap<String, String> currency, String playername) {
        for (String name : currency.keySet()) {
            if (name.equalsIgnoreCase(playername)) {
                return currency.get(name);
            }
        }
        return null;
    }

    public String getTotalPrice(HashMap<String, Double> values, String playername) {
        for (String name : values.keySet()) {
            if (name.equalsIgnoreCase(playername)) {
                return String.valueOf(values.get(name));
            }
        }
        return null;
    }


    public String[] getRecentPayment(int position) {
        if (position < recentPayments.size()) {
            String[] data = new String[3];

            RecentPayment payment = recentPayments.get(position);
            String name = payment.getPlayer().getName();
            data[0] = name;
            String currency = payment.getCurrency().getIso4217();
            data[1] = currency;
            double price = payment.getAmount().doubleValue();

            data[2] = String.valueOf(round(price, 2));
            return data;
        }
        return null;
    }


    public HashMap<String, Double> loadValues() {
        HashMap<String, Double> values = new HashMap<>();


        for (RecentPayment recentPayment : recentPayments) {
            if (!values.containsKey(recentPayment.getPlayer().getName())) {
                values.put(recentPayment.getPlayer().getName(), round(recentPayment.getAmount().doubleValue(), 2));
            } else {
                values.put(recentPayment.getPlayer().getName(), round(recentPayment.getAmount().doubleValue(), 2) + values.get(recentPayment.getPlayer().getName()));
            }

        }
        return values;
    }

    public HashMap<String, String> loadCurrency() {
        HashMap<String, String> currency = new HashMap<>();
        for (RecentPayment recentPayment : recentPayments) {
            if (!currency.containsKey(recentPayment.getPlayer().getName())) {
                currency.put(recentPayment.getPlayer().getName(), recentPayment.getCurrency().getIso4217());
            }
        }
        return currency;
    }

    public boolean checkNumExeption(String num) {

        try {
            Integer.parseInt(num);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public boolean loadPayments() {
        try {
            recentPaymentsCall = plugin.getApiClient().getRecentPayments(maxPayments);
            recentPayments = recentPaymentsCall.execute().body();
            if (recentPayments.size() < maxPayments) maxPayments = recentPayments.size();
        } catch (Exception e) {
            return false;
        }
        return true;
    }


}
