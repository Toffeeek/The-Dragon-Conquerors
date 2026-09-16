# Game review and cleanup — 16 September 2026

> Historical review before the follow-up implementation. Connection recovery, portable Windows
> hosting, broker restrictions, player-blocked knockback, tactical previews/feedback, UI scale,
> a collapsible log and a bundled font are now implemented. See
> [Implemented improvements](IMPLEMENTED_IMPROVEMENTS.md) for current status and limitations.
> The package-size comparison below predates bundling the server and runtime.

## Result

The core is worth polishing, not replacing: combat is server-authoritative, movement uses shared collision rules, and the three maps and six character profiles have automated coverage. This pass preserves the requested small HUD, map order, character mappings, Teleport, Eldritch Blast, action points, and existing balance.

This is a code/build review plus a desktop smoke test, not a guarantee of zero bugs or a completed balance study.

## Cleanup completed

- Removed ten unreferenced Java files: the four old client-side combat classes (`ActionSystem`, `ActionType`, `ActionResult`, `ActionAnimation`), `TurnManager`, `GameMap`, `FacingDirection`, and the unused `PlayerConverter` / `PlayerState` / `TEAM` chain. Active combat continues to use shared `AbilityType`, the server match, and `PlayerCombatState`.
- Removed unused Ashley, gdx-ai, and Box2D dependencies, including Box2D desktop/native-image support. Terrain collision is implemented by this project, not Box2D. Kept FreeType because the UI uses it.
- Removed the unused `assets.txt` generator and its generated list. Previously the task configuration rewrote one shared file across subprojects, despite having no runtime reader.
- Excluded obsolete character sheets, unused duplicate spell strips, map SVG sources, collision-authoring XML, raw/editor assets, and editor session files from desktop packaging. Original art, source packs, collision sources, tools, and fallback TMX resources remain on disk. Generated collision masks remain in the shipped game.
- Made OpenGL profiling opt-in, removed the unused FPS logger, and stopped changing the title every frame. Profiling mode updates the title once a second.
- Effects now skip an empty render pass, interpolate positions without new per-frame vectors, and calculate arrow rotation once per effect.
- Removed an unreachable navigation branch, unused renderer constants/imports, and an unused navigation helper. Corrected the menu's obsolete Warrior/Rogue roster text and stale keyboard/top-centre HUD comments.
- Added three package regression tests checking actual JAR contents, unchanged character/effect/map bytes, collision resources, and absence of excluded legacy items.

The desktop JAR measured **60,373,033 bytes before cleanup and approximately 47.4 MB afterward**: around **21.5% smaller**. This measures package size, not FPS or RAM improvement.

Deleted files were copied first to `build/cleanup-backup-20260916-123956/`, preserving relative paths. They can be restored by copying the desired file back. This backup is local and ignored by Git; save it elsewhere before running a root `clean`. Source artwork was not deleted.

## Changes needed next

### 1. Connection recovery and bounded waits — high priority

Evidence: `NetworkClient.connect()` waits on `.get()` without an application timeout. Its transport-error callback logs but does not notify gameplay; `send()` silently ignores a disconnected session. `BattleInteraction.sentCommand()` locks input until a response arrives. `disconnect()` does not stop the owning STOMP client.

Impact: a failed connection or connection lost during a turn can leave a player waiting with no useful recovery route. Repeated failed/retried connections also need explicit resource cleanup.

Recommended change: connection-state events, a bounded connect timeout, cleanup on every failure/disconnect, and a visible retry/return-to-menu panel. Never clear a timed-out command and blindly replay it: reconnect should resynchronize authoritative state. Test unavailable hosts, disconnect during movement/casting, and repeated reconnect attempts.

### 2. A self-contained host build — high priority before sharing

Evidence: `Main.startLocalServer()` finds the project root and launches `gradle-wrapper.jar :server:bootRun`; the desktop artifact does not bundle the server as a runnable companion.

Impact: Host works in this development checkout, but giving someone just the desktop JAR does not provide a complete host installation. The server also uses Java 21 while the desktop compiles for Java 17.

Recommended change: ship a server companion and a consistent bundled runtime; start the companion directly, without Gradle or a source checkout. Verify on a clean machine with no IDE, Gradle cache, or installed JDK.

### 3. Protect the WebSocket broker — required before public hosting

Evidence: `WebSocketConfig` enables `/match` and `/queue` with wildcard origins, and no inbound authorization interceptor was found. Application commands validate session ownership, but that is distinct from authorizing broker `SEND` and `SUBSCRIBE` frames. Room names are sequential.

Risk from code review: a custom client may subscribe to another room or inject messages directly into broker destinations. This was not exercised against a public server in this pass.

Recommended change: reject client sends to server-owned destinations, restrict subscriptions to the assigned room/private queue, add message/name limits and rate limits, and test two isolated rooms using real WebSocket clients. Use an explicit allowed-origin policy for deployment. Do not treat the current build as hardened for untrusted Internet clients.

### 4. Resolve knockback against other players — gameplay correctness

Evidence: `AuthoritativeMatch.move()` and Teleport reject occupied destinations, but `resolvePushes()` checks terrain/lethal falls only.

Recommended change: explicitly define whether pushes stop at a living player or cause a chain push, then apply that rule server-side. Add tests for pushes into another player, a bridge railing, and a lava edge. Do not silently change the existing instant-death lava rule.

## Best visual and playability improvements

1. **Make decisions readable.** Add hover information showing hit chance, damage estimate, range, and status duration; show the affected area before an area attack. `AbilityResolver` already provides preview helpers, but the battle HUD does not display them. Keep attacks mouse-selected.
2. **Make consequences readable.** Add compact Burn/Poison/Curse/Stun icons with remaining turns, floating damage/heal/miss text, and a short active-player handoff cue. Curse especially needs a visible countdown and explanation of its counterplay.
3. **Add adjustable UI scale and a collapsible log.** Preserve the current compact setting as the default. The fixed 0.60 scale is quite small in a window; player choice is better than repeatedly shrinking or enlarging every panel. Bundle a suitably licensed font instead of depending on whichever system font is present.
4. **Improve targeting clicks.** Current target selection measures distance from the character's feet. Include the visible body in the clickable area, with a clear hover highlight, without enlarging the terrain collider.
5. **Add restrained sound and map ambience.** Prioritize button feedback, footsteps, distinct spell impacts, turn start, and victory, with independent volume/mute controls. There is no audio playback integration in the reviewed game code. Subtle lava/portal ambience would help the static maps feel alive; keep strong effects brief so tactical information remains visible.
6. **Give solo practice useful opponents.** Add optional stationary training targets first, then simple bots with difficulty choices. Present solo practice separately from multiplayer; currently a solo Paladin cannot meaningfully test its enemy-targeted attacks.
7. **Playtest before changing numbers.** Record match length, hit/miss rate, turns without useful actions, win rates by class/map/team composition, and knockback kills. Test whether permanent Bard/Archer stat boosts encourage stalling until the stat cap, whether misses feel too punishing with one AP, and whether instant-kill edge play dominates particular maps. These are balance questions, not established failures.

Keep Teleport, Eldritch Blast, one AP plus movement, and the three-map identity. Add clarity and feedback before adding more abilities or maps.

## Further optimization, guided by measurements

- Cache HUD strings and update log rows incrementally. `HudRenderer.render()` formats text each frame; `recordState()` rebuilds up to 100 labels for each accepted state.
- Batch character sprites/bars/rings across players and consider a sprite atlas. `PlayerRenderer` currently opens separate sprite/shape passes per player. Measure first; four actors do not justify a large rendering rewrite by themselves.
- Avoid parsing the legacy TMX/mask before overwriting it with the image mask on current maps (`TiledTerrain`). Keep the fallback and its tests until the image-only loader is verified.
- Profile navigation search allocations and startup work. `NavGrid` samples terrain again after constructing `BattlefieldNavigation`; the shared search allocates nodes/vectors. Reuse only after measurements show a hitch, while preserving identical route costs and collision behavior.
- For many simultaneous rooms, replace the controller-wide synchronized bottleneck with carefully designed per-room serialization. Preserve message order and prove room isolation under concurrency.
- Review whether the client/shared modules need full Spring Boot application scaffolding. Narrowing dependencies further requires testing the actual STOMP transport and serialization; do not strip libraries based only on their names.

Run `gradlew.bat lwjgl3:run -Pprofile=true` for an FPS/draw-call title, or `java -Dtdc.profile=true -jar "lwjgl3/build/libs/The Dragon Conquerors-1.0.0.jar"`. Normal play leaves profiling off. Compare consistent four-player scenes and frame-time percentiles before claiming performance gains.

## Verification and remaining coverage

- Full Gradle build and 83 tests pass, including the three new packaged-resource tests.
- Existing tests exercise all three map selections through a real local WebSocket connection, movement/collision, turn resources, hazards, room/rematch rules, and character animation strips.
- Desktop smoke test: launched the rebuilt JAR, hosted locally, selected class/race/Lava, inspected the map and top-right turn panel, moved across safe terrain, opened the mouse ability list, ended a turn, observed the battle log, and exited successfully.
- The final source-only cleanup also changes the stale menu class label and removes dead branches/comments; rebuild verification covers those changes.
- Not covered here: a four-human full match, packet-loss/latency soak tests, all animation effects replayed visually, clean-machine distribution, Mac/Linux behavior, or a measured FPS/RAM benchmark. These are the next acceptance gates, not implied by a green unit-test suite.

Suggested order: connection recovery → standalone host packaging → targeting/status feedback and UI settings → training targets/audio → measured balance/performance passes. Broker hardening must precede any public deployment.
