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
import java.util.stream.Collectors;

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

        return new TopValue(name, uuid, amount);
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


    public Payment getRecentPayment(int position) {
        if (payments.size() > position) {
            List<Payment> paymentList = new ArrayList<>(payments);
            paymentList.sort(Comparator.comparing(Payment::getDate));
            Collections.reverse(paymentList);
            return paymentList.get(position);
        }
        buyCraftAPI.getLogger().severe("Position " + position + " is bigger than the size of the list " + payments.size());
        return null;
    }


    public boolean isNotNumeric(String num) {

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

    public String getTotalValue(Type type) {
        switch (type) {
            case GLOBAL:
                return findTop(payments);
            case MONTHLY:
                return findTop(monthlyPayments);
            case CURRENT_MONTH:
                return findTop(currentMonthPayments);
            default:
                return "N/A";
        }
    }

    private String findTop(List<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            return "N/A";
        }

        Map<UUID, Double> map = payments.stream()
                .collect(Collectors.groupingBy(Payment::getUuid, Collectors.summingDouble(Payment::getAmount)));

        Optional<UUID> uuidOptional = map.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey);

        if (!uuidOptional.isPresent()) {
            return "N/A";
        }

        UUID uuid = uuidOptional.get();

        return payments.stream()
                .filter(p -> p.getUuid().equals(uuid))
                .findFirst()
                .map(Payment::getName)
                .orElseThrow(() -> new NoSuchElementException("Payment not found for uuid " + uuid));
    }
}
