package dev.littletextures.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import java.util.concurrent.ConcurrentHashMap;

public final class QuadRemapper {
    private final ConcurrentHashMap<Key, BakedQuad> cache = new ConcurrentHashMap<>();
    private record Key(BakedQuad quad, TextureAtlasSprite sprite) {}

    public BakedQuad remap(BakedQuad quad, TextureAtlasSprite target) {
        if (quad.getSprite() == target) return quad;
        if (cache.size() > 16384) cache.clear();
        return cache.computeIfAbsent(new Key(quad, target), key -> create(quad, target));
    }

    private static BakedQuad create(BakedQuad quad, TextureAtlasSprite target) {
        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        if (stride < 6) return quad;
        TextureAtlasSprite source = quad.getSprite();
        for (int i = 0; i < 4; i++) {
            int uv = i * stride + 4;
            float u = Float.intBitsToFloat(vertices[uv]);
            float v = Float.intBitsToFloat(vertices[uv + 1]);
            float relativeU = (u - source.getU0()) / (source.getU1() - source.getU0());
            float relativeV = (v - source.getV0()) / (source.getV1() - source.getV0());
            vertices[uv] = Float.floatToRawIntBits(target.getU0() + relativeU * (target.getU1() - target.getU0()));
            vertices[uv + 1] = Float.floatToRawIntBits(target.getV0() + relativeV * (target.getV1() - target.getV0()));
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), target, quad.isShade(), quad.hasAmbientOcclusion());
    }
}
