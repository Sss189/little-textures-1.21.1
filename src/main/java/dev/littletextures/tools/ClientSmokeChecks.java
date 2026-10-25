package dev.littletextures.tools;

import dev.littletextures.LittleTextures;
import dev.littletextures.client.*;
import dev.littletextures.compat.LittleTilesSupport;
import dev.littletextures.compat.BiomeView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.ModList;
import net.minecraft.server.packs.resources.ResourceManager;
import team.creative.creativecore.client.render.box.QuadGeneratorContext;
import team.creative.creativecore.client.render.face.RenderBoxFace;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.box.AlignedBox;
import team.creative.littletiles.client.render.tile.LittleRenderBox;
import java.lang.reflect.Proxy;
import java.util.*;

/** Explicit developer-only smoke mode. Never runs unless the JVM flag is set. */
public final class ClientSmokeChecks {
    private static int ticks;
    private static boolean finished;
    private ClientSmokeChecks() {}
    public static void install() { NeoForge.EVENT_BUS.addListener(ClientSmokeChecks::tick); }

    private static void tick(ClientTickEvent.Post event) {
        if (finished || !(Minecraft.getInstance().screen instanceof TitleScreen)) return;
        if (++ticks < 10) return;
        finished = true;
        try {
            run();
            LittleTextures.LOG.info("LTTEXTURES_SMOKE_PASS ordinary CTM, UV preservation, resource aliases; LT={}",LittleTilesSupport.available());
        } catch (Throwable e) {
            LittleTextures.LOG.error("LTTEXTURES_SMOKE_FAIL", e);
        } finally { Minecraft.getInstance().stop(); }
    }

    private static void run() throws Exception {
        Minecraft minecraft = Minecraft.getInstance();
        BlockState state = Blocks.GLASS.defaultBlockState();
        BlockPos pos = new BlockPos(-3,64,-5);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        blocks.put(pos, state);
        LevelAccessor view = fake(blocks);
        var model = minecraft.getBlockRenderer().getBlockModel(state);
        var original = TextureBakedModel.unwrap(model);
        List<BakedQuad> source = original.getQuads(state,Direction.SOUTH,RandomSource.create(0),ModelData.EMPTY,RenderType.translucent());
        check(!source.isEmpty(), "Glass source model has no SOUTH face");
        var data = model.getModelData(view,pos,state,ModelData.EMPTY);
        List<BakedQuad> isolated = model.getQuads(state,Direction.SOUTH,RandomSource.create(0),data,RenderType.translucent());
        checkTile(isolated,0);
        checkPreserved(source.getFirst(),isolated.getFirst());
        blocks.put(pos.west(),state);
        List<BakedQuad> joined = model.getQuads(state,Direction.SOUTH,RandomSource.create(0),data,RenderType.translucent());
        checkTile(joined,3);
        for (int x=-1;x<=1;x++) for (int y=-1;y<=1;y++) blocks.put(pos.offset(x,y,0),state);
        List<BakedQuad> surrounded = model.getQuads(state,Direction.SOUTH,RandomSource.create(0),data,RenderType.translucent());
        checkTile(surrounded,26);
        if (ModList.get().isLoaded("sodium")) checkSodium(view,pos,state,model,data);
        if (LittleTilesSupport.available()) LTChecks.run(view,pos,state,original);
        ResourceLocation normal = ResourceLocation.parse("minecraft:optifine/ctm/bedrock/1_n.png");
        check(minecraft.getResourceManager().getResource(normal).isPresent(), "Expected real bedrock normal map is absent");
        ResourceLocation alias = ResourceLocation.parse("minecraft:textures/lttextures/ctm/optifine/ctm/bedrock/1_n.png");
        var actualResource = minecraft.getResourceManager().getResource(normal).orElseThrow();
        var aliasedResource = minecraft.getResourceManager().getResource(alias).orElseThrow();
        try (var a=actualResource.open();var b=aliasedResource.open()) {
            check(Arrays.equals(a.readAllBytes(),b.readAllBytes()),"Iris alias does not resolve the original PBR image");
        }
        if (ModList.get().isLoaded("iris")) checkIris(minecraft);
    }

    private static void checkSodium(LevelAccessor view,BlockPos pos,BlockState state,
            net.minecraft.client.resources.model.BakedModel model,ModelData data) throws Exception {
        Class<?> accessType=Class.forName("net.caffeinemc.mods.sodium.neoforge.model.NeoForgeModelAccess");
        Class<?> dataType=Class.forName("net.caffeinemc.mods.sodium.client.services.SodiumModelData");
        Class<?> sliceType=Class.forName("net.caffeinemc.mods.sodium.client.world.LevelSlice");
        check(BiomeView.class.isAssignableFrom(sliceType),"Sodium biome adapter was not applied");
        var method=accessType.getMethod("getQuads",net.minecraft.world.level.BlockAndTintGetter.class,BlockPos.class,
                net.minecraft.client.resources.model.BakedModel.class,BlockState.class,Direction.class,
                RandomSource.class,RenderType.class,dataType);
        @SuppressWarnings("unchecked")
        var quads=(List<BakedQuad>)method.invoke(accessType.getConstructor().newInstance(),view,pos,model,state,
                Direction.SOUTH,RandomSource.create(0),RenderType.translucent(),data);
        checkTile(quads,26);
        LittleTextures.LOG.info("LTTEXTURES_SODIUM_PASS actual NeoForge model access and biome adapter");
    }

    private static final class LTChecks {
        static void run(LevelAccessor view, BlockPos pos, BlockState state, net.minecraft.client.resources.model.BakedModel original) {
            var cube = new LittleRenderBox(new AlignedBox(.25f,.25f,.25f,.75f,.75f,.75f),state);
            cube.setFace(Facing.SOUTH,RenderBoxFace.RENDER);
            var clipped = cube.getBakedQuad(new QuadGeneratorContext(),view,pos,BlockPos.ZERO,state,original,
                    ModelData.EMPTY,Facing.SOUTH,RenderType.translucent(),RandomSource.create(0),false,-1);
            checkTile(clipped,26);
            for (BakedQuad quad : clipped) for (int v=0;v<4;v++) {
                int stride=quad.getVertices().length/4;
                for (int axis=0;axis<3;axis++) {
                    float coordinate=Float.intBitsToFloat(quad.getVertices()[v*stride+axis]);
                    check(coordinate>=.249f && coordinate<=.751f,"LT geometry is outside cropped bounds");
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void checkIris(Minecraft minecraft) throws Exception {
        Class<?> loaderType=Class.forName("net.irisshaders.iris.pbr.loader.AtlasPBRLoader");
        Class<? extends Enum> pbrType=(Class<? extends Enum>)Class.forName("net.irisshaders.iris.pbr.texture.PBRType");
        Object loader=loaderType.getConstructor().newInstance();
        var method=loaderType.getDeclaredMethod("createPBRSprite",TextureAtlasSprite.class,ResourceManager.class,TextureAtlas.class,
                int.class,int.class,int.class,pbrType);
        method.setAccessible(true);
        TextureAtlas atlas=(TextureAtlas)minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        var sprite=atlas.getSprite(ResourceLocation.parse("minecraft:lttextures/ctm/optifine/ctm/bedrock/1"));
        int atlasWidth=Math.round(sprite.contents().width()/(sprite.getU1()-sprite.getU0()));
        int atlasHeight=Math.round(sprite.contents().height()/(sprite.getV1()-sprite.getV0()));
        for (String channel:List.of("NORMAL","SPECULAR")) {
            Object result=method.invoke(loader,sprite,minecraft.getResourceManager(),atlas,atlasWidth,atlasHeight,0,
                    Enum.valueOf(pbrType,channel));
            check(result instanceof TextureAtlasSprite,"Iris failed to create the CTM "+channel+" sprite");
            ((TextureAtlasSprite)result).contents().close();
        }
        LittleTextures.LOG.info("LTTEXTURES_IRIS_PBR_PASS actual Iris normal and specular sprite loader");
    }

    private static LevelAccessor fake(Map<BlockPos,BlockState> states) {
        return (LevelAccessor) Proxy.newProxyInstance(ClientSmokeChecks.class.getClassLoader(),new Class[]{LevelAccessor.class},(proxy,method,args)-> {
            return switch(method.getName()) {
                case "getBlockState" -> states.getOrDefault(args[0],Blocks.AIR.defaultBlockState());
                case "getBlockEntity" -> null;
                case "getFluidState" -> Fluids.EMPTY.defaultFluidState();
                case "getHeight" -> 384;
                case "getMinBuildHeight" -> -64;
                case "getShade" -> 1.0f;
                case "getBlockTint" -> -1;
                case "isClientSide" -> true;
                case "hasChunk", "hasChunkAt" -> true;
                case "toString" -> "LittleTexturesSmokeView";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy==args[0];
                default -> throw new UnsupportedOperationException("Unexpected world access: "+method);
            };
        });
    }

    private static void checkTile(List<BakedQuad> quads,int tile) {
        check(!quads.isEmpty(),"No transformed quads");
        String expected="lttextures/ctm/optifine/ctm/glass/glass_normal/"+tile;
        for (BakedQuad quad:quads) check(quad.getSprite().contents().name().getPath().equals(expected),
                "Expected CTM tile "+tile+", got "+quad.getSprite().contents().name());
    }

    private static void checkPreserved(BakedQuad source,BakedQuad target) {
        check(source.getDirection()==target.getDirection(),"Face changed");
        check(source.getTintIndex()==target.getTintIndex(),"Tint changed");
        check(source.isShade()==target.isShade(),"Shade flag changed");
        check(source.hasAmbientOcclusion()==target.hasAmbientOcclusion(),"AO flag changed");
        int stride=source.getVertices().length/4;
        for(int i=0;i<source.getVertices().length;i++) if(i%stride!=4 && i%stride!=5)
            check(source.getVertices()[i]==target.getVertices()[i],"Non-UV vertex data changed");
    }
    private static void check(boolean ok,String message) { if(!ok)throw new IllegalStateException(message); }
}
