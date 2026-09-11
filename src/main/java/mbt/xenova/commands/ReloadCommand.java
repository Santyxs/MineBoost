package mbt.xenova.commands;

import mbt.xenova.MineBoost;
import mbt.xenova.managers.RecipeManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

public class ReloadCommand {

    public void execute(CommandSender sender) {
        MineBoost plugin = MineBoost.getInstance();

        if (plugin.lacksPermission(sender, "mineboost.reload")) return;

        plugin.reloadConfig();
        plugin.reloadLanguage();
        plugin.refreshCaches();
        RecipeManager.registerAll();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.discoverRecipes(RecipeManager.getKeysForPlayer(player));
        }

        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(plugin.getMessage("command.reload-success")));
    }
}