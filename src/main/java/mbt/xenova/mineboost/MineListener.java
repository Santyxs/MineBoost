package mbt.xenova.mineboost;

import mbt.xenova.mineboost.managers.ToolManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class MineListener implements Listener {

    private final Random random = new Random();

    private final Map<UUID, Long> lastNoPermissionNotice = new HashMap<>();
    private static final long NOTICE_COOLDOWN_MS = 4000;

    private final Map<String, Long> lastAreaBreak = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!ToolManager.isMultiTool(tool)) return;

        ToolManager.ToolFamily family = ToolManager.getFamily(tool);
        ToolManager.ToolTier tier = ToolManager.getTier(tool);
        Block origin = event.getBlock();

        if (!ToolManager.isEnabled(tool)) return;

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
            Bukkit.getPluginManager().callEvent(subEvent);
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

    private boolean isOnCooldown(Player player, ToolManager.ToolTier tier) {
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

    private String cooldownKey(Player player, ToolManager.ToolTier tier) {
        return player.getUniqueId() + ":" + tier.name();
    }

    private void notifyNoPermission(Player player, ToolManager.ToolTier tier) {
        long now = System.currentTimeMillis();
        Long last = lastNoPermissionNotice.get(player.getUniqueId());
        if (last != null && now - last < NOTICE_COOLDOWN_MS) return;
        lastNoPermissionNotice.put(player.getUniqueId(), now);

        String message = MineBoost.getInstance()
                .getMessage("mine.no-permission", java.util.Map.of("tier", tier.getLabel()));
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
        if (!(tool.getItemMeta() instanceof Damageable)) return;

        int unbreakingLevel = tool.getEnchantmentLevel(Enchantment.DURABILITY);
        Damageable meta = (Damageable) tool.getItemMeta();

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
            // La herramienta se rompe
            player.getInventory().setItemInMainHand(null);
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
        } else {
            meta.setDamage(newDamage);
            tool.setItemMeta((org.bukkit.inventory.meta.ItemMeta) meta);
        }
    }
}
