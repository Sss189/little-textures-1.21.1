package dev.littletextures.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import team.creative.littletiles.common.block.entity.BETiles;
import java.util.function.Predicate;

public final class LittleTilesSupport {
    private LittleTilesSupport() {}
    public static boolean available() { return ModList.get().isLoaded("littletiles"); }

    public static boolean matches(BlockGetter level, BlockPos pos, Predicate<BlockState> predicate) {
        return Impl.matches(level, pos, predicate);
    }

    /** Kept in a separate class so ordinary-block rendering works without LT installed. */
    private static final class Impl {
        static boolean matches(BlockGetter level, BlockPos pos, Predicate<BlockState> predicate) {
            if (!(level.getBlockEntity(pos) instanceof BETiles tiles)) return false;
            for (var pair : tiles.allTiles()) if (predicate.test(pair.value.getState())) return true;
            return false;
        }
    }
}
