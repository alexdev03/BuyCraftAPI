package org.metadevs.buycraftapi.payments;


import lombok.Getter;
import org.metadevs.buycraftapi.BuyCraftAPI;
import org.metadevs.buycraftapi.data.Payment;
import org.metadevs.buycraftapi.data.TopValue;
import org.metadevs.buycraftapi.data.Type;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class Query {

    private final BuyCraftAPI buyCraftAPI;
    private final List<Payment> payments;
    private List<Payment> monthlyPayments;
    private List<Payment> currentMonthPayments;

    private final ExecutorService executorService;

    public Query(BuyCraftAPI buyCraftAPI) {
        this.buyCraftAPI = buyCraftAPI;
        this.payments = new CopyOnWriteArrayList<>();
        this.monthlyPayments = new CopyOnWriteArrayList<>();
        this.currentMonthPayments = new CopyOnWriteArrayList<>();
        this.executorService = Executors.newFixedThreadPool(25);
    }

    public CompletableFuture<Boolean> loadPayments() {
        return buyCraftAPI.getRequest().getAllPayments().thenApply(payments -> {

            if (payments == null) {
                return false;
            }

            buyCraftAPI.getPlaceholdersIstance().setMaxPayments(payments.size());

            monthlyPayments = new CopyOnWriteArrayList<>();
            currentMonthPayments = new CopyOnWriteArrayList<>();

            for (Payment payment : payments) {

                if (payment.getDate().isAfter(LocalDateTime.now().minusDays(30))) {
                    monthlyPayments.add(payment);
                }

                if (payment.getDate().getMonth().equals(LocalDateTime.now().getMonth()) && payment.getDate().getYear() == LocalDateTime.now().getYear()) {
                    currentMonthPayments.add(payment);
                }
            }

            Comparator<Payment> amount = Comparator.comparingDouble(Payment::getAmount);

            monthlyPayments.sort(amount);
            currentMonthPayments.sort(amount);
            payments.sort(amount);

            this.payments.clear();
            this.payments.addAll(payments);

            return true;
        }).exceptionally(e -> {
            throw new RuntimeException(e);
        });
    }

    public String getPlayerTotal(String player) {
        double total = 0;
        for (Payment payment : payments) {
            if (payment.getName().equals(player)) {
                total += payment.getAmount();
            }
        }
        return String.format("%.2f", total);
    }

    public String getPlayerFromTop(Type top, int position) {
        switch (top) {
            case GLOBAL: {
                if (payments.size() > position) {

                    return payments.get(position).getName();
                }
            }
            case MONTHLY: {
                if (monthlyPayments.size() > position) {
                    return monthlyPayments.get(position).getName();
                }
            }
            case CURRENT_MONTH: {
                if (currentMonthPayments.size() > position) {
                    return currentMonthPayments.get(position).getName();
                }
            }
            default:
                return null;
        }
    }

    public TopValue getTop(Type type, int position) {
        switch (type) {
            case GLOBAL: {
                return getTop(payments, position);
            }
            case MONTHLY: {
                return getTop(monthlyPayments, position);
            }
            case CURRENT_MONTH: {
                return getTop(currentMonthPayments, position);
            }
            default:
                return null;
        }
    }

    private TopValue getTop(List<Payment> payments, int position) {
        if (payments == null) {
            buyCraftAPI.getLogger().severe("Payments list is null");
            return null;
        }

        if (payments.isEmpty()) {
            buyCraftAPI.getLogger().severe("Payments list is empty");
            return null;
        }

        if (position > payments.size()) {
            buyCraftAPI.getLogger().severe("Position " + position + " is bigger than the size of the list " + payments.size());
            return null;
        }


        Map<UUID, Double> map = new HashMap<>();
        for (Payment payment : payments) {
            if (map.containsKey(payment.getUuid())) {
                map.put(payment.getUuid(), map.get(payment.getUuid()) + payment.getAmount());
            } else {
                map.put(payment.getUuid(), payment.getAmount());
            }
        }

        NavigableMap<UUID, Double> sortedMap = new TreeMap<>(Comparator.comparingDouble(map::get).reversed());
        sortedMap.putAll(map);

        if (position > sortedMap.size()) {
//            buyCraftAPI.getLogger().severe("Position " + position + " is bigger than the size of the map " + sortedMap.size());
            return null;
        }

        UUID uuid = sortedMap.keySet().toArray(new UUID[0])[position - 1];

        double amount = sortedMap.get(uuid);

        Optional<Payment> payment = payments.stream().filter(p -> p.getUuid().equals(uuid)).findFirst();

        if (!payment.isPresent()) {
            buyCraftAPI.getLogger().severe("Payment not found for uuid " + uuid);
            return null;
        }

        String name = payment.get().getName();

//        buyCraftAPI.getLogger().info("Top " + position + " " + name + " " + amount);

        return new TopValue(name, uuid, amount);
    }


    public double getValueFromTop(Type top, int position) {
        switch (top) {
            case GLOBAL: {
                if (payments.size() > position) {
                    return payments.get(position).getAmount();
                }
            }
            case MONTHLY: {
                if (monthlyPayments.size() > position) {
                    return monthlyPayments.get(position).getAmount();
                }
            }
            case CURRENT_MONTH: {
                if (currentMonthPayments.size() > position) {
                    return currentMonthPayments.get(position).getAmount();
                }
            }
            default:
                return -1;
        }
    }

    public double getAllMoneySpent(Type type) {
        double amount = 0D;
        switch (type) {
            case GLOBAL:
                for (Payment payment : payments) {
                    amount += payment.getAmount();
                }
                break;
            case MONTHLY:
                for (Payment payment : monthlyPayments) {
                    amount += payment.getAmount();
                }
                break;

            case CURRENT_MONTH:
                for (Payment payment : currentMonthPayments) {
                    amount += payment.getAmount();
                }
                break;
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


    public Payment getRecentPayment(int position) {
        if (payments.size() > position) {
            List<Payment> paymentList = new ArrayList<>(payments);
            paymentList.sort(Comparator.comparing(Payment::getDate));
            Collections.reverse(paymentList);
//            buyCraftAPI.getLogger().info("Recent payment " + paymentList.get(position).getName() + " " + paymentList.get(position).getAmount());
            return paymentList.get(position);
        }
        buyCraftAPI.getLogger().severe("Position " + position + " is bigger than the size of the list " + payments.size());
        return null;
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
