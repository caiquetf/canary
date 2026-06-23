package com.tibiaandroid.data

// Tile types
enum class TileType(val passable: Boolean, val colorHex: Int) {
    GRASS(true, 0xFF3A7A1E.toInt()),
    DIRT(true, 0xFF8B6914.toInt()),
    STONE_FLOOR(true, 0xFF888888.toInt()),
    WATER(false, 0xFF1A5276.toInt()),
    WALL(false, 0xFF555555.toInt()),
    MOUNTAIN(false, 0xFF6D4C41.toInt()),
    LAVA(false, 0xFFCC2200.toInt()),
    DUNGEON_FLOOR(true, 0xFF444444.toInt()),
    DUNGEON_WALL(false, 0xFF222222.toInt()),
    TREE(false, 0xFF1B5E20.toInt()),
    SAND(true, 0xFFDEB887.toInt()),
    ICE(true, 0xFFADD8E6.toInt()),
}

enum class ItemType { WEAPON, ARMOR, HELMET, LEGS, BOOTS, SHIELD, RING, AMULET, POTION, FOOD, QUEST, MISC }

data class ItemTemplate(
    val id: Int,
    val name: String,
    val type: ItemType,
    val attackBonus: Int = 0,
    val defenseBonus: Int = 0,
    val hpRestore: Int = 0,
    val manaRestore: Int = 0,
    val colorHex: Int = 0xFFFFFF00.toInt(),
    val weight: Int = 10,
    val value: Int = 0,
    val requiredLevel: Int = 1
)

object ItemDatabase {
    val items = mapOf(
        1 to ItemTemplate(1, "Sword", ItemType.WEAPON, attackBonus = 10, colorHex = 0xFFCCCCCC.toInt(), value = 50),
        2 to ItemTemplate(2, "Long Sword", ItemType.WEAPON, attackBonus = 18, colorHex = 0xFFBBBBBB.toInt(), value = 150, requiredLevel = 10),
        3 to ItemTemplate(3, "Two-Handed Sword", ItemType.WEAPON, attackBonus = 28, colorHex = 0xFF999999.toInt(), value = 400, requiredLevel = 20),
        4 to ItemTemplate(4, "Axe", ItemType.WEAPON, attackBonus = 9, colorHex = 0xFF888888.toInt(), value = 40),
        5 to ItemTemplate(5, "Battle Axe", ItemType.WEAPON, attackBonus = 20, colorHex = 0xFF777777.toInt(), value = 200, requiredLevel = 15),
        6 to ItemTemplate(6, "Club", ItemType.WEAPON, attackBonus = 6, colorHex = 0xFF8B4513.toInt(), value = 20),
        7 to ItemTemplate(7, "Mace", ItemType.WEAPON, attackBonus = 14, colorHex = 0xFFAAAAAA.toInt(), value = 120, requiredLevel = 8),
        8 to ItemTemplate(8, "Leather Armor", ItemType.ARMOR, defenseBonus = 8, colorHex = 0xFF8B4513.toInt(), value = 60),
        9 to ItemTemplate(9, "Chain Armor", ItemType.ARMOR, defenseBonus = 14, colorHex = 0xFF999999.toInt(), value = 200, requiredLevel = 10),
        10 to ItemTemplate(10, "Plate Armor", ItemType.ARMOR, defenseBonus = 22, colorHex = 0xFFBBBBBB.toInt(), value = 800, requiredLevel = 25),
        11 to ItemTemplate(11, "Leather Helmet", ItemType.HELMET, defenseBonus = 3, colorHex = 0xFF8B4513.toInt(), value = 30),
        12 to ItemTemplate(12, "Iron Helmet", ItemType.HELMET, defenseBonus = 8, colorHex = 0xFF999999.toInt(), value = 150, requiredLevel = 10),
        13 to ItemTemplate(13, "Wooden Shield", ItemType.SHIELD, defenseBonus = 5, colorHex = 0xFF8B4513.toInt(), value = 25),
        14 to ItemTemplate(14, "Iron Shield", ItemType.SHIELD, defenseBonus = 12, colorHex = 0xFF888888.toInt(), value = 180, requiredLevel = 8),
        20 to ItemTemplate(20, "Health Potion", ItemType.POTION, hpRestore = 100, colorHex = 0xFFCC0000.toInt(), value = 50, weight = 2),
        21 to ItemTemplate(21, "Strong Health Potion", ItemType.POTION, hpRestore = 250, colorHex = 0xFFFF0000.toInt(), value = 120, requiredLevel = 10, weight = 2),
        22 to ItemTemplate(22, "Mana Potion", ItemType.POTION, manaRestore = 100, colorHex = 0xFF0044CC.toInt(), value = 50, weight = 2),
        23 to ItemTemplate(23, "Strong Mana Potion", ItemType.POTION, manaRestore = 250, colorHex = 0xFF0000FF.toInt(), value = 120, requiredLevel = 10, weight = 2),
        30 to ItemTemplate(30, "Gold Coin", ItemType.MISC, colorHex = 0xFFFFD700.toInt(), value = 1, weight = 1),
        31 to ItemTemplate(31, "Platinum Coin", ItemType.MISC, colorHex = 0xFFE5E4E2.toInt(), value = 100, weight = 1),
    )
}

enum class MonsterType { PASSIVE, NEUTRAL, AGGRESSIVE }

data class MonsterTemplate(
    val id: Int,
    val name: String,
    val hp: Int,
    val maxHp: Int = hp,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val experience: Int,
    val colorHex: Int,
    val type: MonsterType = MonsterType.AGGRESSIVE,
    val loot: List<Pair<Int, Float>> = emptyList(), // itemId to dropChance(0-1)
    val attackRange: Int = 1,
    val aggroRange: Int = 5,
    val level: Int = 1
)

object MonsterDatabase {
    val monsters = mapOf(
        1 to MonsterTemplate(1, "Rat", 20, attack = 5, defense = 2, speed = 3, experience = 5,
            colorHex = 0xFF8B6914.toInt(), loot = listOf(30 to 0.8f)),

        2 to MonsterTemplate(2, "Snake", 30, attack = 8, defense = 3, speed = 4, experience = 10,
            colorHex = 0xFF228B22.toInt(), loot = listOf(30 to 0.5f)),

        3 to MonsterTemplate(3, "Wolf", 80, attack = 15, defense = 8, speed = 5, experience = 25,
            colorHex = 0xFF888888.toInt(), level = 5, loot = listOf(30 to 1f, 30 to 0.5f)),

        4 to MonsterTemplate(4, "Orc", 150, attack = 22, defense = 14, speed = 4, experience = 60,
            colorHex = 0xFF556B2F.toInt(), level = 10, loot = listOf(4 to 0.2f, 30 to 1f, 30 to 1f)),

        5 to MonsterTemplate(5, "Troll", 200, attack = 28, defense = 18, speed = 3, experience = 100,
            colorHex = 0xFF6D4C41.toInt(), level = 15, loot = listOf(8 to 0.15f, 30 to 2f.coerceAtMost(1f))),

        6 to MonsterTemplate(6, "Skeleton", 120, attack = 20, defense = 10, speed = 4, experience = 50,
            colorHex = 0xFFEEEEEE.toInt(), level = 8, loot = listOf(30 to 1f, 20 to 0.1f)),

        7 to MonsterTemplate(7, "Zombie", 180, attack = 25, defense = 12, speed = 2, experience = 80,
            colorHex = 0xFF5D4037.toInt(), level = 12, loot = listOf(30 to 1f, 20 to 0.15f)),

        8 to MonsterTemplate(8, "Dragon", 1500, attack = 80, defense = 50, speed = 5, experience = 2000,
            colorHex = 0xFFCC2200.toInt(), level = 50, aggroRange = 8,
            loot = listOf(10 to 0.05f, 31 to 1f, 31 to 0.5f, 21 to 0.8f)),

        9 to MonsterTemplate(9, "Demon", 3000, attack = 120, defense = 80, speed = 6, experience = 6000,
            colorHex = 0xFF880000.toInt(), level = 80, aggroRange = 10,
            loot = listOf(31 to 2f.coerceAtMost(1f), 23 to 0.9f)),

        10 to MonsterTemplate(10, "Slime", 60, attack = 10, defense = 5, speed = 2, experience = 15,
            colorHex = 0xFF66FF00.toInt(), level = 3, loot = listOf(30 to 0.6f)),

        11 to MonsterTemplate(11, "Giant Spider", 250, attack = 35, defense = 20, speed = 5, experience = 150,
            colorHex = 0xFF333333.toInt(), level = 18, aggroRange = 6,
            loot = listOf(9 to 0.1f, 22 to 0.2f, 30 to 1f)),
    )
}

enum class Vocation { NONE, KNIGHT, PALADIN, SORCERER, DRUID }

data class VocationTemplate(
    val vocation: Vocation,
    val displayName: String,
    val description: String,
    val hpPerLevel: Int,
    val manaPerLevel: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val colorHex: Int
)

object VocationDatabase {
    val vocations = listOf(
        VocationTemplate(Vocation.KNIGHT, "Knight", "Tank warrior. High HP and defense, melee focused.", 15, 5, 20, 25, 0xFFCC0000.toInt()),
        VocationTemplate(Vocation.PALADIN, "Paladin", "Balanced fighter with ranged attacks and healing.", 10, 10, 15, 15, 0xFF0000CC.toInt()),
        VocationTemplate(Vocation.SORCERER, "Sorcerer", "Master of offensive magic. Low HP, devastating spells.", 5, 30, 8, 8, 0xFF9900CC.toInt()),
        VocationTemplate(Vocation.DRUID, "Druid", "Nature mage with healing and summoning.", 8, 25, 10, 10, 0xFF009900.toInt()),
    )
}
