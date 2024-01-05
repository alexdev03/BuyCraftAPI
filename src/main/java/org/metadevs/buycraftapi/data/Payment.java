package org.metadevs.buycraftapi.data;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class Payment {

    private final int id;
    private final double amount;
    private final LocalDateTime date;
    private final String gateway;
    private final String status;
    private final String currency;
    private final String email;
    private final String name;
    private final UUID uuid;
    private final List<Package> packages;


}
