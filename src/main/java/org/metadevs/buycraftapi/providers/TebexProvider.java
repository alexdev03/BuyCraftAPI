package org.metadevs.buycraftapi.providers;

import io.tebex.sdk.Tebex;

public class TebexProvider extends Provider {
    public TebexProvider() {
        super(Tebex.get().getPlatformConfig().getSecretKey());
    }
}
