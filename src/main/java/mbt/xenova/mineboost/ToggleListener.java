package mbt.xenova.mineboost;

import mbt.xenova.mineboost.managers.ToolManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ToggleListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) return;

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack item = event.getItem();
        if (!ToolManager.isMultiTool(item)) return;

        event.setCancelled(true);

        boolean nowEnabled = ToolManager.toggleEnabled(item);
        MineBoost plugin = MineBoost.getInstance();

        if (nowEnabled) {
            player.sendMessage(plugin.getMessage("toggle.enabled"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.4f);
        } else {
            player.sendMessage(plugin.getMessage("toggle.disabled"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 0.8f);
        }
    }
}
