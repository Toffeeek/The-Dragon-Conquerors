# Character Sprite Sheets

The game now loads the high-detail class sheets directly:

- `assets/characters/Warrior_HD.png`
- `assets/characters/Mage_HD.png`
- `assets/characters/Archer_HD.png`
- `assets/characters/Paladin_HD.png`
- `assets/characters/Rogue_HD.png`

The original 48 x 48 sheets remain in the repository as animation-geometry source
files and as a fallback reference for artists. Runtime rendering uses only the
`*_HD.png` files through `SpriteAssets`.

## Runtime layout

Each high-detail sheet is **576 x 2304 pixels** and contains **96 x 96 pixel
frames**. Every row has six frames. Animation states use four consecutive rows
in this direction order:

1. Down
2. Left
3. Right
4. Up

| Rows | State | Looping |
|---|---|---|
| 0-3 | Idle | Yes |
| 4-7 | Walk | Yes |
| 8-11 | Attack | No |
| 12-15 | Cast / skill | No |
| 16-19 | Hurt | No |
| 20-23 | Death | No |

`PlayerAnimationController` selects the state, direction, and frame.
`PlayerRenderer` splits each HD sheet into 96 x 96 regions, validates the exact
24-row by 6-column grid, and draws the selected region. A malformed sheet is
rejected instead of being cropped silently.

Combat already invokes the animation controller as follows:

- Physical actions call `playAttack(..., false)` and display rows 8-11.
- Spell/skill actions call `playAttack(..., true)` and display rows 12-15.
- A successful hit calls `playHurt(...)` on the selected player.
- A lethal hit calls `playDeath()` and holds the last death frame.
- Movement continuously switches between idle and walk based on the path and
  updates the facing direction.

## Rebuilding the HD files

The high-detail files can be rebuilt deterministically from the animation-safe
source sheets:

```bash
python3 tools/build_hd_character_sprites.py
```

The builder preserves all 144 animation cells while upgrading them to 96 x 96,
adding class-specific material palettes, stronger pixel clusters, metal and
cloth texture, edge lighting, outlines, and one-pixel highlights. It requires
Pillow and SciPy.

The older simple sheets can still be regenerated with:

```bash
python3 tools/generate_character_sprites.py
python3 tools/build_hd_character_sprites.py
```
