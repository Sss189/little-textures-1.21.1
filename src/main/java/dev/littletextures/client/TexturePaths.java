package dev.littletextures.client;

import net.minecraft.resources.ResourceLocation;

/** Atlas names have virtual textures/ aliases so Iris can locate CTM PBR siblings. */
public final class TexturePaths {
    public static final String PREFIX = "lttextures/ctm/";
    private TexturePaths() {}

    public static ResourceLocation sprite(ResourceLocation png) {
        String path = png.getPath();
        if (!path.endsWith(".png")) throw new IllegalArgumentException("Not a PNG: " + png);
        return ResourceLocation.fromNamespaceAndPath(png.getNamespace(), PREFIX + path.substring(0, path.length() - 4));
    }

    public static ResourceLocation unalias(ResourceLocation resource) {
        String prefix = "textures/" + PREFIX;
        if (!resource.getPath().startsWith(prefix)) return resource;
        return ResourceLocation.fromNamespaceAndPath(resource.getNamespace(), resource.getPath().substring(prefix.length()));
    }
}
