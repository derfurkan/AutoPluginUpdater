package de.snap20lp.autopluginupdater.commands;

import de.snap20lp.autopluginupdater.Core;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class Update implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof ConsoleCommandSender) {
            if (args.length == 1) {
                switch (args[0]) {
                    case "resources" -> Core.getInstance().searchForPluginSources();
                    case "plugins" -> Core.getInstance().searchForPluginUpdates();
                    case "full" -> {
                        Bukkit.getConsoleSender().sendMessage("§aPerforming full update...");
                        Core.getInstance().reloadConfig();
                        Core.getInstance().generatePluginNameMap();
                        Core.getInstance().searchForPluginSources();
                        Core.getInstance().searchForPluginUpdates();
                    }
                    case "config" -> {
                        Core.getInstance().reloadConfig();
                        Bukkit.getConsoleSender().sendMessage("§aReloaded config!");
                    }
                    case "pluginmap" -> Core.getInstance().generatePluginNameMap();
                    default -> Core.getInstance().searchForPluginUpdate(args[0]);
                }
            } else {
                sender.sendMessage("§cPlease use §6update <full/plugins/config/pluginmap/resources/PLUGIN_NAME>");
                sender.sendMessage("§efull: §6Performs a full update (config, pluginmap, resources, plugins)");
                sender.sendMessage("§eplugins: §6Searches updates for all plugins");
                sender.sendMessage("§eresources: §6Searches new sources for all plugins");
                sender.sendMessage("§econfig: §6Reloads the config");
                sender.sendMessage("§epluginmap: §6Regenerates the pluginMap");
                sender.sendMessage("§ePLUGIN_NAME: §6Searches updates for the plugin with the given name");
            }
        } else {
            sender.sendMessage("§cYou cannot use this command outside of the console!");
        }

        return false;
    }
}
