package com.joutak.template.Listeners

import org.bukkit.Particle
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
        if(event.isSneaking) return

        if(boots == null) return
        if(!boots.hasItemMeta()) return
        if(!boots.itemMeta.hasCustomModelData()) return
        if(boots.itemMeta.customModelData != 7271234) return

        event.isCancelled = true

        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20, 10))
        player.spawnParticle(Particle.EXPLOSION_NORMAL, player.getLocation(), 3)
    }
}