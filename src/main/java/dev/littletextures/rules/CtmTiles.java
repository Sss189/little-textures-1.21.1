package dev.littletextures.rules;

/** Tile numbering of the interoperable 47-tile CTM format.
 * Edge bits: left, right, bottom, top. Corner bits: RB, LB, RT, LT.
 * A diagonal has an effect only when its two adjacent edges connect.
 */
public final class CtmTiles {
    private static final int[] EDGE_TILES = {0, 3, 1, 2, 12, 15, 13, 14, 36, 39, 37, 38, 24, 27, 25, 26};
    private static final int[] FULL_TILES = {26, 32, 33, 11, 44, 23, 35, 20, 45, 34, 10, 8, 22, 21, 9, 46};
    private static final int[] BOTTOM_ROW = {14,29,31,7}, TOP_ROW = {38,42,40,18};
    private static final int[] LEFT_COLUMN = {27,41,43,19}, RIGHT_COLUMN = {25,30,28,6};

    private CtmTiles() {}

    public static int index(int edges, int connectedCorners) {
        if ((edges & ~15) != 0 || (connectedCorners & ~15) != 0)
            throw new IllegalArgumentException("CTM masks must have four bits");
        int missing = connectedCorners ^ 15;
        return switch (edges) {
            case 5 -> (missing & 2) == 0 ? 15 : 5;
            case 6 -> (missing & 1) == 0 ? 13 : 4;
            case 9 -> (missing & 8) == 0 ? 39 : 17;
            case 10 -> (missing & 4) == 0 ? 37 : 16;
            case 7 -> BOTTOM_ROW[missing & 3];
            case 11 -> TOP_ROW[(missing >> 2) & 3];
            case 13 -> LEFT_COLUMN[((missing >> 1) & 1) | ((missing >> 2) & 2)];
            case 14 -> RIGHT_COLUMN[(missing & 1) | ((missing >> 1) & 2)];
            case 15 -> FULL_TILES[missing];
            default -> EDGE_TILES[edges];
        };
    }

    public static int vertical(boolean below, boolean above) {
        return below ? (above ? 1 : 2) : (above ? 0 : 3);
    }
}
