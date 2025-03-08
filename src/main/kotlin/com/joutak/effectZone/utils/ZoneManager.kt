package com.joutak.effectZone.utils

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.potion.PotionEffectType
import kotlin.random.Random

import com.joutak.effectZone.data.Zone

object ZoneManager {
    private val zones = mutableMapOf<String, Zone>()

    fun add(zone: Zone) {
        if (zones.containsKey(zone.name))
            throw IllegalArgumentException("Зона с таким именем уже существует.")

        zones[zone.name] = zone
    }

    fun get(name: String): Zone {
        if (!zones.containsKey(name))
            throw IllegalArgumentException("Зоны с таким именем не существует.")

        return zones[name]!!
    }

    fun remove(name: String) {
        if (!zones.containsKey(name))
            throw IllegalArgumentException("Зоны с таким именем не существует.")

        zones.remove(name)
    }

    fun getZones(): Map<String, Zone> {
        return zones
    }

    fun clear() {
        zones.clear()
    }

    /**
     * Проверка на нахождение внутри зоны игроком
     */
    fun isInside(zone: Zone, playerLoc: Location): Boolean {
        val world = Bukkit.getWorld(zone.worldName) ?: throw IllegalArgumentException("Мир $zone.worldName не найден.")

        if (playerLoc.world?.name != world.name) return false
        return playerLoc.x in minOf(zone.x1, zone.x2)..maxOf(zone.x1, zone.x2) &&
                playerLoc.y in minOf(zone.y1, zone.y2)..maxOf(zone.y1, zone.y2) &&
                playerLoc.z in minOf(zone.z1, zone.z2)..maxOf(zone.z1, zone.z2)
    }

    /**
     * Создание партиклов по всей зоне на одном уровне с игроком
     */
    fun spawnParticles(zone: Zone, playerLoc: Location) {
        val world = Bukkit.getWorld(zone.worldName) ?: throw IllegalArgumentException("Мир $zone.worldName не найден.")

        for (x in zone.x1.toInt()..zone.x2.toInt()) {
            for (z in zone.z1.toInt()..zone.z2.toInt()) {
                val loc = Location(world, x.toDouble(), playerLoc.y, z.toDouble())
                world.spawnParticle(Particle.CLOUD, loc, 1, Random.nextDouble(0.0, 0.5), 0.1, Random.nextDouble(0.0, 0.5), 0.0)
            }
        }
    }

    fun serialize(zone: Zone): Map<String, Any> {
        return mapOf(
            "name" to zone.name,
            "effect" to zone.effect.name,
            "worldName" to zone.worldName,
            "x1" to zone.x1,
            "y1" to zone.y1,
            "z1" to zone.z1,
            "x2" to zone.x2,
            "y2" to zone.y2,
            "z2" to zone.z2
        )
    }

    fun deserialize(values: Map<String, Any>) : Zone {
        return Zone(
            values["name"] as String,
            PotionEffectType.getByName(values["effect"] as String) ?: throw IllegalArgumentException("Неизвестный эффект ${values["effect"]}"),
            values["worldName"] as String,
            values["x1"] as Double,
            values["y1"] as Double,
            values["z1"] as Double,
            values["x2"] as Double,
            values["y2"] as Double,
            values["z2"] as Double
        )
    }
}