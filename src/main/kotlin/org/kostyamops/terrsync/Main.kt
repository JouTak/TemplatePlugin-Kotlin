package org.kostyamops.terrsync

import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {

    lateinit var syncManager: TerrSyncManager

    override fun onEnable() {
        syncManager = TerrSyncManager(this)
        getCommand("sync")?.setExecutor(SyncCommand(syncManager))
        server.pluginManager.registerEvents(BlockListener(syncManager), this)
        logger.info("TerrSync enabled!")
    }

    override fun onDisable() {
        syncManager.shutdown()
        logger.info("TerrSync disabled!")
    }
}
