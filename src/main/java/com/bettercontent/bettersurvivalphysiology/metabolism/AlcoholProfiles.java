package com.bettercontent.bettersurvivalphysiology.metabolism;

import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;

import java.util.Map;

public final class AlcoholProfiles {
    private AlcoholProfiles() {}

    public static double potency(String itemId) {
        return com.bettercontent.bettersurvivalphysiology.api.BeverageProfiles.alcoholPotency(itemId);
    }

    public static Map<String, Double> all() {
        return SalienceConfig.alcoholPotencies();
    }
}
