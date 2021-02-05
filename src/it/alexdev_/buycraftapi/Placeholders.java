package it.alexdev_.buycraftapi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class Placeholders {


    private final Query query;
    private final int maxPayments;
    private final FileManager fileManager;

    public Placeholders(Query query){

        this.query = query;
        maxPayments = Main.getInstance().getMaxPayments();
        fileManager = Main.getInstance().getFileManager();

    }


    @SuppressWarnings("deprecation")
    public String onPlaceholderRequest(Player p, @NotNull String identifier) {


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


            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(query.getRecentPayment(num)[0]);
            return Main.perms.getPrimaryGroup(null, offlinePlayer);
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


            return query.getRecentPayment(num)[0];
        }


        if (identifier.contains("recent_currency_")) {
            String replace = identifier.replace("recent_currency_", "");
            if (query.checkNumExeption(replace)) return "Invalid number";
            int num = Integer.parseInt(replace);

            if (maxPayments == 0) return "Payments could not be found";
            if (num > maxPayments - 1 || num < 0)
                return "Error, Invalid number! You can put a number from 0 to " + (maxPayments - 1);


            return query.getRecentPayment(num)[1];
        }


        if (identifier.contains("recent_price_")) {
            String replace = identifier.replace("recent_price_", "");
            if (query.checkNumExeption(replace)) return "Invalid number";
            int num = Integer.parseInt(replace);
            if (maxPayments == 0) return "Payments could not be found";
            if (num > maxPayments - 1 || num < 0)
                return "Error, Invalid number! You can put a number from 0 to " + (maxPayments - 1);


            return String.valueOf(Query.round(Double.parseDouble(query.getRecentPayment(num)[2]), 2));
        }

        if (identifier.contains("vault_top_donor_global_name_")) {
            String replace = identifier.replace("vault_top_donor_global_name_", "");
            if (query.checkNumExeption(replace)) return "Error, Invalid number";
            int num = Integer.parseInt(replace);
            String player = query.getPlayerFromTop("Global", num);
            if (player == null) return "Error";
            else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
                return Main.perms.getPrimaryGroup(null, offlinePlayer);
            }
        }
        if (identifier.contains("vault_top_donor_monthly_name_")) {
            String replace = identifier.replace("vault_top_donor_monthly_name_", "");
            if (query.checkNumExeption(replace)) return "Error, Invalid number";
            int num = Integer.parseInt(replace);
            String player = query.getPlayerFromTop("Monthly", num);
            if (player == null) return "Error";
            else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
                return Main.perms.getPrimaryGroup(null, offlinePlayer);
            }
        }

        if (identifier.contains("top_donor_global_name_")) {
            String replace = identifier.replace("top_donor_global_name_", "");
            if (query.checkNumExeption(replace)) return "Error, Invalid number";
            int num = Integer.parseInt(replace);
            String player = query.getPlayerFromTop("Global", num);
            if (player == null) return "Error";
            else {
                return player;
            }
        }

        if (identifier.contains("top_donor_monthly_name_")) {
            String replace = identifier.replace("top_donor_monthly_name_", "");
            if (query.checkNumExeption(replace)) return "Error, Invalid number";
            int num = Integer.parseInt(replace);
            String player = query.getPlayerFromTop("Monthly", num);
            if (player == null) return "Error";
            else {
                return player;
            }
        }

        if (identifier.contains("top_donor_global_price_")) {
            String replace = identifier.replace("top_donor_global_price_", "");
            if (query.checkNumExeption(replace)) return "Error, Invalid number";
            int num = Integer.parseInt(replace);
            double player = query.getValueFromTop("Global", num);
            if (player == -1) return "Error";
            else {
                return player + "";
            }
        }

        if (identifier.contains("top_donor_monthly_price_")) {
            String replace = identifier.replace("top_donor_monthly_price_", "");
            if (query.checkNumExeption(replace)) return "Error, Invalid number";
            int num = Integer.parseInt(replace);
            double player = query.getValueFromTop("Monthly", num);
            if (player == -1) return "Error";
            else {
                return player + "";
            }
        }

        if (identifier.equalsIgnoreCase("total_earnings_global")) {
            double data = query.getAllMoneySpent("Global");
            if (data == -1) return "Error";
            else {
                return data + "";
            }
        }

        if (identifier.equalsIgnoreCase("total_earnings_monthly")) {
            double data = query.getAllMoneySpent("Monthly");
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
            HashMap<String, Double> values = query.loadValues();
            for (String player : values.keySet()) {
                if (p.getName().equalsIgnoreCase(player)) {
                    return String.valueOf(values.get(player));
                }
            }
        }


        if (identifier.equalsIgnoreCase("all")) { //only for test
            HashMap<String, Double> values = query.loadValues();
            HashMap<String, String> currency = query.loadCurrency();
            if (maxPayments == 0) return "Payments could not be found";
            for (int i = 0; i < Query.sortMap(values).size(); i++) {
                p.sendMessage(query.getNameWanted(values, i) + " " + Query.round(Double.parseDouble(query.getPriceWanted(values, i)), 2) + " " + query.getCurrencyWanted(currency, values, i) + " " + Main.perms.getPrimaryGroup(null, Bukkit.getOfflinePlayer(query.getNameWanted(values, i))));
            }
            return "";
        }


        return null;
    }
}
