package ru.joutak.template.command

import org.bukkit.*
import org.bukkit.Difficulty
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import ru.joutak.template.entity.MobManager

class MobRaidCommand(private val mobManager: MobManager) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true

        if (command.name.equals("mobRaid", ignoreCase = true)) {
            processMobSpawnCommand(sender, args.toList().toTypedArray())
        }
        return true
    }

    private fun processMobSpawnCommand(player: Player, args: Array<String>) {
        if (args.isEmpty()) {
            player.sendMessage("§cИспользование: /mobRaid <дистанция> [моб количество]...")
            return
        }
        if (args[0] == "clear") {
            mobManager.clearPlayerMobs(player.uniqueId)
            player.sendMessage("§6Мобы рейда сброшены!")
            return
        }
        val distance = args[0].toIntOrNull()
        if (distance == null) {
            player.sendMessage("§cДистанция должна быть числом!")
            return
        }
        if (args.drop(1).toList().isEmpty()) {
            player.sendMessage("§cУкажите мобов для спвна!")
            return
        }
        val mobsToSpawn = parseMobArgs(args.drop(1).toList())
        spawnMobs(player, mobsToSpawn, distance)
    }

    private fun parseMobArgs(args: List<String>): List<Pair<EntityType, Int>> {
        val mobsToSpawn = mutableListOf<Pair<EntityType, Int>>()
        var currentMob: EntityType? = null

        for (arg in args) {
            val count = arg.toIntOrNull()

            if (count != null) {
                if (count >= 1) {
                    currentMob?.let { mob ->
                        mobsToSpawn.add(mob to count)
                        currentMob = null
                    }
                } else {
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
        return mobsToSpawn
    }

    private fun spawnMobs(player: Player, mobs: List<Pair<EntityType, Int>>, dist: Int) {
        if (mobs.isEmpty()) {
            player.sendMessage("§cНет мобов для спавна!")
            return
        }
        if (player.world.difficulty == Difficulty.PEACEFUL) {
            player.sendMessage("§cНельзя спавнить мобов в мирном режиме!")
            return
        }
        var totalSpawned = 0
        var adjustedSpawns = 0

        for ((entityType, count) in mobs) {
            if (!entityType.isAlive || entityType == EntityType.PLAYER) {
                player.sendMessage("§c'§e${entityType.name}§c' не является мобом!")
                continue
            }

            for (i in 1..count) {
                val spawnLocation = findAdjustedSpawnLocation(player, dist)

                val entity = player.world.spawnEntity(spawnLocation, entityType)

                mobManager.trackMob(
                    entity.uniqueId,
                    ru.joutak.template.entity.MobGroup(player.uniqueId, entityType, System.currentTimeMillis()),
                    player.uniqueId
                )
                totalSpawned++

                if (spawnLocation.distance(player.location) < dist * 0.7) {
                    adjustedSpawns++
                }
            }
        }

        player.sendMessage("§6Всего заспавнено мобов: §e$totalSpawned")
        if (adjustedSpawns > 0) {
            player.sendMessage("§e$adjustedSpawns§6 мобов были спавнены ближе из-за препятствий")
        }
    }

    private fun findAdjustedSpawnLocation(player: Player, maxDistance: Int): Location {
        val world = player.world
        val playerLoc = player.location
        val playerEyeLoc = player.eyeLocation

        for (attempt in 1..10) {
            val x = playerLoc.x + (Math.random() * maxDistance * 2 - maxDistance)
            val z = playerLoc.z + (Math.random() * maxDistance * 2 - maxDistance)

            val spawnY = getSafeSpawnY(world, x, z) ?: continue
            val targetLocation = Location(world, x, spawnY, z)

            if (isLineOfSightClearOptimized(playerEyeLoc, targetLocation) &&
                isLocationSafeForSpawn(targetLocation)
            ) {
                return targetLocation
            }
        }

        return findSpawnLocationWithRayTrace(player, maxDistance)
    }

    private fun findSpawnLocationWithRayTrace(player: Player, maxDistance: Int): Location {
        val world = player.world
        val playerLoc = player.location
        val playerEyeLoc = player.eyeLocation

        val randomDirection = Vector(
            Math.random() * 2 - 1,
            0.0,
            Math.random() * 2 - 1
        ).normalize()

        val targetPoint = playerLoc.clone().add(randomDirection.multiply(maxDistance.toDouble()))
        targetPoint.y = playerLoc.y

        val rayTrace = world.rayTraceBlocks(
            playerEyeLoc,
            randomDirection,
            maxDistance.toDouble(),
            FluidCollisionMode.NEVER,
            true
        )

        return if (rayTrace != null && rayTrace.hitBlock != null) {
            val hitLocation = rayTrace.hitPosition.toLocation(world)
            val safeDistanceFromBlock = 2.0

            val spawnLocation = hitLocation.clone()
                .subtract(randomDirection.clone().multiply(safeDistanceFromBlock))

            val safeY = getSafeSpawnY(world, spawnLocation.x, spawnLocation.z) ?: playerLoc.y
            spawnLocation.y = safeY

            if (!isLocationSafeForSpawn(spawnLocation)) {
                createFallbackLocation(player, 3.0)
            } else {
                spawnLocation
            }
        } else {
            createRandomLocationNearPlayer(player, maxDistance)
        }
    }

    private fun createRandomLocationNearPlayer(player: Player, maxDistance: Int): Location {
        val world = player.world
        val playerLoc = player.location

        for (attempt in 1..5) {
            val minDistance = (maxDistance * 0.3).toInt()
            val actualDistance = minDistance + Math.random() * (maxDistance - minDistance)

            val direction = Vector(
                Math.random() * 2 - 1,
                0.0,
                Math.random() * 2 - 1
            ).normalize()

            val spawnLocation = playerLoc.clone().add(direction.multiply(actualDistance))
            val safeY = getSafeSpawnY(world, spawnLocation.x, spawnLocation.z) ?: continue

            spawnLocation.y = safeY

            if (isLocationSafeForSpawn(spawnLocation)) {
                return spawnLocation
            }
        }

        return createFallbackLocation(player, 5.0)
    }

    private fun createFallbackLocation(player: Player, radius: Double): Location {
        val world = player.world
        val playerLoc = player.location

        for (attempt in 1..10) {
            val spawnLocation = playerLoc.clone().apply {
                add(
                    Math.random() * radius * 2 - radius,
                    0.0,
                    Math.random() * radius * 2 - radius
                )
            }

            val safeY = getSafeSpawnY(world, spawnLocation.x, spawnLocation.z) ?: playerLoc.y
            spawnLocation.y = safeY

            if (isLocationSafeForSpawn(spawnLocation)) {
                return spawnLocation
            }
        }

        return playerLoc.clone().apply {
            add(2.0, 0.0, 2.0)
            y = getSafeSpawnY(world, x, z) ?: y
        }
    }

    private fun getSafeSpawnY(world: World, x: Double, z: Double): Double? {
        val blockX = x.toInt()
        val blockZ = z.toInt()

        val highestBlock = world.getHighestBlockAt(blockX, blockZ)
        val surfaceY = highestBlock.y.toDouble()

        for (yOffset in 0..3) {
            val checkY = surfaceY + yOffset
            val location = Location(world, x, checkY, z)

            if (isLocationSafeForSpawn(location)) {
                return checkY
            }
        }

        return null
    }

    private fun isLineOfSightClearOptimized(from: Location, to: Location): Boolean {
        val world = from.world

        val rayTrace = world.rayTraceBlocks(
            from,
            to.clone().subtract(from).toVector(),
            from.distance(to),
            FluidCollisionMode.NEVER,
            true
        )

        return rayTrace == null
    }

    private fun isLocationSafeForSpawn(location: Location): Boolean {
        val world = location.world
        val block = world.getBlockAt(location)
        val below = world.getBlockAt(location.clone().subtract(0.0, 1.0, 0.0))
        val above = world.getBlockAt(location.clone().add(0.0, 1.0, 0.0))

        if (!below.type.isSolid) {
            return false
        }

        if (block.type.isSolid) {
            return false
        }

        if (above.type.isSolid) {
            return false
        }

        val dangerousBlocks = setOf(
            Material.LAVA,
            Material.FIRE,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.CACTUS,
            Material.MAGMA_BLOCK,
            Material.SWEET_BERRY_BUSH,
            Material.POWDER_SNOW
        )

        if (dangerousBlocks.contains(block.type) || dangerousBlocks.contains(below.type)) {
            return false
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        return emptyList()
    }
}