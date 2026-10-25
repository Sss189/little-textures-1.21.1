package dev.littletextures.client;

import dev.littletextures.LittleTextures;
import dev.littletextures.rules.RuleParser;
import dev.littletextures.rules.TextureRule;
import dev.littletextures.rules.PackCompatibility;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.io.Reader;
import java.util.*;

public final class CtmSpriteSource implements SpriteSource {
    public static final SpriteSourceType TYPE = new SpriteSourceType(MapCodec.unit(CtmSpriteSource::new));
    @Override
    public void run(ResourceManager manager, Output output) {
        List<TextureRule> rules = new ArrayList<>();
        Map<String, Integer> priority = new HashMap<>();
        manager.listPacks().forEach(pack -> priority.put(pack.packId(), priority.size()));
        var resources = new TreeMap<>(manager.listResources("optifine/ctm", id -> id.getPath().endsWith(".properties")));
        resources.putAll(manager.listResources("mcpatcher/ctm", id -> id.getPath().endsWith(".properties")));
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            ResourceLocation id = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                TextureRule rule = RuleParser.parse(id.getNamespace(), id.getPath(), reader,
                        priority.getOrDefault(entry.getValue().sourcePackId(), 0));
                rules.add(rule);
            } catch (Exception e) {
                LittleTextures.LOG.warn("Skipping CTM rule {}: {}", id, e.getMessage());
            }
        });
        Set<ResourceLocation> textures = new HashSet<>();
        for (TextureRule rule : rules) for (String tile : rule.tiles()) {
            ResourceLocation png = ResourceLocation.parse(tile);
            if (textures.add(png)) {
                var resource = manager.getResource(png);
                if (resource.isEmpty() && PackCompatibility.isTransparentPlaceholder(tile)) {
                    resource = manager.getResource(ResourceLocation.fromNamespaceAndPath("lttextures", "textures/block/transparent.png"));
                    LittleTextures.LOG.info("Using built-in transparent placeholder for {}", png);
                }
                if (resource.isPresent()) output.add(TexturePaths.sprite(png), resource.get());
                else LittleTextures.LOG.warn("Missing CTM texture {}", png);
            }
        }
        TextureEngine.prepare(List.copyOf(rules));
        LittleTextures.LOG.info("Loaded {} CTM rules and {} tile resources", rules.size(), textures.size());
    }

    @Override public SpriteSourceType type() { return TYPE; }
}
