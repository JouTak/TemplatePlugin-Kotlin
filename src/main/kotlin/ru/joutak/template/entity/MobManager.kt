package ru.joutak.template.entity

import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.UUID

class MobManager {
    private val trackedMobs = mutableMapOf<UUID, MobGroup>()
    private val playerMobs = mutableMapOf<UUID, MutableSet<UUID>>()

    fun trackMob(mobId: UUID, mobGroup: MobGroup, playerId: UUID) {
        trackedMobs[mobId] = mobGroup
        playerMobs.getOrPut(playerId) { mutableSetOf() }.add(mobId)
    }

    fun removeMob(mobId: UUID): MobGroup? {
        val mobGroup = trackedMobs.remove(mobId)
        mobGroup?.let {
            playerMobs[it.playerId]?.remove(mobId)
        }
        return mobGroup
    }

    fun getPlayerMobsCount(playerId: UUID): Int {
        return playerMobs[playerId]?.size ?: 0
    }

    fun clearPlayerMobs(playerId: UUID) {
        playerMobs.remove(playerId)?.forEach { mobId ->
            trackedMobs.remove(mobId)
        }
    }

    fun clearAll() {
        trackedMobs.clear()
        playerMobs.clear()
    }

    fun checkIfAllMobsKilled(playerId: UUID, player: Player) {
        val playerMobsSet = playerMobs[playerId] ?: return

        if (playerMobsSet.isEmpty()) {
            giveReward(player)
            playerMobs.remove(playerId)
        } else {
            player.sendMessage("§6Осталось мобов: §e${playerMobsSet.size}")
        }
    }

    private fun giveReward(player: Player) {
        player.sendMessage("§a🎉 Тут могла бы быть награда, но не сегодня!")
    }
}