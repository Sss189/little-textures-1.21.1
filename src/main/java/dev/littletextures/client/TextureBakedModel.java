package dev.littletextures.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import java.util.List;
import java.util.ArrayList;

public final class TextureBakedModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<RenderContext> CONTEXT = new ModelProperty<>();
    private final RenderContext fixed;

    private TextureBakedModel(BakedModel model, RenderContext fixed) { super(model); this.fixed = fixed; }

    public static BakedModel wrap(BakedModel model) {
        return model instanceof TextureBakedModel || !ModelCompatibility.canWrap(model) ? model : new TextureBakedModel(model, null);
    }

    public static BakedModel at(BakedModel model, BlockGetter level, BlockPos pos, BlockState state) {
        return new TextureBakedModel(unwrap(model), new RenderContext(level, pos, state));
    }

    public static BakedModel unwrap(BakedModel model) {
        while (model instanceof TextureBakedModel wrapper) model = wrapper.originalModel;
        return model;
    }

    @Override public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        ModelData original = originalModel.getModelData(level, pos, state, data);
        return original.derive().with(CONTEXT, new RenderContext(level, pos, state)).build();
    }

    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType layer) {
        List<BakedQuad> quads = originalModel.getQuads(state, side, random, data, layer);
        if (fixed != null && side != null) {
            // LT asks for one face at a time. Models such as panes keep their faces
            // in the unculled list, which CreativeCore otherwise does not request.
            var unculled = originalModel.getQuads(state, null, RandomSource.create(fixed.state().getSeed(fixed.pos())), data, layer);
            List<BakedQuad> combined = null;
            for (BakedQuad quad : unculled) if (quad.getDirection() == side && !quads.contains(quad)) {
                if (combined == null) combined = new ArrayList<>(quads);
                combined.add(quad);
            }
            if (combined != null) quads = combined;
        }
        RenderContext context = fixed == null ? data.get(CONTEXT) : fixed;
        return context == null ? quads : TextureEngine.transform(quads, context);
    }

    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        List<BakedQuad> quads = originalModel.getQuads(state, side, random);
        return fixed == null ? quads : TextureEngine.transform(quads, fixed);
    }
}
