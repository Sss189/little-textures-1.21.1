package dev.littletextures.rules;

public final class PackCompatibility {
    private PackCompatibility() {}
    public static boolean isTransparentPlaceholder(String png) {
        return png.equals("minecraft:textures/blocks/pure_color/nothing.png");
    }
}
