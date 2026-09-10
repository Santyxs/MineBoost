package mbt.xenova;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import mbt.xenova.commands.*;
import mbt.xenova.managers.*;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MineBoost extends JavaPlugin {

    private static MineBoost instance;

    // ---------------------------------------------------------------
    // LANGUAGES
    // ---------------------------------------------------------------

    private static final String[] BUNDLED_LANGS = {"en", "es", "pt"};
    private static final String FALLBACK_LANG = "en";

    private YamlDocument messages;
    private YamlDocument fallbackMessages;

    // ---------------------------------------------------------------
    // CONFIG
    // ---------------------------------------------------------------

    private static final int MIN_SIZE = 3;
    private static final int ABSOLUTE_MAX_SIZE = 31;
    private static final int DEFAULT_MAX_SIZE = 11;

    private YamlDocument config;

    private final Map<ToolManager.ToolTier, Integer> areaSizeCache = new EnumMap<>(ToolManager.ToolTier.class);
    private final Map<ToolManager.ToolTier, Integer> cooldownCache = new EnumMap<>(ToolManager.ToolTier.class);
    private Set<String> disabledWorldsCache = Collections.emptySet();

    private boolean giveExperience = true;
    private boolean areaBreakEffects = true;
    private Particle areaBreakParticle;
    private Sound areaBreakSound;
    private float soundVolume = 1.0f;
    private float soundPitch = 1.0f;

    public void onEnable() {
        instance = this;

        loadConfig();
        reloadLanguage();
        refreshCaches();

        getServer().getPluginManager().registerEvents(new RecipeManager(), this);
        getServer().getPluginManager().registerEvents(new ToolManager(), this);

        RecipeManager.registerAll();

        if (config.getBoolean("check-updates", true)) {
            new UpdateChecker().checkAsync();
        }

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

    // ---------------------------------------------------------------
    // CONFIG
    // ---------------------------------------------------------------

    private void loadConfig() {
        try {
            config = YamlDocument.create(
                    new File(getDataFolder(), "config.yml"),
                    Objects.requireNonNull(getResource("config.yml")),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("version")).build()
            );
        } catch (IOException e) {
            getLogger().severe("Could not load config.yml: " + e.getMessage());
        }
    }

    public void reloadConfig() {
        if (config == null) return;
        try {
            config.reload();
        } catch (IOException e) {
            getLogger().severe("Could not reload config.yml: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // LANG FILES
    // ---------------------------------------------------------------

    public void reloadLanguage() {
        String currentLang = config.getString("language", FALLBACK_LANG).toLowerCase();

        Map<String, YamlDocument> loaded = new HashMap<>();
        for (String lang : BUNDLED_LANGS) {
            YamlDocument doc = loadLangDocument(lang);
            if (doc != null) loaded.put(lang, doc);
        }

        this.fallbackMessages = loaded.get(FALLBACK_LANG);
        this.messages = loaded.get(currentLang);

        if (this.messages == null) {
            getLogger().warning("Language '" + currentLang + "' not found, using '" + FALLBACK_LANG + "' by default.");
            this.messages = this.fallbackMessages;
        }
    }

    private YamlDocument loadLangDocument(String lang) {
        String resourcePath = "lang/" + lang + ".yml";
        InputStream defaults = getResource(resourcePath);
        if (defaults == null) return null;

        try {
            return YamlDocument.create(
                    new File(getDataFolder(), resourcePath),
                    defaults,
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("version")).build()
            );
        } catch (IOException e) {
            getLogger().severe("Could not load " + resourcePath + ": " + e.getMessage());
            return null;
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

    // ---------------------------------------------------------------
    // TOOLS SETTINGS
    // ---------------------------------------------------------------

    public void refreshCaches() {
        int configuredMax = config.getInt("max-area-size", DEFAULT_MAX_SIZE);
        int effectiveMax = Math.min(configuredMax, ABSOLUTE_MAX_SIZE);
        if (effectiveMax < MIN_SIZE) {
            getLogger().warning("max-area-size (" + effectiveMax + ") is below the minimum of " + MIN_SIZE + "; using " + MIN_SIZE + " instead.");
            effectiveMax = MIN_SIZE;
        }

        for (ToolManager.ToolTier tier : ToolManager.ToolTier.values()) {
            int size = config.getInt("area-sizes." + tier.name(), MIN_SIZE);
            if (size < MIN_SIZE) size = MIN_SIZE;
            if (size > effectiveMax) size = effectiveMax;
            if (size % 2 == 0) size += 1;
            if (size > effectiveMax) size -= 2;
            areaSizeCache.put(tier, size);

            int seconds = config.getInt("cooldowns." + tier.name(), 0);
            if (seconds < 0) seconds = 0;
            if (seconds > 3600) seconds = 3600;
            cooldownCache.put(tier, seconds);
        }

        Set<String> worlds = new HashSet<>();
        List<String> disabledWorlds = config.getStringList("disabled-worlds");
        if (disabledWorlds != null) {
            for (String world : disabledWorlds) {
                worlds.add(world.toLowerCase(Locale.ROOT));
            }
        }
        disabledWorldsCache = worlds;

        giveExperience = config.getBoolean("give-experience", true);
        areaBreakEffects = config.getBoolean("area-break-effects", true);

        String particleName = config.getString("particle", "BLOCK");
        if ("BLOCK".equalsIgnoreCase(particleName)) {
            areaBreakParticle = null;
        } else {
            NamespacedKey particleKey = NamespacedKey.minecraft(particleName.toLowerCase(Locale.ROOT));
            areaBreakParticle = Registry.PARTICLE_TYPE.get(particleKey);
            if (areaBreakParticle == null) {
                getLogger().warning("Invalid particle '" + particleName + "' in config.yml; using BLOCK instead.");
            }
        }

        String soundName = config.getString("sound", "AUTO");
        if ("AUTO".equalsIgnoreCase(soundName)) {
            areaBreakSound = null;
        } else {
            NamespacedKey soundKey = NamespacedKey.minecraft(soundName.toLowerCase(Locale.ROOT));
            areaBreakSound = Registry.SOUNDS.get(soundKey);
            if (areaBreakSound == null) {
                getLogger().warning("Invalid sound '" + soundName + "' in config.yml; using AUTO instead.");
            }
        }

        soundVolume = config.getDouble("sound-volume", 1.0).floatValue();
        soundPitch = config.getDouble("sound-pitch", 1.0).floatValue();
    }

    public int getAreaSize(ToolManager.ToolTier tier) {
        return areaSizeCache.getOrDefault(tier, MIN_SIZE);
    }

    public int getCooldownSeconds(ToolManager.ToolTier tier) {
        return cooldownCache.getOrDefault(tier, 0);
    }

    public boolean isWorldDisabled(String worldName) {
        return disabledWorldsCache.contains(worldName.toLowerCase(Locale.ROOT));
    }

    public boolean isGiveExperienceEnabled() {
        return giveExperience;
    }

    public boolean isAreaBreakEffectsEnabled() {
        return areaBreakEffects;
    }

    public Particle getAreaBreakParticle() {
        return areaBreakParticle;
    }

    public Sound getAreaBreakSound() {
        return areaBreakSound;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }

    public void disableInvalidParticle(Particle attempted) {
        if (areaBreakParticle == attempted) {
            getLogger().warning("Particle '" + attempted + "' needs extra data MineBoost doesn't provide; falling back to BLOCK.");
            areaBreakParticle = null;
        }
    }

    // ---------------------------------------------------------------
    // COMMAND HANDLER
    // ---------------------------------------------------------------

    private static class CommandHandler implements CommandExecutor, TabCompleter {

        private final GiveCommand giveCommand = new GiveCommand();
        private final InfoCommand infoCommand = new InfoCommand();
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
                case "give" -> giveCommand.execute(sender, rest);
                case "info" -> infoCommand.execute(sender, rest);
                case "reload" -> reloadCommand.execute(sender);
                default -> helpCommand.execute(sender);
            }
            return true;
        }

        public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String[] args) {
            List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                for (String name : List.of("give", "info", "reload", "help")) {
                    if (name.startsWith(args[0].toLowerCase())) completions.add(name);
                }
                return completions;
            }

            String[] rest = Arrays.copyOfRange(args, 1, args.length);
            if ("give".equalsIgnoreCase(args[0])) {
                return giveCommand.tabComplete(rest);
            }

            if ("info".equalsIgnoreCase(args[0])) {
                List<String> suggestions = infoCommand.tabComplete(rest);
                String prefix = (rest.length > 0) ? rest[0].toLowerCase() : "";
                suggestions.removeIf(s -> !s.toLowerCase().startsWith(prefix));
                return suggestions;
            }
            return completions;
        }
    }
}