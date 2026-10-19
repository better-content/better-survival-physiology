package com.bettercontent.bettersurvivalphysiology.nutrition;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Pinned native API boundary, not an in-game beverage acceptance test. */
class NativeBeverageDietContractTest {
    @Test
    void nativeConsumptionResolvesDietApiButCannotBeCalledForEachSip() throws Exception {
        Path jar = Path.of(System.getProperty("salience.nativeDiet"));
        assertEquals("c5f5d5980803259931dfa62236e888fb08ecf70a",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(jar))));
        Path output = Files.createTempDirectory(Path.of("build"), "native-beverage-diet-").resolve("tracker.txt");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javap").toString(),
                "-p", "-c", "-classpath", jar.toString(),
                "com.illusivesoulworks.diet.common.capability.PlayerDietTracker")
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "native javap timeout: " + output);
            assertEquals(0, process.exitValue());
            String bytecode = Files.readString(output);
            int start = bytecode.indexOf("public void consume(net.minecraft.world.item.ItemStack);");
            String consume = bytecode.substring(start, bytecode.indexOf("public float getValue", start));
            assertTrue(consume.contains("DietApi.getInstance"));
            assertTrue(consume.contains("DietApi.get:"));
            assertTrue(consume.contains("fireConsumeStackEvent"));
            assertTrue(consume.contains("Method apply:"));
            String source = Files.readString(Path.of("src/main/java/com/bettercontent/bettersurvivalphysiology/compat/BeverageCompat.java"));
            assertTrue(source.contains("DietApi.getInstance().get(event.player(), stack).get()"));
            assertTrue(source.contains("diet.setValue(group.id"));
            assertTrue(source.contains("diet.sync()"));
            assertFalse(source.contains("diet.consume("));
            assertFalse(source.contains("LivingEntityUseItemEvent"));
            assertFalse(source.contains("finishUsingItem("));
            assertFalse(source.contains("amplification("));
            assertTrue(source.contains("if (state.blackout) return;"));
            assertTrue(source.contains("stack.is(Items.MILK_BUCKET)"));
            assertFalse(source.contains("MILK_BOTTLE"), "Bottles use normal Diet lookup, not bucket overrides");
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                assertTrue(process.waitFor(10, TimeUnit.SECONDS), "native javap cleanup failed");
            }
        }
    }
}
