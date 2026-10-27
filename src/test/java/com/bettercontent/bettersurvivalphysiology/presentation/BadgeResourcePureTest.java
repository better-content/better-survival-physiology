package com.bettercontent.bettersurvivalphysiology.presentation;

import com.google.gson.JsonParser;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Executes actual production badge selection and parses actual font/PNG data; no source-string tests. */
public final class BadgeResourcePureTest {
    private static int assertions;
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("Pass physiology root and optional newmod resource root");
        boolean spiceAvailable = args.length == 2;
        Path physiology = Path.of(args[0]), spice = spiceAvailable ? Path.of(args[1]) : null;
        run(path -> {
            try {
                boolean external = path.startsWith("/assets/better_spicy_farmers/");
                check(!external || spiceAvailable, "Standalone resource test MUST NOT request a newmod resource");
                Path file = (external ? spice : physiology).resolve(path.substring(1));
                return Files.newInputStream(file);
            } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        }, spiceAvailable);
        System.out.println("BadgeResourcePureTest PASS assertions=" + assertions + "; optionalmod=" + spiceAvailable + "; metadata/PNG only, not client rendering");
    }
    public static void run(Function<String, InputStream> resources) throws Exception { run(resources, false); }
    public static void run(Function<String, InputStream> resources, boolean spiceAvailable) throws Exception {
        assertions = 0;
        Map<Integer, Glyph> glyphs = new HashMap<>();
        try (InputStream stream = required(resources, "/assets/better_survival_physiology/font/aspects.json")) {
            var providers = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("providers");
            check(providers.size() == 2, "Exactly standalone Spice plus unchanged legacy atlas providers");
            for (var value : providers) {
                var provider = value.getAsJsonObject();
                check(provider.get("type").getAsString().equals("bitmap"), "Native bitmap provider");
                check(provider.get("height").getAsInt() == 18 && provider.get("ascent").getAsInt() == 16,
                        "Existing native font metrics unchanged");
                String file = provider.get("file").getAsString();
                int colon = file.indexOf(':');
                String resource = "/assets/" + file.substring(0, colon) + "/textures/" + file.substring(colon + 1);
                var rows = provider.getAsJsonArray("chars");
                check(rows.size() == 1, "Existing one-row badge layout");
                int[] chars = rows.get(0).getAsString().codePoints().toArray();
                try (var imageStream = required(resources, resource)) {
                    var image = ImageIO.read(imageStream);
                    check(image != null && image.getWidth() == chars.length * 18 && image.getHeight() == 18,
                            "Bitmap dimensions match native cell grid: " + file);
                }
                if (file.equals("better_survival_physiology:gui/aspect_badges.png")) {
                    check(chars.length == 8 && chars[5] == 0, "Only old Earth/Spice slot is NUL");
                    for (int slot = 0; slot < 8; slot++) if (slot != 5)
                        check(chars[slot] == 0xE100 + slot, "Unchanged legacy glyph slot " + slot);
                } else {
                    check(file.equals("better_survival_physiology:gui/spice_badge.png") && chars.length == 1
                            && chars[0] == 0xE105, "Standalone Spice maps only existing U+E105");
                }
                for (int column = 0; column < chars.length; column++) {
                    if (chars[column] == 0) continue;
                    check(glyphs.putIfAbsent(chars[column], new Glyph(resource, column * 18)) == null,
                            "No duplicate codepoint fallback");
                }
            }
        }
        check(glyphs.size() == 8, "Exactly eight identities, no old duplicate Spice glyph");
        for (var aspect : AspectIdentity.values()) {
            var badge = AspectBadge.of(aspect, spiceAvailable);
            var glyph = glyphs.get(aspect.badge().codePointAt(0));
            String resource = "/assets/" + badge.namespace() + "/" + badge.path();
            check(glyph != null && glyph.u() == badge.u(), "HUD and font use the same native cell origin");
            if (aspect == AspectIdentity.ROBUSTNESS) {
                check(badge.namespace().equals(spiceAvailable ? "better_spicy_farmers" : "better_survival_physiology"),
                        "Only actual optional mod presence selects external HUD badge");
                try (var hudBytes = required(resources, resource); var fontBytes = required(resources, glyph.resource())) {
                    check(java.util.Arrays.equals(hudBytes.readAllBytes(), fontBytes.readAllBytes()),
                            "Spice HUD and independently loadable font use byte-identical generated badge");
                }
            } else check(glyph.resource().equals(resource), "Other seven glyph resource identities unchanged");
            try (var stream = required(resources, resource)) {
                var image = ImageIO.read(stream);
                check(image.getWidth() == badge.textureWidth() && image.getHeight() == badge.textureHeight(),
                        "Production HUD source dimensions");
                check(badge.width() == 18 && badge.height() == 18 && badge.v() == 0, "Native badge cell size");
                int visible = 0;
                for (int y = badge.v(); y < badge.v() + badge.height(); y++)
                    for (int x = badge.u(); x < badge.u() + badge.width(); x++)
                        if ((image.getRGB(x, y) >>> 24) >= 128) visible++;
                check(visible >= 24, "Actual selected badge remains visible at native size: " + aspect.displayName);
            }
        }
    }
    private record Glyph(String resource, int u) {}
    private static InputStream required(Function<String, InputStream> resources, String path) {
        InputStream stream = resources.apply(path);
        check(stream != null, "Missing actual resource " + path);
        return stream;
    }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
