# The Dragon Conquerors

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a template including simple application launchers and a main class extending `Game` that sets the first screen.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Play the portable Windows build

Run `gradlew.bat build :lwjgl3:packageWindows` using JDK 21. Open
`lwjgl3/build/portable/The Dragon Conquerors/The Dragon Conquerors.exe`, or copy that **entire folder**
to another Windows PC. It includes a Java 21 runtime and the server companion.
Hosting extracts the bundled server and starts it directly; players do not need
Gradle, an IDE, a source checkout, or a separately installed JDK. Keep the runtime
license folder and font license with the distribution. This is the Windows build;
other platforms have not been verified by this packaging task.

See [`docs/RELEASE_READINESS.md`](docs/RELEASE_READINESS.md) for the
latest implementation and verification details. The earlier review is a historical
baseline, not a list of bugs still present in this build.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.

## Character animation and combat targeting

The supplied 100x100 animation strips now power all six classes: Knight/Paladin,
Necromancer/Wraith, Archer/Archer, Priest/Cleric, Wizard/Mage, and Orc/Bard.
Every class uses the shadowed variant except Wraith. Idle/walk loop; attacks,
casts, hurt, and death use their actual per-strip lengths. Side-view sprites mirror
left/right; vertical movement preserves the last horizontal facing. Death holds
its final frame, and Revive restores animation. See [`docs/SPRITE_SHEETS.md`](docs/SPRITE_SHEETS.md).

Accepted ability events include their origin and target, even on misses. Spell,
arrow, healing, and portal effects use the supplied standalone effect strips.
Inputs wait for animation playback; victory waits for the final death animation.
Teleport and Eldritch Blast retain their original mechanics, as do all other moves.
Display-only renames: Ice Attack -> Ray of Frost; Mace Strike -> Sacred Strike;
Dissonant Chord -> Dissonant Shout.

Run `./gradlew lwjgl3:animationPreview` for the offline six-class renderer check.
Use 1-8 to choose idle/walk/basic/secondary/ultimate/hurt/death/revive, P to play or
pause, and F to step a frame. Press V to switch team perspective and hover the characters
to check their green/red tints and health bars. This is a developer preview, not a gameplay mode.

Combat controls:

- Click **MOVE** or **ACTION** at the start of your turn; world clicks do nothing until a mode is selected.
- **ACTION** opens a compact mouse-clickable ability list (no number-key shortcuts).
- Self-targeted actions execute immediately.
- Enemy-targeted actions show a persistent selection prompt.
- Left-click a highlighted player to use the action on that player.
- Hover colours indicate team, not attack range. Ability previews report range and invalid targets;
  the server still rejects illegal or out-of-range actions without spending resources.
- Click **CANCEL** to cancel targeting or movement selection without spending resources.
- Press `Esc` or click **MENU** to open Match Options (also cancels selection). Resume with
  `Esc` or **RESUME**. Leaving requires confirmation; the menu does not pause an online match.
- While target selection is active, clicks are consumed by targeting and do not move the player.
- Each turn grants **1 action point (AP)** plus your class/race movement allowance.
- Every accepted ability spends 1 AP, including abilities with no mana cost; rejected actions spend nothing.
- Movement and actions can be used in either order. When both reach zero, the server advances the turn automatically.
- Click **END TURN** to end a turn early. In solo testing, an automatic turn end refreshes your next turn's resources.
- In **MOVE** mode, click the soft-blue reachable area to move. Longer routes stop at your movement limit;
  water, cliff terrain, map edges, and living players block movement.
- The map fills the game window with compact, transparent corner overlays. Large HUD backplates
  are removed; outlined ivory/gold text remains legible against terrain. Individual buttons
  retain translucent fills so their interactive boundaries stay clear.
- HP/mana/stamina sit at the top left; the top-right banner names the active player.
- Player rings are removed. A small arrow above the sprite identifies your character throughout
  the match (gold on your turn, white otherwise). A thin gold mark above another character's
  health bar indicates their active turn.
- World health bars are green for your team and red for opponents, from either team's perspective.
  Hover any character body for a subtle 28% green/red tint that preserves the original sprite
  details and alpha. Hover works outside Action mode and never changes targeting legality.
- The bottom-right battle log retains the latest 100 server-confirmed results. Scroll to read older entries.
- Opening abilities does not resize the map. There are no reserved HUD strips.
- HUD clicks never trigger movement; movement route lines and in-game server/map labels are hidden.
- Hover an ability for its range, estimated damage/healing and status duration. Targeted actions
  highlight the hovered character; ground actions preview their affected area before casting.
  The character's visible body is clickable without changing its terrain collision radius.
- Distinct status icons show remaining own turns: flame = Burn, drop = Poison, skull = Curse,
  ice = Sub-Zero, star = Stun. Hover your resource-panel icons for descriptions and durations;
  Curse also shows its counterplay in text.
- Damage, healing and misses appear over affected characters. The turn banner briefly pulses
  when the active player/round changes.
- Click **UI** under your resources to cycle 60%, 75%, and 90% scale. Click the **LOG**
  heading to collapse/expand it. Both settings are saved; the original compact 60% is the default.

The resource display uses icon-led, gold-edged glass bars: green health, blue mana and amber
stamina, with exact remaining/max values and a compact AP count. Hover a resource icon to
identify it. The log toggle is labelled **LOG**.

Movement playback travels at 3.5 world units/second instead of 5 (30% slower), with 0.13-second
walking frames instead of 0.10. This is client presentation only: speed stats, movement points,
path distances, collision and ability animation timings are unchanged.

## Presentation and accessibility

- Accepted hits have brief character flashes and directional particles; healing has green feedback.
  Confirmed HP loss flashes the sprite red; confirmed healing/revival flashes it green for
  0.18 seconds, timed to the impact. These 48% colour washes preserve sprite detail and alpha,
  override the softer hover tint briefly, and are disabled by Reduced Motion. No HP change
  (including a miss or healing at full health) means no health flash.
  Fireball, Eldritch Blast and finishing blows receive stronger effects. These do not add critical
  hits, bonus damage, hit-stop or changes to combat timing.
- Masked water flow/shoreline foam, lava shimmer/bubbles/embers and a flowing Map 3 waterfall
  with splash mist animate the maps. Terrain pixels, collision, bridges and hazard rules are unchanged.
- The top-right portrait strip shows the current initiative order, **NOW**, **NEXT**, **STUN**,
  **DOWN** and **OFFLINE**. NEXT is the next living initiative slot before turn-start effects;
  stun or lethal damage-over-time may skip it. Portrait-only black cards have green ally/red enemy
  borders relative to the viewer, usernames below and a small gold arrow above the active player.
  Hovering the portrait highlights its world sprite without targeting or issuing any command.
  Full names/team information remain available on hover; long labels are ellipsized.
- Ability rows include compact icons, resource costs and reasons they cannot currently be used.
  HP, mana and movement bars ease toward server-confirmed values.
- Class cards animate the supplied idle sprites; map cards show actual artwork thumbnails;
  lobby slots identify the players on each team. Screen transitions use short fades.
- Match Options saves independent **100/125/150% text size**, **Reduced motion** and optional
  **Screen shake** (off by default), and exposes windowed/fullscreen switching. Larger text
  uses two command-button rows without widening the compact HUD. Reduced motion disables
  ambient effects, impact particles/flashes/shake, portrait animation and decorative pulses.
  Essential combat animation, targeting and numerical feedback remain visible.
- The three-page guide is now on demand through **FIELD GUIDE** in Options; it no longer
  opens automatically over the map. Routine explanation paragraphs and duplicate labels
  have been removed. Costs, stats, hazard warnings, targeting errors and connection status remain.
- Match results show server-owned actual damage/healing, knockouts, environmental knockouts,
  and playable turns. Damage-over-time is credited to its caster; map damage has no player
  owner. Environmental KOs are included in total KOs, but lethal fall/lava HP removal does
  not inflate damage. Statistics survive player departure and reset with a new match.
  Winning characters animate; Rematch and Return to Menu remain visible beside scrollable results.

Main menu, character/race/map selection, Options and results share a blue-glass/antique-gold
fantasy theme with an arcane background. The main menu contains only its title and host/join
controls; character portraits and map thumbnails remain in selection screens, not the main menu.
Bundled [Cinzel](https://github.com/google/fonts/tree/main/ofl/cinzel)
is used for headings and primary controls; Inter remains for small text and numbers. Both
fonts' SIL Open Font License notices are packaged in `fonts/`; no system font installation is needed.

For developer visual checks, run `gradlew.bat :lwjgl3:presentationPreview`: **H** shows a
four-player HUD/world fixture, **1/2/3** change animated maps, **V** switches viewer team, **M** shows the menu, **L** the
lobby, **R** the results fixture, **T** cycles text size and **O** toggles Reduced Motion. It uses no server,
restores presentation preferences on close and is excluded from the game distribution.
Sound and practice opponents/bots remain excluded.

## Temporary testing flow (enabled by default)

For the latest cleanup, known issues, and prioritized recommendations, see
[`docs/GAME_REVIEW.md`](docs/GAME_REVIEW.md). Debug profiling is off by default;
run `./gradlew lwjgl3:run -Pprofile=true` to display FPS and draw calls.

Choose **PRACTICE** from the main menu for a loopback-only practice session:

1. Choose a team, class, and race, then click **JOIN GAME**.
2. Choose **Canyon**, **Lava**, or **Bog**, then click **START BATTLE** with 1-4 connected players.
   Map 1 is selected initially. The player pressing Start Test chooses the map; no votes are needed.
   Friends must join before starting; the two-player-per-team limit still applies.
3. Move, use available abilities, and end turns as usual. Solo or single-team tests
   stay playable instead of immediately declaring a winner. Tests that start with
   both teams retain normal victory rules.

Press `Esc`, choose **LEAVE MATCH...**, then **CONFIRM LEAVE** to return to the menu
and try another build. Solo tests do not add bots or enemies.

Choose **HOST LAN** instead for four-player multiplayer. Friends on the same
network use **JOIN LAN** and the host's lobby invite address. The host must stay
running. Practice is isolated to the host computer; no global configuration edit
is needed to switch modes. Internet multiplayer is intentionally out of scope.

## LAN multiplayer selection flow

After connecting, each client completes three server-backed lobby steps:

1. Choose a team and class while inspecting the class's base design tiers and abilities.
2. Choose a race while previewing the fully boosted engine stats and synergy budget.
3. Vote for Map 1 (Canyon), Lava, or Map 3 (Bog), shown in that order. Live totals are broadcast to every lobby client.

Each matchmaking room allows four players, enforces two players per team, and starts its
match only after all four players have voted. A strict majority wins; a tie is randomly
resolved only among the tied environments. The selected race/class build and environment
are carried into the game screen, which loads the matching battlefield presentation and rules.

## Room-based matchmaking

Joining is automatic: the server assigns a connection to the oldest waiting room that can
accept its selected team, or creates a new room. Player IDs are local to a room. The private
join confirmation carries the room ID; the client subscribes to that room's STOMP topic and
acknowledges readiness before roster, vote, or match broadcasts begin.

Every room owns an independent lobby, vote tally, and authoritative match. Combat commands
are routed from server-authenticated session attributes rather than trusting a packet's room
ID. Disconnects update only the affected room, and an empty room is removed automatically.

### Connection recovery

Connections time out after eight seconds; an unanswered command shows recovery controls after
ten seconds. Retry reads the authoritative state instead of replaying the last move or cast.
A dropped transport reserves the player identity for 30 seconds. Reconnect within that grace
period to keep HP, mana, position, AP and room membership; after expiry the normal leave/forfeit
rules apply. Choosing Return to Menu explicitly leaves immediately. The grace period is not a
save game: closing the application, losing its resume token, or restarting the server cannot
restore the match. Other players can continue during your disconnect.

### Server boundary

Client sends are restricted to approved application commands. Subscriptions are restricted
to the connection's private queue and assigned room. Frames are limited to 16 KiB, names to
32 characters without control characters, and SEND/SUBSCRIBE traffic to 60 frames per 10 seconds
per connection. Resume tokens rotate on successful recovery.

Browser origins must be explicit (`game.allowed-origins`, comma-separated; localhost by default).
Native desktop connections do not require an Origin header. These safeguards are not account
authentication or Internet deployment protection: use trusted LAN hosting for this build.
An Internet deployment still needs TLS/WSS, connection/IP-level abuse protection and operational
testing; no firewall, reverse proxy or public deployment settings were changed by this work.

## Environment gameplay

The shared battlefield definition is the canonical source for spawn points, blocked terrain,
hazards, lethal falls, and path validation. Both the Spring Boot server and libGDX client use
that geometry, so the navigation preview agrees with authoritative command validation.

- Bog poison pools are walkable hazards that apply poison when a combatant begins a turn in one.
- Lava pools block walking and teleport landing; being pushed into lava is instant death.
- Knockback stops before the first living player, with no chain push. Existing terrain/railing
  collision and lethal lava/fall rules still apply to the shortened path.
- Glowing lava-map cracks remain walkable and apply Burn for two own turns (8 HP per turn).
  Crossing a crack or teleporting onto one applies it; staying on one refreshes it. Safe stone never applies burn.
- Canyon chasms block ordinary movement; a forced push across a lethal edge defeats the target.

Map 1 replaces Canyon's old artwork and Map 3 is the final map, using Bog's rules.
The original SVGs and their unchanged embedded PNGs are in `assets/maps-new/map1.*`,
`assets/maps-new/map2.*` (Lava), and `assets/maps-new/map3.*`. The original source files
are retained. These maps are selections, not automatic campaign progression.
The latest supplied `Downloads/map3.svg` is installed as the Map 3 source. Its SVG canvas and
embedded PNG are 1536x1024. Pixel comparison against the preceding packaged map found zero
changed pixels, so the image-aligned collision, safe spawns, and poison regions
remain valid. The embedded artwork's proportions are preserved.

Their `map1-collision.xml`, `map2-collision.xml`, and `map3-collision.xml` files describe ground, bridges,
stairs, solid props and hazardous regions in source-image coordinates. The build
compiles shared collision masks through `shared/image-maps.gradle`. Map 3 is centered
at its original aspect ratio, with the same positioning used by server collision.
Water, cliff faces and props block movement; bridges, staircases and paths connect
the main playable regions. Bridge railings stop knockback before water. Canyon's
open falls remain lethal; Bog's violet districts apply poison at turn start.
Lava's stone causeways connect the main islands; its detached island needs teleport.
Its narrow lava moat is lethal, while glowing fissures use passable burn strips.
Collision previews are generated at `shared/build/reports/map1-collision.png`,
`map2-collision.png`, and `map3-collision.png`; these debug overlays are not displayed in-game.

The legacy TMX terrain collision is loaded from the same tile data used for its artwork.
`assets/maps-new/tileset.tsx` marks terrain types and declares the walkable grass palette.
The build generates a pixel collision mask directly from `tileset.png`; mixed shoreline tiles
are no longer blocked as full squares. Water and cliff pixels block a small circular foot collider
(0.10 world units), and path sweeps prevent cutting through thin edges. Rotated/flipped tiles
apply the same transform to artwork and collision. Navigation uses a finer 0.125-unit grid.
The shared JAR packages the generated mask for both server and client; rebuild and restart both
after editing maps or the tileset. Update `walkableColors` when introducing a new ground palette.
There are no synthetic black rectangles or red collision outlines over Canyon.
The new images are displayed without placeholder rectangles or collision-debug overlays.

## Authoritative combat

Once a test starts or voting completes, the Spring Boot server creates the canonical combat state. Clients
send only movement, ability, and end-turn intents; the server validates the connection's
player identity, active turn, stamina, battlefield bounds, player collision, ability
ownership, target team, range, mana, action availability, and cooldown. Accepted commands
are resolved through the shared combat engine and broadcast as full match snapshots.

Snapshots synchronize all participants' positions, HP, mana, mutable stats, status effects,
cooldowns, stamina, action usage, active player, round, and victory state. Turn order is
Speed descending with player ID as a deterministic tie-break. The client no longer executes
damage or healing locally. The server computes collision-safe routes, charges actual path length,
and sends the exact waypoints to clients. Movement animation does not spend resources a second time.

## Match completion and rematches

The final authoritative snapshot disables combat input and opens a dedicated victory,
defeat, or draw screen. Players can return to the menu immediately or submit one rematch
vote. Vote totals remain room-scoped and are broadcast to every connected participant.

In testing mode, a rematch requires only the players still connected to the room.
If someone leaves, existing rematch votes are cleared so the remaining players can
agree to restart with the smaller party.

With testing mode disabled, a rematch starts only after all four original players
agree, preserving the 2v2 format; a disconnect disables rematching.
The server keeps the same room, builds, teams, and environment but constructs a new match,
resetting positions, HP, mana, effects, cooldowns, action resources, round count, and turn
order. Empty completed rooms are removed by the normal room lifecycle cleanup.
