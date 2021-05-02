package alexdev_.buycraftapi.Tasks;

import alexdev_.buycraftapi.Main;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Tasks {

    private final Plugin placeholderapi;
    private final PlaceholderAPIPlugin papi;



    public Tasks(Plugin placeholderapi) {
        this.placeholderapi = placeholderapi;
        papi = (PlaceholderAPIPlugin) placeholderapi;
    }


    public int loadPaymentsTask(){
        return new BukkitRunnable() {
            @Override
            public void run() {
                Main.getInstance().paymentsManager.loadPayments();
                /*if (papi.getLocalExpansionManager().getExpansions().contains(Main.getInstance()))
                else {
                    placeholderapi.getLogger().log(Level.INFO, "[BuyCraftAPI] Task loadPayments cancelled");
                    cancel();
                }*/
            }
        }.runTaskTimerAsynchronously(placeholderapi, 200L, 20*3600+200).getTaskId();
    }

    public int loadSavePaymentsTask(){
       return new BukkitRunnable() {
            @Override
            public void run() {
                Main.getInstance().fileManager.savePaymentsInFile();
                /*if (papi.getLocalExpansionManager().getExpansions().contains(Main.getInstance()))
                else {
                    placeholderapi.getLogger().log(Level.INFO, "[BuyCraftAPI] Task savePaymentsInFile cancelled");
                    cancel();
                }*/
            }
        }.runTaskTimerAsynchronously(placeholderapi, 400L, 20*3600+400).getTaskId();
    }

    public int  loadCalcTotTask(){
        return new BukkitRunnable() {
            @Override
            public void run() {
                Main.getInstance().fileManager.calcTot();
                /*if (papi.getLocalExpansionManager().getExpansions().contains(Main.getInstance()))
                else {
                    placeholderapi.getLogger().log(Level.INFO, "[BuyCraftAPI] Task calcTot cancelled");
                    cancel();
                }*/
            }
        }.runTaskTimerAsynchronously(placeholderapi, 600L, 20*3600+600).getTaskId();
    }

    public int loadCalcMontlhyTask(){
       return new BukkitRunnable() {
            @Override
            public void run() {
                Main.getInstance().fileManager.calcMonthly();
                /*if (papi.getLocalExpansionManager().getExpansions().contains(Main.getInstance()))
                else {
                    placeholderapi.getLogger().log(Level.INFO, "[BuyCraftAPI] Task calcMonthly cancelled");
                    cancel();
                }*/
            }
        }.runTaskTimerAsynchronously(placeholderapi, 800L, 20*3600+800).getTaskId();
    }

    public int loadCalcCurrentMonthTask(){
       return new BukkitRunnable() {
            @Override
            public void run() {
                Main.getInstance().fileManager.calcCurrentMonth();
                /*if (papi.getLocalExpansionManager().getExpansions().contains(Main.getInstance()))
                else {
                    placeholderapi.getLogger().log(Level.INFO, "[BuyCraftAPI] Task calcCurrentMonth cancelled");
                    cancel();
                }*/
            }
        }.runTaskTimerAsynchronously(placeholderapi, 1000L, 20*3600+1000).getTaskId();
    }


}
