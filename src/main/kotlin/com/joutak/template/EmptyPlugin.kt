package com.joutak.template

import com.joutak.template.Listeners.sprintSneakListener
import com.joutak.template.commands.speedBootsCommand
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class EmptyPlugin : JavaPlugin() {
    companion object {
        @JvmStatic
        lateinit var instance: EmptyPlugin
    }

    private var customConfig = YamlConfiguration()
    private fun loadConfig() {
        val fx = File(dataFolder, "config.yml")
        if (!fx.exists()) {
            saveResource("config.yml", true)
        }
    }

    override fun onEnable() {
        // Plugin startup logic
        instance = this

        loadConfig()

        // Register commands and events

        logger.info("Template plugin version ${pluginMeta.version} enabled!")

        registerListeners()
        registerCommands()

    }

    private fun registerCommands(){
        getCommand("speed-boots")?.setExecutor(speedBootsCommand())

        logger.info("Registered commands")

    }

    private fun registerListeners(){
        server.pluginManager.registerEvents(sprintSneakListener(), this)

        logger.info("Registered listeners")
    }

    override fun onDisable() {
        // Plugin shutdown logic
        logger.info("Template plugin version ${pluginMeta.version} disabled!")
    }
}
