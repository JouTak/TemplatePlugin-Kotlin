package ru.joutak.template.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.EntityType


class MobRaidTabCompleter : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String?> {
        val mobsTabCompleter: List<String> = EntityType.entries
            .filter { it.isAlive && it != EntityType.PLAYER && it.name.startsWith(args.last())}
            .map { it.name }
            .sorted()
        if (args.size%2==0)
                return mobsTabCompleter
        else
            return List(1){"1"}
    }
}