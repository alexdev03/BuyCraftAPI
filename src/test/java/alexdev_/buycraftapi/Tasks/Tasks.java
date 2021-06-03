package alexdev_.buycraftapi.Tasks;

import alexdev_.buycraftapi.Main;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Tasks {

    private final Plugin placeholderapi;
    private final PlaceholderAPIPlugin papi;
    private final int time; //times in minutes



    public Tasks(Plugin placeholderapi, int time) {
        this.placeholderapi = placeholderapi;
        papi = (PlaceholderAPIPlugin) placeholderapi;
        this.time = time;
    }


    public int loadPaymentsTask(){
        return new BukkitRunnable() {
            @Override
            public void run() {
                Main.getInstance().paymentsManager.loadPayments();
            }
        }.runTaskTimerAsynchronously(placeholderapi, 200L, (long) time *20*60+200).getTaskId();
    }

    public int loadSavePaymentsTask(){
       return new BukkitRunnable() {
            @Override
            public void run() {
                Main.getInstance().fileManager.savePaymentsInFile();
            }
        }.runTaskTimerAsynchronously(placeholderapi, 400L, (long) time *20*60+400).getTaskId();
    }

    public int  loadCalcTotTask(){
        return new BukkitRunnable() {
            @Override
            public void run() {
                Main.getInstance().fileManager.calcTot();
            }
        }.runTaskTimerAsynchronously(placeholderapi, 600L, (long) time *20*60+600).getTaskId();
    }

    public int loadCalcMontlhyTask(){
       return new BukkitRunnable() {
            @Override
            public void run() {
                Main.getInstance().fileManager.calcMonthly();
            }
        }.runTaskTimerAsynchronously(placeholderapi, 800L, (long) time *20*60+800).getTaskId();
    }

    public int loadCalcCurrentMonthTask(){
       return new BukkitRunnable() {
            @Override
            public void run() {
                Main.getInstance().fileManager.calcCurrentMonth();
            }
        }.runTaskTimerAsynchronously(placeholderapi, 1000L, (long) time *20*60+1000).getTaskId();
    }


}
