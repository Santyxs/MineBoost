package mbt.xenova.mineboost.commands;

import mbt.xenova.mineboost.MineBoost;
import mbt.xenova.mineboost.managers.ToolManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GiveCommand {

    public void execute(CommandSender sender, String[] args) {
        MineBoost plugin = MineBoost.getInstance();

        if (!sender.hasPermission("mineboost.give")) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(plugin.getMessage("command.no-permission")));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(plugin.getMessage("command.usage")));
            return;
        }

        ToolManager.ToolFamily family = parseFamily(args[0]);
        if (family == null) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(plugin.getMessage("command.invalid-tool")));
            return;
        }

        ToolManager.ToolTier tier = parseTier(args[1]);
        if (tier == null) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(plugin.getMessage("command.invalid-material")));
            return;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(plugin.getMessage("command.player-not-found",
                        Map.of("player", args[2]))));
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(plugin.getMessage("command.console-must-specify-player")));
            return;
        }

        if (!target.hasPermission(tier.getPermission())) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(plugin.getMessage("command.target-no-permission", Map.of(
                    "player", target.getName(),
                    "tier", tier.name().toLowerCase(),
                    "permission", tier.getPermission()
            ))));
            return;
        }

        ItemStack item = ToolManager.createTool(family, tier);
        target.getInventory().addItem(item);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String coloredName = LegacyComponentSerializer.legacySection().serialize(Objects.requireNonNull(meta.displayName()));
        String cleanName = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(meta.displayName()));

        target.sendMessage(LegacyComponentSerializer.legacySection().deserialize(plugin.getMessage("command.received", Map.of("tool", coloredName))));

        if (!target.equals(sender)) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(plugin.getMessage("command.given-to",
                    Map.of("tool", cleanName, "player", target.getName()))));
        }
    }

    public List<String> tabComplete(String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(List.of("pickaxe", "shovel", "axe"));
        } else if (args.length == 2) {
            completions.addAll(List.of("wood", "stone", "iron", "gold", "diamond", "netherite"));
        } else if (args.length == 3) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }
        return completions;
    }

    private ToolManager.ToolFamily parseFamily(String arg) {
        return switch (arg.toLowerCase()) {
            case "pickaxe" -> ToolManager.ToolFamily.PICKAXE;
            case "shovel"  -> ToolManager.ToolFamily.SHOVEL;
            case "axe"     -> ToolManager.ToolFamily.AXE;
            default        -> null;
        };
    }

    private ToolManager.ToolTier parseTier(String arg) {
        return switch (arg.toLowerCase()) {
            case "wood" -> ToolManager.ToolTier.WOOD;
            case "stone" -> ToolManager.ToolTier.STONE;
            case "iron" -> ToolManager.ToolTier.IRON;
            case "gold" -> ToolManager.ToolTier.GOLD;
            case "diamond" -> ToolManager.ToolTier.DIAMOND;
            case "netherite" -> ToolManager.ToolTier.NETHERITE;
            default -> null;
        };
    }
}