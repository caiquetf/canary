package com.tibiaandroid.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.tibiaandroid.data.ItemType
import com.tibiaandroid.game.entity.Player

class InventoryDialog(context: Context, private val player: Player) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setBackgroundDrawable(ColorDrawable(Color.argb(220, 30, 20, 10)))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setGravity(Gravity.CENTER)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        root.addView(TextView(context).apply {
            text = "INVENTORY — ${player.name} (Lv.${player.level})"
            textSize = 22f
            setTextColor(0xFFFFD700.toInt())
            gravity = Gravity.CENTER
        })

        root.addView(TextView(context).apply {
            text = "Gold: ${player.gold}"
            textSize = 18f
            setTextColor(0xFFFFD700.toInt())
            setPadding(0, 8, 0, 16)
        })

        // Equipped section
        root.addView(sectionHeader("Equipped"))
        val eq = player.equipped
        val equippedItems = listOfNotNull(
            eq.weapon?.let { "Weapon: ${it.name} (ATK+${it.attackBonus})" },
            eq.armor?.let { "Armor: ${it.name} (DEF+${it.defenseBonus})" },
            eq.helmet?.let { "Helmet: ${it.name} (DEF+${it.defenseBonus})" },
            eq.shield?.let { "Shield: ${it.name} (DEF+${it.defenseBonus})" },
        )
        if (equippedItems.isEmpty()) {
            root.addView(itemLabel("  (nothing equipped)"))
        } else {
            equippedItems.forEach { root.addView(itemLabel(it, 0xFF88FFAA.toInt())) }
        }

        root.addView(TextView(context).apply {
            text = "Total ATK: ${player.totalAttack}  DEF: ${player.totalDefense}"
            textSize = 16f
            setTextColor(0xFFFFFF88.toInt())
            setPadding(0, 8, 0, 16)
        })

        // Inventory items
        root.addView(sectionHeader("Items (${player.inventory.sumOf { it.count }})"))

        val scroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 400
            )
        }
        val itemList = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        if (player.inventory.isEmpty()) {
            itemList.addView(itemLabel("  (empty)"))
        } else {
            for (slot in player.inventory) {
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 4, 0, 4)
                }
                val label = "${slot.item.name} x${slot.count}"
                val tv = itemLabel(label)
                tv.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                row.addView(tv)

                if (slot.item.type == ItemType.POTION) {
                    val useBtn = Button(context).apply {
                        text = "USE"
                        textSize = 14f
                        setTextColor(Color.WHITE)
                        setBackgroundColor(Color.argb(200, 80, 20, 20))
                        setPadding(8, 0, 8, 0)
                        setOnClickListener {
                            player.usePotion(slot.item)
                            dismiss()
                        }
                    }
                    row.addView(useBtn)
                } else if (slot.item.type in listOf(ItemType.WEAPON, ItemType.ARMOR, ItemType.HELMET, ItemType.SHIELD, ItemType.LEGS, ItemType.BOOTS)) {
                    val equipBtn = Button(context).apply {
                        text = "EQUIP"
                        textSize = 14f
                        setTextColor(Color.WHITE)
                        setBackgroundColor(Color.argb(200, 20, 60, 20))
                        setPadding(8, 0, 8, 0)
                        setOnClickListener {
                            player.equip(slot.item)
                            dismiss()
                        }
                    }
                    row.addView(equipBtn)
                }

                itemList.addView(row)
            }
        }

        scroll.addView(itemList)
        root.addView(scroll)

        val closeBtn = Button(context).apply {
            text = "CLOSE"
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(200, 60, 30, 10))
            setOnClickListener { dismiss() }
        }
        root.addView(closeBtn)
        setContentView(root)
    }

    private fun sectionHeader(text: String) = TextView(context).apply {
        this.text = "— $text —"
        textSize = 18f
        setTextColor(0xFFFFD700.toInt())
        gravity = Gravity.CENTER
        setPadding(0, 8, 0, 4)
    }

    private fun itemLabel(text: String, color: Int = 0xFFEEEEEE.toInt()) = TextView(context).apply {
        this.text = text
        textSize = 16f
        setTextColor(color)
        setPadding(8, 2, 8, 2)
    }
}
