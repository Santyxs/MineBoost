package mbt.xenova.commands;

import mbt.xenova.MineBoost;
import mbt.xenova.managers.RecipeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
        plugin.refreshCaches();
        RecipeManager.registerAll();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.discoverRecipes(RecipeManager.getAllKeys());
        }

        sender.sendMessage(plugin.getMessage("command.reload-success"));
    }
}