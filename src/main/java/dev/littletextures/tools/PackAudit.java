package dev.littletextures.tools;

import dev.littletextures.rules.RuleParser;
import dev.littletextures.rules.TextureRule;
import dev.littletextures.rules.PackCompatibility;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Static validation of a real resource pack; does not load the game or alter the pack. */
public final class PackAudit {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: PackAudit <pack folder or zip>");
        Path path = Path.of(args[0]);
        if (Files.isDirectory(path)) {
            try (var paths = Files.walk(path)) {
                Set<String> names = new HashSet<>();
                paths.filter(Files::isRegularFile).forEach(p -> names.add(path.relativize(p).toString().replace('\\','/')));
                audit(names, name -> Files.newBufferedReader(path.resolve(name), StandardCharsets.UTF_8));
            }
        } else try (ZipFile zip = new ZipFile(path.toFile())) {
            Set<String> names = new HashSet<>();
            zip.stream().filter(entry -> !entry.isDirectory()).forEach(entry -> names.add(entry.getName()));
            audit(names, name -> new InputStreamReader(zip.getInputStream(zip.getEntry(name)), StandardCharsets.UTF_8));
        }
    }

    @FunctionalInterface private interface Read { Reader open(String name) throws IOException; }
    private static void audit(Set<String> names, Read read) throws IOException {
        Map<TextureRule.Method, Integer> methods = new EnumMap<>(TextureRule.Method.class);
        int valid = 0, invalid = 0;
        Set<String> tiles = new HashSet<>();
        List<String> rules = names.stream().filter(n -> n.startsWith("assets/") && n.endsWith(".properties")
                && (n.contains("/optifine/ctm/") || n.contains("/mcpatcher/ctm/"))).sorted().toList();
        for (String name : rules) {
            String[] parts = name.split("/", 3);
            try (Reader reader = read.open(name)) {
                TextureRule rule = RuleParser.parse(parts[1], parts[2], reader, 0);
                methods.merge(rule.method(), 1, Integer::sum);
                valid++; tiles.addAll(rule.tiles());
            } catch (IllegalArgumentException e) { invalid++; System.out.println("REJECTED " + name + ": " + e.getMessage()); }
        }
        int missing = 0, normal = 0, specular = 0, emissive = 0, fallback = 0;
        for (String tile : new TreeSet<>(tiles)) {
            String name = "assets/" + tile.replace(':','/');
            if (!names.contains(name)) {
                if (PackCompatibility.isTransparentPlaceholder(tile)) { fallback++; System.out.println("BUILTIN_TRANSPARENT " + name); }
                else { missing++; System.out.println("MISSING " + name); }
            }
            String base = name.substring(0, name.length()-4);
            if (names.contains(base + "_n.png")) normal++;
            if (names.contains(base + "_s.png")) specular++;
            if (names.contains(base + "_e.png")) emissive++;
        }
        System.out.println("Rules=" + rules.size() + " accepted=" + valid + " rejected=" + invalid + " methods=" + methods);
        System.out.println("Unique tiles=" + tiles.size() + " missing=" + missing + " transparentFallback=" + fallback + " PBR: normal=" + normal + " specular=" + specular + " emissive=" + emissive);
        if (invalid != 0 || missing != 0) throw new IllegalStateException("Pack has rejected rules or missing tile resources");
    }
}
