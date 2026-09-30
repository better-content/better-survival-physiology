package com.bettercontent.bettersurvivalphysiology.metabolism;

import com.bettercontent.bettersurvivalphysiology.presentation.AspectIdentity;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DietMetabolicGroupsTest {
    @Test
    void optionalMetabolicGroupsAppearInTheNativeSuiteWithoutGenericEffects() throws Exception {
        var suite = json("/data/diet/diet/suites/builtin.json");
        Set<String> groups = new HashSet<>();
        suite.getAsJsonArray("groups").forEach(group -> groups.add(group.getAsString()));
        assertEquals(Set.of("proteins", "grains", "fruits", "fats", "vegetables", "dairy", "sugars", "alcohol"), groups);
        assertTrue(suite.getAsJsonArray("effects").isEmpty());
        assertEquals(AspectIdentity.TEMPO, AspectIdentity.fromGroupName("diet:sugars"));
        assertEquals(AspectIdentity.CONTROL, AspectIdentity.fromGroupName("diet:alcohol"));
        for (AspectIdentity aspect : AspectIdentity.values()) {
            String group = aspect == AspectIdentity.TEMPO ? "sugars" : aspect.representative;
            var definition = json("/data/diet/diet/groups/" + group + ".json");
            assertEquals(aspect.icon, definition.get("order").getAsInt());
            assertEquals(String.format("#%06X", aspect.color), definition.get("color").getAsString());
        }
    }

    @Test
    void dietFoodTagsCoverEveryConfiguredDrinkAndSugarProfile() throws Exception {
        var drinks = json("/data/diet/tags/items/alcohol.json").getAsJsonArray("values");
        Set<String> tagged = new HashSet<>();
        drinks.forEach(item -> {
            assertFalse(item.getAsJsonObject().get("required").getAsBoolean());
            tagged.add(item.getAsJsonObject().get("id").getAsString());
        });
        assertEquals(AlcoholProfiles.all().keySet(), tagged);

        var sugars = json("/data/diet/tags/items/sugars.json").getAsJsonArray("values");
        Set<String> included = new HashSet<>();
        sugars.forEach(item -> included.add(item.getAsString()));
        assertTrue(included.contains("#better_survival_physiology:sugar_medium"));
        assertTrue(included.contains("#better_survival_physiology:sugar_high"));
        assertFalse(json("/data/diet/diet/groups/alcohol.json").get("beneficial").getAsBoolean());
    }

    private com.google.gson.JsonObject json(String path) throws Exception {
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
