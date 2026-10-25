package dev.littletextures.compat;

import dev.littletextures.client.TextureEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import team.creative.littletiles.common.block.entity.BETiles;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TileInvalidation {
    private static final Set<Change> pending = ConcurrentHashMap.newKeySet();
    private record Change(ClientLevel level, BlockPos pos) {}
    private TileInvalidation() {}

    public static void changed(ClientLevel level, BlockPos pos) {
        if (LittleTilesSupport.available() && TextureEngine.hasNeighbourRules()) pending.add(new Change(level, pos.immutable()));
    }

    public static void flush() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) { pending.clear(); return; }
        Set<BlockPos> affected = new HashSet<>();
        for (Change change : pending.toArray(Change[]::new)) {
            if (!pending.remove(change) || change.level != level) continue;
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++)
                affected.add(change.pos.offset(x,y,z));
        }
        if (!affected.isEmpty()) Impl.refresh(level, affected);
    }

    public static void reloaded() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.level != null && minecraft.player != null && LittleTilesSupport.available())
                Impl.reload(minecraft);
        });
    }

    private static final class Impl {
        static void refresh(ClientLevel level, Set<BlockPos> positions) {
            for (BlockPos pos : positions) {
                if (!level.hasChunkAt(pos)) continue;
                if (level.getBlockEntity(pos) instanceof BETiles tiles && tiles.render != null) {
                    tiles.render.onNeighbourChanged();
                    // Request clearing on LT's worker rather than clearing its shared cache here.
                    tiles.render.queue(true, false, 0L);
                }
                Minecraft.getInstance().levelRenderer.setBlockDirty(pos, level.getBlockState(pos), level.getBlockState(pos));
            }
        }

        static void reload(Minecraft minecraft) {
            int radius = minecraft.options.getEffectiveRenderDistance() + 1;
            int cx = minecraft.player.chunkPosition().x, cz = minecraft.player.chunkPosition().z;
            for (int x = cx-radius; x <= cx+radius; x++) for (int z = cz-radius; z <= cz+radius; z++) {
                var chunk = minecraft.level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (var be : chunk.getBlockEntities().values()) if (be instanceof BETiles tiles && tiles.render != null)
                    tiles.render.queue(true, false, 0L);
            }
        }
    }
}
