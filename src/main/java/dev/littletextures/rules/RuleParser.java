package dev.littletextures.rules;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

public final class RuleParser {
    private static final Set<String> SUPPORTED = Set.of("method", "matchBlocks", "matchTiles", "tiles", "width", "height",
            "faces", "connect", "weights", "symmetry", "biomes", "innerSeams", "source");
    private RuleParser() {}

    public static TextureRule parse(String namespace, String path, Reader reader, int priority) throws IOException {
        Properties p = new Properties();
        p.load(reader);
        Properties normalized = new Properties();
        for (String key : p.stringPropertyNames()) {
            String canonical = SUPPORTED.stream().filter(known -> known.equalsIgnoreCase(key)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported property: " + key));
            String previous = (String) normalized.setProperty(canonical, p.getProperty(key));
            if (previous != null && !previous.equals(p.getProperty(key)))
                throw new IllegalArgumentException("Conflicting property: " + key);
        }
        p = normalized;
        TextureRule.Method method = TextureRule.Method.valueOf(p.getProperty("method", "ctm").strip().toUpperCase(Locale.ROOT));
        List<String> blocks = words(p.getProperty("matchBlocks", ""));
        List<String> matches = words(p.getProperty("matchTiles", ""));
        if (blocks.isEmpty() && matches.isEmpty()) {
            String name = path.substring(path.lastIndexOf('/') + 1, path.length() - ".properties".length());
            if (name.matches("block\\d+.*")) throw new IllegalArgumentException("Legacy numeric block ID needs migration: " + name);
            matches = List.of(name);
        }
        for (String b : blocks) if (b.matches("\\d+.*")) throw new IllegalArgumentException("Legacy block ID needs migration: " + b);
        List<String> raw = expand(p.getProperty("tiles", ""));
        if (raw.isEmpty()) throw new IllegalArgumentException("Missing tiles");
        List<String> tiles = raw.stream().map(t -> resolveTile(namespace, path, t)).toList();
        int width = positive(p, "width", 1), height = positive(p, "height", 1);
        if (method == TextureRule.Method.REPEAT && (long) width * height != tiles.size())
            throw new IllegalArgumentException("repeat dimensions do not match tiles");
        if (method == TextureRule.Method.CTM && tiles.size() != 1 && tiles.size() < 47)
            throw new IllegalArgumentException("ctm requires 47 tiles (or one fixed tile)");
        if (method == TextureRule.Method.VERTICAL && tiles.size() != 4)
            throw new IllegalArgumentException("vertical requires four tiles");
        int[] weights = new int[tiles.size()];
        Arrays.fill(weights, 1);
        List<String> weightStrings = words(p.getProperty("weights", ""));
        if (!weightStrings.isEmpty()) {
            if (weightStrings.size() != tiles.size()) throw new IllegalArgumentException("weights must match tile count");
            long sum = 0;
            for (int i = 0; i < weights.length; i++) {
                weights[i] = Integer.parseInt(weightStrings.get(i));
                if (weights[i] < 0) throw new IllegalArgumentException("Negative weight");
                sum += weights[i];
            }
            if (sum == 0) throw new IllegalArgumentException("All weights are zero");
        }
        int symmetry = switch (p.getProperty("symmetry", "none").strip()) {
            case "none" -> 1; case "opposite" -> 2; case "all" -> 6;
            default -> throw new IllegalArgumentException("Unknown symmetry");
        };
        String connect = p.getProperty("connect", blocks.isEmpty() ? "tile" : "block").strip();
        if (!Set.of("block", "tile", "state").contains(connect)) throw new IllegalArgumentException("Unsupported connect: " + connect);
        String seams = p.getProperty("innerSeams", "false").strip();
        if (!Set.of("true", "false").contains(seams)) throw new IllegalArgumentException("Invalid innerSeams");
        return new TextureRule(namespace, path, priority, method, blocks, matches, tiles, width, height,
                parseFaces(p.getProperty("faces", "all")), connect, weights, symmetry,
                words(p.getProperty("biomes", "")), Boolean.parseBoolean(seams));
    }

    private static int positive(Properties p, String key, int fallback) {
        int value = Integer.parseInt(p.getProperty(key, Integer.toString(fallback)).strip());
        if (value < 1 || value > 4096) throw new IllegalArgumentException("Invalid " + key);
        return value;
    }

    public static List<String> words(String value) {
        return value.isBlank() ? List.of() : List.of(value.strip().split("\\s+"));
    }

    public static List<String> expand(String value) {
        List<String> out = new ArrayList<>();
        for (String token : words(value)) {
            if (token.matches("\\d+-\\d+")) {
                String[] range = token.split("-");
                int first = Integer.parseInt(range[0]), last = Integer.parseInt(range[1]);
                if (last < first || (long) last - first > 16384) throw new IllegalArgumentException("Invalid tile range");
                for (long n = first; n <= last; n++) out.add(Long.toString(n));
            } else out.add(token);
            if (out.size() > 16384) throw new IllegalArgumentException("Too many tiles");
        }
        return List.copyOf(out);
    }

    public static String resolveTile(String namespace, String rulePath, String tile) {
        String value = tile.replace('\\', '/');
        String ns = namespace;
        if (value.startsWith("assets/")) {
            String[] parts = value.split("/", 3);
            if (parts.length != 3) throw new IllegalArgumentException("Invalid assets path");
            ns = parts[1]; value = parts[2];
        } else if (value.contains(":")) {
            int colon = value.indexOf(':'); ns = value.substring(0, colon); value = value.substring(colon + 1);
        } else if (value.startsWith("~/")) value = "optifine/" + value.substring(2);
        else if (value.startsWith("/")) value = value.substring(1);
        else if (value.startsWith("./")) value = rulePath.substring(0, rulePath.lastIndexOf('/') + 1) + value.substring(2);
        else if (!value.startsWith("textures/") && !value.startsWith("optifine/") && !value.startsWith("mcpatcher/"))
            value = rulePath.substring(0, rulePath.lastIndexOf('/') + 1) + value;
        if (value.contains("..") || !ns.matches("[a-z0-9_.-]+") || !value.matches("[a-z0-9/._-]+"))
            throw new IllegalArgumentException("Invalid tile path: " + tile);
        if (!value.endsWith(".png")) value += ".png";
        return ns + ":" + value;
    }

    private static Set<Integer> parseFaces(String text) {
        Set<Integer> faces = new HashSet<>();
        for (String face : words(text)) switch (face) {
            case "all" -> faces.addAll(Set.of(0,1,2,3,4,5));
            case "sides" -> faces.addAll(Set.of(2,3,4,5));
            case "bottom", "down" -> faces.add(0);
            case "top", "up" -> faces.add(1);
            case "north" -> faces.add(2); case "south" -> faces.add(3);
            case "west" -> faces.add(4); case "east" -> faces.add(5);
            default -> throw new IllegalArgumentException("Unknown face: " + face);
        }
        if (faces.isEmpty()) throw new IllegalArgumentException("Empty faces");
        return faces;
    }
}
