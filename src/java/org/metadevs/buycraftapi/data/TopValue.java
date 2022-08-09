package org.metadevs.buycraftapi.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class TopValue {

    private String name;
    private UUID uuid;
    private double amount;

}
