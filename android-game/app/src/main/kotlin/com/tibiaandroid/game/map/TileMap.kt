package com.tibiaandroid.game.map

import com.tibiaandroid.data.TileType
import kotlin.math.abs
import kotlin.random.Random

data class Position(val x: Int, val y: Int) {
    fun distanceTo(other: Position) = maxOf(abs(x - other.x), abs(y - other.y))
    fun manhattanTo(other: Position) = abs(x - other.x) + abs(y - other.y)
    operator fun plus(other: Position) = Position(x + other.x, y + other.y)
}

data class Tile(
    val type: TileType,
    val hasItem: Boolean = false,
    val itemId: Int = 0
)

class TileMap(val width: Int, val height: Int) {
    val tiles = Array(height) { Array(width) { Tile(TileType.GRASS) } }

    fun get(x: Int, y: Int): Tile? {
        if (x < 0 || y < 0 || x >= width || y >= height) return null
        return tiles[y][x]
    }

    fun set(x: Int, y: Int, tile: Tile) {
        if (x < 0 || y < 0 || x >= width || y >= height) return
        tiles[y][x] = tile
    }

    fun isPassable(x: Int, y: Int): Boolean = get(x, y)?.type?.passable == true

    fun neighbors(pos: Position): List<Position> {
        val dirs = listOf(
            Position(0, -1), Position(0, 1), Position(-1, 0), Position(1, 0),
            Position(-1, -1), Position(1, -1), Position(-1, 1), Position(1, 1)
        )
        return dirs.map { pos + it }.filter { isPassable(it.x, it.y) }
    }
}

object MapGenerator {
    fun generateWorld(width: Int = 200, height: Int = 200, seed: Long = System.currentTimeMillis()): TileMap {
        val rng = Random(seed)
        val map = TileMap(width, height)

        // Fill base with grass
        for (y in 0 until height) for (x in 0 until width) {
            map.set(x, y, Tile(TileType.GRASS))
        }

        // Generate rivers
        generateRiver(map, rng)

        // Generate forest patches
        repeat(20) { generateForest(map, rng, width, height) }

        // Generate mountain ranges
        repeat(5) { generateMountains(map, rng, width, height) }

        // Generate sand/beach near water
        generateSandBanks(map, width, height)

        // Generate towns with stone floors
        generateTown(map, rng, width / 4, height / 4, "Main Town")
        generateTown(map, rng, 3 * width / 4, height / 4, "North Town")
        generateTown(map, rng, width / 2, 3 * height / 4, "South Town")

        // Generate dungeon entrance zones
        generateDungeon(map, rng, width / 2, height / 2)
        generateDungeon(map, rng, width / 3, 2 * height / 3)

        // Border walls
        for (x in 0 until width) {
            map.set(x, 0, Tile(TileType.MOUNTAIN))
            map.set(x, height - 1, Tile(TileType.MOUNTAIN))
        }
        for (y in 0 until height) {
            map.set(0, y, Tile(TileType.MOUNTAIN))
            map.set(width - 1, y, Tile(TileType.MOUNTAIN))
        }

        return map
    }

    private fun generateRiver(map: TileMap, rng: Random) {
        var x = rng.nextInt(map.width / 4, 3 * map.width / 4)
        for (y in 1 until map.height - 1) {
            for (w in -2..2) {
                val rx = x + w
                if (rx in 1 until map.width - 1) {
                    map.set(rx, y, Tile(TileType.WATER))
                }
            }
            x += rng.nextInt(-1, 2)
            x = x.coerceIn(5, map.width - 6)
        }
    }

    private fun generateForest(map: TileMap, rng: Random, width: Int, height: Int) {
        val cx = rng.nextInt(10, width - 10)
        val cy = rng.nextInt(10, height - 10)
        val radius = rng.nextInt(5, 15)
        for (dy in -radius..radius) for (dx in -radius..radius) {
            if (dx * dx + dy * dy <= radius * radius) {
                val tx = cx + dx; val ty = cy + dy
                if (tx in 1 until width - 1 && ty in 1 until height - 1) {
                    val t = map.get(tx, ty)
                    if (t?.type == TileType.GRASS && rng.nextFloat() < 0.7f) {
                        map.set(tx, ty, Tile(TileType.TREE))
                    }
                }
            }
        }
    }

    private fun generateMountains(map: TileMap, rng: Random, width: Int, height: Int) {
        val cx = rng.nextInt(20, width - 20)
        val cy = rng.nextInt(20, height - 20)
        val len = rng.nextInt(10, 30)
        var x = cx; var y = cy
        repeat(len) {
            for (dy in -3..3) for (dx in -3..3) {
                if (rng.nextFloat() < 0.6f) {
                    val tx = x + dx; val ty = y + dy
                    if (tx in 1 until width - 1 && ty in 1 until height - 1) {
                        map.set(tx, ty, Tile(TileType.MOUNTAIN))
                    }
                }
            }
            x += rng.nextInt(-2, 3); y += rng.nextInt(-2, 3)
            x = x.coerceIn(5, width - 6); y = y.coerceIn(5, height - 6)
        }
    }

    private fun generateSandBanks(map: TileMap, width: Int, height: Int) {
        for (y in 1 until height - 1) for (x in 1 until width - 1) {
            if (map.get(x, y)?.type == TileType.GRASS) {
                val adjacentWater = listOf(
                    map.get(x - 1, y), map.get(x + 1, y),
                    map.get(x, y - 1), map.get(x, y + 1)
                ).any { it?.type == TileType.WATER }
                if (adjacentWater) map.set(x, y, Tile(TileType.SAND))
            }
        }
    }

    private fun generateTown(map: TileMap, rng: Random, cx: Int, cy: Int, name: String) {
        val size = 15
        // Stone floor plaza
        for (dy in -size..size) for (dx in -size..size) {
            val tx = cx + dx; val ty = cy + dy
            if (tx in 1 until map.width - 1 && ty in 1 until map.height - 1) {
                map.set(tx, ty, Tile(TileType.STONE_FLOOR))
            }
        }
        // Surrounding walls
        for (dx in -(size + 1)..(size + 1)) {
            map.set(cx + dx, cy - size - 1, Tile(TileType.WALL))
            map.set(cx + dx, cy + size + 1, Tile(TileType.WALL))
        }
        for (dy in -(size + 1)..(size + 1)) {
            map.set(cx - size - 1, cy + dy, Tile(TileType.WALL))
            map.set(cx + size + 1, cy + dy, Tile(TileType.WALL))
        }
        // Gates (openings)
        for (d in -2..2) {
            map.set(cx + d, cy - size - 1, Tile(TileType.DIRT))
            map.set(cx + d, cy + size + 1, Tile(TileType.DIRT))
            map.set(cx - size - 1, cy + d, Tile(TileType.DIRT))
            map.set(cx + size + 1, cy + d, Tile(TileType.DIRT))
        }
        // Buildings
        repeat(5) {
            val bx = cx + rng.nextInt(-size + 3, size - 3)
            val by = cy + rng.nextInt(-size + 3, size - 3)
            val bw = rng.nextInt(3, 6)
            val bh = rng.nextInt(3, 6)
            for (dy in 0..bh) for (dx in 0..bw) {
                if (dx == 0 || dy == 0 || dx == bw || dy == bh) {
                    map.set(bx + dx, by + dy, Tile(TileType.WALL))
                } else {
                    map.set(bx + dx, by + dy, Tile(TileType.STONE_FLOOR))
                }
            }
            // Door
            map.set(bx + bw / 2, by + bh, Tile(TileType.STONE_FLOOR))
        }
    }

    private fun generateDungeon(map: TileMap, rng: Random, cx: Int, cy: Int) {
        val size = 20
        // Large stone area
        for (dy in -size..size) for (dx in -size..size) {
            val tx = cx + dx; val ty = cy + dy
            if (tx in 1 until map.width - 1 && ty in 1 until map.height - 1) {
                map.set(tx, ty, Tile(TileType.DUNGEON_FLOOR))
            }
        }
        // Dungeon walls and rooms
        repeat(8) {
            val rx = cx + rng.nextInt(-size + 4, size - 4)
            val ry = cy + rng.nextInt(-size + 4, size - 4)
            val rw = rng.nextInt(4, 8)
            val rh = rng.nextInt(4, 8)
            for (dy in 0..rh) for (dx in 0..rw) {
                if (dx == 0 || dy == 0 || dx == rw || dy == rh) {
                    map.set(rx + dx, ry + dy, Tile(TileType.DUNGEON_WALL))
                }
            }
            map.set(rx + rw / 2, ry + rh, Tile(TileType.DUNGEON_FLOOR))
        }
    }
}
