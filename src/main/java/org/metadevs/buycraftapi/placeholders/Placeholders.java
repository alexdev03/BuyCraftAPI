package org.metadevs.buycraftapi.placeholders;

import lombok.Setter;
import org.metadevs.buycraftapi.BuyCraftAPI;
import org.metadevs.buycraftapi.payments.Query;
import org.metadevs.buycraftapi.data.Payment;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.metadevs.buycraftapi.data.TopValue;
import org.metadevs.buycraftapi.data.Type;

import static org.metadevs.buycraftapi.data.Type.*;

public class Placeholders {


    private final BuyCraftAPI buyCraftAPI;
    private final Query query;
    @Setter
    private int maxPayments;

    public Placeholders(BuyCraftAPI buyCraftAPI) {
        this.buyCraftAPI = buyCraftAPI;
        this.query = buyCraftAPI.getQuery();
    }



    public String onPlaceholderRequest(Player p, @NotNull String identifier) {

        if (identifier.equalsIgnoreCase("value_from_name")) {
            if (p == null) return "Player is Offline";
            String value = query.getPlayerTotal(p.getName());
            return value != null ? value : "0";
        }


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


            if(Bukkit.isPrimaryThread()){
                return "This placeholder is not supported in the main thread";
            }


            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(query.getRecentPayment(num).getUuid());
            return buyCraftAPI.getPerms().getPrimaryGroup(null, offlinePlayer);
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


            return query.getRecentPayment(num).getName();
        }


        if (identifier.contains("recent_currency_")) {
            String replace = identifier.replace("recent_currency_", "");
            if (query.checkNumExeption(replace)) return "Invalid number";
            int num = Integer.parseInt(replace);

            if (maxPayments == 0) return "Payments could not be found";
            if (num > maxPayments - 1 || num < 0)
                return "Error, Invalid number! You can put a number from 0 to " + (maxPayments - 1);


            return query.getRecentPayment(num).getCurrency();
        }


        if (identifier.contains("recent_amount_")) {
            String replace = identifier.replace("recent_amount_", "");
            if (query.checkNumExeption(replace)) return "Invalid number";
            int num = Integer.parseInt(replace);
            if (maxPayments == 0) return "Payments could not be found";
            if (num > maxPayments - 1 || num < 0)
                return "Error, Invalid number! You can put a number from 0 to " + (maxPayments - 1);

            return String.valueOf(Query.round(query.getRecentPayment(num).getAmount(), 2));
        }

        if (identifier.contains("vault_top_donor_global_name_")) {
            String replace = identifier.replace("vault_top_donor_global_name_", "");
            return getVaultTop(replace, GLOBAL);
        }

        if (identifier.contains("vault_top_donor_monthly_name_")) {
            String replace = identifier.replace("vault_top_donor_monthly_name_", "");
            return getVaultTop(replace, MONTHLY);
        }

        if (identifier.contains("vault_top_donor_current_month_name_")) {
            String replace = identifier.replace("vault_top_donor_current_month_name_", "");
            return getVaultTop(replace, CURRENT_MONTH);
        }

        if (identifier.contains("top_donor_global_name_")) {
            String replace = identifier.replace("top_donor_global_name_", "");
            if (query.checkNumExeption(replace)) return "Error, Invalid number";
            int num = Integer.parseInt(replace);
            TopValue payment = query.getTop(GLOBAL, num);
            if (payment == null) return "Error";
            else {
                return payment.getName();
            }
        }

        if (identifier.contains("top_donor_monthly_name_")) {
            String replace = identifier.replace("top_donor_monthly_name_", "");
            if (query.checkNumExeption(replace)) return "Error, Invalid number";
            int num = Integer.parseInt(replace);
            TopValue payment = query.getTop(MONTHLY, num);
            if (payment == null) return "Error";
            else {
                return payment.getName();

            }
        }

        if (identifier.contains("top_donor_current_month_name_")) {
            String replace = identifier.replace("top_donor_current_month_name_", "");
            if (query.checkNumExeption(replace)) return "Error, Invalid number";
            int num = Integer.parseInt(replace);
            TopValue payment = query.getTop(CURRENT_MONTH, num);
            if (payment == null) return "Error";
            else {
                return payment.getName();
            }
        }

        if (identifier.contains("top_donor_global_amount_")) {
            String replace = identifier.replace("top_donor_global_amount_", "");
            return getPrice(replace, GLOBAL);
        }

        if (identifier.contains("top_donor_monthly_amount_")) {
            String replace = identifier.replace("top_donor_monthly_amount_", "");
            return getPrice(replace, MONTHLY);
        }

        if (identifier.contains("top_donor_current_month_amount_")) {
            String replace = identifier.replace("top_donor_current_month_amount_", "");
            return getPrice(replace, CURRENT_MONTH);
        }

        if (identifier.equalsIgnoreCase("total_earnings_global")) {
            double data = query.getAllMoneySpent(GLOBAL);
            if (data == -1) return "Error";
            else {
                return String.format("%.2f", data);
            }
        }

        if (identifier.equalsIgnoreCase("total_earnings_monthly")) {
            double data = query.getAllMoneySpent(MONTHLY);
            if (data == -1) return "Error";
            else {
                return String.format("%.2f", data);
            }
        }

        if (identifier.equalsIgnoreCase("total_earnings_current_month")) {
            double data = query.getAllMoneySpent(CURRENT_MONTH);
            if (data == -1) return "Error";
            else {
                return String.format("%.2f", data);
            }
        }


        if (identifier.equalsIgnoreCase("info")) {
            return query.getPayments().size() + " payments";
        }


        if (identifier.equalsIgnoreCase("all")) { //only for test
            StringBuilder sb = new StringBuilder();
            for (Payment s : query.getPayments()) {
                sb.append(s.getName()).append(" ").append(s.getAmount()).append("\n");
            }
            return sb.toString();
        }


        return null;
    }

    private String getVaultTop(String replace, Type currentMonth) {
        if (query.checkNumExeption(replace)) return "Error, Invalid number";
        int num = Integer.parseInt(replace);

        if(Bukkit.isPrimaryThread()){
            return "This placeholder is not supported in the main thread";
        }

        TopValue payment = query.getTop(currentMonth, num);
        if (payment == null) return "Error";
        else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(payment.getUuid());
            return buyCraftAPI.getPerms().getPrimaryGroup(null, offlinePlayer);
        }
    }

    private String getPrice(String replace, Type monthly) {
        if (query.checkNumExeption(replace)) return "Error, Invalid number";
        int num = Integer.parseInt(replace);
        TopValue payment = query.getTop(monthly, num);
        if (payment == null) return "Error";
        else {
            return String.format("%.2f", payment.getAmount());
        }
    }
}
