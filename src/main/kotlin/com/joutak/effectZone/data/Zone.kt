package com.joutak.effectZone.data

import org.bukkit.potion.PotionEffectType

data class Zone (
    val name : String,
    val effect : PotionEffectType,
    val worldName: String,
    val x1: Double, val y1: Double, val z1: Double,
    val x2: Double, val y2: Double, val z2: Double
    )