package mbt.xenova.mineboost.managers;

import mbt.xenova.mineboost.MineBoost;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeManager implements Listener {

    // ---------------------------------------------------------------
    // REGISTRATION
    // ---------------------------------------------------------------

    public static void registerAll() {
        unregisterAll();
        for (ToolManager.ToolFamily family : ToolManager.ToolFamily.values()) {
            for (ToolManager.ToolTier tier : ToolManager.ToolTier.values()) {
                registerRecipe(family, tier);
            }
        }
    }

    public static void unregisterAll() {
        for (NamespacedKey key : getAllKeys()) {
            Bukkit.removeRecipe(key);
        }
    }

    public static List<NamespacedKey> getAllKeys() {
        List<NamespacedKey> keys = new ArrayList<>();
        for (ToolManager.ToolFamily family : ToolManager.ToolFamily.values()) {
            for (ToolManager.ToolTier tier : ToolManager.ToolTier.values()) {
                keys.add(buildKey(family, tier));
            }
        }
        return keys;
    }

    private static void registerRecipe(ToolManager.ToolFamily family, ToolManager.ToolTier tier) {
        ItemStack result = ToolManager.createTool(family, tier);
        NamespacedKey key = buildKey(family, tier);

        ShapedRecipe recipe = new ShapedRecipe(key, result);

        switch (family) {
            case PICKAXE -> recipe.shape("MMM", " E ", " S ");
            case SHOVEL -> recipe.shape(" M ", " E ", " S ");
            case AXE -> recipe.shape("MM ", "ME ", " S ");
        }

        recipe.setIngredient('M', getMaterialChoice(tier));
        recipe.setIngredient('E', Material.EMERALD);
        recipe.setIngredient('S', Material.STICK);

        Bukkit.addRecipe(recipe);
    }

    private static RecipeChoice getMaterialChoice(ToolManager.ToolTier tier) {
        return switch (tier) {
            case WOOD -> new RecipeChoice.MaterialChoice(new ArrayList<>(Tag.PLANKS.getValues()));
            case STONE -> new RecipeChoice.MaterialChoice(Material.COBBLESTONE);
            case IRON -> new RecipeChoice.MaterialChoice(Material.IRON_INGOT);
            case GOLD -> new RecipeChoice.MaterialChoice(Material.GOLD_INGOT);
            case DIAMOND -> new RecipeChoice.MaterialChoice(Material.DIAMOND);
            case NETHERITE -> new RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT);
        };
    }

    private static NamespacedKey buildKey(ToolManager.ToolFamily family, ToolManager.ToolTier tier) {
        return new NamespacedKey(MineBoost.getInstance(),
                "tool_" + family.name().toLowerCase() + "_" + tier.name().toLowerCase());
    }

    // ---------------------------------------------------------------
    // LISTENER
    // ---------------------------------------------------------------

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (ToolManager.isMultiTool(result)) return;

        HumanEntity crafter = event.getWhoClicked();
        if (!(crafter instanceof Player player)) return;

        ToolManager.ToolTier tier = ToolManager.getTier(result);
        if (tier != null && !player.hasPermission(tier.getPermission())) {
            event.setCancelled(true);
            player.sendMessage(MineBoost.getInstance()
                    .getMessage("recipe.no-permission", java.util.Map.of("tier", tier.getLabel())));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipes(getAllKeys());
    }
}