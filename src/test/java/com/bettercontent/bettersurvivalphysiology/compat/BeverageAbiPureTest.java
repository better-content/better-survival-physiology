package com.bettercontent.bettersurvivalphysiology.compat;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipFile;

/** Exercises admission against actual provider entries plus malformed/partial ABI variants. */
public final class BeverageAbiPureTest {
    public static void main(String[] args) throws Exception {
        String plants = "com/bettercontent/betterdrinkingwater/PlantDrinks.class";
        String events = "com/bettercontent/betterdrinkingwater/api/BeverageEvents.class";
        Set<String> full = new HashSet<>(Set.of(plants, events));
        for (String nested : new String[]{"Profile", "Check", "Sip"})
            full.add(events.replace(".class", "$" + nested + ".class"));
        check(!BeverageAbi.supportsTyped(path -> false), "No optional provider uses native route");
        check(BeverageAbi.supportsTyped(full::contains), "Complete typed ABI admitted");
        for (String missing : full) {
            Set<String> partial = new HashSet<>(full); partial.remove(missing);
            boolean rejected = false;
            try { BeverageAbi.supportsTyped(partial::contains); } catch (IllegalStateException expected) { rejected = true; }
            check(rejected, "Partial typed ABI must fail explicitly: " + missing);
        }
        if (args.length != 1) throw new IllegalArgumentException("Pass actual accepted legacy Water jar path");
        try (ZipFile actual = new ZipFile(Path.of(args[0]).toFile())) {
            check(!BeverageAbi.supportsTyped(path -> actual.getEntry(path) != null), "Actual accepted Water must select legacy route");
        }
        System.out.println("BeverageAbiPureTest PASS: 8 admission cases including actual accepted Water ZIP");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
