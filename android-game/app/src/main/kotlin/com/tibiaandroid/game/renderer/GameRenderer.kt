package com.tibiaandroid.game.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.tibiaandroid.game.World
import com.tibiaandroid.game.combat.FloatingText
import com.tibiaandroid.game.entity.Monster
import com.tibiaandroid.game.entity.Player
import com.tibiaandroid.game.map.Position

class GameRenderer(private val tileSize: Int = 48) {

    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val entityPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        typeface = Typeface.MONOSPACE
    }
    private val hpBarBg = Paint().apply { color = Color.BLACK }
    private val hpBarFg = Paint().apply { color = Color.RED }
    private val manaBarFg = Paint().apply { color = Color.BLUE }
    private val borderPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
    private val overlayPaint = Paint().apply { alpha = 180 }

    // Camera offset in pixels
    var cameraX = 0f
    var cameraY = 0f

    private val rectF = RectF()

    fun render(canvas: Canvas, world: World, screenW: Int, screenH: Int) {
        canvas.drawColor(Color.BLACK)

        val player = world.player

        // Center camera on player
        cameraX = player.pos.x * tileSize - screenW / 2f + tileSize / 2f
        cameraY = player.pos.y * tileSize - screenH / 2f + tileSize / 2f

        val startCol = maxOf(0, (cameraX / tileSize).toInt() - 1)
        val endCol = minOf(world.map.width - 1, ((cameraX + screenW) / tileSize).toInt() + 1)
        val startRow = maxOf(0, (cameraY / tileSize).toInt() - 1)
        val endRow = minOf(world.map.height - 1, ((cameraY + screenH) / tileSize).toInt() + 1)

        // Draw tiles
        for (row in startRow..endRow) {
            for (col in startCol..endCol) {
                val tile = world.map.get(col, row) ?: continue
                val sx = col * tileSize - cameraX
                val sy = row * tileSize - cameraY
                tilePaint.color = tile.type.colorHex
                canvas.drawRect(sx, sy, sx + tileSize, sy + tileSize, tilePaint)
                // Grid line
                canvas.drawRect(sx, sy, sx + tileSize, sy + tileSize, borderPaint)
            }
        }

        // Draw monsters
        for (monster in world.monsters) {
            if (!monster.isAlive) continue
            if (monster.pos.x < startCol - 1 || monster.pos.x > endCol + 1) continue
            if (monster.pos.y < startRow - 1 || monster.pos.y > endRow + 1) continue
            drawMonster(canvas, monster)
        }

        // Draw player
        drawPlayer(canvas, player)

        // Draw floating texts
        for (ft in world.floatingTexts) {
            drawFloatingText(canvas, ft)
        }
    }

    private fun drawPlayer(canvas: Canvas, player: Player) {
        val sx = player.pos.x * tileSize - cameraX
        val sy = player.pos.y * tileSize - cameraY
        val pad = 4f

        // Shadow
        entityPaint.color = Color.argb(60, 0, 0, 0)
        canvas.drawOval(sx + pad + 2, sy + tileSize - 8f, sx + tileSize - pad + 2, sy + tileSize.toFloat(), entityPaint)

        // Body
        entityPaint.color = 0xFF00CC00.toInt()
        canvas.drawRect(sx + pad, sy + pad, sx + tileSize - pad, sy + tileSize - pad, entityPaint)

        // Head
        entityPaint.color = 0xFFFFDBAA.toInt()
        val headSize = tileSize * 0.35f
        canvas.drawCircle(sx + tileSize / 2f, sy + pad + headSize * 0.6f, headSize * 0.6f, entityPaint)

        // HP bar
        drawHpBar(canvas, sx, sy - 10f, tileSize.toFloat(), player.hpPercent(), player.manaPercent())

        // Name
        textPaint.textSize = 18f
        textPaint.color = Color.WHITE
        val nameW = textPaint.measureText(player.name)
        canvas.drawText(player.name, sx + tileSize / 2f - nameW / 2f, sy - 14f, textPaint)
    }

    private fun drawMonster(canvas: Canvas, monster: Monster) {
        val sx = monster.pos.x * tileSize - cameraX
        val sy = monster.pos.y * tileSize - cameraY
        val pad = 6f

        // Shadow
        entityPaint.color = Color.argb(60, 0, 0, 0)
        canvas.drawOval(sx + pad + 2, sy + tileSize - 8f, sx + tileSize - pad + 2, sy + tileSize.toFloat(), entityPaint)

        // Body
        entityPaint.color = monster.colorHex
        canvas.drawRect(sx + pad, sy + pad, sx + tileSize - pad, sy + tileSize - pad, entityPaint)

        // Eyes
        entityPaint.color = Color.RED
        val ey = sy + pad + 10f
        canvas.drawCircle(sx + tileSize * 0.35f, ey, 4f, entityPaint)
        canvas.drawCircle(sx + tileSize * 0.65f, ey, 4f, entityPaint)

        // HP bar
        drawHpBar(canvas, sx, sy - 8f, tileSize.toFloat(), monster.hpPercent(), 0f)

        // Name on hover (always show for now)
        textPaint.textSize = 16f
        textPaint.color = 0xFFFF8800.toInt()
        val nameW = textPaint.measureText(monster.name)
        canvas.drawText(monster.name, sx + tileSize / 2f - nameW / 2f, sy - 10f, textPaint)
    }

    private fun drawHpBar(canvas: Canvas, x: Float, y: Float, width: Float, hpPct: Float, manaPct: Float) {
        val barH = 5f
        // BG
        hpBarBg.color = Color.BLACK
        canvas.drawRect(x, y, x + width, y + barH, hpBarBg)
        // HP
        hpBarFg.color = when {
            hpPct > 0.6f -> Color.GREEN
            hpPct > 0.3f -> Color.YELLOW
            else -> Color.RED
        }
        canvas.drawRect(x, y, x + width * hpPct, y + barH, hpBarFg)

        if (manaPct > 0f) {
            val my = y + barH + 1f
            canvas.drawRect(x, my, x + width, my + barH, hpBarBg)
            manaBarFg.color = Color.BLUE
            canvas.drawRect(x, my, x + width * manaPct, my + barH, manaBarFg)
        }
    }

    private fun drawFloatingText(canvas: Canvas, ft: FloatingText) {
        val alpha = (ft.ttl.toFloat() / 60f * 255).toInt().coerceIn(0, 255)
        textPaint.color = ft.color
        textPaint.alpha = alpha
        textPaint.textSize = 28f
        val sx = ft.x * tileSize - cameraX
        val sy = ft.y
        canvas.drawText(ft.text, sx, sy, textPaint)
        textPaint.alpha = 255
    }

    fun screenToWorld(screenX: Float, screenY: Float): Position {
        val wx = ((screenX + cameraX) / tileSize).toInt()
        val wy = ((screenY + cameraY) / tileSize).toInt()
        return Position(wx, wy)
    }
}
