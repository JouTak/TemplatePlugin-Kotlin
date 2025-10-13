package ru.joutak.template.entity

import org.bukkit.entity.EntityType
import java.util.UUID

data class MobGroup(
    val playerId: UUID,
    val entityType: EntityType,
    val spawnTime: Long,
)