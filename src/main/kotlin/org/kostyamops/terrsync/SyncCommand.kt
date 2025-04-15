package org.kostyamops.terrsync

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SyncCommand(private val manager: TerrSyncManager) : CommandExecutor {

    private var counter = 1
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("Используйте: /sync <create|set|del>")
            return false
        }

        when (args[0].lowercase()) {
            "create" -> {
                val name = args.getOrNull(1) ?: "pair${counter++}"
                if (manager.createPair(name)) {
                    sender.sendMessage("Создана пара областей: $name")
                } else {
                    sender.sendMessage("Пара с именем $name уже существует.")
                }
            }

            "set" -> {
                if (sender !is Player) {
                    sender.sendMessage("Только игроки могут использовать эту команду.")
                    return true
                }

                if (args.size < 3) {
                    sender.sendMessage("Используйте: /sync set <name>_<1|2> <radius>")
                    return true
                }

                val rawName = args[1]
                val radius = args[2].toIntOrNull()

                if (!rawName.endsWith("_1") && !rawName.endsWith("_2") || radius == null) {
                    sender.sendMessage("Неправильный формат. Используйте /sync set <name>_<1|2> <radius>")
                    return true
                }

                val name = rawName.substringBeforeLast("_")
                val index = rawName.takeLast(1).toInt()

                if (manager.setRegion(name, index, sender.location, radius)) {
                    sender.sendMessage("Создана область ${rawName} с радиусом $radius.")
                } else {
                    sender.sendMessage("Область с именем $rawName не найдена.")
                }
            }

            "del" -> {
                val name = args.getOrNull(1)
                if (name != null && manager.deletePair(name)) {
                    sender.sendMessage("Удалена пара областей $name.")
                } else {
                    sender.sendMessage("Пара областей не найдена.")
                }
            }

            else -> {
                sender.sendMessage("Неизвестная команда.")
            }
        }

        return true
    }
}