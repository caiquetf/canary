package com.tibiaandroid.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.tibiaandroid.MainActivity
import com.tibiaandroid.data.VocationDatabase

class CharacterSelectActivity : AppCompatActivity() {

    private var selectedVocation = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(255, 30, 15, 5))
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "TIBIA ANDROID"
            textSize = 42f
            setTextColor(0xFFFFD700.toInt())
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })

        root.addView(TextView(this).apply {
            text = "Offline Single Player"
            textSize = 18f
            setTextColor(0xFFBBBBBB.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        })

        root.addView(TextView(this).apply {
            text = "Enter your character name:"
            textSize = 18f
            setTextColor(Color.WHITE)
        })

        val nameInput = EditText(this).apply {
            hint = "Your Name"
            textSize = 22f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setBackgroundColor(Color.argb(180, 50, 30, 10))
            setText("Hero")
            setPadding(16, 12, 16, 12)
            maxLines = 1
        }
        root.addView(nameInput)

        root.addView(TextView(this).apply {
            text = "Choose your vocation:"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 32, 0, 8)
        })

        val vocGroup = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val vocations = VocationDatabase.vocations
        vocations.forEachIndexed { i, v ->
            val rb = RadioButton(this).apply {
                text = v.displayName
                setTextColor(Color.WHITE)
                id = i
                textSize = 16f
            }
            vocGroup.addView(rb)
        }
        vocGroup.check(0)
        root.addView(vocGroup)

        // Vocation description
        val descView = TextView(this).apply {
            text = vocations[0].description
            textSize = 15f
            setTextColor(0xFFCCCCCC.toInt())
            setPadding(0, 8, 0, 32)
        }
        root.addView(descView)

        vocGroup.setOnCheckedChangeListener { _, checked ->
            selectedVocation = checked
            descView.text = vocations[checked.coerceIn(0, vocations.size - 1)].description
        }

        val startBtn = Button(this).apply {
            text = "START ADVENTURE"
            textSize = 24f
            setTextColor(Color.BLACK)
            setBackgroundColor(0xFFFFD700.toInt())
            setPadding(32, 16, 32, 16)
        }
        root.addView(startBtn)

        startBtn.setOnClickListener {
            val name = nameInput.text.toString().trim().ifEmpty { "Hero" }
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("playerName", name)
                putExtra("vocation", selectedVocation)
            }
            startActivity(intent)
            finish()
        }

        setContentView(root)
    }
}
