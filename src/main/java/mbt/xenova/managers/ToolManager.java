package mbt.xenova.managers;

import mbt.xenova.MineBoost;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ToolManager implements Listener {

    // ---------------------------------------------------------------
    // ENUMS
    // ---------------------------------------------------------------

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
        WOOD("WOODEN", NamedTextColor.WHITE, 0, 0),
        STONE("STONE", NamedTextColor.GRAY, 1, 1),
        IRON("IRON", NamedTextColor.WHITE, 2, 2),
        GOLD("GOLDEN", NamedTextColor.GOLD, 4, 1),
        DIAMOND("DIAMOND", NamedTextColor.AQUA, 3, 3),
        NETHERITE("NETHERITE", NamedTextColor.DARK_PURPLE, 3, 3);

        private final String materialPrefix;
        private final NamedTextColor color;
        private final int efficiencyLevel;
        private final int unbreakingLevel;

        ToolTier(String materialPrefix, NamedTextColor color, int efficiencyLevel, int unbreakingLevel) {
            this.materialPrefix = materialPrefix;
            this.color = color;
            this.efficiencyLevel = efficiencyLevel;
            this.unbreakingLevel = unbreakingLevel;
        }

        public NamedTextColor getColor() {
            return color;
        }

        public String getPermission() {
            return "mineboost.tier." + name().toLowerCase();
        }

        public String getLabel() {
            return MineBoost.getInstance().getRawMessage("tier." + name().toLowerCase(), null);
        }
    }

    // ---------------------------------------------------------------
    // ITEM CREATION
    // ---------------------------------------------------------------

    public static ItemStack createTool(ToolFamily family, ToolTier tier) {
        Material material = Material.valueOf(tier.materialPrefix + "_" + family.materialSuffix);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String displayName = MineBoost.getInstance().getRawMessage("tool.displayname", java.util.Map.of("tier", tier.getLabel(), "family", family.getLabel()));

        meta.displayName(Component.text(displayName, tier.getColor(), TextDecoration.BOLD));

        meta.lore(buildLore(family, tier));

        if (tier.efficiencyLevel > 0) {
            meta.addEnchant(Enchantment.EFFICIENCY, tier.efficiencyLevel, true);
        }
        if (tier.unbreakingLevel > 0) {
            meta.addEnchant(Enchantment.UNBREAKING, tier.unbreakingLevel, true);
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

    private static List<Component> buildLore(ToolFamily family, ToolTier tier) {
        return buildLore(family, tier, true);
    }

    private static List<Component> buildLore(ToolFamily family, ToolTier tier, boolean enabled) {
        MineBoost plugin = MineBoost.getInstance();
        int areaSize = plugin.getAreaSize(tier);
        int totalBlocks = areaSize * areaSize;

        List<Component> lore = new ArrayList<>();

        lore.add(Component.text()
                .color(NamedTextColor.DARK_GRAY)
                .append(Component.text("✦ "))
                .append(Component.text(tier.getLabel(), tier.getColor()))
                .append(Component.text(" · "))
                .append(Component.text(family.getLabel(), NamedTextColor.WHITE))
                .build());

        lore.add(Component.empty());

        lore.add(Component.text(plugin.getRawMessage("tool.lore.area", java.util.Map.of(
                        "size", String.valueOf(areaSize), "blocks", String.valueOf(totalBlocks))),
                NamedTextColor.GRAY));

        // Acción
        lore.add(Component.text(plugin.getRawMessage("tool.lore.action", java.util.Map.of("action", family.getActionWord())),
                NamedTextColor.GRAY));

        // Cooldown
        int cooldownSeconds = plugin.getCooldownSeconds(tier);
        if (cooldownSeconds > 0) {
            lore.add(Component.text(plugin.getRawMessage("tool.lore.cooldown",
                    java.util.Map.of("seconds", String.valueOf(cooldownSeconds))), NamedTextColor.GRAY));
        }

        lore.add(Component.empty());
        lore.add(Component.text(plugin.getRawMessage("tool.lore.compatible", null), NamedTextColor.DARK_GRAY));
        lore.add(Component.text(family.getBlockSummary(), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text()
                .color(NamedTextColor.DARK_GRAY)
                .append(Component.text(plugin.getRawMessage("tool.lore.speed", null) + " "))
                .append(statBar(tier.efficiencyLevel, 4, NamedTextColor.YELLOW))
                .build());

        lore.add(Component.text()
                .color(NamedTextColor.DARK_GRAY)
                .append(Component.text(plugin.getRawMessage("tool.lore.durability", null) + " "))
                .append(statBar(tier.unbreakingLevel, 3, NamedTextColor.GREEN))
                .build());

        lore.add(Component.empty());

        // Status
        lore.add(LegacyComponentSerializer.legacySection()
                .deserialize(plugin.getRawMessage(enabled ? "tool.lore.mode-on" : "tool.lore.mode-off", null)));
        lore.add(LegacyComponentSerializer.legacySection()
                .deserialize(plugin.getRawMessage("tool.lore.toggle-hint", null)));

        lore.add(Component.empty());

        lore.add(LegacyComponentSerializer.legacySection()
                .deserialize(plugin.getRawMessage("tool.lore.epic", null)));
        lore.add(Component.text("MineBoost", NamedTextColor.DARK_PURPLE, TextDecoration.ITALIC));

        return lore;
    }

    private static Component statBar(int level, int max, NamedTextColor color) {
        net.kyori.adventure.text.TextComponent.@org.jetbrains.annotations.NotNull Builder builder = Component.text();
        for (int i = 1; i <= max; i++) {
            builder.append(Component.text("★", i <= level ? color : NamedTextColor.DARK_GRAY));
        }
        return builder.build();
    }

    public static boolean isVanillaTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return true;
        return !item.getItemMeta().getPersistentDataContainer().has(getFamilyKey(), PersistentDataType.STRING);
    }

    public static ToolFamily getFamily(ItemStack item) {
        if (isVanillaTool(item)) return null;
        String name = item.getItemMeta().getPersistentDataContainer().get(getFamilyKey(), PersistentDataType.STRING);
        try {
            return ToolFamily.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    public static ToolTier getTier(ItemStack item) {
        if (isVanillaTool(item)) return null;
        String name = item.getItemMeta().getPersistentDataContainer().get(getTierKey(), PersistentDataType.STRING);
        try {
            return ToolTier.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    public static boolean isEnabled(ItemStack item) {
        return !isDisabled(item);
    }

    private static boolean isDisabled(ItemStack item) {
        if (isVanillaTool(item)) return false;
        Byte value = item.getItemMeta().getPersistentDataContainer().get(getEnabledKey(), PersistentDataType.BYTE);
        return value == null || value != (byte) 1;
    }

    public static boolean toggleEnabled(ItemStack item) {
        ToolFamily family = getFamily(item);
        ToolTier tier = getTier(item);
        if (family == null || tier == null) {
            return false;
        }

        boolean newState = isDisabled(item);

        ItemMeta meta = item.getItemMeta();
        meta.lore(buildLore(family, tier, newState));
        meta.getPersistentDataContainer().set(getEnabledKey(), PersistentDataType.BYTE, (byte) (newState ? 1 : 0));
        item.setItemMeta(meta);

        return newState;
    }

    private static NamespacedKey familyKey;
    private static NamespacedKey tierKey;
    private static NamespacedKey enabledKey;

    private static NamespacedKey getFamilyKey() {
        if (familyKey == null) familyKey = new NamespacedKey(MineBoost.getInstance(), "mineboost-family");
        return familyKey;
    }

    private static NamespacedKey getTierKey() {
        if (tierKey == null) tierKey = new NamespacedKey(MineBoost.getInstance(), "mineboost-tier");
        return tierKey;
    }

    private static NamespacedKey getEnabledKey() {
        if (enabledKey == null) enabledKey = new NamespacedKey(MineBoost.getInstance(), "mineboost-enabled");
        return enabledKey;
    }

    // ---------------------------------------------------------------
    // LISTENER
    // ---------------------------------------------------------------

    private final Random random = new Random();

    private final Map<UUID, Long> lastNoPermissionNotice = new HashMap<>();
    private static final long NOTICE_COOLDOWN_MS = 4000;

    private final Map<String, Long> lastAreaBreak = new HashMap<>();

    private boolean processingSubEvent = false;

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (processingSubEvent) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (isVanillaTool(tool)) return;

        ToolFamily family = getFamily(tool);
        ToolTier tier = getTier(tool);

        if (family == null || tier == null) return;

        Block origin = event.getBlock();

        if (!isEnabled(tool)) return;

        if (!player.hasPermission(tier.getPermission())) {
            notifyNoPermission(player, tier);
            return;
        }

        if (!family.isValidBlock(origin.getType())) return;

        if (MineBoost.getInstance().isWorldDisabled(origin.getWorld().getName())) return;

        if (isOnCooldown(player, tier)) return;

        int areaSize = MineBoost.getInstance().getAreaSize(tier);

        BlockFace face = player.getTargetBlockFace(6);
        if (face == null) face = BlockFace.UP;

        List<Block> plane = getPlaneBlocks(origin, face, areaSize);

        int brokenExtra = 0;
        for (Block b : plane) {
            if (b.getType() != origin.getType()) continue;

            BlockBreakEvent subEvent = new BlockBreakEvent(b, player);
            processingSubEvent = true;
            try {
                Bukkit.getPluginManager().callEvent(subEvent);
            } finally {
                processingSubEvent = false;
            }
            if (subEvent.isCancelled()) continue;

            b.breakNaturally(tool);
            brokenExtra++;
        }

        if (brokenExtra > 0) {
            lastAreaBreak.put(cooldownKey(player, tier), System.currentTimeMillis());
            if (player.getGameMode() != GameMode.CREATIVE) {
                damageTool(player, tool, brokenExtra);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack item = event.getItem();
        if (isVanillaTool(item)) return;

        event.setCancelled(true);

        boolean nowEnabled = toggleEnabled(item);
        player.getInventory().setItemInMainHand(item);
        MineBoost plugin = MineBoost.getInstance();

        if (nowEnabled) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(plugin.getMessage("toggle.enabled")));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.4f);
        } else {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(plugin.getMessage("toggle.disabled")));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 0.8f);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastNoPermissionNotice.remove(id);
        lastAreaBreak.keySet().removeIf(key -> key.startsWith(id + ":"));
    }

    private boolean isOnCooldown(Player player, ToolTier tier) {
        int cooldownSeconds = MineBoost.getInstance().getCooldownSeconds(tier);
        if (cooldownSeconds <= 0) return false;

        Long last = lastAreaBreak.get(cooldownKey(player, tier));
        if (last == null) return false;

        long elapsedMs = System.currentTimeMillis() - last;
        long remainingMs = (cooldownSeconds * 1000L) - elapsedMs;
        if (remainingMs <= 0) return false;

        int remainingSeconds = (int) Math.ceil(remainingMs / 1000.0);
        String message = MineBoost.getInstance()
                .getMessage("mine.cooldown", java.util.Map.of("seconds", String.valueOf(remainingSeconds)));
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(message));
        return true;
    }

    private String cooldownKey(Player player, ToolTier tier) {
        return player.getUniqueId() + ":" + tier.name();
    }

    private void notifyNoPermission(Player player, ToolTier tier) {
        long now = System.currentTimeMillis();
        Long last = lastNoPermissionNotice.get(player.getUniqueId());
        if (last != null && now - last < NOTICE_COOLDOWN_MS) return;
        lastNoPermissionNotice.put(player.getUniqueId(), now);

        String message = MineBoost.getInstance().getMessage("mine.no-permission", java.util.Map.of("tier", tier.getLabel()));
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    private List<Block> getPlaneBlocks(Block origin, BlockFace face, int size) {
        int radius = (size - 1) / 2;
        List<Block> blocks = new ArrayList<>();

        for (int a = -radius; a <= radius; a++) {
            for (int b = -radius; b <= radius; b++) {
                if (a == 0 && b == 0) continue;

                if (face == BlockFace.UP || face == BlockFace.DOWN) {
                    blocks.add(origin.getRelative(a, 0, b)); // X-Z
                } else if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                    blocks.add(origin.getRelative(a, b, 0)); // X-Y
                } else {
                    blocks.add(origin.getRelative(0, a, b)); // Y-Z (EAST/WEST)
                }
            }
        }
        return blocks;
    }

    private void damageTool(Player player, ItemStack tool, int extraBlocksBroken) {
        if (!(tool.getItemMeta() instanceof Damageable meta)) {
            return;
        }

        int unbreakingLevel = tool.getEnchantmentLevel(Enchantment.UNBREAKING);

        int damageToApply = 0;
        for (int i = 0; i < extraBlocksBroken; i++) {
            if (unbreakingLevel == 0 || random.nextInt(unbreakingLevel + 1) == 0) {
                damageToApply++;
            }
        }

        if (damageToApply == 0) return;

        int newDamage = meta.getDamage() + damageToApply;
        int maxDurability = tool.getType().getMaxDurability();

        if (newDamage >= maxDurability) {
            player.getInventory().setItemInMainHand(null);
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
        } else {
            meta.setDamage(newDamage);
            tool.setItemMeta(meta);
            player.getInventory().setItemInMainHand(tool);
        }
    }
}