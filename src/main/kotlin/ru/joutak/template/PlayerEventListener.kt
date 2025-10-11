package ru.joutak.template

import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.projectiles.ProjectileSource

/**
 * Обработчик событий плагина Lifesteal
 *
 * Обрабатывает:
 * - Вход игрока на сервер
 * - Смерти игроков
 * - Урон между игроками в PvE режиме
 */
@Suppress("UNUSED")
class PlayerEventListener(private val plugin: LifeStealPlugin) : Listener {

    /**
     * Обрабатывает вход игрока на сервер
     */
    @EventHandler
    @Suppress("UNUSED")
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val dataManager = plugin.getPlayerDataManager()

        // Загружаем данные игрока
        dataManager.loadPlayerData(player.uniqueId)

        val data = dataManager.getPlayerData(player.uniqueId)

        // Проверяем не забанен ли игрок
        if (data != null && data.isBanned()) {
            val banMinutes = (data.banUntil - System.currentTimeMillis()) / 1000 / 60
            player.kickPlayer("§cВы забанены на $banMinutes минут")
            return
        }

        // Обновляем ник и здоровье игрока
        dataManager.updatePlayerNickname(player)
        dataManager.updatePlayerHealth(player.uniqueId)

        // Если активен PvE режим - показываем таймер
        if (dataManager.isPvEActive()) {
            val timeString = dataManager.getRemainingPvETime()
            player.sendActionBar("§6PvE режим: §e$timeString§6 осталось")
        }

        plugin.logger.info("Игрок ${player.name} зашел на сервер. PvE активен: ${dataManager.isPvEActive()}")
    }

    /**
     * Обрабатывает смерть игрока
     */
    @EventHandler
    @Suppress("UNUSED")
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val deadPlayer = event.entity
        val killer = deadPlayer.killer

        val dataManager = plugin.getPlayerDataManager()

        // Если активен PvE режим и убийца - игрок
        if (dataManager.isPvEActive() && killer != null) {
            // Отменяем сообщение о смерти и уведомляем игроков
            event.deathMessage = null
            killer.sendMessage("§6PvE режим активен! Убийства игроков не учитываются.")
            deadPlayer.sendMessage("§6PvE режим активен! Смерть от игрока не засчитана.")
            return
        }

        // Обрабатываем только смерти от других игроков
        if (killer != null) {
            val deadData = dataManager.getPlayerData(deadPlayer.uniqueId)
            val killerData = dataManager.getPlayerData(killer.uniqueId)

            if (deadData != null && killerData != null) {
                // Обновляем статистику убийств и смертей
                killerData.addKill()
                deadData.addDeath()

                // Проверяем иммунитет жертвы
                if (deadData.hasImmunity()) {
                    killer.sendMessage("§6У жертвы иммунитет! Убийство засчитано, но сердце не передано.")
                    deadPlayer.sendMessage("§6Ваш иммунитет защитил вас от потери сердца!")
                }
                // Проверяем не потерял ли жертва последнее сердце
                else if (deadData.hearts == 1) {
                    dataManager.banPlayer(deadPlayer.uniqueId)
                    killer.sendMessage("§6Жертва потеряла все сердца и была забанена на ${plugin.getBanDurationMinutes()} минут!")
                }
                // Проверяем не достиг ли убийца максимума сердец
                else if (killerData.hearts < plugin.getMaxHearts()) {
                    // Передаем сердце от жертвы к убийце
                    killerData.addHeart(plugin)
                    deadData.removeHeart()

                    // Обновляем здоровье обоих игроков
                    dataManager.updatePlayerHealth(killer.uniqueId)
                    dataManager.updatePlayerHealth(deadPlayer.uniqueId)

                    // Уведомляем игроков
                    killer.sendMessage("§a+1 сердце за убийство! Теперь у вас: §c${killerData.hearts} сердец")
                    deadPlayer.sendMessage("§c-1 сердце за смерть! Теперь у вас: §c${deadData.hearts} сердец")
                } else {
                    killer.sendMessage("§6У вас максимальное количество сердец (${plugin.getMaxHearts()})! Убийство засчитано, но сердце не добавлено.")
                }

                // Сохраняем изменения
                dataManager.savePlayerData(killer.uniqueId)
                dataManager.savePlayerData(deadPlayer.uniqueId)

                plugin.logger.info("Игрок ${killer.name} убил ${deadPlayer.name}")
            }
        }
    }

    /**
     * Обрабатывает урон между сущностями (игроками)
     * Блокирует урон между игроками в PvE режиме
     */
    @EventHandler
    @Suppress("UNUSED")
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val dataManager = plugin.getPlayerDataManager()

        // Если PvE режим не активен - пропускаем
        if (!dataManager.isPvEActive()) return

        // Если жертва не игрок - пропускаем
        if (event.entity !is Player) return

        val damaged = event.entity as Player
        var damager: Player? = null

        // Определяем кто наносит урон
        when (event.damager) {
            is Player -> damager = event.damager as Player
            is Projectile -> {
                val shooter = (event.damager as Projectile).shooter
                if (shooter is Player) damager = shooter
            }
        }

        // Если урон наносит игрок и это не самоповреждение
        if (damager != null && damaged != damager) {
            // Отменяем урон и уведомляем атакующего
            event.isCancelled = true
            damager.sendMessage("§6PvE режим активен! Вы не можете атаковать других игроков.")
        }
    }
}