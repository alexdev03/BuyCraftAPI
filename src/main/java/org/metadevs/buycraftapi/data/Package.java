package org.metadevs.buycraftapi.data;


import lombok.Getter;

@Getter
public class Package {

    private final int id;
    private final String name;
    public Package(int id, String name) {
        this.id = id;
        this.name = name;
    }
}