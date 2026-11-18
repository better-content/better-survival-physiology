package com.bettercontent.bettersurvivalphysiology.presentation;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class IdentityAssetContractTest {
    private static final Map<String, String> MOTIF_HASHES = Map.of(
            "impact", "1371c31e65d10a4b9aff832bc0106ca97c6f81ff81b85b48f3355be864622da3",
            "tempo", "d06478a8c71c6b77fae7118b8f2ec9b87142b87b7b83717366c0d2dfa025ee87",
            "work", "11ee2b74351da1422015c214dd7c590e32eba40007292b1d6038f1961b963d05",
            "mobility", "40a40c4e6827af3e227ebc19e7af79abd5fad8877df05832c69e30e5d319da2c",
            "endurance", "391b23f4db4c7c149e1e6b95df6e7af5b59f20731f5229a7fc7fb2bb2d40f841",
            "robustness", "83b490e48cc0de79b6ca80ee7cd9c07df35d88537d060717835919338078335e",
            "renewal", "85c740fdb60b93f6e55083a6a1e1f76f8225ca1e8972c24ae9209db09cf0edaf",
            "control", "19bde4f9b28e3d769b1a6a2b7675f8f82672b9c1283d2103249c7adca73ccf88"
    );

    @Test
    void sugarCrashHasItsOwnBrokenTempoMotif() throws Exception {
        String sounds = new String(resource("/assets/better_survival_physiology/sounds.json").readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(sounds.contains("aspect.tempo_broken"));
        byte[] bytes = resource("/assets/better_survival_physiology/sounds/aspect/tempo_broken.ogg").readAllBytes();
        assertEquals("fb12d0e2905d11d5dab90f25fc5e635dd65dce57a2aa1e3e57b1c3f9240e8e23",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
    }

    @Test
    void everyIdentityHasARegisteredOggMotif() throws Exception {
        String sounds = new String(resource("/assets/better_survival_physiology/sounds.json").readAllBytes(), StandardCharsets.UTF_8);
        for (AspectIdentity aspect : AspectIdentity.values()) {
            String id = aspect.name().toLowerCase();
            String motif = switch (id) {
                case "power" -> "impact";
                case "motion" -> "mobility";
                case "endurance" -> "robustness";
                default -> id;
            };
            assertTrue(sounds.contains("\"aspect." + id + "\": { \"sounds\": [\"better_survival_physiology:aspect/" + motif + "\"] }"));
            byte[] bytes = resource("/assets/better_survival_physiology/sounds/aspect/" + motif + ".ogg").readAllBytes();
            assertTrue(bytes.length > 4000, id);
            assertArrayEquals(new byte[]{'O', 'g', 'g', 'S'}, java.util.Arrays.copyOf(bytes, 4));
            assertEquals(MOTIF_HASHES.get(motif),
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)), id);
        }
    }

    private InputStream resource(String path) {
        InputStream stream = getClass().getResourceAsStream(path);
        assertNotNull(stream, path);
        return stream;
    }
}
