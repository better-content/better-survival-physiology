import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Normalizes the generated concept atlas into eight frozen 18x18 HUD sprites. */
public final class GenerateNutritionIcons {
    private static final int SIZE = 18;
    private static final int[][] PALETTES = {
            colors("3b2529", "6e343d", "a94c59", "e4717d", "f2a2ab"),
            colors("4a3022", "70431f", "aa652b", "d98b4a", "e8b06f"),
            colors("43413a", "766f3a", "9c8614", "caa903", "e1d66d"),
            colors("394225", "68732c", "8ca72d", "c0e304", "dcf27a"),
            colors("171b1d", "2a3338", "245e6a", "35bbd0", "8bdfea"),
            colors("283442", "314b70", "245aa6", "1175fc", "83b1ff"),
            colors("244b3e", "34735d", "4da382", "6fedba", "b8f7df"),
            colors("19161d", "352c44", "55436d", "8a6cb2", "bba8d0")
    };
    private static final String[][] GLYPHS = {
            {"00100", "10101", "01110", "10101", "00100"},
            {"100100", "010010", "001001", "010010", "100100"},
            {"110010", "110100", "001000", "010100", "100010"},
            {"001000", "000100", "111110", "000100", "001000"},
            {"0000000", "0110110", "1001001", "1001001", "0110110"},
            {"00100", "01110", "11111", "01110", "00100"},
            {"00100", "00100", "11111", "00100", "00100"},
            {"01110", "10001", "10101", "10001", "01110"}
    };

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]);
        BufferedImage source = ImageIO.read(root.resolve("art/reference/nutrition_readiness_concepts.png").toFile());
        if (source.getWidth() != 1536 || source.getHeight() != 1024) throw new IllegalStateException("expected 1536x1024 concept atlas");
        BufferedImage output = new BufferedImage(SIZE * 8, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int index = 0; index < 8; index++) {
            int cellX = (index % 4) * 384;
            int cellY = (index / 4) * 512 + 64;
            for (int y = 1; y < SIZE - 1; y++) for (int x = 1; x < SIZE - 1; x++) {
                int sx = cellX + x * 384 / SIZE + 10;
                int sy = cellY + y * 384 / SIZE + 10;
                int rgb = source.getRGB(sx, sy) & 0xffffff;
                int max = Math.max(rgb >> 16 & 255, Math.max(rgb >> 8 & 255, rgb & 255));
                if (max >= 44) output.setRGB(index * SIZE + x, y, 0xff000000 | nearest(rgb, PALETTES[index]));
            }
            overlayGlyph(output, index);
        }
        Path target = root.resolve("src/main/resources/assets/better_survival_physiology/textures/gui/nutrition_states.png");
        ImageIO.write(output, "png", target.toFile());
        System.out.println("wrote " + target);
    }

    private static void overlayGlyph(BufferedImage image, int index) {
        String[] rows = GLYPHS[index];
        int width = rows[0].length();
        int startX = index * SIZE + (SIZE - width) / 2;
        int startY = (SIZE - rows.length) / 2;
        int shadow = PALETTES[index][0], light = PALETTES[index][4];
        for (int y = 0; y < rows.length; y++) for (int x = 0; x < width; x++) if (rows[y].charAt(x) == '1') {
            image.setRGB(startX + x + 1, startY + y + 1, 0xff000000 | shadow);
        }
        for (int y = 0; y < rows.length; y++) for (int x = 0; x < width; x++) if (rows[y].charAt(x) == '1') {
            image.setRGB(startX + x, startY + y, 0xff000000 | light);
        }
    }

    private static int nearest(int rgb, int[] palette) {
        int best = palette[0], distance = Integer.MAX_VALUE;
        for (int candidate : palette) {
            int dr = (rgb >> 16 & 255) - (candidate >> 16 & 255);
            int dg = (rgb >> 8 & 255) - (candidate >> 8 & 255);
            int db = (rgb & 255) - (candidate & 255);
            int next = dr * dr + dg * dg + db * db;
            if (next < distance) { distance = next; best = candidate; }
        }
        return best;
    }

    private static int[] colors(String... values) {
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) result[i] = Integer.parseInt(values[i], 16);
        return result;
    }
}
