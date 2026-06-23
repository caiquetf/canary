package com.tibiaandroid.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameView(context: Context, val engine: GameEngine) : SurfaceView(context), SurfaceHolder.Callback {

    private var gameJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val targetFps = 30L
    private val frameDuration = 1000L / targetFps

    // HUD paints
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 26f
        typeface = Typeface.MONOSPACE
        isFakeBoldText = true
    }
    private val barBg = Paint().apply { color = Color.argb(180, 0, 0, 0) }
    private val chatBg = Paint().apply { color = Color.argb(150, 0, 0, 0) }

    // D-pad buttons (right-side layout in landscape)
    private val dpadCenterX get() = width - 250f
    private val dpadCenterY get() = height - 200f
    private val dpadBtnSize = 80f

    // Action buttons
    private val hpBtnRect = RectF()
    private val manaBtnRect = RectF()
    private val bagBtnRect = RectF()

    private var touchTarget: String? = null
    private var onInventoryRequested: (() -> Unit)? = null

    init {
        holder.addCallback(this)
    }

    fun setOnInventoryRequested(cb: () -> Unit) {
        onInventoryRequested = cb
    }

    override fun surfaceCreated(h: SurfaceHolder) {
        engine.setScreenSize(width, height)
        engine.isRunning = true
        startLoop()
    }

    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, h2: Int) {
        engine.setScreenSize(w, h2)
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        engine.isRunning = false
        gameJob?.cancel()
    }

    private fun startLoop() {
        gameJob = scope.launch {
            while (engine.isRunning) {
                val start = System.currentTimeMillis()
                engine.update()
                val c = holder.lockCanvas()
                if (c != null) {
                    try {
                        engine.render(c)
                        drawHUD(c)
                    } finally {
                        holder.unlockCanvasAndPost(c)
                    }
                }
                val elapsed = System.currentTimeMillis() - start
                val sleep = frameDuration - elapsed
                if (sleep > 0) delay(sleep)
            }
        }
    }

    private fun drawHUD(canvas: Canvas) {
        val p = engine.player
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()

        // === TOP-LEFT: Player stats ===
        hudPaint.color = Color.argb(180, 0, 0, 0)
        canvas.drawRoundRect(RectF(8f, 8f, 320f, 140f), 12f, 12f, hudPaint)

        textPaint.textSize = 22f
        textPaint.color = Color.WHITE
        canvas.drawText("${p.name}  Lv.${p.level}", 20f, 36f, textPaint)
        textPaint.color = 0xFFFFD700.toInt()
        canvas.drawText(p.vocation.name, 20f, 60f, textPaint)

        // HP bar
        drawStatBar(canvas, 20f, 70f, 280f, 18f, p.hpPercent(),
            Color.RED, "HP: ${p.hp}/${p.maxHp}")

        // Mana bar
        drawStatBar(canvas, 20f, 94f, 280f, 18f, p.manaPercent(),
            Color.BLUE, "MP: ${p.mana}/${p.maxMana}")

        // XP bar
        val xpPct = if (p.xpToNextLevel > 0) p.experience.toFloat() / p.xpToNextLevel else 0f
        drawStatBar(canvas, 20f, 118f, 280f, 14f, xpPct,
            0xFF00AA00.toInt(), "XP: ${p.experience}/${p.xpToNextLevel}")

        // === TOP-RIGHT: Gold + potions ===
        hudPaint.color = Color.argb(180, 0, 0, 0)
        canvas.drawRoundRect(RectF(w - 240f, 8f, w - 8f, 80f), 12f, 12f, hudPaint)
        textPaint.textSize = 22f
        textPaint.color = 0xFFFFD700.toInt()
        canvas.drawText("Gold: ${p.gold}", w - 220f, 36f, textPaint)
        val hpPots = p.inventory.filter { it.item.hpRestore > 0 }.sumOf { it.count }
        val mpPots = p.inventory.filter { it.item.manaRestore > 0 }.sumOf { it.count }
        textPaint.color = Color.RED
        canvas.drawText("HP Pot: $hpPots", w - 220f, 62f, textPaint)
        textPaint.color = Color.BLUE
        canvas.drawText("MP Pot: $mpPots", w - 110f, 62f, textPaint)

        // === CHAT LOG (bottom-left) ===
        val chatLines = engine.world.chatLog.takeLast(5)
        val chatTop = h - 180f
        canvas.drawRect(8f, chatTop, 500f, h - 8f, chatBg)
        textPaint.textSize = 18f
        chatLines.forEachIndexed { i, msg ->
            textPaint.color = msg.colorHex
            canvas.drawText(msg.text.take(55), 16f, chatTop + 22f + i * 26f, textPaint)
        }

        // === D-PAD (bottom-right) ===
        drawDpad(canvas)

        // === ACTION BUTTONS ===
        val btnY = h - 80f
        hpBtnRect.set(w - 440f, btnY, w - 330f, h - 8f)
        manaBtnRect.set(w - 320f, btnY, w - 210f, h - 8f)
        bagBtnRect.set(w - 200f, btnY, w - 90f, h - 8f)

        drawButton(canvas, hpBtnRect, "HP\nPotion", Color.RED)
        drawButton(canvas, manaBtnRect, "MP\nPotion", Color.BLUE)
        drawButton(canvas, bagBtnRect, "BAG", 0xFF8B4513.toInt())

        // Dead screen
        if (!p.isAlive) {
            hudPaint.color = Color.argb(160, 0, 0, 0)
            canvas.drawRect(0f, 0f, w, h, hudPaint)
            textPaint.textSize = 60f
            textPaint.color = Color.RED
            val msg = "YOU DIED"
            val tw = textPaint.measureText(msg)
            canvas.drawText(msg, w / 2 - tw / 2, h / 2, textPaint)
            textPaint.textSize = 30f
            textPaint.color = Color.WHITE
            val sub = "Tap to restart"
            val sw = textPaint.measureText(sub)
            canvas.drawText(sub, w / 2 - sw / 2, h / 2 + 60f, textPaint)
        }
    }

    private fun drawStatBar(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, pct: Float, color: Int, label: String) {
        barBg.color = Color.argb(180, 20, 20, 20)
        canvas.drawRect(x, y, x + w, y + h, barBg)
        hudPaint.color = color
        canvas.drawRect(x, y, x + w * pct.coerceIn(0f, 1f), y + h, hudPaint)
        textPaint.textSize = h - 2f
        textPaint.color = Color.WHITE
        canvas.drawText(label, x + 4f, y + h - 2f, textPaint)
    }

    private fun drawDpad(canvas: Canvas) {
        val cx = dpadCenterX
        val cy = dpadCenterY
        val s = dpadBtnSize / 2f

        // UP
        drawDpadBtn(canvas, cx - s, cy - dpadBtnSize - 4f, s * 2, s * 2, "▲", touchTarget == "UP")
        // DOWN
        drawDpadBtn(canvas, cx - s, cy + 4f, s * 2, s * 2, "▼", touchTarget == "DOWN")
        // LEFT
        drawDpadBtn(canvas, cx - dpadBtnSize - 4f, cy - s, s * 2, s * 2, "◄", touchTarget == "LEFT")
        // RIGHT
        drawDpadBtn(canvas, cx + 4f, cy - s, s * 2, s * 2, "►", touchTarget == "RIGHT")
    }

    private fun drawDpadBtn(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, label: String, pressed: Boolean) {
        hudPaint.color = if (pressed) Color.argb(220, 200, 200, 200) else Color.argb(160, 80, 80, 80)
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 8f, 8f, hudPaint)
        textPaint.textSize = 28f
        textPaint.color = if (pressed) Color.BLACK else Color.WHITE
        canvas.drawText(label, x + w / 2f - 10f, y + h / 2f + 10f, textPaint)
    }

    private fun drawButton(canvas: Canvas, rect: RectF, label: String, color: Int) {
        hudPaint.color = color
        canvas.drawRoundRect(rect, 12f, 12f, hudPaint)
        textPaint.textSize = 20f
        textPaint.color = Color.WHITE
        val lines = label.split("\n")
        lines.forEachIndexed { i, l ->
            val tw = textPaint.measureText(l)
            canvas.drawText(l, rect.centerX() - tw / 2, rect.centerY() - (lines.size - 1) * 12f + i * 26f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // D-pad
                val cx = dpadCenterX; val cy = dpadCenterY; val s = dpadBtnSize / 2f
                val dir = when {
                    x in (cx - s)..(cx + s) && y in (cy - dpadBtnSize - 4f)..(cy - 4f) -> "UP"
                    x in (cx - s)..(cx + s) && y in (cy + 4f)..(cy + dpadBtnSize + 4f) -> "DOWN"
                    x in (cx - dpadBtnSize - 4f)..(cx - 4f) && y in (cy - s)..(cy + s) -> "LEFT"
                    x in (cx + 4f)..(cx + dpadBtnSize + 4f) && y in (cy - s)..(cy + s) -> "RIGHT"
                    else -> null
                }
                if (dir != null && dir != touchTarget) {
                    touchTarget = dir
                    val moveDir = when (dir) {
                        "UP" -> MoveDir.UP
                        "DOWN" -> MoveDir.DOWN
                        "LEFT" -> MoveDir.LEFT
                        else -> MoveDir.RIGHT
                    }
                    engine.onMovePressed(moveDir)
                    return true
                }

                // Action buttons (only on DOWN)
                if (event.action == MotionEvent.ACTION_DOWN) {
                    if (hpBtnRect.contains(x, y)) { engine.useHealthPotion(); return true }
                    if (manaBtnRect.contains(x, y)) { engine.useManaPotion(); return true }
                    if (bagBtnRect.contains(x, y)) { onInventoryRequested?.invoke(); return true }

                    // Tap on world (attack)
                    if (dir == null && !hpBtnRect.contains(x, y) && !manaBtnRect.contains(x, y)) {
                        engine.onTap(x, y)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchTarget = null
                engine.onMoveReleased()
            }
        }
        return true
    }
}
