package org.kostyamops.terrsync

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SyncCommand(private val manager: TerrSyncManager) : CommandExecutor {

    private var counter = 1
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("Usage: /sync <create|set|del>")
            return false
        }

        when (args[0].lowercase()) {
            "create" -> {
                val name = args.getOrNull(1) ?: "pair${counter++}"
                if (manager.createPair(name)) {
                    sender.sendMessage("Created pair: $name")
                } else {
                    sender.sendMessage("Pair $name already exists.")
                }
            }

            "set" -> {
                if (sender !is Player) {
                    sender.sendMessage("Only players can use this command.")
                    return true
                }

                if (args.size < 3) {
                    sender.sendMessage("Usage: /sync set <name>_<1|2> <radius>")
                    return true
                }

                val rawName = args[1]
                val radius = args[2].toIntOrNull()

                if (!rawName.endsWith("_1") && !rawName.endsWith("_2") || radius == null) {
                    sender.sendMessage("Invalid format. Use /sync set <name>_<1|2> <radius>")
                    return true
                }

                val name = rawName.substringBeforeLast("_")
                val index = rawName.takeLast(1).toInt()

                if (manager.setRegion(name, index, sender.location, radius)) {
                    sender.sendMessage("Set region ${rawName} with radius $radius.")
                } else {
                    sender.sendMessage("Region $rawName not found.")
                }
            }

            "del" -> {
                val name = args.getOrNull(1)
                if (name != null && manager.deletePair(name)) {
                    sender.sendMessage("Deleted pair $name.")
                } else {
                    sender.sendMessage("Pair not found.")
                }
            }

            else -> {
                sender.sendMessage("Unknown subcommand.")
            }
        }

        return true
    }
}