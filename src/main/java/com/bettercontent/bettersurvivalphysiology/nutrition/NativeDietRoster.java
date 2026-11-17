package com.bettercontent.bettersurvivalphysiology.nutrition;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Filter Diet's parsed resource map before either namespace pass of its native group loader.
 * A JSON tombstone cannot remove a lower-priority bundled definition on reload. This also
 * covers non-diet namespaces whose paths would otherwise overwrite a canonical group. */
public final class NativeDietRoster {
    public static final Set<String> GROUPS = Set.of("proteins", "carbs", "spice", "vegetables", "alcohol");
    public static final Set<String> RETIRED = Set.of("grains", "fruits", "sugars", "dairy");

    private NativeDietRoster() {}

    public static Map<ResourceLocation, JsonElement> excludeRetired(Map<ResourceLocation, JsonElement> resources) {
        Map<ResourceLocation, JsonElement> filtered = new HashMap<>(resources);
        filtered.keySet().removeIf(id -> RETIRED.contains(id.getPath()));
        return filtered;
    }
}
