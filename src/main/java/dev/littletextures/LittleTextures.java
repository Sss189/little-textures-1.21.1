package dev.littletextures;

import dev.littletextures.client.TextureEngine;
import dev.littletextures.client.TextureBakedModel;
import dev.littletextures.client.CtmSpriteSource;
import dev.littletextures.compat.TileInvalidation;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = LittleTextures.ID, dist = Dist.CLIENT)
public final class LittleTextures {
    public static final String ID = "lttextures";
    public static final Logger LOG = LoggerFactory.getLogger("Little Textures");

    public LittleTextures(IEventBus bus) {
        bus.addListener(this::wrapModels);
        bus.addListener(this::bindAtlas);
        bus.addListener((RegisterSpriteSourceTypesEvent event) -> event.register(ResourceLocation.fromNamespaceAndPath(ID, "ctm"), CtmSpriteSource.TYPE));
        bus.addListener((ModelEvent.BakingCompleted event) -> TileInvalidation.reloaded());
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> TileInvalidation.flush());
        if (Boolean.getBoolean("lttextures.smoke")) dev.littletextures.tools.ClientSmokeChecks.install();
    }

    private void wrapModels(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((id, model) -> id.getVariant().equals("inventory") ? model : TextureBakedModel.wrap(model));
    }

    private void bindAtlas(TextureAtlasStitchedEvent event) {
        if (event.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS)) {
            TextureEngine.bind(event.getAtlas());
        }
    }
}
