# Character animation assets

The runtime uses the user-provided pack in `assets/characters/animated/`.
The original `Character assets/` folder and older HD sheets are preserved, but
the old 24-row/fixed-six-frame sheets are no longer loaded during gameplay.

| Class | Character | Variant | Idle | Walk | Basic | Cast | Special | Hurt | Death |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|
| Paladin | Knight | with shadows | 6 | 8 | 7 | 11 | 10 | 4 | 4 |
| Wraith | Necromancer | without shadows | 6 | 6 | 9 | 10 | 10 | 4 | 9 |
| Archer | Archer | with shadows | 6 | 8 | 9 | 6 | 12 | 4 | 4 |
| Cleric | Priest | with shadows | 6 | 8 | 9 | 6 | 6 | 4 | 4 |
| Mage | Wizard | with shadows | 6 | 8 | 6 | 9 | 9 | 4 | 4 |
| Bard | Orc | with shadows | 6 | 8 | 6 | 6 | 6 | 4 | 4 |

Each strip is one row of 100x100 cells. Transparent padding is intentional:
bodies are roughly 20-30 pixels high. The renderer draws the full cell at four
world units, anchored at source pixel (50,59), keeping feet and collision aligned
without clipping swords or spell gestures. Artwork faces right and is mirrored
for left-facing movement/attacks; vertical motion preserves horizontal facing.

`SpriteAssets` declares paths, lengths, timing, looping and per-ability animation
selection. Idle/walk loop; other clips finish, death holds the final frame
(the Necromancer dissolves), and revive resets the controller. No fake extra
frames or directions are synthesized. Steady Aim intentionally uses an idle pose
with a supplied buff effect.

`SOURCES.sha256` records imported PNG bytes after checking them against the
requested source variants. Tests validate imported assets without requiring the
large original pack to be included in a distribution. If art is intentionally
replaced, update the manifest along with the profile metadata and tests.

## Ability presentation

Player attack/cast strips do not include duplicated baked spell effects.
`EffectAssets` loads the supplied standalone wizard, necromancer, priest, and
arrow strips. Melee swings retain their painted weapon trails. Spell colors,
travel and impact placement distinguish abilities without changing combat rules.
Teleport uses the Necromancer portal at both endpoints; Eldritch Blast uses a
projectile and impact effect and retains its push/stun.

The server sends a monotonic action sequence plus the actual source and target
coordinates. This avoids replaying attacks on unrelated snapshots and supports
missed shots, ally buffs, ground AoE and teleport. Hurt/death reactions wait for
the attack impact, commands wait for presentation to finish, and the post-match
screen waits for the final death animation.

## Verification

`./gradlew core:test server:test shared:test` checks source hashes, dimensions,
class mappings, frame boundaries, looping, facing, full ability playback, delayed
reactions, death/revive, event targeting and existing gameplay/map rules.

`./gradlew lwjgl3:animationPreview` opens an offline six-character preview using
the production renderer and effect code. It starts paused. Keys 1-8 select idle,
walk, basic attack, secondary, ultimate, hurt, death and revive. P plays/pauses;
F steps 0.085 seconds. These keys are only for the developer preview; gameplay
abilities remain mouse-driven.
