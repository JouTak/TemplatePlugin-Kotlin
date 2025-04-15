package org.kostyamops.terrsync

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class RegionStorage(private val plugin: Main) {

    private val saveFile = File(plugin.dataFolder, "regions.yml")

    fun saveRegions(regions: Map<String, Pair<SyncRegion?, SyncRegion?>>) {
        val config = YamlConfiguration()
        for ((name, pair) in regions) {
            pair.first?.let {
                config.set("$name.1", serializeRegion(it))
            }
            pair.second?.let {
                config.set("$name.2", serializeRegion(it))
            }
        }
        config.save(saveFile)
    }

    fun loadRegions(): Map<String, Pair<SyncRegion?, SyncRegion?>> {
        if (!saveFile.exists()) return emptyMap()

        val config = YamlConfiguration.loadConfiguration(saveFile)
        val result = mutableMapOf<String, Pair<SyncRegion?, SyncRegion?>>()

        for (key in config.getKeys(false)) {
            val r1 = config.getConfigurationSection("$key.1")?.let { deserializeRegion(it) }
            val r2 = config.getConfigurationSection("$key.2")?.let { deserializeRegion(it) }
            result[key] = Pair(r1, r2)
        }

        return result
    }

    private fun serializeRegion(region: SyncRegion): Map<String, Any> {
        return mapOf(
            "world" to region.center.world!!.name,
            "x" to region.center.blockX,
            "y" to region.center.blockY,
            "z" to region.center.blockZ,
            "radius" to region.radius
        )
    }

    private fun deserializeRegion(section: org.bukkit.configuration.ConfigurationSection): SyncRegion? {
        val world = Bukkit.getWorld(section.getString("world") ?: return null) ?: return null
        val x = section.getInt("x")
        val y = section.getInt("y")
        val z = section.getInt("z")
        val radius = section.getInt("radius")

        return SyncRegion(Location(world, x.toDouble(), y.toDouble(), z.toDouble()), radius)
    }
}
