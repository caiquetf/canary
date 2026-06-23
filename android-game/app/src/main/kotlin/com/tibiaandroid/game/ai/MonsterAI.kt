package com.tibiaandroid.game.ai

import com.tibiaandroid.game.entity.EntityState
import com.tibiaandroid.game.entity.Monster
import com.tibiaandroid.game.entity.Player
import com.tibiaandroid.game.map.Position
import com.tibiaandroid.game.map.TileMap
import kotlin.random.Random

class MonsterAI(private val map: TileMap) {

    private val pathCache = mutableMapOf<Int, List<Position>>()
    private val pathCacheTick = mutableMapOf<Int, Int>()
    private var tick = 0

    fun update(monsters: List<Monster>, players: List<Player>) {
        tick++
        for (monster in monsters) {
            if (!monster.isAlive) continue
            updateMonster(monster, players)
        }
    }

    private fun updateMonster(monster: Monster, players: List<Player>) {
        monster.moveTickCounter++
        monster.attackTickCounter++

        val moveCooldown = maxOf(1, 8 - monster.speed / 2)
        val target = findTarget(monster, players)

        if (target != null) {
            monster.targetId = target.id
            monster.state = EntityState.WALKING
            val dist = monster.pos.distanceTo(target.pos)

            if (dist <= monster.attackRange && monster.attackTickCounter >= monster.attackCooldown) {
                // Attack
                monster.attackTickCounter = 0
                monster.state = EntityState.ATTACKING
                val dmg = maxOf(1, monster.attack + Random.nextInt(-3, 4))
                target.takeDamage(dmg)
            } else if (dist > monster.attackRange && monster.moveTickCounter >= moveCooldown) {
                monster.moveTickCounter = 0
                moveToward(monster, target.pos)
            }

            // Lose aggro if too far
            if (dist > monster.aggroRange * 2) {
                monster.targetId = null
                monster.state = EntityState.IDLE
            }
        } else {
            // Wander
            monster.targetId = null
            monster.wanderTickCounter++
            if (monster.wanderTickCounter >= 20 && monster.moveTickCounter >= moveCooldown) {
                monster.moveTickCounter = 0
                monster.wanderTickCounter = 0
                wander(monster)
            } else {
                monster.state = EntityState.IDLE
            }
        }
    }

    private fun findTarget(monster: Monster, players: List<Player>): Player? {
        if (!monster.isAggressive) return null
        var best: Player? = null
        var bestDist = Int.MAX_VALUE
        for (player in players) {
            if (!player.isAlive) continue
            val d = monster.pos.distanceTo(player.pos)
            if (d <= monster.aggroRange && d < bestDist) {
                best = player
                bestDist = d
            }
        }
        return best
    }

    private fun moveToward(monster: Monster, target: Position) {
        val cacheAge = tick - (pathCacheTick[monster.id] ?: 0)
        val path = if (cacheAge > 5 || pathCache[monster.id].isNullOrEmpty()) {
            val p = Pathfinding.findPath(map, monster.pos, target)
            pathCache[monster.id] = p
            pathCacheTick[monster.id] = tick
            p
        } else {
            pathCache[monster.id]!!
        }

        val next = path.firstOrNull { it != monster.pos } ?: run {
            pathCache.remove(monster.id)
            return
        }

        if (map.isPassable(next.x, next.y)) {
            pathCache[monster.id] = path.drop(1)
            monster.pos = next
        } else {
            pathCache.remove(monster.id)
        }
    }

    private fun wander(monster: Monster) {
        val dirs = listOf(
            Position(0, -1), Position(0, 1), Position(-1, 0), Position(1, 0)
        )
        val dir = dirs[Random.nextInt(dirs.size)]
        val next = monster.pos + dir
        val distFromSpawn = next.distanceTo(monster.spawnPos)
        if (map.isPassable(next.x, next.y) && distFromSpawn <= 8) {
            monster.pos = next
            monster.state = EntityState.WALKING
        }
    }
}
