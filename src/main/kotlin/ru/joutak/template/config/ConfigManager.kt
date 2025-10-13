package ru.joutak.template.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ConfigManager(private val plugin: JavaPlugin) {
    private var customConfig = YamlConfiguration()

    fun loadConfig() {
        val configFile = File(plugin.dataFolder, "config.yml")
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", true)
        }
    }

    fun reloadConfig() {
        loadConfig()
    }
}