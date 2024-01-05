package org.metadevs.buycraftapi.providers;

import net.buycraft.plugin.bukkit.BuycraftPlugin;

public class BuyCraftXProvider extends Provider {
    public BuyCraftXProvider() {
        super(BuycraftPlugin.getPlugin(BuycraftPlugin.class).getConfiguration().getServerKey());
    }
}
