package org.metadevs.buycraftapi.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.metadevs.buycraftapi.BuyCraftAPI;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static java.lang.Thread.sleep;

public class Request {

    public static final int MAX_REQUESTS = 500;
    public static final int PERIOD = 300;
    private final List<Package> packages;
    private final String secret;
    private final BuyCraftAPI buyCraftAPI;
    private final OkHttpClient client;
    private final Gson gson;

    public Request(String secret, BuyCraftAPI buyCraftAPI) {
        this.packages = new CopyOnWriteArrayList<>();
        this.secret = secret;
        this.buyCraftAPI = buyCraftAPI;
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public CompletableFuture<List<Payment>> getAllPayments() {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject response;
            long start = System.currentTimeMillis();

            try {
                response = getPaymentsByPage2(1).get(2, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }

            final int finalPage = response.get("last_page").getAsInt();
            final long total = response.get("total").getAsLong();
            final boolean onlyLastMonth = buyCraftAPI.getConfigManager().isLoadOnlyLastMonthPayments();

            System.out.println("onlyLastMonth = " + onlyLastMonth);

            if (onlyLastMonth) {
                buyCraftAPI.getLogger().info("Loading only last month payments...");
            }

            buyCraftAPI.getLogger().info("Loading " + total + " payments..." + " Pages: " + finalPage);

            final List<Payment> totalPayments = new CopyOnWriteArrayList<>(getPayment(response));

            if (onlyLastMonth) {
                totalPayments.removeIf(p -> p.getDate().isBefore(LocalDateTime.now().minusMonths(1)));
                if (totalPayments.isEmpty()) {
                    buyCraftAPI.getLogger().info("Payments are empty");
                    return totalPayments;
                }
            }

            buyCraftAPI.getLogger().info("Loaded " + totalPayments.size() + " payments of " + total + ". Page " + 1 + " of " + finalPage);

            if (finalPage <= 1) {
                return totalPayments;
            }

            int requests = 1;
            for (int i = 2; i <= finalPage; i++) {
                List<Payment> payments;
                try {
                    payments = getPayment(getPaymentsByPage2(i).get(5, TimeUnit.SECONDS));

                    if (onlyLastMonth) {
                        payments = getAllPaymentsInThisMonth(payments);
                        if(payments.isEmpty()) {
                            break;
                        }
                    }

                    sleep(20);
                    requests++;
                    if (requests >= MAX_REQUESTS - 1) {
                        requests = 0;
                        buyCraftAPI.getLogger().info("Waiting " + PERIOD + " seconds to avoid rate limit");
                        sleep(PERIOD);
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    buyCraftAPI.getLogger().info("Error: " + e.getMessage());
                    continue;
                }

                totalPayments.addAll(payments);
                buyCraftAPI.getLogger().info("Loaded " + totalPayments.size() + " payments of " + total + ". Page " + i + " of " + finalPage);
            }

            buyCraftAPI.getLogger().info("Loaded " + totalPayments.size() + " payments of " + total + " in " + (System.currentTimeMillis() - start) + "ms");
            buyCraftAPI.getLogger().info((total - totalPayments.size()) + " were not loaded as they are manual payments");

            return totalPayments;
        }, buyCraftAPI.getQuery().getExecutorService());
    }

    private List<Payment> getAllPaymentsInThisMonth(@NotNull List<Payment> payments) {
        final LocalDateTime month = LocalDateTime.now().minusMonths(1);
        return payments.stream()
                .filter(p -> p.getDate().isAfter(month))
                .toList();
    }

    public List<Payment> getPayment(JsonObject jsonObject) {

        final JsonArray array = jsonObject.getAsJsonArray("data");
        final List<Payment> payments = new ArrayList<>();

        for (JsonElement payment : array) {
            try {
                final JsonObject paymentObject = payment.getAsJsonObject();
                final int id = paymentObject.get("id").getAsInt();
                final String status = paymentObject.get("status").getAsString();

                if (!status.equals("Complete")) {
                    continue;
                }

                final double amount = paymentObject.get("amount").getAsDouble();
                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                final LocalDateTime date = LocalDateTime.parse(paymentObject.get("date").getAsString(), formatter);
                final String email = paymentObject.get("email").getAsString();
                final String gateway = paymentObject.getAsJsonObject("gateway").get("name").getAsString();
                final String currency = paymentObject.getAsJsonObject("currency").get("iso_4217").getAsString();
                final UUID uuid = convertUUID(paymentObject.getAsJsonObject("player").get("uuid").getAsString());
                final String name = paymentObject.getAsJsonObject("player").get("name").getAsString();
                final List<Package> packageList = new ArrayList<>();
                final JsonArray packages = paymentObject.getAsJsonArray("packages");

                for (JsonElement packageObject : packages) {
                    final JsonObject pack = packageObject.getAsJsonObject();
                    final int idPack = pack.get("id").getAsInt();
                    final String namePack = pack.get("name").getAsString();
                    final Optional<Package> optional = this.packages.stream().filter(p -> p.getId() == idPack).findFirst();

                    if (optional.isPresent()) {
                        packageList.add(optional.get());
                    } else {
                        Package package1 = new Package(idPack, namePack);
                        packageList.add(package1);
                        this.packages.add(package1);
                    }
                }

                final Payment currentPayment = new Payment(id, amount, date, gateway, status, currency, email, name, uuid, packageList);
                payments.add(currentPayment);
            } catch (Exception e) {
                buyCraftAPI.getLogger().info("Error: " + e.getMessage());
            }

        }

        return payments;
    }

    public CompletableFuture<JsonObject> getPaymentsByPage2(int page) {
        final CompletableFuture<JsonObject> completableFuture = new CompletableFuture<>();
        final String url = constructURL(page);
        final okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Tebex-Secret", secret)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.code() == 200) {

                    if (response.body() == null) {
                        completableFuture.completeExceptionally(new Exception("Error: " + response.code()));
                        return;
                    }

                    final String body = response.body().string();
                    final JsonObject json = gson.fromJson(body, JsonObject.class);
                    completableFuture.complete(json);
                } else if (response.code() == 429) {
                    completableFuture.completeExceptionally(new Exception("Rate Limit Exception: " + response.code() + " - " + response.message()));
                } else {
                    completableFuture.completeExceptionally(new Exception("Error: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                completableFuture.completeExceptionally(e);
            }
        });

        return completableFuture;
    }

    private String constructURL(int page) {
        return "https://plugin.tebex.io/payments?paged=1&page=" + page;
    }

    public UUID convertUUID(String uuid) {
        return java.util.UUID.fromString(
                uuid.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"
                )
        );
    }


}
