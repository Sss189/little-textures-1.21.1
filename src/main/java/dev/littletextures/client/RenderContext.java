package dev.littletextures.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public record RenderContext(BlockGetter level, BlockPos pos, BlockState state) {
    public RenderContext { pos = pos.immutable(); }
}
