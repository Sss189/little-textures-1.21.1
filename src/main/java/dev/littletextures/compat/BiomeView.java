package dev.littletextures.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public interface BiomeView {
    Holder<Biome> lttextures$biome(BlockPos pos);
}
