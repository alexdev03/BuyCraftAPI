package org.metadevs.buycraftapi.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@ToString
public class Payment {

    private int id;
    private double amount;
    private LocalDateTime date;
    private String gateway;
    private String status;
    private String currency;
    private String email;
    private String name;
    private UUID uuid;
    private List<Package> packages;


}
