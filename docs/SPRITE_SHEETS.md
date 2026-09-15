# Character animation sprites

Runtime character rendering now uses the supplied action-strip sprite pack rather
than the older generated `*_HD.png` sheets.

## Class art mapping

| Game class | Supplied sprite set |
|---|---|
| Paladin | Knight Templar |
| Mage | Wizard |
| Wraith | Necromancer |
| Cleric | Priest |
| Archer | Archer |
| Soldier | Soldier |

The Bard class has been removed and replaced by Soldier.

## Runtime files

The selected source strips are copied under:

`assets/characters/sprites/<class>/`

Each playable class has:

- `idle.png`
- `walk.png`
- `attack1.png`
- `attack2.png`
- `hurt.png`
- `death.png`

Every strip is one row of 100 x 100 animation cells. Frame counts vary by class
and action, so `SpriteAssets` records the expected frame count and
`PlayerAnimationController` uses that count when deciding when a one-shot
animation has finished.

The supplied art faces right. `PlayerRenderer` uses it directly when facing
right/up/down and creates a horizontally mirrored cached region when facing
left. Textures use nearest-neighbour filtering so the original pixel art stays
crisp.

## Animation selection

- Standing still -> Idle
- Moving -> Walk
- Free/basic attack -> Attack 1
- Mana/skill action -> Attack 2
- Taking non-lethal damage -> Hurt
- Reaching zero HP -> Death, holding the final death frame

For Cleric, the Priest `Heal` strip is used as Attack 2 because the supplied
Priest set names its second action Heal rather than Attack02.
