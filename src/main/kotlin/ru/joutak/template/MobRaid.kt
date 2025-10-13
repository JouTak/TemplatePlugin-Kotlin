package ru.joutak.template

import org.bukkit.plugin.java.JavaPlugin
import ru.joutak.template.command.MobRaidCommand
import ru.joutak.template.config.ConfigManager
import ru.joutak.template.entity.MobManager
import ru.joutak.template.event.MobDeathListener
import ru.joutak.template.command.MobRaidTabCompleter

class MobRaid : JavaPlugin() {
    companion object {
        @JvmStatic
        lateinit var instance: MobRaid
    }

    private lateinit var configManager: ConfigManager
    private lateinit var mobManager: MobManager

    override fun onEnable() {
        instance = this

        configManager = ConfigManager(this)
        mobManager = MobManager()

        server.pluginManager.registerEvents(MobDeathListener(mobManager), this)
        getCommand("mobRaid")!!.setExecutor(MobRaidCommand(mobManager))
        getCommand("mobRaid")!!.tabCompleter = MobRaidTabCompleter()
        logger.info("Плагин ${pluginMeta.name} версии ${pluginMeta.version} включен!")
    }

    override fun onDisable() {
        mobManager.clearAll()
    }
}