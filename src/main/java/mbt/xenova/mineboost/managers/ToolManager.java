package mbt.xenova.mineboost.managers;

import mbt.xenova.mineboost.MineBoost;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ToolManager {

    public enum ToolFamily {
        PICKAXE("PICKAXE", Tag.MINEABLE_PICKAXE),
        SHOVEL("SHOVEL", Tag.MINEABLE_SHOVEL),
        AXE("AXE", Tag.MINEABLE_AXE);

        private final String materialSuffix;
        private final Tag<Material> mineableTag;

        ToolFamily(String materialSuffix, Tag<Material> mineableTag) {
            this.materialSuffix = materialSuffix;
            this.mineableTag = mineableTag;
        }

        public boolean isValidBlock(Material material) {
            return mineableTag.isTagged(material);
        }

        private String langKey() {
            return "family." + name().toLowerCase();
        }

        public String getLabel() {
            return MineBoost.getInstance().getRawMessage(langKey() + ".label", null);
        }

        public String getActionWord() {
            return MineBoost.getInstance().getRawMessage(langKey() + ".action", null);
        }

        public String getBlockSummary() {
            return MineBoost.getInstance().getRawMessage(langKey() + ".blocks", null);
        }
    }

    public enum ToolTier {
        WOOD("WOODEN", ChatColor.WHITE, 0, 0),
        STONE("STONE", ChatColor.GRAY, 1, 1),
        IRON("IRON", ChatColor.WHITE, 2, 2),
        GOLD("GOLDEN", ChatColor.GOLD, 4, 1),
        DIAMOND("DIAMOND", ChatColor.AQUA, 3, 3),
        NETHERITE("NETHERITE", ChatColor.DARK_PURPLE, 3, 3);

        private final String materialPrefix;
        private final ChatColor color;
        private final int efficiencyLevel;
        private final int unbreakingLevel;

        ToolTier(String materialPrefix, ChatColor color, int efficiencyLevel, int unbreakingLevel) {
            this.materialPrefix = materialPrefix;
            this.color = color;
            this.efficiencyLevel = efficiencyLevel;
            this.unbreakingLevel = unbreakingLevel;
        }

        public String getPermission() {
            return "mineboost.tier." + name().toLowerCase();
        }

        public String getLabel() {
            return MineBoost.getInstance().getRawMessage("tier." + name().toLowerCase(), null);
        }
    }

    public static ItemStack createTool(ToolFamily family, ToolTier tier) {
        Material material = Material.valueOf(tier.materialPrefix + "_" + family.materialSuffix);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String displayName = MineBoost.getInstance().getRawMessage("tool.displayname",
                java.util.Map.of("tier", tier.getLabel(), "family", family.getLabel()));
        meta.setDisplayName(tier.color + "" + ChatColor.BOLD + displayName);
        meta.setLore(buildLore(family, tier));

        if (tier.efficiencyLevel > 0) {
            meta.addEnchant(Enchantment.DIG_SPEED, tier.efficiencyLevel, true);
        }
        if (tier.unbreakingLevel > 0) {
            meta.addEnchant(Enchantment.DURABILITY, tier.unbreakingLevel, true);
        }

        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DYE
        );

        meta.getPersistentDataContainer().set(getFamilyKey(), PersistentDataType.STRING, family.name());
        meta.getPersistentDataContainer().set(getTierKey(), PersistentDataType.STRING, tier.name());
        meta.getPersistentDataContainer().set(getEnabledKey(), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private static List<String> buildLore(ToolFamily family, ToolTier tier) {
        return buildLore(family, tier, true);
    }

    private static List<String> buildLore(ToolFamily family, ToolTier tier, boolean enabled) {
        MineBoost plugin = MineBoost.getInstance();
        int areaSize = plugin.getAreaSize(tier);
        int totalBlocks = areaSize * areaSize;

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "✦ " + tier.getLabel() + " · " + family.getLabel());
        lore.add("");
        lore.add(ChatColor.GRAY + plugin.getRawMessage("tool.lore.area", java.util.Map.of(
                "size", String.valueOf(areaSize), "blocks", String.valueOf(totalBlocks))));
        lore.add(ChatColor.GRAY + plugin.getRawMessage("tool.lore.action", java.util.Map.of("action", family.getActionWord())));
        int cooldownSeconds = plugin.getCooldownSeconds(tier);
        if (cooldownSeconds > 0) {
            lore.add(ChatColor.GRAY + plugin.getRawMessage("tool.lore.cooldown",
                    java.util.Map.of("seconds", String.valueOf(cooldownSeconds))));
        }
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + plugin.getRawMessage("tool.lore.compatible", null));
        lore.add(ChatColor.GRAY + family.getBlockSummary());
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + plugin.getRawMessage("tool.lore.speed", null) + " " + statBar(tier.efficiencyLevel, 4, ChatColor.YELLOW));
        lore.add(ChatColor.DARK_GRAY + plugin.getRawMessage("tool.lore.durability", null) + " " + statBar(tier.unbreakingLevel, 3, ChatColor.GREEN));
        lore.add("");
        lore.add(plugin.getRawMessage(enabled ? "tool.lore.mode-on" : "tool.lore.mode-off", null));
        lore.add(plugin.getRawMessage("tool.lore.toggle-hint", null));
        lore.add("");
        lore.add(plugin.getRawMessage("tool.lore.epic", null));
        lore.add(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "MineBoost");
        return lore;
    }

    private static String statBar(int level, int max, ChatColor color) {
        StringBuilder bar = new StringBuilder();
        for (int i = 1; i <= max; i++) {
            bar.append(i <= level ? color : ChatColor.DARK_GRAY).append("★");
        }
        return bar.toString();
    }

    public static boolean isMultiTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(getFamilyKey(), PersistentDataType.STRING);
    }

    public static ToolFamily getFamily(ItemStack item) {
        if (!isMultiTool(item)) return null;
        String name = item.getItemMeta().getPersistentDataContainer().get(getFamilyKey(), PersistentDataType.STRING);
        try {
            return ToolFamily.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    public static ToolTier getTier(ItemStack item) {
        if (!isMultiTool(item)) return null;
        String name = item.getItemMeta().getPersistentDataContainer().get(getTierKey(), PersistentDataType.STRING);
        try {
            return ToolTier.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    public static boolean isEnabled(ItemStack item) {
        if (!isMultiTool(item)) return false;
        Byte value = item.getItemMeta().getPersistentDataContainer().get(getEnabledKey(), PersistentDataType.BYTE);
        return value == null || value == (byte) 1; // por compatibilidad, si no existe se asume activado
    }

    public static boolean toggleEnabled(ItemStack item) {
        ToolFamily family = getFamily(item);
        ToolTier tier = getTier(item);
        boolean newState = !isEnabled(item);

        ItemMeta meta = item.getItemMeta();
        meta.setLore(buildLore(family, tier, newState));
        meta.getPersistentDataContainer().set(getEnabledKey(), PersistentDataType.BYTE, (byte) (newState ? 1 : 0));
        item.setItemMeta(meta);

        return newState;
    }

    private static NamespacedKey getFamilyKey() {
        return new NamespacedKey(MineBoost.getInstance(), "mineboost-family");
    }

    private static NamespacedKey getTierKey() {
        return new NamespacedKey(MineBoost.getInstance(), "mineboost-tier");
    }

    private static NamespacedKey getEnabledKey() {
        return new NamespacedKey(MineBoost.getInstance(), "mineboost-enabled");
    }
}
