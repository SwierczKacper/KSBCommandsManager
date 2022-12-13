Spigot plugin for whitelisting server commands. Blocks all commands, checks if command defined in config then deciding if it should be allowed or not. If command is not defined in config, it will be blocked.

Permissions:

- ksbcommandsmanager.bypass - Allows player to bypass command blocking
- ksbcommandsmanager.bypass.cmd_alerts - Allows player to bypass command alerts
- ksbcommandsmanager.worldedit - allow using worldedit commands
- ksbcommandsmanager.group.<group> - allow using commands from group <group> and all subgroups
- ksbcommandsmanager.receive_cmd_alert - allow receiving command alerts

For properly working of permissions system, every group should have permission corresponding to its name. For example, if you have group named "admin", it should have permission "ksbcommandsmanager.group.admin".