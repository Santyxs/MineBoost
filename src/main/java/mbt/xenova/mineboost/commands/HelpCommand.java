package mbt.xenova.mineboost.commands;

import mbt.xenova.mineboost.MineBoost;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class HelpCommand {

    private record Entry(String usage, String description, String permission) {}

    private static final Entry[] ENTRIES = {
            new Entry("/mineboost give <pickaxe|shovel|axe> <wood|stone|iron|gold|diamond|netherite> [player]",
                    "Gives an x2 tool.", "mineboost.give"),
            new Entry("/mineboost reload",
                    "Reloads config.yml and the language files.", "mineboost.reload"),
            new Entry("/mineboost help",
                    "Shows this command list.", null)
    };

    public void execute(CommandSender sender, String[] args) {
        MineBoost plugin = MineBoost.getInstance();

        sender.sendMessage(plugin.getMessage("command.help.header"));
        for (Entry entry : ENTRIES) {
            if (entry.permission() != null && !sender.hasPermission(entry.permission())) continue;

            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&e" + entry.usage() + " &7- " + entry.description()));
        }
    }
}