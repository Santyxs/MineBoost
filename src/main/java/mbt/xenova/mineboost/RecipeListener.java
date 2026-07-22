package mbt.xenova.mineboost;

import mbt.xenova.mineboost.managers.RecipeManager;
import mbt.xenova.mineboost.managers.ToolManager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class RecipeListener implements Listener {

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (!ToolManager.isMultiTool(result)) return;

        HumanEntity crafter = event.getWhoClicked();
        if (!(crafter instanceof Player player)) return;

        ToolManager.ToolTier tier = ToolManager.getTier(result);
        if (!player.hasPermission(tier.getPermission())) {
            event.setCancelled(true);
            player.sendMessage(MineBoost.getInstance()
                    .getMessage("recipe.no-permission", java.util.Map.of("tier", tier.getLabel())));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipes(RecipeManager.getAllKeys());
    }
}
