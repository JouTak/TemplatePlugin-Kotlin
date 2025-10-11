package ru.joutak.template

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Обработчик команд плагина Lifesteal
 *
 * Обрабатывает команды:
 * - /stats <игрок> - просмотр статистики
 * - /lifesteal give <игрок> <количество> - передача сердец
 * - /top - топ игроков по убийствам
 */
@Suppress("UNUSED", "DEPRECATION")
class LifestealCommand(private val plugin: LifeStealPlugin) : CommandExecutor, TabCompleter {

    /**
     * Основной метод обработки команд
     * @param sender Отправитель команды
     * @param command Объект команды
     * @param label Метка команды
     * @param args Аргументы команды
     * @return true если команда обработана успешно
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // Логируем полученную команду для отладки
        plugin.logger.info("Команда получена: $label, аргументы: ${args.joinToString()}")

        return when (command.name.lowercase()) {
            "stats" -> {
                handleStatsCommand(sender, args)
                true
            }
            "top" -> {
                handleTopCommand(sender)
                true
            }
            "lifesteal" -> {
                handleLifestealCommand(sender, args)
                true
            }
            else -> false
        }
    }

    /**
     * Обрабатывает команду /stats
     * @param sender Отправитель команды
     * @param args Аргументы команды
     */
    @Suppress("SameReturnValue")
    private fun handleStatsCommand(sender: CommandSender, args: Array<out String>) {
        when {
            // /stats - показать свою статистику
            args.isEmpty() -> {
                if (sender is Player) {
                    showPlayerStats(sender, sender)
                } else {
                    sender.sendMessage("${ChatColor.RED}Эта команда только для игроков!")
                }
            }
            // /stats <ник> - показать статистику другого игрока
            args.size == 1 -> {
                val target = Bukkit.getPlayer(args[0])
                if (target != null) {
                    showPlayerStats(sender, target)
                } else {
                    sender.sendMessage("${ChatColor.RED}Игрок '${args[0]}' не найден или оффлайн!")
                }
            }
            // Слишком много аргументов
            else -> {
                sender.sendMessage("${ChatColor.RED}Используйте: /stats [player]")
            }
        }
    }

    /**
     * Обрабатывает команду /top
     * @param sender Отправитель команды
     */
    @Suppress("SameReturnValue")
    private fun handleTopCommand(sender: CommandSender) {
        showTopPlayers(sender)
    }

    /**
     * Обрабатывает команду /lifesteal
     * @param sender Отправитель команды
     * @param args Аргументы команды
     */
    @Suppress("SameReturnValue")
    private fun handleLifestealCommand(sender: CommandSender, args: Array<out String>) {
        // /lifesteal give <игрок> <количество>
        if (args.size == 3 && args[0].equals("give", ignoreCase = true)) {
            handleGiveCommand(sender, args)
            return
        }

        // Неправильное использование команды
        sender.sendMessage("${ChatColor.RED}Используйте: /lifesteal give <player> <amount>")
        sender.sendMessage("${ChatColor.GRAY}Пример: /lifesteal give Notch 5")
    }

    /**
     * Показывает статистику игрока
     * @param sender Отправитель команды (кто запросил статистику)
     * @param player Игрок, чью статистику нужно показать
     */
    private fun showPlayerStats(sender: CommandSender, player: Player) {
        val data = plugin.getPlayerDataManager().getPlayerData(player.uniqueId)
        if (data == null) {
            sender.sendMessage("${ChatColor.RED}Данные игрока не найдены!")
            return
        }

        // Получаем позицию игрока в топе
        val rank = plugin.getPlayerDataManager().getPlayerRank(player.uniqueId)
        val rankText = if (rank > 0) "#$rank" else "Не в топе"

        // Формируем и отправляем сообщение со статистикой
        sender.sendMessage("${ChatColor.GOLD}=== Статистика ${player.name} ===")
        sender.sendMessage("${ChatColor.RED}❤ Сердец: ${data.hearts}")
        sender.sendMessage("${ChatColor.GREEN}⚔ Убийств: ${data.kills}")
        sender.sendMessage("${ChatColor.GRAY}💀 Смертей: ${data.deaths}")
        sender.sendMessage("${ChatColor.YELLOW}🏆 Рейтинг: $rankText")

        // Если у игрока активен иммунитет - показываем время до окончания
        if (data.hasImmunity()) {
            val remainingTime = (data.immunityUntil - System.currentTimeMillis()) / 1000 / 60
            val hours = remainingTime / 60
            val minutes = remainingTime % 60
            sender.sendMessage("${ChatColor.YELLOW}🛡️ Иммунитет: ${hours}ч ${minutes}м осталось")
        }

        plugin.logger.info("Показана статистика игрока ${player.name}")
    }

    /**
     * Показывает топ игроков по убийствам
     * @param sender Отправитель команды
     */
    private fun showTopPlayers(sender: CommandSender) {
        val topPlayers = plugin.getPlayerDataManager().getTopPlayers(10)

        // Если в топе никого нет
        if (topPlayers.isEmpty()) {
            sender.sendMessage("${ChatColor.GOLD}Топ игроков: ${ChatColor.RED}Пока тут никого нет")
            sender.sendMessage("${ChatColor.GRAY}Совершите первое убийство, чтобы попасть в топ!")
            return
        }

        sender.sendMessage("${ChatColor.GOLD}=== Топ-10 игроков по убийствам ===")

        // Выводим каждого игрока из топа
        topPlayers.forEachIndexed { index, entry ->
            val player = Bukkit.getOfflinePlayer(entry.key)
            val playerName = player.name ?: "Unknown"
            val data = entry.value

            // Определяем медальку для позиции
            val medal = when (index) {
                0 -> "🥇"  // Золото
                1 -> "🥈"  // Серебро
                2 -> "🥉"  // Бронза
                else -> "${index + 1}."
            }

            // Формируем строку с информацией об игроке
            sender.sendMessage("${ChatColor.YELLOW}$medal $playerName - " +
                    "${ChatColor.GREEN}${data.kills} убийств${ChatColor.YELLOW} / " +
                    "${ChatColor.GRAY}${data.deaths} смертей")
        }
    }

    /**
     * Обрабатывает команду передачи сердец
     * @param sender Отправитель команды (игрок, который передает сердца)
     * @param args Аргументы команды [give, игрок, количество]
     */
    @Suppress("SameReturnValue")
    private fun handleGiveCommand(sender: CommandSender, args: Array<out String>) {
        // Проверяем что команду вызвал игрок
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Эта команда только для игроков!")
            return
        }

        val toPlayer = Bukkit.getPlayer(args[1])

        // Проверяем что целевой игрок существует и онлайн
        if (toPlayer == null) {
            sender.sendMessage("${ChatColor.RED}Игрок '${args[1]}' не найден или оффлайн!")
            return
        }

        // Нельзя передавать сердца самому себе
        if (sender.uniqueId == toPlayer.uniqueId) {
            sender.sendMessage("${ChatColor.RED}Нельзя передавать сердца самому себе!")
            return
        }

        // Парсим количество сердец
        val amount = args[2].toIntOrNull()
        if (amount == null || amount <= 0) {
            sender.sendMessage("${ChatColor.RED}Введите корректное положительное число!")
            sender.sendMessage("${ChatColor.GRAY}Пример: /lifesteal give ${toPlayer.name} 5")
            return
        }

        val dataManager = plugin.getPlayerDataManager()
        val fromData = dataManager.getPlayerData(sender.uniqueId)
        val toData = dataManager.getPlayerData(toPlayer.uniqueId)

        // Проверяем что данные игроков загружены
        if (fromData == null || toData == null) {
            sender.sendMessage("${ChatColor.RED}Ошибка загрузки данных игроков!")
            return
        }

        // Проверяем что у отправителя достаточно сердец
        if (fromData.hearts < amount) {
            sender.sendMessage("${ChatColor.RED}У вас недостаточно сердец!")
            sender.sendMessage("${ChatColor.RED}У вас: ${fromData.hearts}, хотите передать: $amount")
            return
        }

        // Проверяем что у получателя не максимальное количество сердец
        if (toData.hearts >= plugin.getMaxHearts()) {
            sender.sendMessage("${ChatColor.RED}У игрока ${toPlayer.name} уже максимальное количество сердец (${plugin.getMaxHearts()})!")
            return
        }

        // Передаем сердца
        fromData.hearts -= amount
        toData.hearts += amount

        // Обновляем здоровье обоих игроков
        dataManager.updatePlayerHealth(sender.uniqueId)
        dataManager.updatePlayerHealth(toPlayer.uniqueId)

        // Сохраняем изменения
        dataManager.savePlayerData(sender.uniqueId)
        dataManager.savePlayerData(toPlayer.uniqueId)

        // Уведомляем игроков о передаче
        sender.sendMessage("${ChatColor.GREEN}✓ Вы передали $amount ${getHeartWord(amount)} игроку ${toPlayer.name}")
        toPlayer.sendMessage("${ChatColor.GREEN}✓ Вы получили $amount ${getHeartWord(amount)} от ${sender.name}")

        plugin.logger.info("Игрок ${sender.name} передал $amount сердец игроку ${toPlayer.name}")
    }

    /**
     * Возвращает правильную форму слова "сердце" в зависимости от количества
     * @param amount Количество сердец
     * @return Правильная форма слова
     */
    private fun getHeartWord(amount: Int): String {
        return when {
            amount % 10 == 1 && amount % 100 != 11 -> "сердце"
            amount % 10 in 2..4 && amount % 100 !in 12..14 -> "сердца"
            else -> "сердец"
        }
    }

    /**
     * Обрабатывает автодополнение команд (Tab-completion)
     * @param sender Отправитель команды
     * @param command Объект команды
     * @param label Метка команды
     * @param args Аргументы команды
     * @return Список предложений для автодополнения
     */
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        val completions = mutableListOf<String>()

        when (command.name.lowercase()) {
            "stats" -> {
                // Автодополнение для /stats <игрок>
                if (args.size == 1) {
                    completions.addAll(Bukkit.getOnlinePlayers().map { it.name })
                }
            }
            "lifesteal" -> {
                when (args.size) {
                    1 -> completions.add("give")  // Предлагаем подкоманду give
                    2 -> {
                        if (args[0].equals("give", ignoreCase = true)) {
                            // Предлагаем онлайн-игроков, исключая себя
                            completions.addAll(Bukkit.getOnlinePlayers()
                                .filter { it.name != sender.name }
                                .map { it.name })
                        }
                    }
                    3 -> {
                        if (args[0].equals("give", ignoreCase = true)) {
                            // Предлагаем стандартные количества сердец
                            completions.addAll(listOf("1", "2", "5", "10"))
                        }
                    }
                }
            }
            "top" -> {
                // Для этой команды автодополнение не нужно
            }
        }

        // Фильтруем предложения по введенному тексту
        return completions.filter { it.startsWith(args.last(), ignoreCase = true) }
    }
}