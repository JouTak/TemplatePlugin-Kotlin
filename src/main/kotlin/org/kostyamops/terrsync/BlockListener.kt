package org.kostyamops.terrsync

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent

class BlockListener(private val syncManager: TerrSyncManager) : Listener {

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val (name, sourceRegion) = syncManager.isInSourceRegion(event.block.location) ?: return
        val targetRegion = syncManager.getTargetRegion(name) ?: return
        val targetLoc = sourceRegion.translateToTarget(event.block.location, targetRegion)

        // safe place even if chunk isn't loaded
        targetLoc.block.type = event.block.type
        targetLoc.block.blockData = event.block.blockData
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val (name, sourceRegion) = syncManager.isInSourceRegion(event.block.location) ?: return
        val targetRegion = syncManager.getTargetRegion(name) ?: return
        val targetLoc = sourceRegion.translateToTarget(event.block.location, targetRegion)

        targetLoc.block.type = org.bukkit.Material.AIR
    }
}