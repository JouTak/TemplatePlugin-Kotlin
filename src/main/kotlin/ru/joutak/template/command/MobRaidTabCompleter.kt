package ru.joutak.template.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.EntityType

class MobRaidTabCompleter : TabCompleter {

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> getDistanceCompletions(args[0])
            2 -> getMobTypeCompletions(args[1])
            else -> getSubsequentCompletions(args)
        }
    }

    private fun getDistanceCompletions(arg: String): List<String> {
        val suggestions = listOf("clear", "10", "20", "30", "40", "50")
        return suggestions.filter { it.startsWith(arg) }
    }

    private fun getMobTypeCompletions(arg: String): List<String> {
        val mobTypes = EntityType.values()
            .filter { it.isAlive && it != EntityType.PLAYER && it.isSpawnable }
            .map { it.name.lowercase() }

        return mobTypes.filter { it.startsWith(arg.lowercase()) }
    }

    private fun getSubsequentCompletions(args: Array<out String>): List<String> {
        val currentArg = args.last()
        val previousArg = args[args.size - 2]

        return if (previousArg.toIntOrNull() != null) {
            getMobTypeCompletions(currentArg)
        } else {
            getCountCompletions(currentArg)
        }
    }

    private fun getCountCompletions(arg: String): List<String> {
        val suggestions = listOf("1", "5", "10", "20", "50")
        return suggestions.filter { it.startsWith(arg) }
    }
}