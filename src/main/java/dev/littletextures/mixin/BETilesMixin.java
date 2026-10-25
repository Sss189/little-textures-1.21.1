package dev.littletextures.mixin;

import dev.littletextures.compat.TileInvalidation;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.littletiles.common.block.entity.BETiles;

@Mixin(value = BETiles.class, remap = false)
public abstract class BETilesMixin {
    @Inject(method = {"updateTiles(ZZ)V", "handleUpdate"}, at = @At("RETURN"), remap = false)
    private void lttextures$edited(CallbackInfo callback) {
        BETiles tiles = (BETiles) (Object) this;
        if (tiles.getLevel() instanceof ClientLevel level) TileInvalidation.changed(level, tiles.getBlockPos());
    }
}
