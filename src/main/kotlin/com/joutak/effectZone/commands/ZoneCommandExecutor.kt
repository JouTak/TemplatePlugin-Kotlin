package com.joutak.effectZone.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType

import com.joutak.effectZone.data.Zone
import com.joutak.effectZone.utils.ZoneManager

object ZoneCommandExecutor: CommandExecutor, TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (!sender.hasPermission("zone.admin")) {
            sender.sendMessage("Недостаточно прав для использования команды.")
            return true
        }

        when (args?.getOrNull(0)) {
            "add" -> {
                if (sender !is Player) {
                    sender.sendMessage("Данную команду можно использовать только в игре.")
                    return true        
                }

                if (args.size != 9) {
                    return false
                }

                try {
                    val newZone = Zone(
                        args[1],
                        PotionEffectType.getByName(args[2]) ?: throw IllegalArgumentException("Неизвестный эффект ${args[2]}"),
                        sender.world.name,
                        // sender.world.environment, 
                        args[3].toDouble(), 
                        args[4].toDouble(), 
                        args[5].toDouble(), 
                        args[6].toDouble(), 
                        args[7].toDouble(), 
                        args[8].toDouble()
                    )
                    ZoneManager.add(newZone)
                    sender.sendMessage("Добавлена зона с именем ${newZone.name}.")
                } catch (e: NumberFormatException) {
                    sender.sendMessage("Координаты должны быть числами.")
                } catch (e: IllegalArgumentException) {
                    sender.sendMessage("${e.message}")
                }
            }
            "remove" -> {
                if (args.size != 2) {
                    return false
                }

                try {
                    ZoneManager.remove(args[1])
                    sender.sendMessage("Зона ${args[1]} удалена.")
                }
                catch (e: IllegalArgumentException) {
                    sender.sendMessage("${e.message}")
                }
            }
            "list" -> {
                if (args.size != 1) {
                    return false
                }

                if (ZoneManager.getZones().isEmpty()) {
                    sender.sendMessage("Нет активных зон.")
                } else {
                    sender.sendMessage("Список зон:")
                    ZoneManager.getZones().values.forEach {
                        sender.sendMessage(it.name)
                    }
                }
            }
            "info" -> {
                if (args.size != 2) {
                    return false
                }

                try {
                    val zone = ZoneManager.get(args[1])

                    sender.sendMessage("Информация о зоне ${zone.name}:")
                    sender.sendMessage("Эффект: ${zone.effect.name}")
                    sender.sendMessage("Мир: ${zone.worldName}")
                    // sender.sendMessage("Измерение: ${Bukkit.getWorld(zone.worldName)?.environment.toString()}")
                    sender.sendMessage("Координаты: (${zone.x1}, ${zone.y1}, ${zone.z1} ; ${zone.x2}, ${zone.y2}, ${zone.z2})")
                }
                catch (e: IllegalArgumentException) {
                    sender.sendMessage("${e.message}")
                }
            }
            else -> {
                return false
            }
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> listOf("add", "remove", "list", "info").filter { it.startsWith(args[0], true) }
            2 -> if (args[0] == "remove" || args[0] == "info") ZoneManager.getZones().values.map { it.name }.filter { it.startsWith(args[1], true) } else emptyList()
            3 -> if (args[0] == "add") PotionEffectType.values().map { it.name }.filter { it.startsWith(args[2], true) } else emptyList()
            else -> emptyList()
        }
    }
}