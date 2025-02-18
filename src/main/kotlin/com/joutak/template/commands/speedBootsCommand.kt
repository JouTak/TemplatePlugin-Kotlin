package com.joutak.template.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class speedBootsCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if(sender !is Player) return false
        if(!sender.isOp) return false

        val speedBootsItem: ItemStack = ItemStack(Material.IRON_BOOTS)
        val itemMeta = speedBootsItem.itemMeta

        itemMeta.displayName(Component.text("Speed Boots").color(TextColor.color(200, 200, 200)))

        itemMeta.setCustomModelData(7271234)
        speedBootsItem.setItemMeta(itemMeta)
        speedBootsItem.itemMeta = itemMeta

        sender.inventory.addItem(speedBootsItem)

        return false
    }

}