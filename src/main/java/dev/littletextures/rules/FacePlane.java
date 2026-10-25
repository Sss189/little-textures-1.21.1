package dev.littletextures.rules;

/** Vanilla direction order: down, up, north, south, west, east. */
public final class FacePlane {
    private FacePlane() {}

    public static int repeat(int x, int y, int z, int face, int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid repeat dimensions");
        long u = switch (face) {
            case 0, 1, 3 -> x;
            case 2 -> -(long) x - 1;
            case 4 -> z;
            case 5 -> -(long) z - 1;
            default -> throw new IllegalArgumentException("Invalid face " + face);
        };
        long v = switch (face) { case 0 -> -(long) z - 1; case 1 -> z; default -> -(long) y; };
        return (int) (Math.floorMod(v, height) * width + Math.floorMod(u, width));
    }

    /** Offsets for left, right, bottom, top in the CTM face plane. */
    public static int[][] offsets(int face) {
        return switch (face) {
            case 0 -> new int[][]{{-1,0,0},{1,0,0},{0,0,-1},{0,0,1}};
            case 1 -> new int[][]{{-1,0,0},{1,0,0},{0,0,1},{0,0,-1}};
            case 2 -> new int[][]{{1,0,0},{-1,0,0},{0,-1,0},{0,1,0}};
            case 3 -> new int[][]{{-1,0,0},{1,0,0},{0,-1,0},{0,1,0}};
            case 4 -> new int[][]{{0,0,-1},{0,0,1},{0,-1,0},{0,1,0}};
            case 5 -> new int[][]{{0,0,1},{0,0,-1},{0,-1,0},{0,1,0}};
            default -> throw new IllegalArgumentException("Invalid face " + face);
        };
    }

    public static int random(int x, int y, int z, int face, int[] weights, int salt) {
        long h = x * 0x632BE59BD9B4E019L ^ y * 0x9E3779B97F4A7C15L ^ z * 0xC6BC279692B5CC83L;
        h ^= (long) face * 0xD1B54A32D192ED03L ^ salt;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        long total = 0;
        for (int w : weights) total += w;
        long pick = Math.floorMod(h, total);
        for (int i = 0; i < weights.length; i++) {
            pick -= weights[i];
            if (pick < 0) return i;
        }
        throw new IllegalStateException("No random tile");
    }
}
