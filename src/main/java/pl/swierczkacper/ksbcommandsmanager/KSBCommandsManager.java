package pl.swierczkacper.ksbcommandsmanager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import java.util.List;
import java.util.Objects;

public final class KSBCommandsManager extends JavaPlugin implements Listener {

    private static Permission perms = null;

    @Override
    public void onEnable() {
        getLogger().info("KSBCommandsManager starting...");

        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupPermissions();

        getServer().getPluginManager().registerEvents(this, this); // Registering events

        getConfig().options().copyDefaults(); // Copy default config
        saveDefaultConfig(); // Save default config
    }

    @Override
    public void onDisable() {
        getLogger().info("KSBCommandsManager stopping...");
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e)
    {
        Player player = e.getPlayer();

        String[] cmd = e.getMessage().split(" ");

        logger("Player " + player.getName() + " executed command: " + cmd[0]);

        if(cmd[0].startsWith("//")) { // Check if world edit command
            if(perms.has(player, "ksbcommandsmanager.worldedit")) { // Check if player has permission to use world edit
                return; // If yes, allow command
            }
        } else {
            String clearCommand = e.getMessage().substring(1); // Remove slash from command
            String clearCommandKey = clearCommand.replaceAll(" ","-"); // Replace spaces with dashes
            String commandFinalKey = getCommandFinalKey(clearCommandKey); // Get final key

            logger("Clear command: " + clearCommand);
            logger("Clear command key: " + clearCommandKey);
            logger("Command final key: " + commandFinalKey);

            logger("Exists in config: " + getConfig().contains("commands." + commandFinalKey));

            if(getConfig().contains("commands." + commandFinalKey)) { // Check if command exists in config
                if(checkIfPlayerHasGroupPermission(player, getConfig().getStringList("commands." + commandFinalKey + ".groups"))) { // Check if player has proper permission
                    logger("Type: " + getConfig().getString("commands." + commandFinalKey + ".type"));

                    if(Objects.equals(getConfig().getString("commands." + commandFinalKey + ".type"), "text")) { // Check if command type is text
                        List<String> lines = getConfig().getStringList("commands." + commandFinalKey + ".output");

                        for(String line : lines) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
                        }

                        e.setCancelled(true);
                        return;
                    } else if(Objects.equals(getConfig().getString("commands." + commandFinalKey + ".type"), "redirect")) { // Check if command type is redirect
                        String outputCommand = getConfig().getString("commands." + commandFinalKey + ".output");

                        String finalOutputCommand = outputCommand + clearCommand.replaceFirst(commandFinalKey.replaceAll("-"," "), "");

                        logger("Output command: " + finalOutputCommand);

                        player.performCommand(finalOutputCommand);
                        e.setCancelled(true);
                        return;
                    }
                } else {
                    informAdministrators(player, cmd[0]);
                }
            } else {
                informAdministrators(player, cmd[0] + " &c&l[?]");
            }
        }

        String unknownCommandMessage = getConfig().getString("messages.unknown_command");

        if(!perms.has(player, "ksbcommandsmanager.bypass")) {
            e.setCancelled(true);
            player.sendMessage(unknownCommandMessage != null ? ChatColor.translateAlternateColorCodes('&', unknownCommandMessage) : "Unknown command. Type \"/help\" for help.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(command.getName().equalsIgnoreCase("ksbcommandsmanager")) {
            if(sender instanceof Player) {
                Player player = (Player) sender;

                if(args.length > 0) {
                    if(args[0].equalsIgnoreCase("reload")) {
                        if(perms.has(player, "ksbcommandsmanager.reload")) {
                            reloadConfig();
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.reload")));
                        } else {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.no_permission")));
                        }
                    }
                }
            }
        }

        return true;
    }

    public String getCommandFinalKey(String command) {
        if(!command.contains("-")) {
            return command;
        }

        while (command.contains("-")) {
            if(getConfig().contains("commands." + command)) {
                return command;
            }

            command = command.substring(0, command.lastIndexOf("-"));

            if(!command.contains("-")) {
                return command;
            }
        }

        return "";
    }

    public void logger(String message) {
        if(getConfig().getBoolean("debug")) {
            getLogger().info(message);
        }

        if(getConfig().getBoolean("broadcast-debug")) {
            Bukkit.broadcastMessage(message);
        }
    }

    public void informAdministrators(Player sender, String message) {
        if(!perms.has(sender, "ksbcommandsmanager.bypass") && !perms.has(sender, "ksbcommandsmanager.bypass.cmd_alerts")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (perms.has(player, "ksbcommandsmanager.receive_cmd_alert")) {
                    String prefix = getPlayerPrefix(sender);

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l[CMD] Gracz " + prefix + sender.getName() + " &c&lpróbował &d&l" + message));
                }
            }
        }
    }

    public String getPlayerPrefix(Player player) {
        String group = perms.getPrimaryGroup(player); // Get player group
        String prefix = getConfig().getString("group_prefixes." + group); // Get prefix from config

        return prefix == null ? getConfig().getString("group_prefixes.default") : prefix; // Return default prefix if player group prefix is null
    }

    public boolean checkIfPlayerHasGroupPermission(Player player, List<String> groups)
    {
        for (String group : groups) {
            if(perms.has(player, "ksbcommandsmanager.group." + group)) {
                return true;
            }
        }

        return false;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        return true;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }
}
