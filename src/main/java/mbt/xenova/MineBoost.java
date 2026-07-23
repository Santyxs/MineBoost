package mbt.xenova;

import mbt.xenova.commands.*;
import mbt.xenova.managers.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MineBoost extends JavaPlugin {

    private static MineBoost instance;

    // ---------------------------------------------------------------
    // LANGUAGES
    // ---------------------------------------------------------------

    private static final String[] BUNDLED_LANGS = {"en", "es", "pt"};
    private static final String FALLBACK_LANG = "en";

    private FileConfiguration messages;
    private FileConfiguration fallbackMessages;
    private String currentLang;

    // ---------------------------------------------------------------
    // CONFIG
    // ---------------------------------------------------------------

    private static final int MIN_SIZE = 3;
    private static final int ABSOLUTE_MAX_SIZE = 31;
    private static final int DEFAULT_MAX_SIZE = 11;

    private final Map<ToolManager.ToolTier, Integer> areaSizeCache = new EnumMap<>(ToolManager.ToolTier.class);
    private final Map<ToolManager.ToolTier, Integer> cooldownCache = new EnumMap<>(ToolManager.ToolTier.class);
    private Set<String> disabledWorldsCache = Collections.emptySet();

    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        reloadLanguage();
        refreshCaches();

        getServer().getPluginManager().registerEvents(new RecipeManager(), this);
        getServer().getPluginManager().registerEvents(new ToolManager(), this);

        RecipeManager.registerAll();

        CommandHandler commandHandler = new CommandHandler();
        Objects.requireNonNull(getCommand("mineboost")).setExecutor(commandHandler);
        Objects.requireNonNull(getCommand("mineboost")).setTabCompleter(commandHandler);

        getLogger().info("MineBoost enabled.");
    }

    public void onDisable() {
        RecipeManager.unregisterAll();
        getLogger().info("MineBoost disabled.");
    }

    public static MineBoost getInstance() {
        return instance;
    }

    public void reloadLanguage() {
        this.currentLang = getConfig().getString("language", FALLBACK_LANG).toLowerCase();

        for (String lang : BUNDLED_LANGS) {
            saveDefaultLangFile(lang);
        }

        File langFile = new File(getDataFolder(), "lang/" + currentLang + ".yml");
        if (!langFile.exists()) {
            getLogger().warning("Language '" + currentLang + "' not found, using '" + FALLBACK_LANG + "' by default.");
            currentLang = FALLBACK_LANG;
            langFile = new File(getDataFolder(), "lang/" + FALLBACK_LANG + ".yml");
        }

        this.messages = YamlConfiguration.loadConfiguration(langFile);

        File fallbackFile = new File(getDataFolder(), "lang/" + FALLBACK_LANG + ".yml");
        this.fallbackMessages = YamlConfiguration.loadConfiguration(fallbackFile);
    }

    public void refreshCaches() {
        FileConfiguration config = getConfig();

        int configuredMax = config.getInt("max-area-size", DEFAULT_MAX_SIZE);
        int effectiveMax = Math.min(configuredMax, ABSOLUTE_MAX_SIZE);
        if (effectiveMax < MIN_SIZE) {
            getLogger().warning("max-area-size (" + effectiveMax + ") is below the minimum of "
                    + MIN_SIZE + "; using " + MIN_SIZE + " instead.");
            effectiveMax = MIN_SIZE;
        }

        for (ToolManager.ToolTier tier : ToolManager.ToolTier.values()) {
            int size = config.getInt("area-sizes." + tier.name(), MIN_SIZE);
            if (size < MIN_SIZE) size = MIN_SIZE;
            if (size > effectiveMax) size = effectiveMax;
            if (size % 2 == 0) size += 1;
            if (size > effectiveMax) size -= 2;
            areaSizeCache.put(tier, size);

            int seconds = config.getInt("cooldown-seconds." + tier.name(), 0);
            if (seconds < 0) seconds = 0;
            if (seconds > 3600) seconds = 3600;
            cooldownCache.put(tier, seconds);
        }

        Set<String> worlds = new HashSet<>();
        for (String world : config.getStringList("disabled-worlds")) {
            worlds.add(world.toLowerCase(Locale.ROOT));
        }
        disabledWorldsCache = worlds;
    }

    private void saveDefaultLangFile(String lang) {
        File file = new File(getDataFolder(), "lang/" + lang + ".yml");
        if (file.exists()) return;

        String resourcePath = "lang/" + lang + ".yml";
        InputStream in = getResource(resourcePath);
        if (in == null) return;

        File parent = file.getParentFile();
        if (!parent.mkdirs() && !parent.isDirectory()) {
            getLogger().warning("Could not create directory: " + parent.getAbsolutePath());
            return;
        }

        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            cfg.save(file);
        } catch (IOException e) {
            getLogger().severe("Could not save " + resourcePath + ": " + e.getMessage());
        }
    }

    public String getMessage(String key) {
        return getMessage(key, null);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String raw = messages.getString(key);
        if (raw == null) {
            raw = fallbackMessages.getString(key, "&cMissing message: " + key);
        }

        String prefix = messages.getString("prefix", fallbackMessages.getString("prefix", ""));
        raw = raw.replace("%prefix%", prefix);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        return raw.replace('&', '§');
    }

    public String getRawMessage(String key, Map<String, String> placeholders) {
        String raw = messages.getString(key);
        if (raw == null) {
            raw = fallbackMessages.getString(key, "&cMissing message: " + key);
        }

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        return raw.replace('&', '§');
    }

    @SuppressWarnings("unused")
    public String getCurrentLang() {
        return currentLang;
    }

    @SuppressWarnings("unused")
    public String[] getAvailableLangs() {
        return BUNDLED_LANGS.clone();
    }

    // ---------------------------------------------------------------
    // TOOLS SETTINGS
    // ---------------------------------------------------------------

    public int getAreaSize(ToolManager.ToolTier tier) {
        return areaSizeCache.getOrDefault(tier, MIN_SIZE);
    }

    public int getCooldownSeconds(ToolManager.ToolTier tier) {
        return cooldownCache.getOrDefault(tier, 0);
    }

    public boolean isWorldDisabled(String worldName) {
        return disabledWorldsCache.contains(worldName.toLowerCase(Locale.ROOT));
    }

    // ---------------------------------------------------------------
    // COMMAND HANDLER
    // ---------------------------------------------------------------

    private static class CommandHandler implements CommandExecutor, TabCompleter {

        private final GiveCommand giveCommand = new GiveCommand();
        private final ReloadCommand reloadCommand = new ReloadCommand();
        private final HelpCommand helpCommand = new HelpCommand();

        public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command,
                                 @NonNull String label, String[] args) {
            if (args.length == 0) {
                helpCommand.execute(sender);
                return true;
            }

            String[] rest = Arrays.copyOfRange(args, 1, args.length);

            switch (args[0].toLowerCase()) {
                case "give"   -> giveCommand.execute(sender, rest);
                case "reload" -> reloadCommand.execute(sender);
                default       -> helpCommand.execute(sender);
            }
            return true;
        }

        public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command,
                                          @NonNull String alias, String[] args) {
            List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                for (String name : List.of("give", "reload", "help")) {
                    if (name.startsWith(args[0].toLowerCase())) completions.add(name);
                }
                return completions;
            }

            String[] rest = Arrays.copyOfRange(args, 1, args.length);
            if ("give".equalsIgnoreCase(args[0])) {
                return giveCommand.tabComplete(rest);
            }
            return completions;
        }
    }
}