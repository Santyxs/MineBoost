package mbt.xenova.commands;

import mbt.xenova.MineBoost;
import mbt.xenova.managers.RecipeManager;
import org.bukkit.command.CommandSender;

public class ReloadCommand {

    public void execute(CommandSender sender) {
        MineBoost plugin = MineBoost.getInstance();

        if (!sender.hasPermission("mineboost.reload")) {
            sender.sendMessage(plugin.getMessage("command.no-permission"));
            return;
        }

        plugin.reloadConfig();
        plugin.reloadLanguage();
        RecipeManager.registerAll();
        sender.sendMessage(plugin.getMessage("command.reload-success"));
    }
}
