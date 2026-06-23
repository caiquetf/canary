package com.tibiaandroid.game

import android.graphics.Canvas
import com.tibiaandroid.game.entity.EntityState
import com.tibiaandroid.game.entity.Player
import com.tibiaandroid.game.map.Position
import com.tibiaandroid.game.renderer.GameRenderer

enum class MoveDir { UP, DOWN, LEFT, RIGHT }

class GameEngine(playerName: String, vocationOrdinal: Int) {

    val player: Player
    val world: World
    val renderer = GameRenderer()

    var isRunning = false
    private var screenW = 1
    private var screenH = 1

    // Movement input queue
    private var pendingMove: MoveDir? = null
    private var moveHeld: MoveDir? = null
    private var moveTick = 0

    init {
        val vocation = com.tibiaandroid.data.Vocation.values()[vocationOrdinal.coerceIn(0, 3)]
        player = Player(Position(0, 0), playerName, vocation)
        world = World(player)
    }

    fun setScreenSize(w: Int, h: Int) {
        screenW = w
        screenH = h
    }

    fun update() {
        if (!isRunning || !player.isAlive) return

        // Handle movement
        handleMovement()

        world.update()
    }

    private fun handleMovement() {
        val dir = pendingMove ?: moveHeld ?: return
        moveTick++

        val moveCooldown = maxOf(2, 8 - player.speed / 2)
        if (pendingMove != null || moveTick >= moveCooldown) {
            moveTick = 0
            pendingMove = null

            val next = when (dir) {
                MoveDir.UP -> Position(player.pos.x, player.pos.y - 1)
                MoveDir.DOWN -> Position(player.pos.x, player.pos.y + 1)
                MoveDir.LEFT -> Position(player.pos.x - 1, player.pos.y)
                MoveDir.RIGHT -> Position(player.pos.x + 1, player.pos.y)
            }

            if (world.map.isPassable(next.x, next.y)) {
                player.pos = next
                player.state = EntityState.WALKING
            }
        }
    }

    fun render(canvas: Canvas) {
        renderer.render(canvas, world, screenW, screenH)
    }

    fun onTap(screenX: Float, screenY: Float) {
        val worldPos = renderer.screenToWorld(screenX, screenY)
        val target = world.monsters.firstOrNull { it.pos == worldPos && it.isAlive }
        if (target != null) {
            val dist = player.pos.distanceTo(target.pos)
            if (dist <= 1) {
                val result = world.playerAttack(target)
                if (result != null && !result.isMiss) {
                    val floatY = (renderer.cameraY * -1) + screenY - 30f
                    world.addFloatingText(
                        "-${result.damage}",
                        target.pos.x.toFloat(),
                        floatY,
                        if (result.isCritical) 0xFFFFAA00.toInt() else 0xFFFF4444.toInt()
                    )
                }
            } else {
                world.addChat("Move closer to attack ${target.name}!", 0xFFFF8800.toInt())
            }
        }
    }

    fun onMovePressed(dir: MoveDir) {
        pendingMove = dir
        moveHeld = dir
    }

    fun onMoveReleased() {
        moveHeld = null
    }

    fun useHealthPotion() {
        val potion = player.inventory.firstOrNull { it.item.hpRestore > 0 }
        if (potion != null) {
            val restored = potion.item.hpRestore
            player.usePotion(potion.item)
            world.addChat("You drink a health potion and recover $restored HP.", 0xFFCC0000.toInt())
        } else {
            world.addChat("No health potions left!", 0xFFFF0000.toInt())
        }
    }

    fun useManaPotion() {
        val potion = player.inventory.firstOrNull { it.item.manaRestore > 0 }
        if (potion != null) {
            val restored = potion.item.manaRestore
            player.usePotion(potion.item)
            world.addChat("You drink a mana potion and recover $restored mana.", 0xFF0044CC.toInt())
        } else {
            world.addChat("No mana potions left!", 0xFFFF0000.toInt())
        }
    }
}
