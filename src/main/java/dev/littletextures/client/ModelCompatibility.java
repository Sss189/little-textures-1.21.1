package dev.littletextures.client;

import dev.littletextures.LittleTextures;
import net.minecraft.client.resources.model.BakedModel;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class ModelCompatibility {
    private static final Set<Class<?>> warned = ConcurrentHashMap.newKeySet();
    private static final Method vanillaAdapter = findFabricAdapter();
    private ModelCompatibility() {}

    static boolean canWrap(BakedModel model) {
        // LT's container models have their own renderer; only material models need this wrapper.
        if (model.getClass().getName().startsWith("team.creative.")) return false;
        if (vanillaAdapter == null || !vanillaAdapter.getDeclaringClass().isInstance(model)) return true;
        try {
            if (Boolean.TRUE.equals(vanillaAdapter.invoke(model))) return true;
        } catch (ReflectiveOperationException e) {
            if (warned.add(model.getClass())) LittleTextures.LOG.warn("Could not inspect custom model {}: {}",model.getClass().getName(),e.getMessage());
            return false;
        }
        if (warned.add(model.getClass())) LittleTextures.LOG.warn("Preserving custom Fabric model {} without CTM processing",model.getClass().getName());
        return false;
    }

    private static Method findFabricAdapter() {
        try {
            return Class.forName("net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel",false,BakedModel.class.getClassLoader())
                    .getMethod("isVanillaAdapter");
        } catch (ClassNotFoundException | NoSuchMethodException e) { return null; }
    }
}
