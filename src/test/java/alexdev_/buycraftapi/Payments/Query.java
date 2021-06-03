package alexdev_.buycraftapi.Payments;

import alexdev_.buycraftapi.FileManager.FileManager;
import alexdev_.buycraftapi.Main;
import net.buycraft.plugin.data.RecentPayment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class Query {

    private final FileManager fileManager;

    public Query(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    public String getPlayerTotal(String player){
        for(String index : Objects.requireNonNull(fileManager.getFileConfiguration().getConfigurationSection("Global")).getKeys(false)){
            String tmpPlayer = fileManager.getFileConfiguration().getString("Global."+index+".Name");
            if(tmpPlayer!=null && tmpPlayer.equalsIgnoreCase(player)){
                return fileManager.getFileConfiguration().getDouble("Global."+index+".Value")+"";
            }
        }
        return null;
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
            case "CurrentMonth": {
                if (fileManager.getFileConfiguration().isConfigurationSection("CurrentMonth." + position)) {
                    return fileManager.getFileConfiguration().getString("CurrentMonth." + position + ".Name");
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
            case "CurrentMonth": {
                if (fileManager.getFileConfiguration().isConfigurationSection("CurrentMonth." + position)) {
                    return fileManager.getFileConfiguration().getDouble("CurrentMonth." + position + ".Value");
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
                    amount += Double.parseDouble(fileManager.getFileConfiguration().getString("Global." + id + ".Value"));
                }
                break;
            }
            case "Monthly":{
                for(String id : fileManager.getFileConfiguration().getConfigurationSection("Monthly").getKeys(false)){
                    amount += Double.parseDouble(fileManager.getFileConfiguration().getString("Monthly." + id + ".Value"));
                }
                break;
            }
            case "CurrentMonth": {
                for(String id : fileManager.getFileConfiguration().getConfigurationSection("CurrentMonth").getKeys(false)){
                    amount += Double.parseDouble(fileManager.getFileConfiguration().getString("CurrentMonth." + id + ".Value"));
                }
                break;
            }
            default:
                amount = -1D;
        }
        return amount;

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
        if (position < Main.recentPayments.size()) {
            String[] data = new String[3];

            RecentPayment payment = Main.recentPayments.get(position);
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


        for (RecentPayment recentPayment : Main.recentPayments) {
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
        for (RecentPayment recentPayment : Main.recentPayments) {
            if (!currency.containsKey(recentPayment.getPlayer().getName())) {
                currency.put(recentPayment.getPlayer().getName(), recentPayment.getCurrency().getIso4217());
            }
        }
        return currency;
    }

    public boolean checkNumExeption(String num) {

        try {
            Integer.parseInt(num);
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static List<Double> sortMap(HashMap<String, Double> values) {
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
}
