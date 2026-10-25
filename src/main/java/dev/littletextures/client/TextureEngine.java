package dev.littletextures.client;

import dev.littletextures.LittleTextures;
import dev.littletextures.compat.LittleTilesSupport;
import dev.littletextures.compat.BiomeView;
import dev.littletextures.rules.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TextureEngine {
    private static volatile List<TextureRule> pending = List.of();
    private static volatile Snapshot active = new Snapshot(List.of());
    private record MatchKey(BlockState state, ResourceLocation sprite) {}
    private static final class Snapshot {
        final List<BoundRule> rules;
        final boolean neighbourRules;
        final QuadRemapper remapper = new QuadRemapper();
        final ConcurrentHashMap<MatchKey, List<BoundRule>> candidates = new ConcurrentHashMap<>();
        Snapshot(List<BoundRule> rules) {
            this.rules = rules;
            this.neighbourRules = rules.stream().anyMatch(r -> r.rule.method() == TextureRule.Method.CTM || r.rule.method() == TextureRule.Method.VERTICAL);
        }
    }

    private record BoundRule(TextureRule rule, List<BlockSelector> blocks, List<TextureAtlasSprite> tiles) {
        boolean matches(BlockState state, ResourceLocation sprite) {
            if (!blocks.isEmpty() && blocks.stream().noneMatch(selector -> selector.matches(state))) return false;
            return rule.matchTiles().isEmpty() || rule.matchTiles().stream().anyMatch(token -> tileMatches(rule.namespace(), token, sprite));
        }
    }

    private TextureEngine() {}
    public static boolean hasNeighbourRules() {
        return active.neighbourRules;
    }
    public static void prepare(List<TextureRule> rules) { pending = rules; active = new Snapshot(List.of()); }

    public static void bind(TextureAtlas atlas) {
        List<BoundRule> rules = new ArrayList<>();
        for (TextureRule rule : pending) {
            try {
                List<BlockSelector> blocks = rule.matchBlocks().stream().map(BlockSelector::new).toList();
                List<TextureAtlasSprite> tiles = rule.tiles().stream().map(ResourceLocation::parse).map(TexturePaths::sprite).map(atlas::getSprite).toList();
                if (tiles.stream().anyMatch(sprite -> sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())))
                    throw new IllegalArgumentException("One or more tiles are missing from the atlas");
                rules.add(new BoundRule(rule, blocks, tiles));
            } catch (Exception e) { LittleTextures.LOG.warn("Disabling CTM rule {}: {}", rule.id(), e.getMessage()); }
        }
        rules.sort(Comparator.<BoundRule>comparingInt(r -> r.rule.priority()).reversed().thenComparing(r -> r.rule.id()));
        active = new Snapshot(List.copyOf(rules));
        LittleTextures.LOG.info("Activated {} CTM rules", rules.size());
    }

    public static List<BakedQuad> transform(List<BakedQuad> quads, RenderContext context) {
        Snapshot snapshot = active;
        if (quads.isEmpty() || snapshot.rules.isEmpty()) return quads;
        List<BakedQuad> result = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad original = quads.get(i), replacement = original;
            ResourceLocation sprite = original.getSprite().contents().name();
            List<BoundRule> candidates = snapshot.candidates.computeIfAbsent(new MatchKey(context.state(), sprite),
                    key -> snapshot.rules.stream().filter(r -> r.matches(key.state, key.sprite)).toList());
            for (BoundRule bound : candidates) {
                if (!bound.rule.faces().contains(original.getDirection().ordinal())) continue;
                if (!biomeMatches(bound.rule, context)) continue;
                int index = choose(bound.rule, context, original);
                if (index >= 0) replacement = snapshot.remapper.remap(original, bound.tiles.get(index));
                break;
            }
            if (result != null) result.add(replacement);
            else if (replacement != original) {
                result = new ArrayList<>(quads.size());
                result.addAll(quads.subList(0, i)); result.add(replacement);
            }
        }
        return result == null ? quads : result;
    }

    private static boolean biomeMatches(TextureRule rule, RenderContext context) {
        if (rule.biomes().isEmpty()) return true;
        var biome = context.level() instanceof LevelReader reader ? reader.getBiome(context.pos())
                : context.level() instanceof BiomeView view ? view.lttextures$biome(context.pos()) : null;
        if (biome == null) return false;
        return biome.unwrapKey().map(key -> rule.biomes().contains(key.location().toString())
                || rule.biomes().contains(key.location().getPath())).orElse(false);
    }

    private static int choose(TextureRule rule, RenderContext context, BakedQuad quad) {
        if (rule.tiles().size() == 1) return 0;
        BlockPos pos = context.pos();
        int face = quad.getDirection().ordinal();
        return switch (rule.method()) {
            case REPEAT -> FacePlane.repeat(pos.getX(), pos.getY(), pos.getZ(), face, rule.width(), rule.height());
            case RANDOM -> rule.randomIndex(pos.getX(), pos.getY(), pos.getZ(), face);
            case VERTICAL -> face < 2 ? -1 : CtmTiles.vertical(
                    connects(rule, context, quad, pos.below()), connects(rule, context, quad, pos.above()));
            case CTM -> {
                int[][] offsets = FacePlane.offsets(face);
                int edges = 0, corners = 0;
                for (int i = 0; i < 4; i++) if (connectsOffset(rule, context, quad, offsets[i])) edges |= 1 << i;
                int[][] pairs = {{1,2},{0,2},{1,3},{0,3}};
                for (int i = 0; i < 4; i++) {
                    int a = pairs[i][0], b = pairs[i][1];
                    if ((edges & (1 << a)) != 0 && (edges & (1 << b)) != 0) {
                        int[] first = offsets[a], second = offsets[b];
                        if (connects(rule, context, quad, pos.offset(first[0]+second[0],first[1]+second[1],first[2]+second[2]))) corners |= 1 << i;
                    }
                }
                yield CtmTiles.index(edges, corners);
            }
        };
    }

    private static boolean connectsOffset(TextureRule rule, RenderContext context, BakedQuad quad, int[] offset) {
        return connects(rule, context, quad, context.pos().offset(offset[0], offset[1], offset[2]));
    }

    private static boolean connects(TextureRule rule, RenderContext context, BakedQuad quad, BlockPos pos) {
        boolean connected = neighbourMatches(rule, context, quad, pos);
        return connected && (!rule.innerSeams() || !neighbourMatches(rule, context, quad, pos.relative(quad.getDirection())));
    }

    private static boolean neighbourMatches(TextureRule rule, RenderContext context, BakedQuad quad, BlockPos pos) {
        BlockState neighbour = context.level().getBlockState(pos);
        if (LittleTilesSupport.available() && LittleTilesSupport.matches(context.level(), pos,
                material -> materialMatches(rule, context.state(), material, quad.getSprite().contents().name(), quad.getDirection(), pos))) return true;
        return materialMatches(rule, context.state(), neighbour, quad.getSprite().contents().name(), quad.getDirection(), pos);
    }

    private static boolean materialMatches(TextureRule rule, BlockState source, BlockState neighbour, ResourceLocation sprite, Direction face, BlockPos pos) {
        if (neighbour.isAir()) return false;
        if (rule.connect().equals("block")) return source.getBlock() == neighbour.getBlock();
        if (rule.connect().equals("state")) return source == neighbour;
        var model = TextureBakedModel.unwrap(Minecraft.getInstance().getBlockRenderer().getBlockModel(neighbour));
        var random = RandomSource.create(neighbour.getSeed(pos));
        for (BakedQuad q : model.getQuads(neighbour, face, random)) if (q.getSprite().contents().name().equals(sprite)) return true;
        random.setSeed(neighbour.getSeed(pos));
        for (BakedQuad q : model.getQuads(neighbour, null, random)) if (q.getDirection() == face && q.getSprite().contents().name().equals(sprite)) return true;
        return false;
    }

    private static boolean tileMatches(String namespace, String token, ResourceLocation sprite) {
        String match = token;
        String ns = namespace;
        if (match.contains(":")) { int i = match.indexOf(':'); ns = match.substring(0,i); match = match.substring(i+1); }
        if (!sprite.getNamespace().equals(ns)) return false;
        if (match.endsWith(".png")) match = match.substring(0, match.length()-4);
        if (match.startsWith("textures/")) match = match.substring(9);
        String path = sprite.getPath();
        return path.equals(match) || (!match.contains("/") && path.substring(path.lastIndexOf('/')+1).equals(match));
    }
}
