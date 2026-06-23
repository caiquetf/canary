package com.tibiaandroid.game.entity

import com.tibiaandroid.game.map.Position
import java.util.concurrent.atomic.AtomicInteger

enum class Direction { NORTH, SOUTH, EAST, WEST, NONE }

enum class EntityState { IDLE, WALKING, ATTACKING, DEAD }

private val idCounter = AtomicInteger(0)

abstract class Entity(
    var pos: Position,
    var name: String,
    var maxHp: Int,
    var maxMana: Int = 0,
    var attack: Int = 10,
    var defense: Int = 5,
    var speed: Int = 4,
    val colorHex: Int = 0xFFFF0000.toInt()
) {
    val id: Int = idCounter.incrementAndGet()
    var hp: Int = maxHp
    var mana: Int = maxMana
    var state: EntityState = EntityState.IDLE
    var direction: Direction = Direction.SOUTH
    var moveTickCounter: Int = 0
    var attackTickCounter: Int = 0
    val attackCooldown: Int get() = maxOf(1, 10 - speed / 3)

    val isAlive get() = hp > 0

    fun takeDamage(amount: Int): Int {
        val mitigated = maxOf(1, amount - defense / 2)
        hp = maxOf(0, hp - mitigated)
        if (hp == 0) state = EntityState.DEAD
        return mitigated
    }

    fun heal(amount: Int) {
        hp = minOf(maxHp, hp + amount)
    }

    fun restoreMana(amount: Int) {
        mana = minOf(maxMana, mana + amount)
    }

    fun hpPercent() = hp.toFloat() / maxHp
    fun manaPercent() = if (maxMana > 0) mana.toFloat() / maxMana else 0f
}
