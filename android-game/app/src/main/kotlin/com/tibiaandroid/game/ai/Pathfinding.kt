package com.tibiaandroid.game.ai

import com.tibiaandroid.game.map.Position
import com.tibiaandroid.game.map.TileMap
import java.util.PriorityQueue

object Pathfinding {
    private const val MAX_STEPS = 200

    fun findPath(map: TileMap, from: Position, to: Position): List<Position> {
        if (from == to) return emptyList()
        if (!map.isPassable(to.x, to.y)) {
            // Find nearest passable to target
            val alt = nearestPassable(map, to) ?: return emptyList()
            return findPath(map, from, alt)
        }

        data class Node(val pos: Position, val g: Int, val f: Int)

        val open = PriorityQueue<Node>(compareBy { it.f })
        val gScore = mutableMapOf(from to 0)
        val cameFrom = mutableMapOf<Position, Position>()

        open.add(Node(from, 0, from.manhattanTo(to)))

        while (open.isNotEmpty()) {
            val current = open.poll()!!
            if (current.pos == to) return reconstruct(cameFrom, to)
            if (gScore.size > MAX_STEPS) break

            for (neighbor in map.neighbors(current.pos)) {
                val tentG = (gScore[current.pos] ?: Int.MAX_VALUE) + 1
                if (tentG < (gScore[neighbor] ?: Int.MAX_VALUE)) {
                    gScore[neighbor] = tentG
                    cameFrom[neighbor] = current.pos
                    open.add(Node(neighbor, tentG, tentG + neighbor.manhattanTo(to)))
                }
            }
        }
        return emptyList()
    }

    private fun reconstruct(cameFrom: Map<Position, Position>, end: Position): List<Position> {
        val path = mutableListOf<Position>()
        var cur = end
        while (cameFrom.containsKey(cur)) {
            path.add(0, cur)
            cur = cameFrom[cur]!!
        }
        return path
    }

    private fun nearestPassable(map: TileMap, pos: Position): Position? {
        for (r in 1..5) {
            for (dy in -r..r) for (dx in -r..r) {
                val p = Position(pos.x + dx, pos.y + dy)
                if (map.isPassable(p.x, p.y)) return p
            }
        }
        return null
    }
}
