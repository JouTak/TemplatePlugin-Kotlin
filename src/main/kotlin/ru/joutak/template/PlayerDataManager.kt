package ru.joutak.template

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Менеджер для работы с данными игроков
 *
 * Отвечает за:
 * - Загрузку и сохранение данных игроков
 * - Управление PvE режимом
 * - Обновление здоровья игроков
 * - Бан систему
 */
class PlayerDataManager(private val plugin: LifeStealPlugin) {

    /** Карта для хранения данных игроков в оперативной памяти */
    private val playerDataMap = HashMap<UUID, PlayerData>()

    /** Файл для хранения данных игроков */
    private lateinit var dataFile: File

    /** Конфигурация файла данных */
    private lateinit var dataConfig: YamlConfiguration

    /** Время запуска сервера (для расчета PvE режима) */
    private var serverStartTime: Long = 0

    init {
        // Инициализируем файл данных и время запуска
        setupDataFile()
        serverStartTime = System.currentTimeMillis()

        val pveDuration = plugin.getPveDurationMinutes()
        plugin.logger.info("PvE режим начался. Будет активен $pveDuration минут")
    }

    /**
     * Настраивает файл для хранения данных игроков
     */
    private fun setupDataFile() {
        dataFile = File(plugin.dataFolder, "playerdata.yml")
        if (!dataFile.exists()) {
            // Создаем папки и файл если они не существуют
            dataFile.parentFile.mkdirs()
            dataFile.createNewFile()
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile)
    }

    /**
     * Загружает данные игрока по его UUID
     * @param uuid UUID игрока
     */
    fun loadPlayerData(uuid: UUID) {
        val path = uuid.toString()
        val data = if (dataConfig.contains(path)) {
            // Загружаем существующие данные игрока
            PlayerData(
                hearts = dataConfig.getInt("$path.hearts", plugin.getStartingHearts()),
                kills = dataConfig.getInt("$path.kills", 0),
                deaths = dataConfig.getInt("$path.deaths", 0),
                immunityUntil = dataConfig.getLong("$path.immunityUntil", 0),
                banUntil = dataConfig.getLong("$path.banUntil", 0)
            )
        } else {
            // Создаем новые данные для нового игрока
            PlayerData(hearts = plugin.getStartingHearts())
        }
        playerDataMap[uuid] = data
        updatePlayerHealth(uuid)
    }

    /**
     * Сохраняет данные игрока в файл
     * @param uuid UUID игрока
     */
    fun savePlayerData(uuid: UUID) {
        val data = playerDataMap[uuid] ?: return
        val path = uuid.toString()

        // Сохраняем все поля данных игрока
        dataConfig.set("$path.hearts", data.hearts)
        dataConfig.set("$path.kills", data.kills)
        dataConfig.set("$path.deaths", data.deaths)
        dataConfig.set("$path.immunityUntil", data.immunityUntil)
        dataConfig.set("$path.banUntil", data.banUntil)

        try {
            dataConfig.save(dataFile)
        } catch (e: IOException) {
            plugin.logger.severe("Не удалось сохранить данные игрока $uuid")
        }
    }

    /**
     * Сохраняет данные всех игроков
     */
    fun saveAllData() {
        playerDataMap.keys.forEach { savePlayerData(it) }
    }

    /**
     * Загружает данные всех онлайн игроков
     */
    fun loadAllData() {
        Bukkit.getOnlinePlayers().forEach { loadPlayerData(it.uniqueId) }
    }

    /**
     * Возвращает данные игрока по UUID
     * @param uuid UUID игрока
     * @return Данные игрока или null если не найдены
     */
    fun getPlayerData(uuid: UUID): PlayerData? = playerDataMap[uuid]

    /**
     * Обновляет здоровье игрока в игре на основе количества сердец
     * @param uuid UUID игрока
     */
    fun updatePlayerHealth(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid) ?: return
        val data = playerDataMap[uuid] ?: return

        // Устанавливаем максимальное здоровье (1 сердце = 2 здоровья)
        player.setMaxHealth(data.hearts * 2.0)

        // Если текущее здоровье больше максимума - уменьшаем его
        if (player.health > player.maxHealth) {
            player.health = player.maxHealth
        }
    }

    /**
     * Обновляет никнейм игрока (добавляет "Immunity" если есть иммунитет)
     * @param player Игрок
     */
    fun updatePlayerNickname(player: Player) {
        val data = getPlayerData(player.uniqueId) ?: return

        if (data.hasImmunity()) {
            // Добавляем пометку об иммунитете в ник
            player.setDisplayName("§6${player.name} §eImmunity")
            player.setPlayerListName("§6${player.name} §eImmunity")
        } else {
            // Возвращаем обычный ник
            player.setDisplayName(player.name)
            player.setPlayerListName(player.name)
        }
    }

    /**
     * Банит игрока при потере всех сердец
     * @param uuid UUID игрока
     */
    fun banPlayer(uuid: UUID) {
        val data = getPlayerData(uuid) ?: return

        // Устанавливаем время окончания бана и иммунитета
        data.banUntil = System.currentTimeMillis() + plugin.getBanDurationMinutes() * 60 * 1000
        data.immunityUntil = System.currentTimeMillis() + plugin.getImmunityDurationHours() * 60 * 60 * 1000
        data.hearts = 1

        // Кикаем игрока с сообщением о бане
        val player = Bukkit.getPlayer(uuid)
        player?.kickPlayer("§cВы потеряли все сердца! Бан на ${plugin.getBanDurationMinutes()} минут.\n§6После возврата у вас будет иммунитет на ${plugin.getImmunityDurationHours()} часов.")

        // Сохраняем изменения
        savePlayerData(uuid)
    }

    /**
     * Проверяет, активен ли PvE режим
     * @return true если PvE режим активен
     */
    fun isPvEActive(): Boolean {
        val pveDurationMs = plugin.getPveDurationMinutes() * 60 * 1000L
        return System.currentTimeMillis() < serverStartTime + pveDurationMs
    }

    /**
     * Возвращает оставшееся время PvE режима в формате строки
     * @return Строка с оставшимся временем (ЧЧ:ММ или ММ:СС)
     */
    fun getRemainingPvETime(): String {
        val pveDurationMs = plugin.getPveDurationMinutes() * 60 * 1000L
        val remaining = (serverStartTime + pveDurationMs) - System.currentTimeMillis()

        // Если время вышло
        if (remaining <= 0) {
            return "00:00"
        }

        // Вычисляем часы, минуты и секунды
        val totalMinutes = remaining / (60 * 1000)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val seconds = (remaining % (60 * 1000)) / 1000

        return when {
            // Если остались часы - показываем ЧЧ:ММ
            hours > 0 -> String.format("%02d:%02d", hours, minutes)
            // Если остались минуты - показываем ММ:СС
            minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
            // Если остались только секунды - показываем СС
            else -> String.format("00:%02d", seconds)
        }
    }

    /**
     * Возвращает позицию игрока в топе по убийствам
     * @param uuid UUID игрока
     * @return Позиция в топе или -1 если игрок не в топе
     */
    fun getPlayerRank(uuid: UUID): Int {
        // Сортируем игроков по убийствам (по убыванию)
        val sortedPlayers = playerDataMap.entries.sortedByDescending { it.value.kills }
        val index = sortedPlayers.indexOfFirst { it.key == uuid }
        return if (index >= 0) index + 1 else -1
    }

    /**
     * Возвращает список топ игроков по убийствам
     * @param count Количество игроков в топе
     * @return Список записей (UUID -> PlayerData) топ игроков
     */
    fun getTopPlayers(count: Int): List<Map.Entry<UUID, PlayerData>> {
        return playerDataMap.entries
            // Сортируем по убийствам (по убыванию)
            .sortedByDescending { it.value.kills }
            // Фильтруем игроков с хотя бы одним убийством
            .filter { it.value.kills > 0 }
            // Берем указанное количество
            .take(count)
    }
}