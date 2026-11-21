package com.bettercontent.bettersurvivalphysiology.compat;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Pinned-provider and source fail-closed contract; actual thermal timing needs native observation. */
class AmbientRateSeamContractTest {
    @Test void pinnedDefaultGatherStartsWithLiveModifiersRatherThanAnEmptyList() throws Exception {
        Path jar = Path.of(System.getProperty("salience.nativeColdSweat"));
        Path bytecode = Files.createTempDirectory(Path.of("build"), "native-temp-gather-")
                .resolve("DefaultTempModifiersEvent.javap.txt");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javap").toString(),
                "-p", "-c", "-classpath", jar.toString(),
                "com.momosoftworks.coldsweat.api.event.core.init.DefaultTempModifiersEvent")
                .redirectErrorStream(true).redirectOutput(bytecode.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "pinned javap timed out");
            assertEquals(0, process.exitValue(), bytecode.toString());
            String constructor = Files.readString(bytecode).split("  public java.util.Map", 2)[0];
            assertTrue(constructor.contains("Temperature.getModifiers"),
                    "Cold Sweat re-gathers on dimension change from the already installed WORLD/RATE layout");
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                assertTrue(process.waitFor(10, TimeUnit.SECONDS), "javap cleanup failed");
            }
        }
    }

    @Test void localProbeSequenceRetainsTheFinalWorldEvenWhenSourcesCancel() throws Exception {
        Class<?> frameClass = Class.forName("com.bettercontent.bettersurvivalphysiology.climate.AmbientRateSeam$Frame");
        var constructor = frameClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        var accept = frameClass.getDeclaredMethod("accept", int.class, int.class, double.class);
        accept.setAccessible(true);
        var ambient = frameClass.getDeclaredField("ambient");
        ambient.setAccessible(true);
        var previous = frameClass.getDeclaredField("previous");
        previous.setAccessible(true);
        var complete = frameClass.getDeclaredField("complete");
        complete.setAccessible(true);
        var safe = frameClass.getDeclaredField("safe");
        safe.setAccessible(true);
        Object canceling = constructor.newInstance();
        for (int stage = 0; stage <= 3; stage++)
            accept.invoke(canceling, stage, 2, stage == 1 ? 2.0 : 1.0);
        assertTrue(complete.getBoolean(canceling) && safe.getBoolean(canceling));
        assertEquals(1.0, ambient.getDouble(canceling));
        assertEquals(1.0, previous.getDouble(canceling),
                "equal opposite sources must produce zero net WORLD change, not a native-speed switch");
        Object aiding = constructor.newInstance();
        for (int stage = 0; stage <= 3; stage++)
            accept.invoke(aiding, stage, 2, stage >= 1 ? 2.0 : 1.0);
        assertTrue(complete.getBoolean(aiding) && safe.getBoolean(aiding));
        assertEquals(2.0, previous.getDouble(aiding),
                "local emissions must remain available to continuously weight the final rate");
    }

    @Test void inventoryItemsRemainClassifiedAsLocalWorldSources() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettersurvivalphysiology/climate/AmbientRateSeam.java"));
        String local = source.substring(source.indexOf("private static boolean local("), source.indexOf("private static String signature("));
        assertTrue(local.contains("type == InventoryItemsTempModifier.class"),
                "carried heating/cooling must contribute to final WORLD, not ambient baseline");
        assertTrue(source.contains("frame.ambient, frame.previous"),
                "the computed ambient and final WORLD values must both reach the rate calculation");
        assertFalse(source.contains("frame.sourceActive) return nativeRate"),
                "any nearby weak source must no longer switch immediately to native rate");
        assertTrue(source.contains("!local(list.get(index))"),
                "the live modifier list must reject unknown sources after installation");
        assertTrue(source.contains("original.stream().anyMatch(WorldProbe.class::isInstance)")
                && source.contains("if (verifiedWorldLayout(original) < 0)"),
                "dimension re-gathers must reuse only a complete, independently verified WORLD layout");
        assertTrue(source.contains("rates.removeIf(AmbientRateModifier.class::isInstance)")
                && source.contains("if (ownCount > 1 || own != null"),
                "dimension re-gathers must normalize only own duplicate RATE modifiers");

        Path jar = Path.of(System.getProperty("salience.nativeColdSweat"));
        assertEquals("e5e12a8b01f6b26be3e0bd5e0f819b92bd7d4a2f",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(jar))));
        Path bytecode = Files.createTempDirectory(Path.of("build"), "native-inventory-temp-")
                .resolve("InventoryItemsTempModifier.javap.txt");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javap").toString(),
                "-p", "-c", "-classpath", jar.toString(),
                "com.momosoftworks.coldsweat.api.temperature.modifier.InventoryItemsTempModifier")
                .redirectErrorStream(true).redirectOutput(bytecode.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "pinned javap timed out");
            assertEquals(0, process.exitValue(), bytecode.toString());
            String nativeCode = Files.readString(bytecode);
            assertTrue(nativeCode.contains("EntityTempManager.getItemTemperaturesOnEntity"),
                    "classification requires actual carried item source ownership");
            assertTrue(nativeCode.contains("ItemTempData.trait"), "do not treat inventory temperature as a fixed ambient offset");
            String apply = nativeCode.substring(nativeCode.indexOf("private java.lang.Double lambda$calculate$0("));
            assertTrue(apply.contains("java/lang/Double.doubleValue:()D") && apply.contains("dadd"),
                    "the pinned WORLD modifier must add its item effect to the preceding WORLD value");
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                assertTrue(process.waitFor(10, TimeUnit.SECONDS), "javap cleanup failed");
            }
        }
    }
}
