# LAN release pass — 2026-09-16

## Playing

Build with `gradlew.bat build :lwjgl3:animationPreviewClasses :lwjgl3:packageWindows`.
Open `lwjgl3/build/portable/The Dragon Conquerors/The Dragon Conquerors.exe`.
Copy the whole enclosing folder, not just the executable. Java 21 and the server
are bundled; no IDE, installed Java, terminal, or source checkout is needed.
The launcher is an unsigned Windows app image, not an installer.

- **Practice** starts a loopback-only server and allows map selection alone.
- **Host LAN** starts a four-player 2v2 lobby; all four players vote for a map.
- **Join LAN** connects using the host's lobby address on the same network.
- The host must remain open. A lost connection has a 30-second reconnect lease.
- Allow LAN access only on networks you trust if Windows asks. Guest Wi-Fi/client
  isolation can prevent players reaching one another. No router port forwarding
  or internet hosting is required or included.

Native packaging uses JDK 21's [jpackage app-image support](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html).

## Balance baseline

All classes now have 48 base tiers plus four racial boost steps. This is a
controlled starting point, not proof of identical power or win rates. Abilities,
range, team composition, hazards and player skill remain important.

| Class | HP | Mana | Accuracy | Strength | Speed | Inspiration | Wisdom |
|---|---:|---:|---:|---:|---:|---:|---:|
| Paladin | 8 | 5 | 8 | 8 | 6 | 6 | 7 |
| Mage | 6 | 8 | 8 | 4 | 7 | 7 | 8 |
| Wraith | 6 | 6 | 8 | 8 | 9 | 5 | 6 |
| Cleric | 7 | 7 | 8 | 5 | 6 | 7 | 8 |
| Bard | 7 | 7 | 8 | 5 | 6 | 8 | 7 |
| Archer | 7 | 5 | 9 | 7 | 8 | 6 | 6 |

Numbers above are design tiers, not final HP or mana. Paladin keeps durability
and melee burst; Mage trades durability for spells; Wraith keeps top mobility;
Cleric/Bard retain healing/utility; Archer gains the stats to support its role.
Evasion now grants 2 points per speed rating above five, with a 3-point
inspiration bonus and 30% cap. This prevents speed from simultaneously dominating
initiative, movement and defense. All 576 race/class matchup hit chances are
checked to remain 58–95%. Divine Smite's base damage is 40 (formerly 48), offsetting
Paladin's improved sustain and the more reliable hit system. Teleport, Eldritch
Blast, action points and each class's core ability set remain intact.

Human playtesting is still needed to tune win rates; deterministic tests do not
establish competitive balance.

## Optimization and cleanup

- Image maps directly load their authoritative masks, skipping legacy XML,
  tileset masks, decompression and per-tile pixel classification that was then
  overwritten. The legacy loader remains for fallback/regression fixtures.
- Client navigation reuses the shared node classification instead of repeating
  32,640 collision queries and point allocations per map load.
- HUD portraits rebuild only when visible portrait state/order changes.
- The combat log reuses existing labels and keeps a 100-entry bound.
- Movement text formats only when the displayed tenth changes; hover coordinates
  and selected-move color reuse existing objects.
- Removed redundant HUD state/local values and stale balance/launch documentation.
- Existing unused-art/package exclusions remain; raw authoring art, license
  notices, developer previews and collision regression assets are preserved.

These are specific work/allocation reductions, not a claimed percentage FPS gain.
The renderer is vsync-limited; a hardware profile is needed for FPS comparisons.

## Verification scope

Final build: **129 tests, zero failures/errors**, plus native-launcher smoke tests.
The executable was launched from a directory outside the checkout. Practice
started its bundled server and reached a playable Canyon match; crossing the
bridge updated movement points correctly. Host LAN exposed the local address,
accepted a map vote, and remained in the lobby with one player instead of
starting practice. The test application was closed normally afterward.

- Four real network clients: all three map votes, normal-mode rejection of the
  practice shortcut, repeated turns, reconnect/token rotation, disconnect victory
  and rejection of an incomplete-party rematch.
- Deterministic four-combatant battles: damage, heal, death, revive and victory on
  every map; these are test fixtures, not bots shipped in the game.
- Rematch reset plus stale-vote clearing and readiness gating after disconnect.
- All class/race stat budgets, castable abilities and matchup hit-chance bounds.
- Cached navigation classification checked against collision on all 97,920 nodes
  across the three maps, alongside existing bridge/lava/burn/teleport tests.
- Existing asset/package, animation, presentation, broker isolation and solo tests.

Four humans on different PCs and real Wi-Fi/firewall conditions have not been
tested here. This remains the recommended final acceptance session.
