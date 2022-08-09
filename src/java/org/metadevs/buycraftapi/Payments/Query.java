package org.metadevs.buycraftapi.Payments;


import org.metadevs.buycraftapi.BuyAPI;
import org.metadevs.buycraftapi.data.Payment;
import lombok.Getter;
import org.metadevs.buycraftapi.data.TopValue;
import org.metadevs.buycraftapi.data.Type;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class Query {

    private final BuyAPI buyAPI;
    private List<Payment> payments;
    private List<Payment> monthlyPayments;
    private List<Payment> currentMonthPayments;

    public Query(BuyAPI buyAPI) {
        this.buyAPI = buyAPI;
        this.payments = new ArrayList<>();
        this.monthlyPayments = new ArrayList<>();
        this.currentMonthPayments = new ArrayList<>();
    }

    public CompletableFuture<Boolean> loadPayments() {
        return CompletableFuture.supplyAsync(() -> {

            Semaphore semaphore = new Semaphore(0);

            AtomicBoolean success = new AtomicBoolean(true);

            buyAPI.getRequest().getAllPayments().whenComplete((payments, throwable) -> {

                if (throwable != null) {
                    throwable.printStackTrace();
                    success.set(false);
                }

                buyAPI.getPlaceholdersClass().setMaxPayments(payments.size());

                monthlyPayments = new ArrayList<>();
                currentMonthPayments = new ArrayList<>();

                for (Payment payment : payments) {

                    //check if payments is in the last 30 days
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


                this.payments = payments;
                semaphore.release();
            });

            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            return success.get();
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
                if (payments.size() > 0) {
                    Map<UUID, Double> map = new HashMap<>();
                    for(Payment payment : payments) {
                        if(map.containsKey(payment.getUuid())) {
                            map.put(payment.getUuid(), map.get(payment.getUuid()) + payment.getAmount());
                        } else {
                            map.put(payment.getUuid(), payment.getAmount());
                        }
                    }

                    if(position > map.size()) {
                        return null;
                    }

                    Map<UUID, Double> sortedMap = new TreeMap<>(Comparator.comparingDouble(map::get).reversed());
                    sortedMap.putAll(map);

                    String name = payments.stream().filter(p -> p.getUuid().equals(sortedMap.keySet().toArray()[position - 1])).findFirst().orElse(null).getName();
                    UUID uuid = (UUID) sortedMap.keySet().toArray()[position - 1];
                    double amount = sortedMap.get(uuid);

                    return new TopValue(name, uuid, amount);
                }
            }
            case MONTHLY: {
                if (monthlyPayments.size() > 0) {
                    Map<UUID, Double> map = new HashMap<>();
                    for(Payment payment : monthlyPayments) {
                        if(map.containsKey(payment.getUuid())) {
                            map.put(payment.getUuid(), map.get(payment.getUuid()) + payment.getAmount());
                        } else {
                            map.put(payment.getUuid(), payment.getAmount());
                        }
                    }

                    if(position > map.size()) {
                        return null;
                    }

                    Map<UUID, Double> sortedMap = new TreeMap<>(Comparator.comparingDouble(map::get).reversed());
                    sortedMap.putAll(map);

                    String name = payments.stream().filter(p -> p.getUuid().equals(sortedMap.keySet().toArray()[position - 1])).findFirst().orElse(null).getName();
                    UUID uuid = (UUID) sortedMap.keySet().toArray()[position - 1];
                    double amount = sortedMap.get(uuid);

                    return new TopValue(name, uuid, amount);
                }
            }
            case CURRENT_MONTH: {
                if (currentMonthPayments.size() > 0) {
                    Map<UUID, Double> map = new HashMap<>();
                    for(Payment payment : currentMonthPayments) {
                        if(map.containsKey(payment.getUuid())) {
                            map.put(payment.getUuid(), map.get(payment.getUuid()) + payment.getAmount());
                        } else {
                            map.put(payment.getUuid(), payment.getAmount());
                        }
                    }

                    if(position > map.size()) {
                        return null;
                    }

                    Map<UUID, Double> sortedMap = new TreeMap<>(Comparator.comparingDouble(map::get).reversed());
                    sortedMap.putAll(map);




                    String name = payments.stream().filter(p -> p.getUuid().equals(sortedMap.keySet().toArray()[position - 1])).findFirst().orElse(null).getName();
                    UUID uuid = (UUID) sortedMap.keySet().toArray()[position - 1];
                    double amount = sortedMap.get(uuid);

                    return new TopValue(name, uuid, amount);
                }
            }
            default:
                return null;
        }
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
            case GLOBAL -> {
                for (Payment payment : payments) {
                    amount += payment.getAmount();
                }
            }
            case MONTHLY -> {
                for (Payment payment : monthlyPayments) {
                    amount += payment.getAmount();
                }
            }
            case CURRENT_MONTH -> {
                for (Payment payment : currentMonthPayments) {
                    amount += payment.getAmount();
                }
            }
            default -> amount = -1D;
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
            return paymentList.get(position);
        }
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
