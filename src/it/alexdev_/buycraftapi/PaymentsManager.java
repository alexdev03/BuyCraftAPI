package it.alexdev_.buycraftapi;

import net.buycraft.plugin.bukkit.BuycraftPlugin;
import net.buycraft.plugin.data.RecentPayment;
import net.buycraft.plugin.internal.retrofit2.Call;
import org.bukkit.Bukkit;

import java.util.List;

public class PaymentsManager {

    private final BuycraftPlugin plugin;


    private  int maxPayments;

    public PaymentsManager(int maxpayments){
        plugin = (BuycraftPlugin) Bukkit.getPluginManager().getPlugin("BuyCraftX");
        this.maxPayments = maxpayments;
    }

    public boolean loadPayments() {
        Call<List<RecentPayment>> recentPaymentsCall;
        try {
            recentPaymentsCall = plugin.getApiClient().getRecentPayments(maxPayments);
            Main.recentPayments = recentPaymentsCall.execute().body();
            if (Main.recentPayments.size() < maxPayments) maxPayments = Main.recentPayments.size();
        } catch (Exception e) {
            return false;
        }
        return true;
    }



}
