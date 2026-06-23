package com.tibiaandroid

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.tibiaandroid.game.GameEngine
import com.tibiaandroid.game.GameView
import com.tibiaandroid.ui.CharacterSelectActivity
import com.tibiaandroid.ui.InventoryDialog

class MainActivity : AppCompatActivity() {

    private lateinit var gameEngine: GameEngine
    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // If no player data, go to character select
        val playerName = intent.getStringExtra("playerName")
        if (playerName == null) {
            startActivity(Intent(this, CharacterSelectActivity::class.java))
            finish()
            return
        }

        val vocation = intent.getIntExtra("vocation", 0)

        gameEngine = GameEngine(playerName, vocation)
        gameView = GameView(this, gameEngine)

        gameView.setOnInventoryRequested {
            runOnUiThread {
                InventoryDialog(this, gameEngine.player).show()
            }
        }

        setContentView(gameView)
    }

    override fun onPause() {
        super.onPause()
        gameEngine.isRunning = false
    }

    override fun onResume() {
        super.onResume()
        if (::gameEngine.isInitialized) {
            gameEngine.isRunning = true
        }
    }
}
