package com.bettercontent.bettersurvivalphysiology.nutrition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Authored, never executed without explicit test-run permission. */
final class NativeDietRosterTest {
    @Test
    void filterPrecedesBothNativeNamespacePassesAndDoesNotDiscardUnknownConfiguration() {
        JsonObject definition = new JsonObject();
        var diet = new ResourceLocation("diet", "fruits");
        var alternate = new ResourceLocation("other_mod", "sugars");
        var accepted = new ResourceLocation("diet", "carbs");
        var unknown = new ResourceLocation("other_mod", "unexpected_nutrient");
        Map<ResourceLocation, JsonElement> loaded = Map.of(
                diet, definition, alternate, definition, accepted, definition, unknown, definition);
        var filtered = NativeDietRoster.excludeRetired(loaded);
        assertEquals(Set.of(accepted, unknown), filtered.keySet());
        assertEquals(4, loaded.size(), "resource listener input stays intact across reloads");
        assertThrows(IllegalStateException.class, () -> DietAwardGuard.validateRoster(
                Set.of("proteins", "carbs", "spice", "vegetables", "alcohol", "unexpected_nutrient"),
                NativeDietRoster.GROUPS));
        assertThrows(IllegalStateException.class, () -> DietAwardGuard.validateRoster(
                NativeDietRoster.GROUPS, Set.of("proteins", "carbs", "spice", "vegetables")));
        assertDoesNotThrow(() -> DietAwardGuard.validateRoster(NativeDietRoster.GROUPS, NativeDietRoster.GROUPS));
    }
}
