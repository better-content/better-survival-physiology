package com.bettercontent.bettersurvivalphysiology.runtime;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Wiring/native bytecode contracts, not a claim of observed in-game audiovisual acceptance. */
class HeavyBlowWiringTest {
    private static final Path SOURCE = Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology");

    @Test
    void presentationAndBothImpactBackendsShareOnePredicateAndOneAttackCue() throws Exception {
        String events = Files.readString(SOURCE.resolve("runtime/SalienceEvents.java"));
        String presentation = Files.readString(SOURCE.resolve("presentation/PresentationSnapshot.java"));
        assertTrue(presentation.contains("HeavyBlow.ready(nutrition.proteins(), prepared, state.heavyBlowCooldown,"));
        assertTrue(presentation.contains("player.getAttackStrengthScale(0.5f), state.blackout)"));
        String attack = events.substring(events.indexOf("public static void onAttack("),
                events.indexOf("public static void onMeleeDamage("));
        assertTrue(attack.contains("HeavyBlow.ready(proteins, SalienceConfig.SECOND, state.heavyBlowCooldown,"));
        assertTrue(attack.contains("player.getAttackStrengthScale(0.5f), state.blackout)"));
        // Nutrition owns damage; RPG Strength owns stagger and knockback.
        assertFalse(attack.contains("EpicFightCompat.tryImpact"));
        assertFalse(attack.contains("target.knockback("));
        assertEquals(1, occurrences(events, "cueAt(player, AspectIdentity.IMPACT, \"Heavy Blow\""));
        assertEquals(1, occurrences(events, "activation(player, AspectIdentity.IMPACT, \"Heavy Blow ready\")"));
        assertTrue(attack.contains("state.heavyBlowFeedback.attacked();"));
        assertFalse(Files.readString(SOURCE.resolve("compat/EpicFightCompat.java")).contains("cueAt("));
        assertFalse(Files.readString(SOURCE.resolve("compat/EpicFightCompat.java")).contains("playSound("));
    }

    @Test
    void pinnedEpicFightHitsUseNativePlayerAttackRatherThanNeedingAnotherCueListener() throws Exception {
        Path jar = Path.of(System.getProperty("salience.nativeEpicFight"));
        assertEquals("69566cf70ae2d91d3f2564c608f014c87e290cef6215c2a27719851165485f73",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));
        Path evidence = Files.createTempDirectory(Path.of("build"), "native-heavy-blow-").resolve("PlayerPatch.javap.txt");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javap").toString(),
                "-p", "-c", "-classpath", jar.toString(),
                "yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch")
                .redirectErrorStream(true).redirectOutput(evidence.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "native javap timed out: " + evidence);
            assertEquals(0, process.exitValue(), evidence.toString());
            String bytecode = Files.readString(evidence);
            int start = bytecode.indexOf("public yesman.epicfight.api.utils.AttackResult attack(");
            assertTrue(start >= 0);
            String attack = bytecode.substring(start, bytecode.indexOf("public yesman.epicfight.world.damagesource.EpicFightDamageSource getDamageSource(", start));
            // The native patch intentionally forces a full vanilla attack charge before Player.attack.
            int fullCharge = attack.indexOf("int 2147483647");
            int nativeAttack = attack.indexOf("net/minecraft/world/entity/player/Player.m_5706_:");
            assertTrue(fullCharge >= 0 && nativeAttack > fullCharge, attack);
            assertTrue(attack.contains("net/minecraft/world/entity/player/Player.f_20922_:I"));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                assertTrue(process.waitFor(10, TimeUnit.SECONDS), "javap cleanup failed");
            }
        }
    }

    private static int occurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
