package mbt.xenova.mineboost;

import mbt.xenova.mineboost.commands.GiveCommand;
import mbt.xenova.mineboost.commands.HelpCommand;
import mbt.xenova.mineboost.commands.ReloadCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MineBoostCommandHandler implements CommandExecutor, TabCompleter {

    private final GiveCommand giveCommand = new GiveCommand();
    private final ReloadCommand reloadCommand = new ReloadCommand();
    private final HelpCommand helpCommand = new HelpCommand();

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            helpCommand.execute(sender, args);
            return true;
        }

        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        switch (args[0].toLowerCase()) {
            case "give" -> giveCommand.execute(sender, rest);
            case "reload" -> reloadCommand.execute(sender, rest);
            case "help" -> helpCommand.execute(sender, rest);
            default -> helpCommand.execute(sender, args);
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (String name : List.of("give", "reload", "help")) {
                if (name.startsWith(args[0].toLowerCase())) completions.add(name);
            }
            return completions;
        }

        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        return switch (args[0].toLowerCase()) {
            case "give" -> giveCommand.tabComplete(sender, rest);
            default -> completions;
        };
    }
}
