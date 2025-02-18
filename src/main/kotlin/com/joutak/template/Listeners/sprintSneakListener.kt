package com.joutak.template.Listeners

import org.apache.commons.lang3.ObjectUtils.Null
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType


class sprintSneakListener: Listener {
    @EventHandler
    fun sprintSneakEvent(event : PlayerToggleSneakEvent) {
        val player = event.player
        val boots = player.inventory.boots

        if(!player.isSprinting) return

        if(boots == null) return
        if(!boots.hasItemMeta()) return
        if(!boots.itemMeta.hasCustomModelData()) return
        if(boots.itemMeta.customModelData != 7271234) return

        event.isCancelled = true

        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20, 10))
    }
}