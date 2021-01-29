package it.alexdev_.buycraftapi.buycraftapi;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.events.PlaceholderHookUnloadEvent;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.buycraft.plugin.bukkit.BuycraftPlugin;
import net.buycraft.plugin.data.RecentPayment;
import net.buycraft.plugin.internal.retrofit2.Call;
import net.milkbowl.vault.Vault;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;


public class Main extends PlaceholderExpansion {

    private static Call<List<RecentPayment>> recentPaymentsCall;
    private static List<RecentPayment> recentPayments = null;
    private BuycraftPlugin plugin;
    private int maxPayments = 100;
    private static boolean times = true;
    private Vault vault;
    private static Permission perms = null;

    private final File folder = new File("plugins/PlaceholderAPI/expansions/BuyCraftAPI");
    private final File dataFolder = new File("plugins/PlaceholderAPI/expansions/BuyCraftAPI/Data");
    private final File config = new File("plugins/PlaceholderAPI/expansions/BuyCraftAPI/config.yml");
    private final FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(config);

    private String defaultCurrency = "EUR";

    private final PlaceholderExpansion placeholderExpansion = this;


    @Override
    public boolean canRegister() {
        plugin = (BuycraftPlugin) Bukkit.getServer().getPluginManager().getPlugin("BuycraftX");
        vault = (Vault) Bukkit.getServer().getPluginManager().getPlugin("Vault");

        Plugin placeholderAPI = Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI");

        if (!loadPayments() || recentPayments.size() == 0) {
            System.out.println("[PlaceholderAPI] [BuyCraftAPI] Could not load expansion. There are no payments yet.");
            PlaceholderAPI.unregisterExpansion(this);
            return false;
        }
        countPayments();
        if (times) {
            times = false;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (PlaceholderAPI.getExpansions().contains(placeholderExpansion)) loadPayments();
                    else {
                        placeholderAPI.getLogger().log(Level.INFO, "Task loadPayments cancelled");
                        cancel();
                    }
                }
            }.runTaskTimerAsynchronously(placeholderAPI, 0L, 20*3600+200);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (PlaceholderAPI.getExpansions().contains(placeholderExpansion)) savePaymentsInFile();
                    else {
                        placeholderAPI.getLogger().log(Level.INFO, "Task savePaymentsInFile cancelled");
                        cancel();
                    }
                }
            }.runTaskTimerAsynchronously(placeholderAPI, 0L, 20*3600+400);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (PlaceholderAPI.getExpansions().contains(placeholderExpansion)) calcTot();
                    else {
                        placeholderAPI.getLogger().log(Level.INFO, "Task calcTot cancelled");
                        cancel();
                    }
                }
            }.runTaskTimerAsynchronously(placeholderAPI, 0L, 20*3600+600);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (PlaceholderAPI.getExpansions().contains(placeholderExpansion)) calcMonthly();
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
                System.out.println("[PlaceholderAPI] Successfully hooked into Vault for BuyCraftAPI v" + getVersion());
            }
        }
        return true;
    }

    public void savePaymentsInFile() {
        if (!folder.exists()) folder.mkdir();
        if (!dataFolder.exists()) dataFolder.mkdir();
        for (RecentPayment recentPayment : recentPayments) {
            try {


                if (recentPayment.getPlayer().getUuid().contains("00000000000000000000")) continue;
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(recentPayment.getPlayer().getName());
                if (!offlinePlayer.hasPlayedBefore()) continue;
                File player = new File("plugins/PlaceholderAPI/expansions/BuyCraftAPI/Data/" + Bukkit.getOfflinePlayer(recentPayment.getPlayer().getName()).getUniqueId().toString() + ".yml");
                if (!player.exists()) {
                    try {
                        player.createNewFile();
                    } catch (IOException e) {
                        //e.printStackTrace();
                        System.out.println("Errore nel creare il file");
                    }
                    FileConfiguration fileConfiguration = new YamlConfiguration();
                    try {
                        fileConfiguration.load(player);
                    } catch (IOException | InvalidConfigurationException e) {
                        e.printStackTrace();
                    }
                    fileConfiguration.createSection("Data");
                    fileConfiguration.set("Data.Player", recentPayment.getPlayer().getName());
                    fileConfiguration.createSection("Data.Payments");
                    fileConfiguration.createSection("Data.Payments." + recentPayment.getDate().toString());
                    fileConfiguration.set("Data.Payments." + recentPayment.getDate().toString() + ".Amount", recentPayment.getAmount().doubleValue());
                /*if(!recentPayment.getCurrency().getIso4217().contains("net.buycraft")) {
                    fileConfiguration.set("Data.Payments." + recentPayment.getDate().toString() + ".Currency", recentPayment.getCurrency().getIso4217());
                }else {
                    fileConfiguration.set("Data.Payments."+recentPayment.getDate().toString()+".Currency", defaultCurrency);
                }*/
                    fileConfiguration.set("Data.Payments." + recentPayment.getDate().toString() + ".Currency", defaultCurrency);
                    save(fileConfiguration, player);
                } else {
                    FileConfiguration fileConfiguration = new YamlConfiguration();
                    try {
                        fileConfiguration.load(player);
                    } catch (IOException | InvalidConfigurationException e) {
                        e.printStackTrace();
                    }
                    if (fileConfiguration.isConfigurationSection("Data.Payments." + recentPayment.getDate().toString()))
                        continue;
                    fileConfiguration.createSection("Data.Payments." + recentPayment.getDate().toString());
                    fileConfiguration.set("Data.Payments." + recentPayment.getDate().toString() + ".Amount", recentPayment.getAmount().doubleValue());
                /*if(!recentPayment.getCurrency().getIso4217().contains("net.buycraft")) {
                    fileConfiguration.set("Data.Payments." + recentPayment.getDate().toString() + ".Currency", recentPayment.getCurrency().getIso4217());
                }else {
                    fileConfiguration.set("Data.Payments."+recentPayment.getDate().toString()+".Currency", defaultCurrency);
                }*/
                    fileConfiguration.set("Data.Payments." + recentPayment.getDate().toString() + ".Currency", defaultCurrency);

                    save(fileConfiguration, player);
                }
            } catch (IllegalArgumentException e) {
            }
        }
    }

    public void save(FileConfiguration fileConfiguration, File file) {
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void calcTot() {
        HashMap<String, Double> value = new HashMap<>();
        for (File player : dataFolder.listFiles()) {
            double count = 0D;
            FileConfiguration fileConfiguration = new YamlConfiguration();
            try {
                fileConfiguration.load(player);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }

            for (String payment : fileConfiguration.getConfigurationSection("Data.Payments").getKeys(false)) {
                count += fileConfiguration.getDouble("Data.Payments." + payment + ".Amount");

            }
            value.put(player.getName().replace(".yml", ""), count);
        }
        List<Double> toSort = new ArrayList<>(value.values());


        Collections.sort(toSort);

        Collections.reverse(toSort);




        if (!config.exists()) {
            try {
                config.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



        if (!fileConfiguration.isConfigurationSection("DefaultCurrency")) fileConfiguration.set("DefaultCurrency", "EUR");
        defaultCurrency = fileConfiguration.getString("DefaultCurrency");

        fileConfiguration.set("Global", null);
        fileConfiguration.createSection("Global");

        int position = 1;


        for (Double data : toSort) {
            for (String player : value.keySet()) {
                if (value.get(player).equals(data) && data > 0) {
                    fileConfiguration.createSection("Global." + position);
                    fileConfiguration.set("Global." + position + ".UUID", player);
                    fileConfiguration.set("Global." + position + ".Name", Bukkit.getOfflinePlayer(UUID.fromString(player)).getName());
                    fileConfiguration.set("Global." + position + ".Value", data);
                    position++;
                }
            }
        }
        save(fileConfiguration, config);
    }


    public void calcMonthly() {
        HashMap<String, Double> value = new HashMap<>();
        for (File player : dataFolder.listFiles()) {
            double count = 0D;
            FileConfiguration fileConfiguration = new YamlConfiguration();
            try {
                fileConfiguration.load(player);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }

            for (String payment : fileConfiguration.getConfigurationSection("Data.Payments").getKeys(false)) {
                SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
                Date date = null;
                try {
                    date = format.parse(payment);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Date now = new Date(System.currentTimeMillis());
                Date monthago = new Date(System.currentTimeMillis());
                monthago.setMonth(monthago.getMonth() - 1);
                if (isBetween(monthago, now, date))
                    count += fileConfiguration.getDouble("Data.Payments." + payment + ".Amount");
            }
            value.put(player.getName().replace(".yml", ""), count);
        }

        List<Double> toSort = new ArrayList<>(value.values());


        Collections.sort(toSort);

        Collections.reverse(toSort);


        if (!config.exists()) {
            try {
                config.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



        if (!fileConfiguration.isConfigurationSection("DefaultCurrency"))
            fileConfiguration.set("DefaultCurrency", "EUR");
        defaultCurrency = fileConfiguration.getString("DefaultCurrency");

        fileConfiguration.set("Monthly", null);
        fileConfiguration.createSection("Monthly");

        int position = 1;


        for (Double data : toSort) {
            for (String player : value.keySet()) {
                if (value.get(player).equals(data) && data > 0) {
                    fileConfiguration.createSection("Monthly." + position);
                    fileConfiguration.set("Monthly." + position + ".UUID", player);
                    fileConfiguration.set("Monthly." + position + ".Name", Bukkit.getOfflinePlayer(UUID.fromString(player)).getName());
                    fileConfiguration.set("Monthly." + position + ".Value", data);
                    position++;
                }
            }
        }
        save(fileConfiguration, config);
    }


    public String getPlayerFromTop(String top, int position) {
        switch (top) {
            case "Global": {
                if (fileConfiguration.isConfigurationSection("Global." + position)) {
                    return fileConfiguration.getString("Global." + position + ".Name");
                } else {
                    return null;
                }
            }
            case "Monthly": {
                if (fileConfiguration.isConfigurationSection("Monthly." + position)) {
                    return fileConfiguration.getString("Monthly." + position + ".Name");
                } else {
                    return null;
                }
            }
            default:
                return null;
        }
    }

    public boolean isBetween(Date a, Date b, Date d) {
        return a.compareTo(d) * d.compareTo(b) > 0;
    }

    public double getValueFromTop(String top, int position) {
        switch (top) {
            case "Global": {
                if (fileConfiguration.isConfigurationSection("Global." + position)) {
                    return fileConfiguration.getDouble("Global." + position + ".Value");
                } else {
                    return -1;
                }
            }
            case "Monthly": {
                if (fileConfiguration.isConfigurationSection("Monthly." + position)) {
                    return fileConfiguration.getDouble("Monthly." + position + ".Value");
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
                for(String id : fileConfiguration.getConfigurationSection("Global").getKeys(false)){
                    amount += fileConfiguration.getDouble("Global." + id + ".Value");
                }
                break;
            }
            case "Monthly":{
                for(String id : fileConfiguration.getConfigurationSection("Monthly").getKeys(false)){
                    amount += fileConfiguration.getDouble("Monthly." + id + ".Value");
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
    public String getAuthor() {
        return "AlexDev_";
    }

    @Override
    public String getIdentifier() {
        return "buycraftAPI";
    }


    @Override
    public String getVersion() {
        return "2.0";
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
            return defaultCurrency;
        }

        if (identifier.contains("top_donor_monthly_currency_")) {
            return defaultCurrency;
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
        int number = 0;
        try {
            number = Integer.parseInt(num);
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
