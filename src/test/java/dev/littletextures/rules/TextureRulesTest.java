package dev.littletextures.rules;

import org.junit.jupiter.api.Test;
import java.io.StringReader;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TextureRulesTest {
    @Test void allNeighbourConfigurationsReachExactly47Tiles() {
        Set<Integer> indices = new HashSet<>();
        for (int edges = 0; edges < 16; edges++) for (int corners = 0; corners < 16; corners++) {
            int index = CtmTiles.index(edges, corners);
            assertTrue(index >= 0 && index < 47);
            indices.add(index);
        }
        assertEquals(47, indices.size());
        assertEquals(0, CtmTiles.index(0, 15));
        assertEquals(26, CtmTiles.index(15, 15));
        assertEquals(46, CtmTiles.index(15, 0));
        assertEquals(3, CtmTiles.index(1, 0));
        assertEquals(3, CtmTiles.index(1, 15));
        assertEquals(5, CtmTiles.index(5, 0));
        assertEquals(15, CtmTiles.index(5, 2));
    }

    @Test void diagonalCannotConnectWithoutBothEdges() {
        int[][] adjacent = {{1,2},{0,2},{1,3},{0,3}};
        for (int edges = 0; edges < 16; edges++) for (int corner = 0; corner < 4; corner++) {
            int pair = (1 << adjacent[corner][0]) | (1 << adjacent[corner][1]);
            if ((edges & pair) != pair) for (int corners = 0; corners < 16; corners++)
                assertEquals(CtmTiles.index(edges, corners), CtmTiles.index(edges, corners ^ (1 << corner)));
        }
    }

    @Test void repeatIsPeriodicOnAllFacesIncludingNegativePositions() {
        for (int face = 0; face < 6; face++) for (int x = -17; x <= 17; x++) for (int y = -5; y <= 5; y++) {
            int a = FacePlane.repeat(x,y,-13,face,4,8);
            assertTrue(a >= 0 && a < 32);
            assertEquals(a, FacePlane.repeat(x+32,y+32,19,face,4,8));
        }
        assertEquals(3, FacePlane.repeat(-1,0,0,1,4,4));
        assertEquals(0, FacePlane.repeat(-1,0,0,2,4,4));
        assertEquals(0, FacePlane.repeat(0,Integer.MIN_VALUE,0,3,4,4));
    }

    @Test void randomIsStableAndRespectsZeroWeights() {
        int[] weights = {0,1,0,3};
        Set<Integer> seen = new HashSet<>();
        for (int x = -100; x <= 100; x++) {
            int selected = FacePlane.random(x,17,-31,2,weights,9);
            assertEquals(selected, FacePlane.random(x,17,-31,2,weights,9));
            assertTrue(selected == 1 || selected == 3); seen.add(selected);
        }
        assertEquals(Set.of(1,3), seen);
    }

    @Test void verticalUsesStandardTileOrder() {
        assertEquals(3, CtmTiles.vertical(false,false));
        assertEquals(0, CtmTiles.vertical(false,true));
        assertEquals(2, CtmTiles.vertical(true,false));
        assertEquals(1, CtmTiles.vertical(true,true));
    }

    @Test void parsesRealRepeatAndGlassRuleShapes() throws Exception {
        TextureRule repeat = parse("method=repeat\nmatchTiles=bedrock\ntiles=1-64\nwidth=8\nheight=8\n");
        assertEquals(64, repeat.tiles().size());
        assertEquals("minecraft:optifine/ctm/demo/64.png", repeat.tiles().get(63));
        TextureRule glass = parse("method=ctm\nmatchBlocks=glass_pane\nmatchTiles=glass\ntiles=0-46\nfaces=sides\n");
        assertEquals(Set.of(2,3,4,5), glass.faces());
        TextureRule cap = parse("method=ctm\nmatchBlocks=glass_pane\ntiles=assets/minecraft/textures/blocks/pure_color/nothing\nfaces=top\n");
        assertEquals(List.of("minecraft:textures/blocks/pure_color/nothing.png"), cap.tiles());
    }

    @Test void badOrLegacyRulesAreRejectedRatherThanPartiallyApplied() {
        for (String rule : List.of("method=repeat\ntiles=0-3\nwidth=3\nheight=3", "method=ctm\ntiles=0-4",
                "method=random\ntiles=0-2\nweights=0 0 0", "method=random\ntiles=0-2\nmetadata=5",
                "method=random\ntiles=0-2\nmatchBlocks=95", "method=overlay\ntiles=0-16", "tiles=../../bad"))
            assertThrows(IllegalArgumentException.class, () -> parse(rule));
        assertThrows(IllegalArgumentException.class, () -> RuleParser.expand("8-2"));
        assertThrows(IllegalArgumentException.class, () -> RuleParser.expand("0-2147483647"));
        assertEquals(List.of("2147483647"), RuleParser.expand("2147483647-2147483647"));
    }

    @Test void resolvesExplicitAndRelativePaths() {
        String path = "optifine/ctm/demo/demo.properties";
        assertEquals("test:textures/block/a.png", RuleParser.resolveTile("minecraft",path,"assets/test/textures/block/a"));
        assertEquals("test:textures/block/a.png", RuleParser.resolveTile("minecraft",path,"test:textures/block/a.png"));
        assertEquals("minecraft:optifine/ctm/demo/a.png", RuleParser.resolveTile("minecraft",path,"./a"));
    }

    @Test void acceptsObservedLowercaseKeysWithoutAmbiguousDuplicates() throws Exception {
        assertEquals(List.of("bedrock"), parse("method=random\nmatchtiles=bedrock\ntiles=1-4").matchTiles());
        assertThrows(IllegalArgumentException.class, () -> parse("method=random\nmatchTiles=a\nmatchtiles=b\ntiles=1-4"));
    }

    private static TextureRule parse(String text) throws Exception {
        return RuleParser.parse("minecraft", "optifine/ctm/demo/demo.properties", new StringReader(text), 0);
    }
}
