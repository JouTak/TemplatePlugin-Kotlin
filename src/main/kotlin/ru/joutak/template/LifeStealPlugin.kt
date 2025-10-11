package ru.joutak.template

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

/**
 * Главный класс плагина Lifesteal
 *
 * Основные функции:
 * - Управление системой сердец
 * - PvE режим с таймером
 * - Обработка команд и событий
 * - Сохранение данных игроков
 */
class LifeStealPlugin : JavaPlugin() {

    companion object {
        /**
         * Статическая ссылка на экземпляр плагина для доступа из других классов
         */
        @JvmStatic
        lateinit var instance: LifeStealPlugin
    }

    // Менеджер данных игроков
    private lateinit var playerDataManager: PlayerDataManager

    // Обработчик команд
    private lateinit var lifestealCommand: LifestealCommand

    // Задача таймера PvE режима
    private var pveTask: BukkitRunnable? = null

    /**
     * Вызывается при включении плагина
     */
    override fun onEnable() {
        // Сохраняем ссылку на экземпляр плагина
        instance = this

        // Загружаем конфигурацию
        loadConfiguration()

        // Инициализируем менеджер данных игроков
        playerDataManager = PlayerDataManager(this)

        // Инициализируем обработчик команд
        lifestealCommand = LifestealCommand(this)

        // Регистрируем команды
        registerCommands()

        // Регистрируем обработчики событий
        registerEvents()

        // Запускаем PvE таймер
        startPvETimer()

        // Логируем информацию о запуске
        logStartupInfo()

        logger.info("${description.name} v${description.version} успешно запущен!")
    }

    /**
     * Загружает конфигурацию плагина
     */
    private fun loadConfiguration() {
        // Сохраняем конфиг по умолчанию если он не существует
        saveDefaultConfig()
        // Перезагружаем конфиг чтобы убедиться что значения загружены
        reloadConfig()
        logger.info("Конфигурация загружена")
    }

    /**
     * Регистрирует команды плагина
     */
    private fun registerCommands() {
        // Регистрируем команду /lifesteal
        getCommand("lifesteal")?.setExecutor(lifestealCommand)
        getCommand("lifesteal")?.tabCompleter = lifestealCommand

        // Регистрируем команду /stats
        getCommand("stats")?.setExecutor(lifestealCommand)
        getCommand("stats")?.tabCompleter = lifestealCommand

        // Регистрируем команду /top
        getCommand("top")?.setExecutor(lifestealCommand)
        getCommand("top")?.tabCompleter = lifestealCommand

        logger.info("Команды зарегистрированы")
    }

    /**
     * Регистрирует обработчики событий
     */
    private fun registerEvents() {
        server.pluginManager.registerEvents(PlayerEventListener(this), this)
        logger.info("Обработчики событий зарегистрированы")
    }

    /**
     * Запускает таймер PvE режима
     */
    private fun startPvETimer() {
        pveTask = object : BukkitRunnable() {
            override fun run() {
                // Проверяем активен ли еще PvE режим
                if (!playerDataManager.isPvEActive()) {
                    // PvE режим закончился - отменяем задачу
                    cancel()
                    pveTask = null

                    // Уведомляем всех игроков об окончании PvE режима
                    server.onlinePlayers.forEach { player ->
                        player.sendActionBar("§6PvE режим окончен! PvP включен!")
                    }

                    logger.info("PvE режим окончен")
                    return
                }

                // Получаем оставшееся время PvE режима
                val timeString = playerDataManager.getRemainingPvETime()
                val message = "§6PvE режим: §e$timeString§6 осталось"

                // Отправляем сообщение всем онлайн игрокам над хотбаром
                server.onlinePlayers.forEach { player ->
                    player.sendActionBar(message)
                }
            }
        }

        // Запускаем задачу каждую секунду (20 тиков = 1 секунда)
        pveTask?.runTaskTimer(this, 0L, 20L)
        logger.info("PvE таймер запущен на ${getPveDurationMinutes()} минут")
    }

    /**
     * Вызывается при выключении плагина
     */
    override fun onDisable() {
        // Останавливаем PvE таймер
        pveTask?.cancel()
        pveTask = null

        // Сохраняем данные всех игроков
        playerDataManager.saveAllData()
        logger.info("${description.name} v${description.version} отключен!")
    }

    /**
     * Логирует информацию о запуске плагина
     */
    private fun logStartupInfo() {
        logger.info("=== LifeSteal Plugin Debug ===")
        logger.info("Plugin: ${description.name} v${description.version}")
        logger.info("PvE длительность: ${getPveDurationMinutes()} минут")
        logger.info("Начальные сердца: ${getStartingHearts()}")
        logger.info("Максимум сердец: ${getMaxHearts()}")
        logger.info("Длительность бана: ${getBanDurationMinutes()} минут")
        logger.info("Длительность иммунитета: ${getImmunityDurationHours()} часов")
        logger.info("==============================")
    }

    // ========== ГЕТТЕРЫ ДЛЯ КОНФИГУРАЦИОННЫХ ЗНАЧЕНИЙ ==========

    /**
     * Возвращает длительность PvE режима в минутах
     * @return Длительность PvE режима в минутах
     */
    fun getPveDurationMinutes(): Int {
        return if (config.contains("pve-duration")) {
            config.getInt("pve-duration")
        } else {
            logger.warning("pve-duration не найден в конфиге, используется значение по умолчанию: 180")
            180
        }
    }

    /**
     * Возвращает начальное количество сердец у игроков
     * @return Начальное количество сердец
     */
    fun getStartingHearts(): Int {
        return if (config.contains("starting-hearts")) {
            config.getInt("starting-hearts")
        } else {
            logger.warning("starting-hearts не найден в конфиге, используется значение по умолчанию: 10")
            10
        }
    }

    /**
     * Возвращает максимальное количество сердец
     * @return Максимальное количество сердец
     */
    fun getMaxHearts(): Int {
        return if (config.contains("max-hearts")) {
            config.getInt("max-hearts")
        } else {
            logger.warning("max-hearts не найден в конфиге, используется значение по умолчанию: 20")
            20
        }
    }

    /**
     * Возвращает длительность бана в минутах
     * @return Длительность бана в минутах
     */
    fun getBanDurationMinutes(): Int {
        return if (config.contains("ban-duration")) {
            config.getInt("ban-duration")
        } else {
            logger.warning("ban-duration не найден в конфиге, используется значение по умолчанию: 30")
            30
        }
    }

    /**
     * Возвращает длительность иммунитета в часах
     * @return Длительность иммунитета в часах
     */
    fun getImmunityDurationHours(): Int {
        return if (config.contains("immunity-duration")) {
            config.getInt("immunity-duration")
        } else {
            logger.warning("immunity-duration не найден в конфиге, используется значение по умолчанию: 24")
            24
        }
    }

    /**
     * Возвращает менеджер данных игроков
     * @return Менеджер данных игроков
     */
    fun getPlayerDataManager(): PlayerDataManager = playerDataManager
}