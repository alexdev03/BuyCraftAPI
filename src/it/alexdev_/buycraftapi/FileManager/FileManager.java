package it.alexdev_.buycraftapi.FileManager;

import it.alexdev_.buycraftapi.Main;
import net.buycraft.plugin.data.RecentPayment;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

public class FileManager {


    private final File folder = new File("plugins/PlaceholderAPI/expansions/BuyCraftAPI");
    private final File dataFolder = new File("plugins/PlaceholderAPI/expansions/BuyCraftAPI/Data");
    private final File config = new File("plugins/PlaceholderAPI/expansions/BuyCraftAPI/config.yml");
    private final FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(config);
    private String defaultCurrency = "EUR";

    public FileConfiguration getFileConfiguration(){
        return fileConfiguration;
    }


    public String getDefaultCurrency() {
        return defaultCurrency;
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

    public void savePaymentsInFile() {
        if (!folder.exists()) folder.mkdir();
        if (!dataFolder.exists()) dataFolder.mkdir();
        for (RecentPayment recentPayment : Main.recentPayments) {

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(recentPayment.getPlayer().getName());
            if (!offlinePlayer.hasPlayedBefore()) continue;
            File player = new File("plugins/PlaceholderAPI/expansions/BuyCraftAPI/Data/" + Bukkit.getOfflinePlayer(recentPayment.getPlayer().getName()).getUniqueId().toString() + ".yml");

            if (!player.exists()) {
                try {
                    player.createNewFile();
                } catch (IOException e) {
                    //e.printStackTrace();
                    Bukkit.getLogger().log(Level.SEVERE, "[BuyCraftAPI] Error whilst creating file for " + offlinePlayer.getName());
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
                fileConfiguration.set("Data.Payments." + recentPayment.getDate().toString() + ".Currency", defaultCurrency);

                save(fileConfiguration, player);
            }
        }
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

        List<Double> list = new ArrayList<>(value.values());


        Collections.sort(list);

        Collections.reverse(list);


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


        for (Double data : list) {
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

    public boolean isBetween(Date a, Date b, Date d) {
        return a.compareTo(d) * d.compareTo(b) > 0;
    }

    public void save(FileConfiguration fileConfiguration, File file) {
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
