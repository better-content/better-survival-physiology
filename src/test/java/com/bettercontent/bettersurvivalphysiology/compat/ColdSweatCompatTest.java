package com.bettercontent.bettersurvivalphysiology.compat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Source/native API contracts; does not claim an in-game temperature observation. */
class ColdSweatCompatTest {
    @Test
    void bothCorrectionsWriteCoreWhilePreservingBase() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettersurvivalphysiology/compat/ColdSweatCompat.java"));
        assertFalse(source.contains("Temperature.set(player, Temperature.Trait.BODY"));
        assertTrue(source.contains("setBody(player, corrected);"));
        assertTrue(source.contains("setBody(player, Math.copySign(Math.min(Math.abs(body), 0.25), body));"));
        assertTrue(source.contains("double base = Temperature.get(player, Temperature.Trait.BASE);"));
        assertTrue(source.contains("Temperature.set(player, Temperature.Trait.CORE, body - base);"));
        assertEquals(1, source.split("Temperature\\.set\\(", -1).length - 1,
                "All temperature writes must go through the CORE conversion");
    }

    @Test
    void pinnedNativeBodyIsDerivedAndSetterValidatesTemperatureTraits() throws Exception {
        Path jar = Path.of(System.getProperty("salience.nativeColdSweat"));
        assertEquals("e5e12a8b01f6b26be3e0bd5e0f819b92bd7d4a2f",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(jar))));
        Path evidence = Files.createTempDirectory(Path.of("build"), "native-cold-sweat-")
                .resolve("AbstractTempCap.javap.txt");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javap").toString(),
                "-p", "-c", "-classpath", jar.toString(),
                "com.momosoftworks.coldsweat.common.capability.temperature.AbstractTempCap")
                .redirectErrorStream(true).redirectOutput(evidence.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "native javap timed out: " + evidence);
            assertEquals(0, process.exitValue(), evidence.toString());
            String bytecode = Files.readString(evidence);
            String getter = bytecode.substring(bytecode.indexOf("public double getTrait("),
                    bytecode.indexOf("public java.util.EnumMap"));
            int body = getter.indexOf("Temperature$Trait.BODY:");
            int core = getter.indexOf("Temperature$Trait.CORE:");
            int base = getter.indexOf("Temperature$Trait.BASE:");
            assertTrue(body >= 0 && core > body && base > core && getter.indexOf("dadd") > base, getter);
            int setterStart = bytecode.indexOf("public void setTrait(");
            assertTrue(setterStart >= 0, bytecode);
            String setter = bytecode.substring(setterStart, bytecode.indexOf("\n  public ", setterStart));
            assertTrue(setter.contains("Temperature$Trait.isForTemperature:()Z"), setter);
            assertTrue(setter.contains("java/lang/IllegalArgumentException"), setter);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                assertTrue(process.waitFor(10, TimeUnit.SECONDS), "javap cleanup failed");
            }
        }
    }
}
