package alexdev_.buycraftapi.Payments;

import alexdev_.buycraftapi.BuyAPI;
import net.buycraft.plugin.bukkit.BuycraftPlugin;
import net.buycraft.plugin.data.RecentPayment;
import net.buycraft.plugin.internal.retrofit2.Call;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.logging.Level;

public class PaymentsManager {

    private final BuycraftPlugin plugin;


    private  int maxPayments;

    public PaymentsManager(int maxpayments){
        plugin = (BuycraftPlugin) Bukkit.getPluginManager().getPlugin("BuycraftX");
        this.maxPayments = maxpayments;
    }

    public boolean loadPayments() {
        Call<List<RecentPayment>> recentPaymentsCall;
        try {
            recentPaymentsCall = plugin.getApiClient().getRecentPayments(maxPayments);
            BuyAPI.recentPayments = recentPaymentsCall.execute().body();
            assert BuyAPI.recentPayments != null;
            int num = BuyAPI.recentPayments.size();
            Bukkit.getPluginManager().getPlugin("PlaceholderAPI").getLogger().log(Level.INFO, "[BuyCraftAPI] Succesfully loaded "+ num + " payments");
            if (BuyAPI.recentPayments.size() < maxPayments)
                maxPayments = BuyAPI.recentPayments.size();
        } catch (Exception e) {
            System.out.println("Error whilst retrieve data ");
            e.printStackTrace();
            return false;
        }
        return true;
    }



}
