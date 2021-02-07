package it.alexdev_.buycraftapi.Tasks;

import it.alexdev_.buycraftapi.Main;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public class Tasks {

    private final Plugin placeholderapi;
    private final PlaceholderAPIPlugin papi;



    public Tasks(Plugin placeholderapi) {
        this.placeholderapi = placeholderapi;
        papi = (PlaceholderAPIPlugin) placeholderapi;
    }


    public void loadPaymentsTask(){
        new BukkitRunnable() {
            @Override
            public void run() {

                if (papi.getLocalExpansionManager().getExpansions().contains(Main.getInstance())) Main.getInstance().paymentsManager.loadPayments();
                else {
                    placeholderapi.getLogger().log(Level.INFO, "Task loadPayments cancelled");
                    cancel();
                }
            }
        }.runTaskTimerAsynchronously(placeholderapi, 0L, 20*3600+200);
    }

    public void loadSavePaymentsTask(){
        new BukkitRunnable() {
            @Override
            public void run() {
                if (papi.getLocalExpansionManager().getExpansions().contains(Main.getInstance())) Main.getInstance().fileManager.savePaymentsInFile();
                else {
                    placeholderapi.getLogger().log(Level.INFO, "Task savePaymentsInFile cancelled");
                    cancel();
                }
            }
        }.runTaskTimerAsynchronously(placeholderapi, 0L, 20*3600+400);
    }

    public void loadCalcTotTask(){
        new BukkitRunnable() {
            @Override
            public void run() {
                if (papi.getLocalExpansionManager().getExpansions().contains(Main.getInstance())) Main.getInstance().fileManager.calcTot();
                else {
                    placeholderapi.getLogger().log(Level.INFO, "Task calcTot cancelled");
                    cancel();
                }
            }
        }.runTaskTimerAsynchronously(placeholderapi, 0L, 20*3600+600);
    }

    public void loadCalcMontlhyTask(){
        new BukkitRunnable() {
            @Override
            public void run() {
                if (papi.getLocalExpansionManager().getExpansions().contains(Main.getInstance())) Main.getInstance().fileManager.calcMonthly();
                else {
                    placeholderapi.getLogger().log(Level.INFO, "Task calcMonthly cancelled");
                    cancel();
                }
            }
        }.runTaskTimerAsynchronously(placeholderapi, 0L, 20*3600+600);
    }


}
