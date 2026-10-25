package dev.littletextures.mixin;

import dev.littletextures.client.TexturePaths;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
import java.util.Optional;

@Mixin(FallbackResourceManager.class)
public abstract class FallbackResourceManagerMixin {
    @Shadow public abstract Optional<Resource> getResource(ResourceLocation id);
    @Shadow public abstract List<Resource> getResourceStack(ResourceLocation id);

    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    private void lttextures$resolve(ResourceLocation id, CallbackInfoReturnable<Optional<Resource>> callback) {
        ResourceLocation actual = TexturePaths.unalias(id);
        if (!actual.equals(id)) callback.setReturnValue(getResource(actual));
    }

    @Inject(method = "getResourceStack", at = @At("HEAD"), cancellable = true)
    private void lttextures$resolveStack(ResourceLocation id, CallbackInfoReturnable<List<Resource>> callback) {
        ResourceLocation actual = TexturePaths.unalias(id);
        if (!actual.equals(id)) callback.setReturnValue(getResourceStack(actual));
    }
}
