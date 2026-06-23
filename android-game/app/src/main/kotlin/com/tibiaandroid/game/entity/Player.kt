package com.tibiaandroid.game.entity

import com.tibiaandroid.data.ItemTemplate
import com.tibiaandroid.data.ItemType
import com.tibiaandroid.data.Vocation
import com.tibiaandroid.data.VocationDatabase
import com.tibiaandroid.game.map.Position

data class EquippedItems(
    var weapon: ItemTemplate? = null,
    var armor: ItemTemplate? = null,
    var helmet: ItemTemplate? = null,
    var legs: ItemTemplate? = null,
    var boots: ItemTemplate? = null,
    var shield: ItemTemplate? = null,
    var ring: ItemTemplate? = null,
    var amulet: ItemTemplate? = null
)

data class InventorySlot(val item: ItemTemplate, var count: Int = 1)

class Player(
    pos: Position,
    name: String,
    val vocation: Vocation = Vocation.KNIGHT
) : Entity(pos, name, 150, 50, 15, 10, 4, 0xFF00FF00.toInt()) {

    var level: Int = 1
    var experience: Long = 0
    var gold: Int = 0

    val inventory = mutableListOf<InventorySlot>()
    val equipped = EquippedItems()

    val totalAttack get() = attack + (equipped.weapon?.attackBonus ?: 0)
    val totalDefense get() = defense + (equipped.armor?.defenseBonus ?: 0) +
            (equipped.shield?.defenseBonus ?: 0) + (equipped.helmet?.defenseBonus ?: 0) +
            (equipped.legs?.defenseBonus ?: 0) + (equipped.boots?.defenseBonus ?: 0)

    val xpToNextLevel get() = (level * level * 100L).toLong()

    fun gainExperience(xp: Int) {
        experience += xp
        while (experience >= xpToNextLevel) {
            experience -= xpToNextLevel
            levelUp()
        }
    }

    private fun levelUp() {
        level++
        val tmpl = VocationDatabase.vocations.find { it.vocation == vocation }
        if (tmpl != null) {
            maxHp += tmpl.hpPerLevel
            maxMana += tmpl.manaPerLevel
            attack += 1
            defense += 1
        }
        hp = maxHp
        mana = maxMana
    }

    fun addItem(item: ItemTemplate, count: Int = 1) {
        val existing = inventory.find { it.item.id == item.id }
        if (existing != null) {
            existing.count += count
        } else {
            inventory.add(InventorySlot(item, count))
        }
        if (item.type == ItemType.MISC && item.name.contains("Gold")) {
            gold += count * item.value
            inventory.removeAll { it.item.id == item.id }
        }
    }

    fun removeItem(itemId: Int, count: Int = 1): Boolean {
        val slot = inventory.find { it.item.id == itemId } ?: return false
        if (slot.count < count) return false
        slot.count -= count
        if (slot.count == 0) inventory.removeAll { it.item.id == itemId }
        return true
    }

    fun equip(item: ItemTemplate): Boolean {
        if (level < item.requiredLevel) return false
        when (item.type) {
            ItemType.WEAPON -> equipped.weapon = item
            ItemType.ARMOR -> equipped.armor = item
            ItemType.HELMET -> equipped.helmet = item
            ItemType.LEGS -> equipped.legs = item
            ItemType.BOOTS -> equipped.boots = item
            ItemType.SHIELD -> equipped.shield = item
            ItemType.RING -> equipped.ring = item
            ItemType.AMULET -> equipped.amulet = item
            else -> return false
        }
        return true
    }

    fun usePotion(item: ItemTemplate): Boolean {
        if (!removeItem(item.id)) return false
        if (item.hpRestore > 0) heal(item.hpRestore)
        if (item.manaRestore > 0) restoreMana(item.manaRestore)
        return true
    }
}
