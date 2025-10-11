package ru.joutak.template

/**
 * Класс для хранения данных игрока
 *
 * Содержит:
 * - Количество сердец
 * - Статистику убийств и смертей
 * - Время иммунитета и бана
 */
class PlayerData(
    /** Текущее количество сердец игрока */
    var hearts: Int = 10,

    /** Количество убийств, совершенных игроком */
    var kills: Int = 0,

    /** Количество смертей от других игроков */
    var deaths: Int = 0,

    /** Время окончания иммунитета (в миллисекундах) */
    var immunityUntil: Long = 0,

    /** Время окончания бана (в миллисекундах) */
    var banUntil: Long = 0
) {

    /**
     * Проверяет, активен ли у игрока иммунитет
     * @return true если иммунитет активен
     */
    fun hasImmunity(): Boolean = System.currentTimeMillis() < immunityUntil

    /**
     * Проверяет, забанен ли игрок
     * @return true если игрок забанен
     */
    fun isBanned(): Boolean = System.currentTimeMillis() < banUntil

    /**
     * Добавляет одно сердце с проверкой максимального количества
     * @param plugin Экземпляр плагина для получения максимального количества сердец
     */
    fun addHeart(plugin: LifeStealPlugin) {
        if (hearts < plugin.getMaxHearts()) hearts++
    }

    /**
     * Убирает одно сердце с проверкой минимального количества
     */
    fun removeHeart() {
        if (hearts > 1) hearts--
    }

    /**
     * Увеличивает счетчик убийств
     */
    fun addKill() { kills++ }

    /**
     * Увеличивает счетчик смертей
     */
    fun addDeath() { deaths++ }
}