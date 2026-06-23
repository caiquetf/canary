# Tibia Terminal — Como jogar no Termux

## Instalação (1 vez só)

```bash
# 1. Instalar o Termux pela F-Droid (não pela Play Store — versão desatualizada)
#    https://f-droid.org/packages/com.termux/

# 2. Dentro do Termux:
pkg update && pkg upgrade
pkg install python git

# 3. Clonar o repositório
git clone https://github.com/caiquetf/canary.git
cd canary/termux-game
```

## Jogar

```bash
python tibia.py
```

**Sem dependências extras** — só Python padrão.

## Controles

| Tecla | Ação |
|-------|------|
| `W A S D` ou `↑ ↓ ← →` | Mover |
| `F` | Atacar (monstro adjacente) |
| `P` | Usar poção de HP |
| `M` | Usar poção de MP |
| `I` | Abrir inventário |
| `Q` | Sair |

## Dica: teclado no Termux

- Ative o teclado extra do Termux: deslize da esquerda para exibir a gaveta e toque em "Keyboard"
- Ou use um teclado físico bluetooth
- Funciona melhor em tela maior (tablet ou celular na horizontal)

## Legenda do mapa

```
.  Grama (passável)
,  Terra (passável)
_  Chão de masmorra
~  Água (bloqueado)
T  Árvore (bloqueado)
^  Montanha (bloqueado)
#  Parede (bloqueado)
@  Você
r  Rato     s  Cobra    w  Lobo
O  Orc      T  Troll    K  Esqueleto
Z  Zumbi    S  Aranha   D  Dragão
M  Minotauro  &  Demônio
```

## Sistemas de jogo

- **4 vocações**: Knight, Paladin, Sorcerer, Druid
- **11 tipos de monstros** com IA (A* pathfinding, aggro, errância)
- **Loot e XP** ao matar monstros
- **Level up** automático com aumento de stats
- **Inventário** com equipamentos e poções
- **Mundo 120×120** gerado proceduralmente (cidades, masmorras, florestas, rios)
- **Spawn dinâmico** — monstros mais fortes conforme você explora
- **Regen** de HP e MP fora de combate
