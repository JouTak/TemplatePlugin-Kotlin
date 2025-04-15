package org.kostyamops.terrsync

import org.bukkit.Location
import java.util.concurrent.ConcurrentHashMap

class TerrSyncManager(private val plugin: Main) {

    private val regions = ConcurrentHashMap<String, Pair<SyncRegion?, SyncRegion?>>()
    private val storage = RegionStorage(plugin)

    init {
        regions.putAll(storage.loadRegions())
    }

    fun createPair(name: String): Boolean {
        return if (regions.containsKey(name)) {
            false
        } else {
            regions[name] = Pair(null, null)
            storage.saveRegions(regions)
            true
        }
    }

    fun setRegion(name: String, index: Int, center: Location, radius: Int): Boolean {
        val current = regions[name] ?: return false
        val region = SyncRegion(center, radius)
        regions[name] = if (index == 1) Pair(region, current.second) else Pair(current.first, region)

        storage.saveRegions(regions)
        return true
    }

    fun deletePair(name: String): Boolean {
        val removed = regions.remove(name)
        if (removed != null) {
            storage.saveRegions(regions)
            return true
        }
        return false
    }

    fun getRegionPairs(): Map<String, Pair<SyncRegion, SyncRegion>> {
        return regions.filterValues { it.first != null && it.second != null }
            .mapValues { Pair(it.value.first!!, it.value.second!!) }
    }

    fun isInSourceRegion(loc: Location): Pair<String, SyncRegion>? {
        for ((name, pair) in getRegionPairs()) {
            if (pair.first.contains(loc)) return name to pair.first
        }
        return null
    }

    fun getTargetRegion(name: String): SyncRegion? {
        return regions[name]?.second
    }

    fun shutdown() {
        storage.saveRegions(regions)
    }
}
