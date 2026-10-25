package dev.littletextures.rules;

import java.util.List;
import java.util.Set;

public record TextureRule(String namespace, String path, int priority, Method method,
                          List<String> matchBlocks, List<String> matchTiles, List<String> tiles,
                          int width, int height, Set<Integer> faces, String connect,
                          int[] weights, int symmetry, List<String> biomes, boolean innerSeams) {
    public enum Method { REPEAT, RANDOM, CTM, VERTICAL }
    public TextureRule {
        matchBlocks = List.copyOf(matchBlocks);
        matchTiles = List.copyOf(matchTiles);
        tiles = List.copyOf(tiles);
        faces = Set.copyOf(faces);
        weights = weights.clone();
        biomes = List.copyOf(biomes);
    }
    @Override public int[] weights() { return weights.clone(); }
    public String id() { return namespace + ":" + path; }
    public int randomIndex(int x, int y, int z, int face) {
        int group = symmetry == 6 ? 0 : symmetry == 2 ? face / 2 * 2 : face;
        return FacePlane.random(x, y, z, group, weights, id().hashCode());
    }
}
