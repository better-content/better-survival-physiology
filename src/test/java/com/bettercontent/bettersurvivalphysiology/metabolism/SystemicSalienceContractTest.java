package com.bettercontent.bettersurvivalphysiology.metabolism;

import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemicSalienceContractTest {
    @Test
    void thresholdsPreservePreparationOrdering() {
        assertTrue(SalienceConfig.FIRST < SalienceConfig.SECOND);
        assertTrue(SalienceConfig.SECOND < SalienceConfig.THIRD);
        assertTrue(SalienceConfig.THIRD < SalienceConfig.FOURTH);
    }

}
