package org.metadevs.buycraftapi.config;

import com.google.common.collect.Maps;
import lombok.Getter;
import org.metadevs.buycraftapi.BuyCraftAPI;

import java.util.Map;

@Getter
public class ConfigManager {

    private final BuyCraftAPI instance;
    private boolean loadOnlyLastMonthPayments;

    public ConfigManager(BuyCraftAPI instance) {
        this.instance = instance;
        this.loadConfig();
    }

    private void loadConfig() {
        loadOnlyLastMonthPayments = instance.getBoolean("load_only_last_month", false);
    }

    public Map<String, Object> getDefaults() {
        final Map<String, Object> map = Maps.newHashMap();
        map.put("load_only_last_month", false);
        return map;
    }
}
