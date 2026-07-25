package mbt.xenova.commands;

import mbt.xenova.MineBoost;
import mbt.xenova.managers.ToolManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InfoCommand {

    public void execute(CommandSender sender, String[] args) {
        MineBoost plugin = MineBoost.getInstance();

        if (args.length < 1) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(plugin.getMessage("command.info.usage")));
            return;
        }

        ToolManager.ToolTier tier = parseTier(args[0]);
        if (tier == null) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(plugin.getMessage("command.invalid-material")));
            return;
        }

        int areaSize = plugin.getAreaSize(tier);
        int totalBlocks = areaSize * areaSize;
        int cooldown = plugin.getCooldownSeconds(tier);
        boolean hasPermission = sender.hasPermission(tier.getPermission());

        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                plugin.getMessage("command.info.header", Map.of("tier", tier.getLabel()))));

        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                plugin.getMessage("command.info.area", Map.of(
                        "size", String.valueOf(areaSize),
                        "blocks", String.valueOf(totalBlocks)))));

        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                plugin.getMessage("command.info.cooldown", Map.of("seconds", String.valueOf(cooldown)))));

        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                plugin.getMessage("command.info.permission", Map.of("permission", tier.getPermission()))));

        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                plugin.getMessage(hasPermission ? "command.info.has-permission" : "command.info.no-permission")));
    }

    public List<String> tabComplete(String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(List.of("wood", "stone", "copper", "iron", "gold", "diamond", "netherite"));
        }
        return completions;
    }

    private ToolManager.ToolTier parseTier(String arg) {
        return switch (arg.toLowerCase()) {
            case "wood" -> ToolManager.ToolTier.WOOD;
            case "stone" -> ToolManager.ToolTier.STONE;
            case "copper" -> ToolManager.ToolTier.COPPER;
            case "iron" -> ToolManager.ToolTier.IRON;
            case "gold" -> ToolManager.ToolTier.GOLD;
            case "diamond" -> ToolManager.ToolTier.DIAMOND;
            case "netherite" -> ToolManager.ToolTier.NETHERITE;
            default -> null;
        };
    }
}