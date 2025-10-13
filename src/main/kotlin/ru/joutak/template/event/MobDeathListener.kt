package ru.joutak.template.event

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import ru.joutak.template.entity.MobManager

class MobDeathListener(private val mobManager: MobManager) : Listener {

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        val mobGroup = mobManager.removeMob(entity.uniqueId) ?: return

        val player = Bukkit.getPlayer(mobGroup.playerId) ?: return
        mobManager.checkIfAllMobsKilled(mobGroup.playerId, player)
    }
}