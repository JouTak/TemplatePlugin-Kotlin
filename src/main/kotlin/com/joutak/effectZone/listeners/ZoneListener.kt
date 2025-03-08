package com.joutak.effectZone.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect

import com.joutak.effectZone.utils.ZoneManager

object ZoneListener : Listener {
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val to = event.to

        // Если игрок находится в одной из зон, то даем ему соответствующий эффект и создаем партиклы внутри зоны
        for (zone in ZoneManager.getZones().values) {
            if (ZoneManager.isInside(zone, to)) {
                ZoneManager.spawnParticles(zone, to)
                player.addPotionEffect(
                    PotionEffect(zone.effect, 100, 1, false, true, true)
                )
                return
            }
        }
    }
}