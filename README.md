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

## Multiplayer selection flow

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
- Lava applies the environment's global burn effect at turn start and contains impassable fissures.
- Canyon chasms block ordinary movement; a forced push across a lethal edge defeats the target.

Each environment currently uses a dedicated placeholder TMX map plus a colored hazard overlay.
The rules are authoritative and complete for this slice; detailed map artwork and Tiled object
layers can replace the placeholders without changing the combat protocol.

## Authoritative combat

Once voting completes, the Spring Boot server creates the canonical combat state. Clients
send only movement, ability, and end-turn intents; the server validates the connection's
player identity, active turn, stamina, battlefield bounds, player collision, ability
ownership, target team, range, mana, action availability, and cooldown. Accepted commands
are resolved through the shared combat engine and broadcast as full match snapshots.

Snapshots synchronize all four players' positions, HP, mana, mutable stats, status effects,
cooldowns, stamina, action usage, active player, round, and victory state. Turn order is
Speed descending with player ID as a deterministic tie-break. The client no longer executes
damage or healing locally.
