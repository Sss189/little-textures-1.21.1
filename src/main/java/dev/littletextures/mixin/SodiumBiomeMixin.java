package dev.littletextures.mixin;

import dev.littletextures.compat.BiomeView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public abstract class SodiumBiomeMixin implements BiomeView {
    @Shadow public abstract Holder<Biome> getBiomeFabric(BlockPos pos);
    @Override public Holder<Biome> lttextures$biome(BlockPos pos) { return getBiomeFabric(pos); }
}
