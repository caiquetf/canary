package com.tibiaandroid.game.entity

import com.tibiaandroid.data.MonsterTemplate
import com.tibiaandroid.data.MonsterType
import com.tibiaandroid.game.map.Position

class Monster(
    pos: Position,
    val template: MonsterTemplate
) : Entity(
    pos, template.name, template.hp, 0,
    template.attack, template.defense, template.speed, template.colorHex
) {
    var targetId: Int? = null
    var spawnPos: Position = pos
    var wanderTickCounter: Int = 0
    var aggroTick: Int = 0

    val isAggressive get() = template.type == MonsterType.AGGRESSIVE
    val aggroRange get() = template.aggroRange
    val attackRange get() = template.attackRange

    fun dropLoot(): List<Pair<Int, Int>> { // itemId, count
        val drops = mutableListOf<Pair<Int, Int>>()
        for ((itemId, chance) in template.loot) {
            if (Math.random() < chance) drops.add(itemId to 1)
        }
        return drops
    }
}
