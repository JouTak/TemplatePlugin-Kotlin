package org.kostyamops.terrsync

import org.bukkit.Location

data class SyncRegion(val center: Location, val radius: Int) {

    fun contains(loc: Location): Boolean {
        return loc.world == center.world &&
                Math.abs(loc.x - center.x) <= radius &&
                Math.abs(loc.y - center.y) <= radius &&
                Math.abs(loc.z - center.z) <= radius
    }

    fun translateToTarget(src: Location, target: SyncRegion): Location {
        val dx = src.blockX - center.blockX
        val dy = src.blockY - center.blockY
        val dz = src.blockZ - center.blockZ

        return target.center.clone().add(dx.toDouble(), dy.toDouble(), dz.toDouble()).block.location
    }
}