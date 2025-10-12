package ru.joutak.template

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import ru.joutak.template.command.MobRaidTabCompleter
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.UUID


class EmptyPlugin : JavaPlugin(), Listener {
    companion object {
        @JvmStatic
        lateinit var instance: EmptyPlugin
    }
    data class MobGroup(
        val playerId: UUID,
        val entityType: EntityType,
        val spawnTime: Long,
    )
    private val trackedMobs = mutableMapOf<UUID, MobGroup>()
    private val playerMobs = mutableMapOf<UUID, MutableSet<UUID>>()
    private var customConfig = YamlConfiguration()

    private fun loadConfig()
    {
        val fx = File(dataFolder, "config.yml")
        if (!fx.exists()) {
            saveResource("config.yml", true)
        }
    }

    /**
     * Plugin startup logic
     */
    override fun onEnable()
    {
        instance = this

        loadConfig()

        Bukkit.getPluginManager().registerEvents(this, this)
        getCommand("mobRaid")!!.setTabCompleter(MobRaidTabCompleter())
        logger.info("Плагин ${pluginMeta.name} версии ${pluginMeta.version} включен!")
    }


    /**
     * Plugin shutdown logic
     */
    override fun onDisable()
    {
        trackedMobs.clear()
        playerMobs.clear()
    }
    private fun checkIfAllMobsKilled(playerId: UUID, player: Player)
    {
        val playerMobsSet = playerMobs[playerId] ?: return

        if (playerMobsSet.isEmpty())
        {
            giveReward(player)
            playerMobs.remove(playerId)
        }
        else
        {
            player.sendMessage("§6Осталось мобов: §e${playerMobsSet.size}")
        }
    }
    private fun giveReward(player: Player)
    {
        player.sendMessage("§a🎉 Тут могла бы быть награда, но не сегодня!")
    }
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean
    {
        if (sender !is Player) return true
        if (command.getName().equals("mobRaid", ignoreCase = true))
        {
            processMobSpawnCommand(sender,args.toList().toTypedArray())
        }

        return true
    }
    private fun processMobSpawnCommand(player: Player, args: Array<String>)
    {
        val mobsToSpawn = mutableListOf<Pair<EntityType, Int>>()
        var currentMob: EntityType? = null

        for (arg in args.drop(1)) {
            val count = arg.toIntOrNull()

            if (count != null)
            {
                currentMob?.let{ mob ->
                    mobsToSpawn.add(mob to count)
                    currentMob = null
                }
            } else {
                currentMob?.let {
                    mobsToSpawn.add(it to 1)
                }

                currentMob = try {
                    EntityType.valueOf(arg.uppercase().replace(" ", "_"))
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
        currentMob?.let {
            mobsToSpawn.add(it to 1)
        }
        spawnMobs(player, mobsToSpawn, args[0].toInt())
    }
    private fun spawnMobs(player: Player, mobs: List<Pair<EntityType, Int>>, dist: Int):Set<UUID>? {
        val spawnedEntities = mutableSetOf<UUID>()
        if (mobs.isEmpty())
        {
            player.sendMessage("§cНет мобов для спавна!")
            return null
        }

        var totalSpawned = 0

        for ((entityType, count) in mobs)
        {
            if (!entityType.isAlive || entityType == EntityType.PLAYER)
            {
                player.sendMessage("§c'§e${entityType.name}§c' не является мобом!")
                continue
            }

            for (i in 1..count)
            {
                val location = player.location.clone().apply {
                    add((-dist..dist).random().toDouble(), 0.0, (-dist..dist).random().toDouble())
                }
                val entity = player.world.spawnEntity(location, entityType)
                spawnedEntities.add(entity.uniqueId)
                trackedMobs[entity.uniqueId] = MobGroup(player.uniqueId, entityType, System.currentTimeMillis())
                totalSpawned++
            }
        }
        playerMobs.getOrPut(player.uniqueId) { mutableSetOf() }.addAll(spawnedEntities)
        player.sendMessage("§6Всего заспавнено мобов: §e$totalSpawned")
        return spawnedEntities
    }
    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        val mobGroup = trackedMobs[entity.uniqueId] ?: return

        val player = Bukkit.getPlayer(mobGroup.playerId) ?: return
        val playerId = mobGroup.playerId
        trackedMobs.remove(entity.uniqueId)
        playerMobs[playerId]?.remove(entity.uniqueId)
        checkIfAllMobsKilled(playerId, player)
    }
}
