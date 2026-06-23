package com.tibiaandroid.game

import com.tibiaandroid.data.ItemDatabase
import com.tibiaandroid.data.MonsterDatabase
import com.tibiaandroid.data.MonsterTemplate
import com.tibiaandroid.game.ai.MonsterAI
import com.tibiaandroid.game.combat.CombatResult
import com.tibiaandroid.game.combat.CombatSystem
import com.tibiaandroid.game.combat.FloatingText
import com.tibiaandroid.game.entity.Monster
import com.tibiaandroid.game.entity.Player
import com.tibiaandroid.game.map.MapGenerator
import com.tibiaandroid.game.map.Position
import com.tibiaandroid.game.map.TileMap
import kotlin.random.Random

data class ChatMessage(val text: String, val colorHex: Int = 0xFFFFFFFF.toInt(), val timestamp: Long = System.currentTimeMillis())

class World(val player: Player) {

    val map: TileMap = MapGenerator.generateWorld(200, 200)
    val monsters = mutableListOf<Monster>()
    val floatingTexts = mutableListOf<FloatingText>()
    val chatLog = mutableListOf<ChatMessage>()

    private val ai = MonsterAI(map)
    private var tick = 0L
    private val spawnedZones = mutableSetOf<String>()

    // Mana regen tick
    private var regenTick = 0

    init {
        // Place player at spawn (center of main town)
        player.pos = Position(50, 50)
        ensurePassable(player)

        // Give starter equipment
        ItemDatabase.items[1]?.let { player.addItem(it) ; player.equip(it) }
        ItemDatabase.items[8]?.let { player.addItem(it) ; player.equip(it) }
        ItemDatabase.items[11]?.let { player.addItem(it) ; player.equip(it) }
        repeat(5) { ItemDatabase.items[20]?.let { p -> player.addItem(p) } }
        repeat(3) { ItemDatabase.items[22]?.let { p -> player.addItem(p) } }
        player.gold = 200

        // Initial monsters near player area
        spawnMonstersAround(player.pos, 15, 30)
        addChat("Welcome to Tibia Android! Move with the D-pad. Tap monsters to attack.", 0xFFFFD700.toInt())
    }

    private fun ensurePassable(player: Player) {
        if (!map.isPassable(player.pos.x, player.pos.y)) {
            for (r in 1..10) {
                for (dy in -r..r) for (dx in -r..r) {
                    val p = Position(player.pos.x + dx, player.pos.y + dy)
                    if (map.isPassable(p.x, p.y)) {
                        player.pos = p
                        return
                    }
                }
            }
        }
    }

    fun update() {
        tick++
        regenTick++

        // Spawn monsters dynamically near player
        val zoneKey = "${player.pos.x / 20}_${player.pos.y / 20}"
        if (!spawnedZones.contains(zoneKey)) {
            spawnedZones.add(zoneKey)
            spawnMonstersAround(player.pos, 10, 25)
        }

        // Remove dead monsters, drop loot
        val dead = monsters.filter { !it.isAlive }
        for (m in dead) {
            val loot = m.dropLoot()
            for ((itemId, _) in loot) {
                ItemDatabase.items[itemId]?.let { player.addItem(it) }
            }
            if (loot.isNotEmpty()) {
                addChat("${m.name} dropped: ${loot.mapNotNull { ItemDatabase.items[it.first]?.name }.joinToString(", ")}", 0xFFFFD700.toInt())
            }
            player.gainExperience(m.template.experience)
            addChat("You gained ${m.template.experience} experience.", 0xFF00FF00.toInt())
        }
        monsters.removeAll { !it.isAlive }

        // AI update
        ai.update(monsters, listOf(player))

        // Mana/HP regen every 3 seconds (90 ticks at 30fps)
        if (regenTick >= 90) {
            regenTick = 0
            player.restoreMana(maxOf(1, player.maxMana / 20))
            if (monsters.none { it.pos.distanceTo(player.pos) <= 5 }) {
                player.heal(maxOf(1, player.maxHp / 30))
            }
        }

        // Update floating texts
        val iter = floatingTexts.iterator()
        while (iter.hasNext()) {
            val ft = iter.next()
            ft.ttl--
            ft.y += ft.dy
            if (ft.ttl <= 0) iter.remove()
        }
    }

    fun playerAttack(targetMonster: Monster): CombatResult? {
        if (!targetMonster.isAlive) return null
        val dist = player.pos.distanceTo(targetMonster.pos)
        if (dist > 1) return null
        val result = CombatSystem.playerAttack(player, targetMonster)
        if (result.isMiss) {
            addChat("You missed!", 0xFFAAAAAA.toInt())
        } else {
            val critText = if (result.isCritical) " [CRITICAL!]" else ""
            addChat("You deal ${result.damage} damage to ${targetMonster.name}.$critText", 0xFFFFFFFF.toInt())
        }
        return result
    }

    fun addFloatingText(text: String, worldX: Float, worldY: Float, color: Int) {
        floatingTexts.add(FloatingText(text, worldX, worldY, color))
    }

    fun addChat(text: String, color: Int = 0xFFFFFFFF.toInt()) {
        chatLog.add(ChatMessage(text, color))
        if (chatLog.size > 50) chatLog.removeAt(0)
    }

    private fun spawnMonstersAround(center: Position, minDist: Int, maxDist: Int) {
        // Choose monster types based on distance from origin
        val distFromOrigin = center.distanceTo(Position(50, 50))
        val eligibleMonsters = MonsterDatabase.monsters.values.filter {
            it.level <= maxOf(1, distFromOrigin / 5 + 3)
        }
        if (eligibleMonsters.isEmpty()) return

        val count = Random.nextInt(3, 8)
        repeat(count) {
            val angle = Random.nextDouble() * 2 * Math.PI
            val dist = Random.nextInt(minDist, maxDist)
            val mx = (center.x + dist * Math.cos(angle)).toInt()
            val my = (center.y + dist * Math.sin(angle)).toInt()
            if (map.isPassable(mx, my)) {
                val template = eligibleMonsters.random()
                monsters.add(Monster(Position(mx, my), template))
            }
        }
    }
}
