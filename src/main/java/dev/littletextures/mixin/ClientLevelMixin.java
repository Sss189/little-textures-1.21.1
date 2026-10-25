package dev.littletextures.mixin;

import dev.littletextures.compat.TileInvalidation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "sendBlockUpdated", at = @At("RETURN"))
    private void lttextures$changed(BlockPos pos, BlockState before, BlockState after, int flags, CallbackInfo callback) {
        TileInvalidation.changed((ClientLevel) (Object) this, pos);
    }
}
