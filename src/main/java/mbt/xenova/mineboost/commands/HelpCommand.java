package mbt.xenova.mineboost.commands;

import mbt.xenova.mineboost.MineBoost;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public class HelpCommand {

    private record CommandEntry(String usageKey, String descKey, String permission) {}

    private static final CommandEntry[] ENTRIES = {
            new CommandEntry("command.help.give.usage", "command.help.give.description", "mineboost.give"),
            new CommandEntry("command.help.reload.usage", "command.help.reload.description", "mineboost.reload"),
            new CommandEntry("command.help.help.usage", "command.help.help.description", null)
    };

    public void execute(CommandSender sender) {
        MineBoost plugin = MineBoost.getInstance();

        Component header = LegacyComponentSerializer.legacySection().deserialize(plugin.getMessage("command.help.header"));
        sender.sendMessage(header);

        for (CommandEntry entry : ENTRIES) {
            if (entry.permission() != null && !sender.hasPermission(entry.permission())) {
                continue;
            }

            String usageStr = plugin.getMessage(entry.usageKey());
            String descStr = plugin.getMessage(entry.descKey());

            Component usageComp = LegacyComponentSerializer.legacySection().deserialize(usageStr);
            Component descComp = LegacyComponentSerializer.legacySection().deserialize(descStr);

            Component line = Component.text()
                    .append(usageComp)
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(descComp)
                    .build();

            sender.sendMessage(line);
        }
    }
}