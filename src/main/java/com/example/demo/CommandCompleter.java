package com.example.demo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 命令补全：启动器命令 / Minecraft 命令 / 常用参数。
 * 只做前缀补全（不区分大小写），由命令栏决定单条直接补全还是弹层选择。
 */
public final class CommandCompleter {

    public record Completion(String value, String description) {
    }

    private static final List<String> LAUNCHER = List.of(
            "help", "launch", "kill", "versions", "java", "memory", "open", "clear");

    private static final List<String> MC_COMMANDS = List.of(
            "advancement", "ban", "clear", "clone", "damage", "data", "deop", "difficulty",
            "effect", "enchant", "execute", "experience", "fill", "forceload", "gamemode",
            "gamerule", "give", "help", "kill", "list", "locate", "loot", "me", "msg", "op",
            "pardon", "particle", "playsound", "reload", "say", "scoreboard", "seed",
            "setblock", "setworldspawn", "spawnpoint", "spectate", "stop", "summon", "tag",
            "team", "tell", "tellraw", "time", "title", "tp", "weather", "whitelist", "xp");

    private static final Map<String, List<String>> MC_ARGS = new LinkedHashMap<>();

    static {
        MC_ARGS.put("gamemode", List.of("survival", "creative", "adventure", "spectator"));
        MC_ARGS.put("difficulty", List.of("peaceful", "easy", "normal", "hard"));
        MC_ARGS.put("time", List.of("set", "add", "query", "day", "noon", "night", "midnight"));
        MC_ARGS.put("weather", List.of("clear", "rain", "thunder"));
        MC_ARGS.put("effect", List.of("give", "clear"));
        MC_ARGS.put("xp", List.of("add", "set", "query"));
        MC_ARGS.put("title", List.of("title", "subtitle", "actionbar", "times", "clear", "reset"));
        MC_ARGS.put("scoreboard", List.of("objectives", "players"));
        MC_ARGS.put("advancement", List.of("grant", "revoke"));
        MC_ARGS.put("execute", List.of("as", "at", "run", "if", "unless", "positioned", "facing",
                "align", "anchored", "in", "rotated", "store"));
        MC_ARGS.put("tp", List.of("@s", "@p", "@a", "@r", "@e"));
        MC_ARGS.put("teleport", List.of("@s", "@p", "@a", "@r", "@e"));
        MC_ARGS.put("kill", List.of("@s", "@p", "@a", "@r", "@e"));
        MC_ARGS.put("clear", List.of("@s", "@p", "@a", "@r"));
        MC_ARGS.put("gamerule", List.of("keepInventory", "doDaylightCycle", "doMobSpawning",
                "mobGriefing", "randomTickSpeed", "commandBlockOutput", "sendCommandFeedback",
                "doFireTick", "naturalRegeneration", "showDeathMessages", "doWeatherCycle",
                "playersSleepingPercentage"));
        MC_ARGS.put("give", List.of("diamond", "diamond_block", "netherite_ingot", "netherite_sword",
                "golden_apple", "enchanted_golden_apple", "ender_pearl", "elytra", "totem_of_undying",
                "iron_sword", "bow", "arrow", "torch", "oak_log", "emerald", "nether_star"));
        MC_ARGS.put("summon", List.of("zombie", "skeleton", "creeper", "spider", "enderman",
                "villager", "iron_golem", "armor_stand", "item", "pig", "cow", "sheep", "wolf",
                "cat", "horse", "wither", "ender_dragon"));
        MC_ARGS.put("enchant", List.of("sharpness", "protection", "efficiency", "unbreaking",
                "mending", "fortune", "silk_touch", "power", "punch", "flame", "infinity",
                "looting", "respiration", "aqua_affinity", "thorns", "depth_strider",
                "frost_walker", "feather_falling", "fire_aspect", "knockback"));
    }

    private CommandCompleter() {
    }

    public static List<Completion> complete(String input, File gameDir) {
        if (input == null) return List.of();
        String text = input;
        int lastSpace = text.lastIndexOf(' ');
        String fragment = lastSpace >= 0 ? text.substring(lastSpace + 1) : text;
        boolean firstToken = lastSpace < 0;

        if (text.startsWith("/")) {
            if (firstToken) {
                return filter(MC_COMMANDS, fragment.substring(1), "/", "Minecraft 命令");
            }
            String body = text.substring(1);
            String[] tokens = body.split("\\s+", 2);
            String command = tokens[0].toLowerCase(Locale.ROOT);
            List<String> args = MC_ARGS.get(command);
            if (args == null) return List.of();
            return filter(args, fragment, "", "参数");
        }

        if (firstToken) {
            return filter(LAUNCHER, fragment, "", "启动器命令");
        }

        String[] tokens = text.split("\\s+", 2);
        String command = tokens[0].toLowerCase(Locale.ROOT);
        if ("open".equals(command) && gameDir != null) {
            File[] dirs = gameDir.listFiles(File::isDirectory);
            if (dirs != null) {
                Arrays.sort(dirs, java.util.Comparator.comparing(File::getName));
                List<String> names = new ArrayList<>();
                for (File dir : dirs) names.add(dir.getName());
                return filter(names, fragment, "", "目录");
            }
        }
        return List.of();
    }

    private static List<Completion> filter(List<String> values, String fragment, String prefix,
                                           String description) {
        String needle = fragment == null ? "" : fragment.toLowerCase(Locale.ROOT);
        List<Completion> starts = new ArrayList<>();
        List<Completion> contains = new ArrayList<>();
        for (String value : values) {
            String lower = value.toLowerCase(Locale.ROOT);
            if (lower.startsWith(needle)) {
                starts.add(new Completion(prefix + value, description));
            } else if (!needle.isEmpty() && lower.contains(needle)) {
                contains.add(new Completion(prefix + value, description));
            }
        }
        starts.addAll(contains);
        return starts;
    }

    /** 用补全值替换最后一个词；补命令名时补一个空格，方便接着输入参数。 */
    public static String apply(String input, String value) {
        int lastSpace = input.lastIndexOf(' ');
        String prefix = lastSpace >= 0 ? input.substring(0, lastSpace + 1) : "";
        boolean firstToken = lastSpace < 0;
        String result = prefix + value;
        if (firstToken) result += " ";
        return result;
    }
}
