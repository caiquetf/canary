package com.tibiaandroid.game.combat

import com.tibiaandroid.game.entity.Entity
import com.tibiaandroid.game.entity.Monster
import com.tibiaandroid.game.entity.Player
import kotlin.random.Random

data class CombatResult(
    val attacker: Entity,
    val target: Entity,
    val damage: Int,
    val isCritical: Boolean,
    val isMiss: Boolean
)

data class FloatingText(
    var text: String,
    var x: Float,
    var y: Float,
    var color: Int,
    var ttl: Int = 60,
    var dy: Float = -1f
)

object CombatSystem {

    fun playerAttack(player: Player, target: Monster): CombatResult {
        val miss = Random.nextFloat() < 0.05f // 5% miss
        if (miss) return CombatResult(player, target, 0, false, true)

        val critical = Random.nextFloat() < 0.1f // 10% crit
        val base = player.totalAttack + Random.nextInt(-3, 4)
        val damage = if (critical) (base * 2) else base
        val actual = target.takeDamage(damage)
        return CombatResult(player, target, actual, critical, false)
    }

    fun castSpell(player: Player, target: Monster, spellDamage: Int, manaCost: Int): CombatResult? {
        if (player.mana < manaCost) return null
        player.mana -= manaCost
        val critical = Random.nextFloat() < 0.15f
        val damage = if (critical) spellDamage * 2 else spellDamage + Random.nextInt(-10, 11)
        val actual = target.takeDamage(damage)
        return CombatResult(player, target, actual, critical, false)
    }
}
