package org.metadevs.buycraftapi.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

public class Request {

    private List<Payment> payments;
    private List<Package> packages;
    private final String secret;

    public Request(String secret) {
        this.payments = new CopyOnWriteArrayList<>();
        this.packages = new CopyOnWriteArrayList<>();
        this.secret = secret;
    }

    public CompletableFuture<List<Payment>> getAllPayments() {
        return CompletableFuture.supplyAsync(() -> {


            JSONObject response = null;
            try {
                response = getPaymentsByPage(1).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            int finalPage = response.getInt("last_page");

            List<Payment> payments = new CopyOnWriteArrayList<>(getPayment(response));

            if (finalPage <= 1) {
                return payments;
            }

            for (int i = 2; i <= finalPage; i++) {
                List<Payment> pays = null;
                try {
                    pays = getPayment(getPaymentsByPage(i).get());
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                payments.addAll(pays);
            }

            return payments;

        });
    }

    public List<Payment> getPayment(JSONObject jsonObject) {

        JSONArray array = jsonObject.getJSONArray("data");
        List<Payment> payments = new ArrayList<>();

        for (Object payment : array) {
            JSONObject paymentObject = (JSONObject) payment;

            int id = paymentObject.getInt("id");

            String status = paymentObject.getString("status");

            double amount = paymentObject.getDouble("amount");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
            LocalDateTime date = LocalDateTime.parse(paymentObject.getString("date"), formatter);


            String email = paymentObject.getString("email");

            String gateway = paymentObject.getJSONObject("gateway").getString("name");

            String currency = paymentObject.getJSONObject("currency").getString("iso_4217");

            UUID uuid = convertUUID(paymentObject.getJSONObject("player").getString("uuid"));
            String name = paymentObject.getJSONObject("player").getString("name");

            List<Package> packageList = new ArrayList<>();
            JSONArray packages = paymentObject.getJSONArray("packages");

            for (Object packageObject : packages) {
                JSONObject pack = (JSONObject) packageObject;
                int idPack = pack.getInt("id");
                String namePack = pack.getString("name");

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

        }

        return payments;
    }

    public CompletableFuture<JSONObject> getPaymentsByPage(int page) {
        return CompletableFuture.supplyAsync(() -> {


            String url = "https://plugin.tebex.io/payments?paged=1&page=" + page;


            HttpURLConnection con = null;

            try {
                con = (HttpURLConnection) URI.create(url).toURL().openConnection();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                con.setRequestMethod("GET");
            } catch (ProtocolException e) {
                throw new RuntimeException(e);
            }

            con.setRequestProperty("Content-Type", "application/json");

            con.setRequestProperty("X-Tebex-Secret", secret);

            con.setDoOutput(true);

            try {
                con.connect();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            JSONObject json;

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                json = new JSONObject(response.toString());
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }


            return json;
        });
    }

    public UUID convertUUID(String uuid) {//from f1633fcc17ed11ed861d0242ac120002 to f1633fcc-17ed-11ed-861d-0242ac120002
        return java.util.UUID.fromString(
                uuid
                        .replaceFirst(
                                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"
                        )
        );
    }


}
