package dev.littletextures.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import java.util.*;

final class BlockSelector {
    private final Block block;
    private final Map<Property<?>, Set<String>> properties;

    BlockSelector(String token) {
        String[] parts = token.split(":");
        int start = parts.length >= 2 && !parts[1].contains("=") ? 2 : 1;
        ResourceLocation id = ResourceLocation.parse(start == 2 ? parts[0] + ":" + parts[1] : parts[0]);
        if (!BuiltInRegistries.BLOCK.containsKey(id)) throw new IllegalArgumentException("Unknown block: " + id);
        block = BuiltInRegistries.BLOCK.get(id);
        properties = new HashMap<>();
        for (int i = start; i < parts.length; i++) {
            String[] pair = parts[i].split("=", 2);
            if (pair.length != 2) throw new IllegalArgumentException("Invalid state matcher: " + token);
            Property<?> property = block.getStateDefinition().getProperty(pair[0]);
            if (property == null) throw new IllegalArgumentException("Unknown property: " + token);
            Set<String> allowed = Set.of(pair[1].split(","));
            for (String value : allowed) if (property.getValue(value).isEmpty()) throw new IllegalArgumentException("Invalid property value: " + token);
            properties.put(property, allowed);
        }
    }

    boolean matches(BlockState state) {
        if (state.getBlock() != block) return false;
        for (var entry : properties.entrySet())
            if (!entry.getValue().contains(valueName(state, entry.getKey()))) return false;
        return true;
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
}
