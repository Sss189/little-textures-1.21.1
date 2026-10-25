package dev.littletextures.mixin;

import dev.littletextures.client.TextureBakedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import team.creative.creativecore.client.render.box.QuadGeneratorContext;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.littletiles.client.render.tile.LittleRenderBox;

@Mixin(value = RenderBox.class, remap = false)
public abstract class RenderBoxMixin {
    @ModifyVariable(method = "getBakedQuad", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private BakedModel lttextures$beforeClipping(BakedModel model, QuadGeneratorContext holder, LevelAccessor level,
            BlockPos pos, BlockPos offset, BlockState state, BakedModel argument, ModelData data, Facing facing,
            RenderType layer, RandomSource random, boolean overrideTint, int defaultColor) {
        if (pos == null || !((Object) this instanceof LittleRenderBox)) return model;
        return TextureBakedModel.at(model, level, pos, state);
    }
}
