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

            int finalPage = response.get("last_page").getAsInt();

            long total = response.get("total").getAsLong();

            buyCraftAPI.getLogger().info("Loading " + total + " payments..." + " Pages: " + finalPage);

            List<Payment> payments = new CopyOnWriteArrayList<>(getPayment(response));

            if (finalPage <= 1) {
                return payments;
            }

            for (int i = 2; i <= finalPage; i++) {
                List<Payment> pays;
                try {
                    pays = getPayment(getPaymentsByPage2(i).get(5, TimeUnit.SECONDS));
                    sleep(1200);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    buyCraftAPI.getLogger().info("Error: " + e.getMessage());
                    continue;
                }

                payments.addAll(pays);
                buyCraftAPI.getLogger().info("Loaded " + payments.size() + " payments of " + total + ". Page " + i + " of " + finalPage);
            }

            buyCraftAPI.getLogger().info("Loaded " + payments.size() + " payments of " + total + " in " + (System.currentTimeMillis() - start) + "ms");

            return payments;

        }, buyCraftAPI.getQuery().getExecutorService());
    }

    public List<Payment> getPayment(JsonObject jsonObject) {

        JsonArray array = jsonObject.getAsJsonArray("data");
        List<Payment> payments = new ArrayList<>();

        for (JsonElement payment : array) {
            try {
                JsonObject paymentObject = payment.getAsJsonObject();

                int id = paymentObject.get("id").getAsInt();

                String status = paymentObject.get("status").getAsString();

                if (!status.equals("Complete")) {
                    continue;
                }

                double amount = paymentObject.get("amount").getAsDouble();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                LocalDateTime date = LocalDateTime.parse(paymentObject.get("date").getAsString(), formatter);


                String email = paymentObject.get("email").getAsString();

                String gateway = paymentObject.getAsJsonObject("gateway").get("name").getAsString();

                String currency = paymentObject.getAsJsonObject("currency").get("iso_4217").getAsString();

                UUID uuid = convertUUID(paymentObject.getAsJsonObject("player").get("uuid").getAsString());
                String name = paymentObject.getAsJsonObject("player").get("name").getAsString();

                List<Package> packageList = new ArrayList<>();
                JsonArray packages = paymentObject.getAsJsonArray("packages");

                for (JsonElement packageObject : packages) {
                    JsonObject pack = packageObject.getAsJsonObject();
                    int idPack = pack.get("id").getAsInt();
                    String namePack = pack.get("name").getAsString();

                    Optional<Package> optional = this.packages.stream().filter(p -> p.getId() == idPack).findFirst();

                    if (optional.isPresent()) {
                        packageList.add(optional.get());
                    } else {
                        Package package1 = new Package(idPack, namePack);
                        packageList.add(package1);
                        this.packages.add(package1);
                    }
                }

                Payment payment1 = new Payment(id, amount, date, gateway, status, currency, email, name, uuid, packageList);

                payments.add(payment1);
            } catch (Exception e) {
                buyCraftAPI.getLogger().info("Error: " + e.getMessage());
            }

        }

        return payments;
    }

    public CompletableFuture<JsonObject> getPaymentsByPage2(int page) {
        CompletableFuture<JsonObject> completableFuture = new CompletableFuture<>();

        String url = constructURL(page);

        okhttp3.Request request = new okhttp3.Request.Builder()
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

                    String body = response.body().string();
                    JsonObject json = gson.fromJson(body, JsonObject.class);
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
