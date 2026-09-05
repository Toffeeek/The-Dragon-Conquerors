# The Dragon Conquerors

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a template including simple application launchers and a main class extending `Game` that sets the first screen.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

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

Each playable class now has its own 48x48-frame sprite sheet with idle, walking,
attack, cast, hurt, and death animations in four directions. The exact sheet layout
is documented in [`docs/SPRITE_SHEETS.md`](docs/SPRITE_SHEETS.md).

Combat controls:

- Press `1`-`4` to choose an action.
- Self-targeted actions execute immediately.
- Enemy-targeted actions show a persistent selection prompt.
- Left-click a highlighted player to use the action on that player.
- Green targets are in range; red targets are outside the action's range.
- Press `Esc` to cancel target selection.
- While target selection is active, clicks are consumed by targeting and do not move the player.
- Each turn grants **1 action point (AP)** plus your class/race movement allowance.
- Every accepted ability spends 1 AP, including abilities with no mana cost; rejected actions spend nothing.
- Movement and actions can be used in either order. When both reach zero, the server advances the turn automatically.
- Press `E` to end a turn early. In solo testing, an automatic turn end refreshes your next turn's resources.
- Click a reachable ground destination to move. Longer routes stop at your movement limit;
  water, cliff terrain, map edges, and living players block movement.

## Temporary testing flow (enabled by default)

Map voting and the four-player minimum are temporarily bypassed:

1. Choose a team, class, and race, then click **JOIN GAME**.
2. Click **START TEST** to enter Canyon immediately with 1-4 connected players.
   Friends must join before starting; the two-player-per-team limit still applies.
3. Move, use available abilities, and end turns as usual. Solo or single-team tests
   stay playable instead of immediately declaring a winner. Tests that start with
   both teams retain normal victory rules.

Press `Esc` during a test to return to the menu and try another build. If targeting
is active, the first `Esc` cancels targeting. Solo tests do not add bots or enemies.

To restore map voting later, set `game.testing-mode=false` in
`server/src/main/resources/application.properties` and restart the server. Restart
the server and game after updating to use the new testing flow.

## Multiplayer selection flow (when testing mode is disabled)

After connecting, each client completes three server-backed lobby steps:

1. Choose a team and class while inspecting the class's base design tiers and abilities.
2. Choose a race while previewing the fully boosted engine stats and synergy budget.
3. Vote for Bog, Lava, or Canyon. Live totals are broadcast to every lobby client.

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

## Environment gameplay

The shared battlefield definition is the canonical source for spawn points, blocked terrain,
hazards, lethal falls, and path validation. Both the Spring Boot server and libGDX client use
that geometry, so the navigation preview agrees with authoritative command validation.

- Bog poison pools are walkable hazards that apply poison when a combatant begins a turn in one.
- Lava applies the environment's global burn effect at turn start.
- Canyon chasms block ordinary movement; a forced push across a lethal edge defeats the target.

Terrain collision is loaded from the same TMX tile data used for the map artwork.
`assets/maps-new/tileset.tsx` marks terrain types and declares the walkable grass palette.
The build generates a pixel collision mask directly from `tileset.png`; mixed shoreline tiles
are no longer blocked as full squares. Water and cliff pixels block a small circular foot collider
(0.10 world units), and path sweeps prevent cutting through thin edges. Rotated/flipped tiles
apply the same transform to artwork and collision. Navigation uses a finer 0.125-unit grid.
The shared JAR packages the generated mask for both server and client; rebuild and restart both
after editing maps or the tileset. Update `walkableColors` when introducing a new ground palette.
There are no synthetic black rectangles or red collision outlines over Canyon.
Bog's poison overlay only shades walkable ground. These maps still share placeholder artwork.

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
