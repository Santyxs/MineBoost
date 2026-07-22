package mbt.xenova.mineboost.commands;

import mbt.xenova.mineboost.MineBoost;
import mbt.xenova.mineboost.managers.RecipeManager;
import org.bukkit.command.CommandSender;

public class ReloadCommand {

    public void execute(CommandSender sender, String[] args) {
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
