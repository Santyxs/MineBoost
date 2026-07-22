package mbt.xenova.mineboost.commands;

import mbt.xenova.mineboost.MineBoost;
import mbt.xenova.mineboost.managers.ToolManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GiveCommand {

    public void execute(CommandSender sender, String[] args) {
        MineBoost plugin = MineBoost.getInstance();

        if (!sender.hasPermission("mineboost.give")) {
            sender.sendMessage(plugin.getMessage("command.no-permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("command.usage"));
            return;
        }

        ToolManager.ToolFamily family = parseFamily(args[0]);
        if (family == null) {
            sender.sendMessage(plugin.getMessage("command.invalid-tool"));
            return;
        }

        ToolManager.ToolTier tier = parseTier(args[1]);
        if (tier == null) {
            sender.sendMessage(plugin.getMessage("command.invalid-material"));
            return;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(plugin.getMessage("command.player-not-found", Map.of("player", args[2])));
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(plugin.getMessage("command.console-must-specify-player"));
            return;
        }

        if (!target.hasPermission(tier.getPermission())) {
            sender.sendMessage(plugin.getMessage("command.target-no-permission", Map.of(
                    "player", target.getName(),
                    "tier", tier.name().toLowerCase(),
                    "permission", tier.getPermission()
            )));
            return;
        }

        ItemStack item = ToolManager.createTool(family, tier);
        target.getInventory().addItem(item);
        String cleanName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        target.sendMessage(plugin.getMessage("command.received", Map.of("tool", item.getItemMeta().getDisplayName())));

        if (!target.equals(sender)) {
            sender.sendMessage(plugin.getMessage("command.given-to", Map.of("tool", cleanName, "player", target.getName())));
        }
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(List.of("pico", "pala", "hacha"));
        } else if (args.length == 2) {
            completions.addAll(List.of("madera", "piedra", "hierro", "oro", "diamante", "netherite"));
        } else if (args.length == 3) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }
        return completions;
    }

    private ToolManager.ToolFamily parseFamily(String arg) {
        return switch (arg.toLowerCase()) {
            case "pico", "pickaxe" -> ToolManager.ToolFamily.PICKAXE;
            case "pala", "shovel" -> ToolManager.ToolFamily.SHOVEL;
            case "hacha", "axe" -> ToolManager.ToolFamily.AXE;
            default -> null;
        };
    }

    private ToolManager.ToolTier parseTier(String arg) {
        return switch (arg.toLowerCase()) {
            case "madera", "wood", "wooden" -> ToolManager.ToolTier.WOOD;
            case "piedra", "stone" -> ToolManager.ToolTier.STONE;
            case "hierro", "iron" -> ToolManager.ToolTier.IRON;
            case "oro", "gold", "golden" -> ToolManager.ToolTier.GOLD;
            case "diamante", "diamond" -> ToolManager.ToolTier.DIAMOND;
            case "netherite" -> ToolManager.ToolTier.NETHERITE;
            default -> null;
        };
    }
}
