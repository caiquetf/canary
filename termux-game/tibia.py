#!/usr/bin/env python3
"""
Tibia Terminal — Offline RPG para Termux
Controles: WASD / setas para mover | f para atacar | p poção HP | m poção MP | i inventário | q sair
"""

import curses
import random
import math
import time
from dataclasses import dataclass, field
from typing import Optional, List, Tuple, Dict
from collections import deque
from heapq import heappush, heappop

# ─── PALETA DE CORES ─────────────────────────────────────────────────────────

def init_colors():
    curses.start_color()
    curses.use_default_colors()
    # pair_number, fg, bg
    curses.init_pair(1,  curses.COLOR_GREEN,   -1)  # grass
    curses.init_pair(2,  curses.COLOR_BLUE,    -1)  # water
    curses.init_pair(3,  curses.COLOR_WHITE,   -1)  # stone/wall
    curses.init_pair(4,  curses.COLOR_YELLOW,  -1)  # sand/gold/item
    curses.init_pair(5,  curses.COLOR_RED,     -1)  # monster/damage
    curses.init_pair(6,  curses.COLOR_CYAN,    -1)  # player/mana
    curses.init_pair(7,  curses.COLOR_MAGENTA, -1)  # special
    curses.init_pair(8,  curses.COLOR_BLACK,   curses.COLOR_RED)    # hp bar fill
    curses.init_pair(9,  curses.COLOR_BLACK,   curses.COLOR_BLUE)   # mp bar fill
    curses.init_pair(10, curses.COLOR_BLACK,   curses.COLOR_GREEN)  # xp bar fill
    curses.init_pair(11, curses.COLOR_BLACK,   curses.COLOR_WHITE)  # bar bg
    curses.init_pair(12, curses.COLOR_BLACK,   curses.COLOR_YELLOW) # crit
    curses.init_pair(13, curses.COLOR_WHITE,   curses.COLOR_BLACK)  # chat bg
    curses.init_pair(14, curses.COLOR_YELLOW,  curses.COLOR_BLACK)  # hud border

C_GRASS   = curses.color_pair(1)
C_WATER   = curses.color_pair(2)
C_STONE   = curses.color_pair(3)
C_GOLD    = curses.color_pair(4)
C_RED     = curses.color_pair(5)
C_CYAN    = curses.color_pair(6)
C_MAGENTA = curses.color_pair(7)
C_HPBAR   = curses.color_pair(8)
C_MPBAR   = curses.color_pair(9)
C_XPBAR   = curses.color_pair(10)
C_BARBG   = curses.color_pair(11)
C_CRIT    = curses.color_pair(12)
C_YELLOW  = curses.color_pair(4)

# ─── TILES ───────────────────────────────────────────────────────────────────

@dataclass
class TileInfo:
    ch: str
    color: int
    passable: bool

TILES = {
    'G':  TileInfo('.',  C_GRASS,   True),   # Grass
    'D':  TileInfo(',',  C_GOLD,    True),   # Dirt
    'S':  TileInfo('~',  C_STONE,   True),   # Stone floor
    'W':  TileInfo('≈',  C_WATER,   False),  # Water
    'X':  TileInfo('#',  C_STONE,   False),  # Wall
    'M':  TileInfo('^',  C_STONE,   False),  # Mountain
    'T':  TileInfo('T',  C_GRASS,   False),  # Tree
    'N':  TileInfo('"',  C_GOLD,    True),   # Sand
    'F':  TileInfo('_',  C_MAGENTA, True),   # Dungeon floor
    'V':  TileInfo('▓',  C_STONE,   False),  # Dungeon wall
    'L':  TileInfo('*',  C_RED,     False),  # Lava
}

# ─── MAPA ────────────────────────────────────────────────────────────────────

MAP_W, MAP_H = 120, 120

class WorldMap:
    def __init__(self, seed=None):
        rng = random.Random(seed or time.time())
        self.w = MAP_W
        self.h = MAP_H
        self.data = [['G'] * MAP_W for _ in range(MAP_H)]
        self._generate(rng)

    def _set(self, x, y, t):
        if 0 < x < self.w-1 and 0 < y < self.h-1:
            self.data[y][x] = t

    def get(self, x, y) -> str:
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.data[y][x]
        return 'M'

    def passable(self, x, y) -> bool:
        return TILES[self.get(x, y)].passable

    def _generate(self, rng):
        # Border mountains
        for x in range(self.w):
            self._set(x, 0, 'M'); self._set(x, self.h-1, 'M')
        for y in range(self.h):
            self._set(0, y, 'M'); self._set(self.w-1, y, 'M')

        # River
        rx = rng.randint(self.w//4, 3*self.w//4)
        for y in range(1, self.h-1):
            for dx in range(-2, 3):
                self._set(rx+dx, y, 'W')
            rx += rng.randint(-1, 1)
            rx = max(5, min(self.w-6, rx))

        # Sand near water
        for y in range(1, self.h-1):
            for x in range(1, self.w-1):
                if self.get(x, y) == 'G':
                    if any(self.get(x+dx, y+dy) == 'W'
                           for dx,dy in [(-1,0),(1,0),(0,-1),(0,1)]):
                        self._set(x, y, 'N')

        # Forests
        for _ in range(15):
            cx = rng.randint(10, self.w-10)
            cy = rng.randint(10, self.h-10)
            r = rng.randint(4, 10)
            for dy in range(-r, r+1):
                for dx in range(-r, r+1):
                    if dx*dx+dy*dy <= r*r and rng.random() < 0.65:
                        if self.get(cx+dx, cy+dy) == 'G':
                            self._set(cx+dx, cy+dy, 'T')

        # Mountains
        for _ in range(4):
            cx = rng.randint(15, self.w-15)
            cy = rng.randint(15, self.h-15)
            for _ in range(rng.randint(8, 20)):
                for dy in range(-3, 4):
                    for dx in range(-3, 4):
                        if rng.random() < 0.55:
                            self._set(cx+dx, cy+dy, 'M')
                cx += rng.randint(-2, 3)
                cy += rng.randint(-2, 3)

        # Towns
        self._town(30, 30, rng)
        self._town(90, 30, rng)
        self._town(60, 90, rng)

        # Dungeons
        self._dungeon(60, 55, rng)
        self._dungeon(30, 80, rng)

    def _town(self, cx, cy, rng):
        s = 10
        for dy in range(-s, s+1):
            for dx in range(-s, s+1):
                self._set(cx+dx, cy+dy, 'S')
        for dx in range(-(s+1), s+2):
            self._set(cx+dx, cy-s-1, 'X')
            self._set(cx+dx, cy+s+1, 'X')
        for dy in range(-(s+1), s+2):
            self._set(cx-s-1, cy+dy, 'X')
            self._set(cx+s+1, cy+dy, 'X')
        # Gates
        for d in (-1, 0, 1):
            self._set(cx+d, cy-s-1, 'D')
            self._set(cx+d, cy+s+1, 'D')
            self._set(cx-s-1, cy+d, 'D')
            self._set(cx+s+1, cy+d, 'D')
        # Buildings
        for _ in range(4):
            bx = cx + rng.randint(-s+3, s-3)
            by = cy + rng.randint(-s+3, s-3)
            bw = rng.randint(3, 5); bh = rng.randint(3, 5)
            for dy in range(bh+1):
                for dx in range(bw+1):
                    t = 'X' if (dx==0 or dy==0 or dx==bw or dy==bh) else 'S'
                    self._set(bx+dx, by+dy, t)
            self._set(bx+bw//2, by+bh, 'S')  # door

    def _dungeon(self, cx, cy, rng):
        s = 14
        for dy in range(-s, s+1):
            for dx in range(-s, s+1):
                self._set(cx+dx, cy+dy, 'F')
        for _ in range(6):
            rx = cx + rng.randint(-s+3, s-3)
            ry = cy + rng.randint(-s+3, s-3)
            rw = rng.randint(4, 7); rh = rng.randint(3, 6)
            for dy in range(rh+1):
                for dx in range(rw+1):
                    if dx==0 or dy==0 or dx==rw or dy==rh:
                        self._set(rx+dx, ry+dy, 'V')

# ─── PATHFINDING A* ──────────────────────────────────────────────────────────

def astar(wmap: WorldMap, sx, sy, gx, gy, max_steps=150):
    if (sx, sy) == (gx, gy):
        return []
    if not wmap.passable(gx, gy):
        # find nearest passable
        for r in range(1, 4):
            for dy in range(-r, r+1):
                for dx in range(-r, r+1):
                    if wmap.passable(gx+dx, gy+dy):
                        gx, gy = gx+dx, gy+dy
                        break
                else: continue
                break
            else: continue
            break

    open_set = []
    heappush(open_set, (0, sx, sy))
    came_from = {}
    g_score = {(sx, sy): 0}
    dirs = [(-1,0),(1,0),(0,-1),(0,1),(-1,-1),(1,-1),(-1,1),(1,1)]

    while open_set and len(g_score) < max_steps:
        _, cx, cy = heappop(open_set)
        if (cx, cy) == (gx, gy):
            path = []
            cur = (gx, gy)
            while cur in came_from:
                path.append(cur)
                cur = came_from[cur]
            path.reverse()
            return path
        for dx, dy in dirs:
            nx, ny = cx+dx, cy+dy
            if not wmap.passable(nx, ny):
                continue
            ng = g_score[(cx, cy)] + 1
            if ng < g_score.get((nx, ny), 999999):
                g_score[(nx, ny)] = ng
                came_from[(nx, ny)] = (cx, cy)
                f = ng + abs(nx-gx) + abs(ny-gy)
                heappush(open_set, (f, nx, ny))
    return []

# ─── DADOS DO JOGO ───────────────────────────────────────────────────────────

VOCATIONS = {
    'Knight':   {'hp': 15, 'mp': 5,  'atk': 3, 'def': 3, 'color': C_RED,     'desc': 'Guerreiro tank. Alto HP/DEF.'},
    'Paladin':  {'hp': 10, 'mp': 10, 'atk': 2, 'def': 2, 'color': C_CYAN,    'desc': 'Equilíbrio de luta e magia.'},
    'Sorcerer': {'hp': 5,  'mp': 30, 'atk': 1, 'def': 1, 'color': C_MAGENTA, 'desc': 'Magia ofensiva devastadora.'},
    'Druid':    {'hp': 8,  'mp': 25, 'atk': 1, 'def': 2, 'color': C_GRASS,   'desc': 'Cura e natureza.'},
}

ITEMS = {
    # id: (name, type, atk, def, hp_restore, mp_restore, value, min_level)
    1:  ('Sword',          'weapon', 10, 0,   0,   0,  50,  1),
    2:  ('Long Sword',     'weapon', 18, 0,   0,   0, 150, 10),
    3:  ('2H Sword',       'weapon', 28, 0,   0,   0, 400, 20),
    4:  ('Axe',            'weapon',  9, 0,   0,   0,  40,  1),
    5:  ('Battle Axe',     'weapon', 20, 0,   0,   0, 200, 15),
    6:  ('Club',           'weapon',  6, 0,   0,   0,  20,  1),
    7:  ('Mace',           'weapon', 14, 0,   0,   0, 120,  8),
    8:  ('Leather Armor',  'armor',   0, 8,   0,   0,  60,  1),
    9:  ('Chain Armor',    'armor',   0,14,   0,   0, 200, 10),
    10: ('Plate Armor',    'armor',   0,22,   0,   0, 800, 25),
    11: ('Leather Helmet', 'helmet',  0, 3,   0,   0,  30,  1),
    12: ('Iron Helmet',    'helmet',  0, 8,   0,   0, 150, 10),
    13: ('Wooden Shield',  'shield',  0, 5,   0,   0,  25,  1),
    14: ('Iron Shield',    'shield',  0,12,   0,   0, 180,  8),
    20: ('Health Potion',  'potion',  0, 0, 100,   0,  50,  1),
    21: ('Str HP Potion',  'potion',  0, 0, 250,   0, 120, 10),
    22: ('Mana Potion',    'potion',  0, 0,   0, 100,  50,  1),
    23: ('Str MP Potion',  'potion',  0, 0,   0, 250, 120, 10),
    30: ('Gold Coin',      'gold',    0, 0,   0,   0,   1,  1),
    31: ('Platinum Coin',  'gold',    0, 0,   0,   0, 100,  1),
}

# Monstros: name, ch, color, hp, atk, def, spd, xp, aggro_range, loot, level
MONSTER_TEMPLATES = [
    ('Rat',          'r', C_GOLD,    20,   5,  2, 3,    5,  4, [(30, 0.8)],                  1),
    ('Snake',        's', C_GRASS,   30,   8,  3, 4,   10,  5, [(30, 0.5)],                  2),
    ('Wolf',         'w', C_STONE,   80,  15,  8, 5,   25,  5, [(30, 1.0), (30, 0.5)],       5),
    ('Orc',          'O', C_GRASS,  150,  22, 14, 4,   60,  6, [(4, 0.2), (30, 1.0)],       10),
    ('Troll',        'T', C_GOLD,   200,  28, 18, 3,  100,  6, [(8, 0.15), (30, 1.0)],      15),
    ('Skeleton',     'K', C_STONE,  120,  20, 10, 4,   50,  5, [(30, 1.0), (20, 0.1)],       8),
    ('Zombie',       'Z', C_MAGENTA,180,  25, 12, 2,   80,  5, [(30, 1.0), (20, 0.15)],     12),
    ('Giant Spider', 'S', C_STONE,  250,  35, 20, 5,  150,  6, [(9, 0.1), (22, 0.2)],       18),
    ('Slime',        'j', C_GRASS,   60,  10,  5, 2,   15,  4, [(30, 0.6)],                  3),
    ('Minotaur',     'M', C_RED,    400,  45, 28, 4,  300,  7, [(2, 0.15), (31, 0.5)],      25),
    ('Dragon',       'D', C_RED,   1500,  80, 50, 5, 2000,  8, [(10, 0.05), (31, 1.0)],     50),
    ('Demon',        '&', C_MAGENTA,3000,120, 80, 6, 6000, 10, [(31, 1.0), (23, 0.9)],      80),
]

# ─── ENTIDADES ───────────────────────────────────────────────────────────────

_id_counter = 0
def next_id():
    global _id_counter
    _id_counter += 1
    return _id_counter

class Entity:
    def __init__(self, x, y, name, max_hp, max_mp=0, atk=10, defense=5, spd=4):
        self.id = next_id()
        self.x = x; self.y = y
        self.name = name
        self.max_hp = max_hp; self.hp = max_hp
        self.max_mp = max_mp; self.mp = max_mp
        self.atk = atk; self.defense = defense; self.spd = spd
        self.alive = True
        self.move_cd = 0; self.atk_cd = 0

    def take_damage(self, amount):
        mit = max(1, amount - self.defense // 2)
        self.hp = max(0, self.hp - mit)
        if self.hp == 0:
            self.alive = False
        return mit

    def heal(self, amount):
        self.hp = min(self.max_hp, self.hp + amount)

    def restore_mp(self, amount):
        self.mp = min(self.max_mp, self.mp + amount)

class Player(Entity):
    def __init__(self, x, y, name, vocation):
        v = VOCATIONS[vocation]
        super().__init__(x, y, name, 150, 50, 15, 10, 4)
        self.vocation = vocation
        self.voc_data = v
        self.level = 1
        self.xp = 0
        self.gold = 200
        self.inventory: Dict[int, int] = {}   # item_id -> count
        self.equipped = {
            'weapon': None, 'armor': None, 'helmet': None,
            'shield': None, 'legs': None, 'boots': None,
        }
        # Starter gear
        self.add_item(1); self.equip_item(1)
        self.add_item(8); self.equip_item(8)
        self.add_item(11); self.equip_item(11)
        for _ in range(5): self.add_item(20)
        for _ in range(3): self.add_item(22)

    @property
    def total_atk(self):
        base = self.atk
        eq = self.equipped.get('weapon')
        if eq: base += ITEMS[eq][2]
        return base

    @property
    def total_def(self):
        base = self.defense
        for slot in ['armor','shield','helmet','legs','boots']:
            eq = self.equipped.get(slot)
            if eq: base += ITEMS[eq][3]
        return base

    @property
    def xp_next(self):
        return self.level * self.level * 100

    def add_item(self, item_id, count=1):
        idata = ITEMS.get(item_id)
        if not idata: return
        if idata[1] == 'gold':
            self.gold += count * idata[6]
            return
        self.inventory[item_id] = self.inventory.get(item_id, 0) + count

    def remove_item(self, item_id, count=1):
        if self.inventory.get(item_id, 0) < count: return False
        self.inventory[item_id] -= count
        if self.inventory[item_id] == 0:
            del self.inventory[item_id]
        return True

    def equip_item(self, item_id):
        idata = ITEMS.get(item_id)
        if not idata: return False
        if self.level < idata[7]: return False
        slot = idata[1]
        if slot in self.equipped:
            self.equipped[slot] = item_id
            return True
        return False

    def use_potion(self, item_id):
        idata = ITEMS.get(item_id)
        if not idata or idata[1] != 'potion': return False
        if not self.remove_item(item_id): return False
        if idata[4]: self.heal(idata[4])
        if idata[5]: self.restore_mp(idata[5])
        return True

    def gain_xp(self, amount):
        self.xp += amount
        leveled = False
        while self.xp >= self.xp_next:
            self.xp -= self.xp_next
            self.level += 1
            v = self.voc_data
            self.max_hp += v['hp']; self.max_mp += v['mp']
            self.atk += v['atk']; self.defense += v['def']
            self.hp = self.max_hp; self.mp = self.max_mp
            leveled = True
        return leveled

class Monster(Entity):
    def __init__(self, x, y, template_idx):
        name, ch, color, hp, atk, defense, spd, xp, aggro, loot, lvl = MONSTER_TEMPLATES[template_idx]
        super().__init__(x, y, name, hp, 0, atk, defense, spd)
        self.ch = ch
        self.color = color
        self.xp_reward = xp
        self.aggro_range = aggro
        self.loot_table = loot
        self.min_level = lvl
        self.spawn_x = x; self.spawn_y = y
        self.path = []
        self.path_age = 0
        self.target_id = None
        self.wander_cd = random.randint(0, 15)

    def drop_loot(self):
        drops = []
        for (item_id, chance) in self.loot_table:
            if random.random() < chance:
                drops.append(item_id)
        return drops

# ─── MUNDO ───────────────────────────────────────────────────────────────────

class World:
    def __init__(self, player: Player):
        self.player = player
        self.map = WorldMap()
        self.monsters: List[Monster] = []
        self.chat: deque = deque(maxlen=50)
        self.floating: List[list] = []  # [text, x, y, color, ttl]
        self.tick = 0
        self.regen_tick = 0
        self.spawned_zones = set()

        # Ensure player spawns on passable tile
        self.player.x, self.player.y = 30, 30
        self._fix_pos(self.player)

        self._spawn_zone(self.player.x, self.player.y, 8, 20)
        self.log("Bem-vindo a Tibia Terminal! WASD=mover f=atacar p=poção i=inv", C_GOLD)

    def _fix_pos(self, entity):
        if not self.map.passable(entity.x, entity.y):
            for r in range(1, 10):
                for dy in range(-r, r+1):
                    for dx in range(-r, r+1):
                        if self.map.passable(entity.x+dx, entity.y+dy):
                            entity.x += dx; entity.y += dy
                            return

    def log(self, msg, color=None):
        self.chat.append((msg, color or C_STONE))

    def float_text(self, text, x, y, color):
        self.floating.append([text, x, y, color, 25])

    def update(self):
        self.tick += 1
        self.regen_tick += 1

        # Dynamic spawn
        zk = f"{self.player.x//20}_{self.player.y//20}"
        if zk not in self.spawned_zones:
            self.spawned_zones.add(zk)
            self._spawn_zone(self.player.x, self.player.y, 8, 20)

        # Remove dead, collect loot
        dead = [m for m in self.monsters if not m.alive]
        for m in dead:
            drops = m.drop_loot()
            for iid in drops:
                self.player.add_item(iid)
            if drops:
                names = [ITEMS[i][0] for i in drops if i in ITEMS]
                self.log(f"{m.name} largou: {', '.join(names)}", C_GOLD)
            leveled = self.player.gain_xp(m.xp_reward)
            self.log(f"+{m.xp_reward} XP de {m.name}.", C_GRASS)
            if leveled:
                self.log(f">>> LEVEL UP! Você é nível {self.player.level}! <<<", C_GOLD)
        self.monsters = [m for m in self.monsters if m.alive]

        # AI
        self._update_ai()

        # Regen every ~3s (90 ticks @ 30fps-ish)
        if self.regen_tick >= 90:
            self.regen_tick = 0
            self.player.restore_mp(max(1, self.player.max_mp // 20))
            near_enemy = any(
                abs(m.x-self.player.x) + abs(m.y-self.player.y) <= 4
                for m in self.monsters
            )
            if not near_enemy:
                self.player.heal(max(1, self.player.max_hp // 30))

        # Floating text decay
        self.floating = [[t,x,y,c,ttl-1] for t,x,y,c,ttl in self.floating if ttl > 0]

    def _update_ai(self):
        px, py = self.player.x, self.player.y
        for m in self.monsters:
            m.move_cd = max(0, m.move_cd - 1)
            m.atk_cd  = max(0, m.atk_cd  - 1)
            dist = max(abs(m.x-px), abs(m.y-py))

            if dist <= m.aggro_range:
                # Chase & attack
                if dist <= 1 and m.atk_cd == 0:
                    m.atk_cd = max(1, 10 - m.spd // 2)
                    dmg = max(1, m.atk + random.randint(-3, 4))
                    actual = self.player.take_damage(dmg)
                    self.float_text(f"-{actual}", px, py, C_RED)
                elif dist > 1 and m.move_cd == 0:
                    move_speed = max(1, 8 - m.spd // 2)
                    m.move_cd = move_speed
                    self._move_toward(m, px, py)
            else:
                # Wander
                m.wander_cd -= 1
                if m.wander_cd <= 0 and m.move_cd == 0:
                    m.wander_cd = random.randint(10, 25)
                    m.move_cd = max(1, 8 - m.spd // 2)
                    self._wander(m)

    def _move_toward(self, m, tx, ty):
        m.path_age += 1
        if m.path_age > 5 or not m.path:
            m.path = astar(self.map, m.x, m.y, tx, ty)
            m.path_age = 0
        if m.path:
            nx, ny = m.path[0]
            # Check no other monster is there
            occupied = any(o.x==nx and o.y==ny for o in self.monsters if o.id!=m.id)
            if not occupied:
                m.x, m.y = nx, ny
                m.path = m.path[1:]

    def _wander(self, m):
        dx, dy = random.choice([(-1,0),(1,0),(0,-1),(0,1)])
        nx, ny = m.x+dx, m.y+dy
        dist_spawn = max(abs(nx-m.spawn_x), abs(ny-m.spawn_y))
        if self.map.passable(nx, ny) and dist_spawn <= 8:
            m.x, m.y = nx, ny

    def player_attack(self):
        px, py = self.player.x, self.player.y
        # Find adjacent monster
        targets = [m for m in self.monsters
                   if m.alive and max(abs(m.x-px), abs(m.y-py)) <= 1]
        if not targets:
            self.log("Nenhum inimigo adjacente para atacar.", C_STONE)
            return
        target = min(targets, key=lambda m: m.hp)
        miss = random.random() < 0.05
        if miss:
            self.log(f"Você errou {target.name}!", C_STONE)
            return
        crit = random.random() < 0.10
        base = self.player.total_atk + random.randint(-3, 4)
        dmg = base * 2 if crit else base
        actual = target.take_damage(dmg)
        msg = f"{'CRÍTICO! ' if crit else ''}Você causa {actual} de dano em {target.name}."
        self.log(msg, C_GOLD if crit else C_STONE)
        self.float_text(f"-{actual}", target.x, target.y, C_CRIT if crit else C_RED)
        if not target.alive:
            self.log(f"Você matou {target.name}!", C_GRASS)

    def use_hp_potion(self):
        for iid in [21, 20]:
            if iid in self.player.inventory:
                old = self.player.hp
                if self.player.use_potion(iid):
                    gained = self.player.hp - old
                    self.log(f"Você bebe {ITEMS[iid][0]} e recupera {gained} HP.", C_RED)
                    return
        self.log("Sem poções de HP!", C_RED)

    def use_mp_potion(self):
        for iid in [23, 22]:
            if iid in self.player.inventory:
                old = self.player.mp
                if self.player.use_potion(iid):
                    gained = self.player.mp - old
                    self.log(f"Você bebe {ITEMS[iid][0]} e recupera {gained} MP.", C_CYAN)
                    return
        self.log("Sem poções de MP!", C_CYAN)

    def _spawn_zone(self, cx, cy, min_d, max_d):
        dist_from_origin = max(abs(cx-30), abs(cy-30))
        max_monster_level = max(1, dist_from_origin // 8 + 2)
        eligible = [i for i, t in enumerate(MONSTER_TEMPLATES)
                    if t[10] <= max_monster_level]
        if not eligible:
            eligible = [0]
        count = random.randint(3, 7)
        for _ in range(count):
            angle = random.uniform(0, 2*math.pi)
            dist = random.randint(min_d, max_d)
            mx = int(cx + dist * math.cos(angle))
            my = int(cy + dist * math.sin(angle))
            if self.map.passable(mx, my):
                idx = random.choice(eligible)
                self.monsters.append(Monster(mx, my, idx))

# ─── RENDERER TUI ─────────────────────────────────────────────────────────────

def draw_bar(win, y, x, width, filled_pct, color_fill, color_bg, label=''):
    filled = int(width * max(0, min(1, filled_pct)))
    empty  = width - filled
    win.addstr(y, x, '█' * filled, color_fill | curses.A_BOLD)
    win.addstr(y, x+filled, '░' * empty, color_bg)
    if label:
        lx = x + max(0, (width - len(label)) // 2)
        try:
            win.addstr(y, lx, label, curses.A_BOLD)
        except: pass

def render(stdscr, world: World, view_x, view_y):
    stdscr.erase()
    sh, sw = stdscr.getmaxyx()

    MAP_COLS = sw - 30  # right panel width = 30
    MAP_ROWS = sh - 8   # bottom panel height = 8

    # ── MAP ──────────────────────────────────────────────────────────────────
    px, py = world.player.x, world.player.y
    cam_x = px - MAP_COLS // 2
    cam_y = py - MAP_ROWS // 2

    for row in range(MAP_ROWS):
        for col in range(MAP_COLS):
            wx = cam_x + col
            wy = cam_y + row
            tile_key = world.map.get(wx, wy)
            tile = TILES[tile_key]
            try:
                stdscr.addch(row, col, tile.ch, tile.color)
            except: pass

    # Draw monsters
    for m in world.monsters:
        sc = m.x - cam_x
        sr = m.y - cam_y
        if 0 <= sc < MAP_COLS and 0 <= sr < MAP_ROWS:
            try:
                stdscr.addch(sr, sc, m.ch, m.color | curses.A_BOLD)
            except: pass

    # Draw player
    sc = px - cam_x
    sr = py - cam_y
    if 0 <= sc < MAP_COLS and 0 <= sr < MAP_ROWS:
        try:
            stdscr.addch(sr, sc, '@', C_CYAN | curses.A_BOLD)
        except: pass

    # Draw floating texts
    for text, fx, fy, color, ttl in world.floating:
        sc = fx - cam_x
        sr = fy - cam_y - (25 - ttl) // 4
        if 0 <= sc < MAP_COLS-len(text) and 0 <= sr < MAP_ROWS:
            try:
                stdscr.addstr(sr, sc, text, color | curses.A_BOLD)
            except: pass

    # ── RIGHT PANEL ──────────────────────────────────────────────────────────
    p = world.player
    px_col = MAP_COLS + 1

    def rline(r, text, attr=0):
        if r < sh:
            try:
                stdscr.addstr(r, px_col, text[:sw-px_col-1], attr)
            except: pass

    rline(0,  "╔══ PERSONAGEM ══╗", C_YELLOW)
    rline(1,  f" {p.name[:14]}", C_CYAN | curses.A_BOLD)
    rline(2,  f" {p.vocation}  Lv.{p.level}", C_YELLOW)
    rline(3,  f" Gold: {p.gold}", C_GOLD)
    rline(4,  "")

    # HP bar
    rline(5, " HP:", C_RED)
    bar_w = min(20, sw - px_col - 2)
    draw_bar(stdscr, 5, px_col+4, bar_w, p.hp/p.max_hp, C_HPBAR, C_BARBG,
             f"{p.hp}/{p.max_hp}")

    # MP bar
    rline(6, " MP:", C_CYAN)
    mp_pct = p.mp/p.max_mp if p.max_mp > 0 else 0
    draw_bar(stdscr, 6, px_col+4, bar_w, mp_pct, C_MPBAR, C_BARBG,
             f"{p.mp}/{p.max_mp}")

    # XP bar
    xp_pct = p.xp/p.xp_next if p.xp_next > 0 else 0
    rline(7, " XP:", C_GRASS)
    draw_bar(stdscr, 7, px_col+4, bar_w, xp_pct, C_XPBAR, C_BARBG,
             f"{p.xp}/{p.xp_next}")

    rline(8,  "")
    rline(9,  f" ATK: {p.total_atk}  DEF: {p.total_def}", C_STONE)
    rline(10, "")
    rline(11, "╠══ EQUIPAMENTOS ╣", C_YELLOW)

    eq_labels = [
        ('weapon', '⚔ Arma'),
        ('armor',  '🛡 Armor'),
        ('helmet', '⛑ Helmet'),
        ('shield', '🛡 Shield'),
    ]
    row = 12
    for slot, label in eq_labels:
        eq = p.equipped.get(slot)
        if eq:
            name = ITEMS[eq][0][:12]
            rline(row, f" {label}: {name}", C_GRASS)
        else:
            rline(row, f" {label}: —", C_STONE)
        row += 1

    rline(row, ""); row += 1
    rline(row, "╠══ POCÕES ═══════╣", C_YELLOW); row += 1
    hp_pots = sum(v for k,v in p.inventory.items()
                  if k in ITEMS and ITEMS[k][1]=='potion' and ITEMS[k][4] > 0)
    mp_pots = sum(v for k,v in p.inventory.items()
                  if k in ITEMS and ITEMS[k][1]=='potion' and ITEMS[k][5] > 0)
    rline(row, f" HP Pots: {hp_pots}  [p]", C_RED); row += 1
    rline(row, f" MP Pots: {mp_pots}  [m]", C_CYAN); row += 1
    rline(row, ""); row += 1
    rline(row, "╠══ CONTROLES ════╣", C_YELLOW); row += 1
    controls = ["[WASD/↑↓←→] Mover", "[f] Atacar adj.",
                "[p] Poção HP",       "[m] Poção MP",
                "[i] Inventário",     "[q] Sair"]
    for ctrl in controls:
        if row < sh - 1:
            rline(row, f" {ctrl}", C_STONE)
            row += 1

    # ── MONSTERS NEARBY ──────────────────────────────────────────────────────
    nearby = sorted(
        [m for m in world.monsters if abs(m.x-p.x)+abs(m.y-p.y) <= 10],
        key=lambda m: abs(m.x-p.x)+abs(m.y-p.y)
    )[:4]
    if nearby and row < sh - 1:
        rline(row, "╠══ INIMIGOS ══════╣", C_RED); row += 1
        for m in nearby:
            if row < sh - 1:
                hp_pct = m.hp / m.max_hp
                bar = '█' * int(hp_pct * 8) + '░' * (8 - int(hp_pct * 8))
                rline(row, f" {m.ch} {m.name[:8]:8} [{bar}]", m.color)
                row += 1

    # ── CHAT LOG ──────────────────────────────────────────────────────────────
    chat_top = sh - 8
    stdscr.hline(chat_top-1, 0, curses.ACS_HLINE, sw-1)
    try:
        stdscr.addstr(chat_top-1, 0, "── MENSAGENS ", C_YELLOW | curses.A_BOLD)
    except: pass

    messages = list(world.chat)[-7:]
    for i, (msg, color) in enumerate(messages):
        r = chat_top + i
        if r < sh - 1:
            try:
                stdscr.addstr(r, 0, f" {msg[:sw-3]}", color)
            except: pass

    # Status bar
    pos_str = f" Pos({p.x},{p.y}) | Monstros:{len(world.monsters)} | Tick:{world.tick}"
    try:
        stdscr.addstr(sh-1, 0, pos_str[:sw-1], C_STONE)
    except: pass

    # Map border
    try:
        stdscr.vline(0, MAP_COLS, curses.ACS_VLINE, sh-1)
    except: pass

    stdscr.refresh()

# ─── TELA DE INVENTÁRIO ──────────────────────────────────────────────────────

def show_inventory(stdscr, player: Player):
    sh, sw = stdscr.getmaxyx()
    while True:
        stdscr.erase()
        try:
            stdscr.addstr(0, 0, f"══ INVENTÁRIO DE {player.name} (Lv.{player.level}) ══", C_GOLD | curses.A_BOLD)
            stdscr.addstr(1, 0, f"Gold: {player.gold}", C_GOLD)
            stdscr.addstr(2, 0, f"ATK: {player.total_atk}  DEF: {player.total_def}", C_CYAN)
            stdscr.addstr(3, 0, "─" * (sw-1))

            row = 4
            stdscr.addstr(row, 0, "EQUIPADO:", C_YELLOW | curses.A_BOLD); row += 1
            eq_list = [(s, i) for s, i in player.equipped.items() if i]
            if not eq_list:
                stdscr.addstr(row, 2, "(nada)", C_STONE); row += 1
            for slot, iid in eq_list:
                idata = ITEMS[iid]
                extra = f"ATK+{idata[2]}" if idata[2] else f"DEF+{idata[3]}"
                stdscr.addstr(row, 2, f"{slot}: {idata[0]} ({extra})", C_GRASS)
                row += 1

            row += 1
            stdscr.addstr(row, 0, "ITENS:", C_YELLOW | curses.A_BOLD); row += 1

            items_list = [(iid, cnt) for iid, cnt in player.inventory.items() if iid in ITEMS]
            if not items_list:
                stdscr.addstr(row, 2, "(vazio)", C_STONE); row += 1
            for idx, (iid, cnt) in enumerate(items_list):
                idata = ITEMS[iid]
                line = f"[{idx+1}] {idata[0]} x{cnt}"
                itype = idata[1]
                if itype == 'potion':
                    restore = f"HP+{idata[4]}" if idata[4] else f"MP+{idata[5]}"
                    line += f"  {restore}  (pressione {idx+1} p/ usar)"
                elif itype in ('weapon','armor','helmet','shield','legs','boots'):
                    extra = f"ATK+{idata[2]}" if idata[2] else f"DEF+{idata[3]}"
                    req = f"  reqLv{idata[7]}" if idata[7] > 1 else ""
                    line += f"  {extra}{req}  (pressione {idx+1} p/ equipar)"
                if row < sh - 2:
                    stdscr.addstr(row, 2, line[:sw-3], C_STONE)
                    row += 1

            stdscr.addstr(sh-2, 0, "─" * (sw-1))
            stdscr.addstr(sh-1, 0, "Pressione número p/ usar/equipar  |  ESC/i = fechar", C_YELLOW)
            stdscr.refresh()

            key = stdscr.getch()
            if key in (27, ord('i'), ord('q')):
                break
            elif ord('1') <= key <= ord('9'):
                n = key - ord('1')
                if n < len(items_list):
                    iid, _ = items_list[n]
                    idata = ITEMS[iid]
                    if idata[1] == 'potion':
                        player.use_potion(iid)
                    elif idata[1] in ('weapon','armor','helmet','shield','legs','boots'):
                        player.equip_item(iid)
        except curses.error:
            break

# ─── TELA DE SELEÇÃO DE PERSONAGEM ──────────────────────────────────────────

def character_select(stdscr):
    curses.curs_set(1)
    sh, sw = stdscr.getmaxyx()
    voc_list = list(VOCATIONS.keys())
    sel_voc = 0
    name = "Herói"

    while True:
        stdscr.erase()
        try:
            title = "TIBIA TERMINAL — OFFLINE RPG"
            stdscr.addstr(1, max(0, sw//2-len(title)//2), title, C_GOLD | curses.A_BOLD)
            stdscr.addstr(2, max(0, sw//2-14), "por caiquetf — Powered by IA", C_STONE)

            stdscr.addstr(4, 4, "Nome do personagem:", C_YELLOW)
            stdscr.addstr(5, 4, f"> {name}_", C_CYAN | curses.A_BOLD)

            stdscr.addstr(7, 4, "Escolha a vocação (← → para mudar):", C_YELLOW)
            for i, voc in enumerate(voc_list):
                attr = C_GOLD | curses.A_BOLD if i == sel_voc else C_STONE
                mark = "►" if i == sel_voc else " "
                stdscr.addstr(8+i, 6, f"{mark} {voc}", attr)

            v = voc_list[sel_voc]
            vdata = VOCATIONS[v]
            stdscr.addstr(8+len(voc_list)+1, 4, f"Descrição: {vdata['desc']}", C_CYAN)
            stdscr.addstr(8+len(voc_list)+2, 4,
                f"HP/lv:{vdata['hp']}  MP/lv:{vdata['mp']}  ATK/lv:{vdata['atk']}  DEF/lv:{vdata['def']}",
                C_STONE)

            stdscr.addstr(sh-3, 4, "Digite o nome e pressione ENTER para começar", C_YELLOW)
            stdscr.addstr(sh-2, 4, "← → para mudar vocação | Backspace para editar nome", C_STONE)
            stdscr.refresh()
        except curses.error:
            pass

        key = stdscr.getch()
        if key == curses.KEY_LEFT:
            sel_voc = (sel_voc - 1) % len(voc_list)
        elif key == curses.KEY_RIGHT:
            sel_voc = (sel_voc + 1) % len(voc_list)
        elif key == curses.KEY_BACKSPACE or key == 127:
            name = name[:-1]
        elif key == ord('\n') or key == curses.KEY_ENTER:
            break
        elif 32 <= key <= 126 and len(name) < 15:
            name += chr(key)

    curses.curs_set(0)
    return name.strip() or "Herói", voc_list[sel_voc]

# ─── LOOP PRINCIPAL ──────────────────────────────────────────────────────────

def main(stdscr):
    curses.curs_set(0)
    stdscr.nodelay(True)
    stdscr.keypad(True)
    init_colors()

    name, vocation = character_select(stdscr)

    player = Player(30, 30, name, vocation)
    world = World(player)

    move_keys = {
        ord('w'): (0,-1), ord('W'): (0,-1), curses.KEY_UP:    (0,-1),
        ord('s'): (0, 1), ord('S'): (0, 1), curses.KEY_DOWN:  (0, 1),
        ord('a'): (-1,0), ord('A'): (-1,0), curses.KEY_LEFT:  (-1,0),
        ord('d'): (1, 0), ord('D'): (1, 0), curses.KEY_RIGHT: (1, 0),
    }

    move_cd = 0
    move_held = None
    last_time = time.monotonic()

    while True:
        now = time.monotonic()
        dt = now - last_time

        # Process input
        keys_pressed = []
        while True:
            k = stdscr.getch()
            if k == -1:
                break
            keys_pressed.append(k)

        for k in keys_pressed:
            if k == ord('q') or k == ord('Q'):
                return
            elif k == ord('f') or k == ord('F'):
                world.player_attack()
            elif k == ord('p') or k == ord('P'):
                world.use_hp_potion()
            elif k == ord('m'):
                world.use_mp_potion()
            elif k == ord('i') or k == ord('I'):
                show_inventory(stdscr, player)
            elif k in move_keys:
                move_held = move_keys[k]
                # immediate first step
                if move_cd <= 0:
                    dx, dy = move_held
                    nx, ny = player.x+dx, player.y+dy
                    if world.map.passable(nx, ny):
                        player.x, player.y = nx, ny
                    move_cd = 0.15

        # Held movement
        if move_held and move_cd <= 0:
            dx, dy = move_held
            nx, ny = player.x+dx, player.y+dy
            if world.map.passable(nx, ny):
                player.x, player.y = nx, ny
            move_cd = 0.15

        move_cd = max(0, move_cd - dt)

        # Death screen
        if not player.alive:
            stdscr.erase()
            sh, sw = stdscr.getmaxyx()
            msg = "VOCÊ MORREU"
            sub = f"Chegou ao nível {player.level} com {player.gold} gold."
            sub2 = "Pressione q para sair."
            try:
                stdscr.addstr(sh//2-1, sw//2-len(msg)//2, msg, C_RED | curses.A_BOLD)
                stdscr.addstr(sh//2+1, sw//2-len(sub)//2, sub, C_GOLD)
                stdscr.addstr(sh//2+2, sw//2-len(sub2)//2, sub2, C_STONE)
            except: pass
            stdscr.refresh()
            while True:
                k = stdscr.getch()
                if k == ord('q'): return
                time.sleep(0.05)

        # Update world
        world.update()

        # Render
        render(stdscr, world, 0, 0)

        last_time = now
        time.sleep(0.033)  # ~30fps


if __name__ == '__main__':
    curses.wrapper(main)
