# Implemented improvements — 16 September 2026

Scope: the accepted review recommendations, excluding sound and the training-target/bot item.
Core abilities, Teleport, Eldritch Blast, one AP plus movement, compact corner layout, character
art and three-map rules are preserved. No balance retuning or new practice opponents were added.

## Changes

- Bounded connection/response waits, heartbeats, visible Retry/Sync and Return to Menu controls.
  A rotating-token 30-second reconnect lease preserves the same player and resources.
  Recovery reads current state and never replays an uncertain action.
- Java 21 across modules; portable Windows folder with bundled runtime/server. Host no longer
  invokes Gradle or searches for the source checkout.
- Broker SEND/SUBSCRIBE authorization, explicit browser origins, payload/name/rate limits.
- Swept knockback collision stops before the first living player, including long pushes;
  terrain, bridge railings and instant-death lava/falls retain their existing rules.
- Read-only damage/healing/range/hit-chance/status previews, ground-area outlines,
  body-click targeting and hover highlights without larger terrain colliders.
- Compact status badges/countdowns and Curse counterplay text; floating damage/heal/miss
  feedback and a short turn-handoff pulse.
- Saved 60/75/90% UI scale, collapsible battle log, and bundled Inter under SIL OFL 1.1.
  The default remains 60%. Font attribution/license are in `assets/fonts/` and packaged.

## Earlier baseline verification

`gradlew.bat build :lwjgl3:packageWindows` passes: **99 tests, zero failures/skips**.
The portable runtime launched the menu from the Windows temporary directory, loaded the
bundled Inter font and started the extracted server without Gradle (Java 21, port 8080).
After the user handled the Windows firewall prompt, the live Lava smoke test verified solo
hosting, Mage ability hover details, movement, Burn 2t -> 1t and 8 HP damage, all three UI scales,
log collapse/expansion, and clean return to menu. The compact 60%/expanded-log settings were
restored. Computer-use inspection also caught and led to correcting world-font integer
rounding in status badges/floating numbers. No security setting was changed automatically.
The rebuilt game then visibly displayed `[B2]` / `[B1]`, verified Wraith Teleport landing
previews (safe stone versus lava), charged AP/mana, applied Burn at the destination, and
showed the confirmed HP loss. Floating numbers are offset beside the status row to avoid
overlap. Both test game processes exited successfully.

The Gradle suite covers existing map, animation, combat, turn, room and package behavior plus
new swept-player-collision, read-only preview, reconnect lease, broker restriction, real
WebSocket recovery and cross-room isolation cases. A recovery test spends AP, reconnects,
checks the unchanged action sequence/mana/AP, and then successfully ends the turn.

The negative WebSocket tests use a subsequent accepted command as an ordered processing
barrier: the foreign subscription receives no room state, and forged broker state does not
reach an existing player. Spring's ordered inbound channel drops denied frames rather than
necessarily producing a client-visible STOMP ERROR.

## Limits and follow-up acceptance checks

- Graceful reconnect is not persistent saves: application/server restarts cannot restore a match.
- Keep public hosting behind TLS/WSS and deployment-level connection/IP limits. Per-session
  frame limits and room isolation are not account authentication or a full security audit.
- Run a four-human match, artificial latency/packet-loss soak test, and clean Windows-machine
  distribution test before broad distribution. Local portable testing is not a clean-machine test.
- Profile frame-time percentiles and real match balance before changing rendering architecture,
  thread serialization, ability numbers or stat-boost behavior. No FPS improvement is claimed.
- Sound/audio ambience, training targets and bots are deliberately excluded.

## Modern presentation pass

The subsequently accepted eight recommendations are implemented:

1. Server-confirmed hit flashes, directional impact bursts, healing feedback and more prominent
   Fireball/Eldritch Blast/finishing hits. Screen shake is optional and off by default. There is
   no new critical-hit mechanic or balance change.
2. Bounded cosmetic map effects: sampled water ripples, lava shimmer and embers, fire-source
   glows and Bog wisps. Sampling occurs once when a map loads; navigation masks are untouched.
3. Procedural pixel-art ability/status glyphs, status countdowns/descriptions, animated resource
   bars and explicit unavailable-ability reasons. No added external art dependency.
4. Compact turn-order portraits, current/next slots and down/stun/disconnected state. Next is
   a read-only initiative preview, not a promise to survive turn-start status resolution.
5. Animated class cards, map thumbnails, team-member slots and short UI transitions.
6. Escape menu with Resume, saved text size/reduced motion/shake, display toggle, guide reset
   and leave confirmation. Online matches continue while the menu is open. Team triangle/square
   markers supplement colour. Enlarged command labels reflow to two rows.
7. Server-owned damage/healing, KO/environmental KO and playable-turn statistics; DoT attribution;
   retained rows for departed players; animated winners. Results scroll without hiding the
   rematch/menu buttons. All counters reset when a new authoritative match is created.
8. Skippable three-page first-match guide, permanently dismissed on completion or skip, with
   an explicit option to reopen it.

Particles are capped, original idle strips are shared by the asset service, and all new
renderers dispose owned GPU resources. The original compact layout, three maps, core attacks,
Teleport, Eldritch Blast, AP/movement rules and collision remain intact. Sound and bots remain out.

Presentation verification includes a live portable solo Lava match: lobby cards/thumbnails,
team slots, ability clicks and AP spending, guide navigation/reopening, 100/125/150% text,
reduced motion, options/leave confirmation and return to menu. An offline four-player fixture
checks NOW/NEXT/OFFLINE/DOWN portraits and results at all text sizes without creating bots.
The fixture is in the existing developer-only preview source set, not the distribution.

The modern-pass build (`build :lwjgl3:animationPreviewClasses :lwjgl3:packageWindows`) passes
**105 tests, zero failures/errors/skips**. New coverage checks actual-HP statistics, caster-owned
Burn ticks, environmental-KO accounting, retained disconnected-player rows, presence recovery,
ability/status icon mappings and read-only next-turn prediction across a changed round order.
At 150% text, the live fixture verified two-row command controls and scrolled results while
Rematch/Return to Menu remained accessible. Preview preferences are restored on exit.

The earlier broad-distribution checks still apply: this is not a four-human LAN soak test,
clean-machine certification or a guarantee of zero bugs. No measured FPS gain is claimed.

## Ring-free character identity update

- Replaced player/team/target rings and underfoot shapes with a small arrow above the local
  character. It remains visible out of turn; white becomes gold on the local turn. Another
  active character gets a thin gold mark above their health bar; turn-order UI is unchanged.
- World HP bars are green for allies (including self) and red for opponents, relative to the
  viewer's team, not fixed team IDs. The local HUD HP bar is green too.
- Hovering a body applies a 28% green/red colour wash using the original sprite's alpha.
  Original detail, facing and animation remain visible. Hover works without choosing an action
  and while waiting for another turn, but is suppressed behind HUD/options/recovery UI.
- Affiliation colours no longer mean attack range; tactical range/ground previews and server
  target validation remain unchanged. Hit flashes briefly take priority over hover.

`build :lwjgl3:animationPreviewClasses :lwjgl3:packageWindows` passes **108 tests, zero
failures/errors**. New tests cover both team perspectives, self/ally/enemy body hover outside
turn selection, misses and overlapping sprites. Computer-use inspection of the real six-class
renderer verified no player rings, the local arrow, green/red bars and both hover colours;
switching the viewer's team correctly reversed the colours and moved the arrow. The portable
package is rebuilt. The animation preview now exposes V to repeat this developer-only check.

## Damage and healing flashes

Confirmed HP loss produces a red sprite flash; confirmed HP gain (including revival) produces
green. Both last 0.18 seconds, respect the existing impact delay and use a 48% colour wash that
preserves texture alpha/detail. A flash overrides the 28% team-hover tint, then restores it.
Reduced Motion disables these pulses. Misses, rejected actions, unchanged HP and initial match
loading do not trigger them. Flash timing correctly accounts for frames crossing the delay.

The full build and portable package pass **111 tests, zero failures/errors**. Three added tests
cover flash type, delay, expiry, negative time and long frames. Computer-use inspection verified
red and green pulses across all six class sprites and restoration of normal colours afterward.
The developer animation preview supports 6 for damage and 9 for healing; pause/frame-step
holds the visual for inspection and does not change runtime gameplay.

## Transparent HUD and fantasy menu redesign

- Removed the large HUD panel backgrounds and turn-portrait boxes. Outlined ivory/gold text,
  small translucent controls and green/blue/amber bevelled resource bars remain readable over
  the maps. Resource values use small medallion icons instead of repeated labels.
- Main menu, class/race/map selection, options and results share a deep-blue glass and muted-gold
  theme. Bundled, OFL-licensed Cinzel supplies headings/primary controls; Inter remains the
  small-text font. Font source attribution and license are included in assets/fonts.
- Removed repeated explanatory paragraphs and automatic tutorial panels. Actual costs, race
  bonuses, map hazards, connection errors and leave warnings are retained. FIELD GUIDE opens
  help on demand. The solo start button is now START BATTLE; its behavior is unchanged.
- Large text grows resource tracks and uses two-line ability entries; lobby cards and menu
  scrolling preserve access to controls. HUD scale remains independent of text size.
- Client movement playback is 3.5 world units/second instead of 5; walk frames last 0.13 seconds
  instead of 0.10. Server movement budgets, path distance, ability timing and combat balance
  are unchanged. New tests cover authoritative playback, local distance cost and all six clips.

The build and Windows package pass **114 tests, zero failures/errors**. Native computer-use
checks covered all three map backgrounds, main menu/lobby/results, 100-150% text, options and
on-demand help. A packaged solo match was hosted, selected and started successfully, and the
character completed a bridge crossing with the expected movement-point cost. The package
was launched outside the workspace with its bundled Java runtime. This is a local visual and
functional smoke test, not a four-player LAN soak test.

## Compact portrait initiative and animated surfaces

- Removed character/map thumbnails from the main menu, preserving them in selection screens.
- Removed the large top-right turn heading. Portraits now have small black backplates,
  viewer-relative green ally/red opponent borders, player names below and a gold arrow above
  the active portrait. NOW/NEXT and exceptional states remain readable. Long names ellipsize;
  hover retains the full name/team and highlights the corresponding world sprite without an action.
- Replaced assets/maps-new/map3.svg and its extracted PNG with the supplied Downloads/map3.svg.
  The new wrapper is 1536x1024. Decoded RGB comparison to the prior package found **zero changed
  pixels**; existing image-aligned collision, connected bridges, safe spawns and poison remain valid.
  A regression test ensures the installed PNG exactly matches the supplied SVG's embedded bytes.
- Added one masked surface shader draw for water/lava, shoreline foam and downward waterfall flow,
  plus existing ripple/bubble/ember effects and new waterfall mist. The mask combines artwork colour
  with lethal-liquid geometry so walkable terrain, bridges and violet poison ground are not warped.
  The waterfall is explicitly restricted to its image opening. Sampling displacement cannot pull
  land pixels into water. Masks are generated once per screen, and their textures/shader are disposed.
- Reduced Motion draws the original static map and disables ambient particles. Unsupported shader
  compilation logs a diagnostic and falls back to the static map. No gameplay timing/rules changed.

Full build and Windows packaging pass **119 tests, zero failures/errors**. Computer-use checks
verified the clean main menu, all three surfaces, both team viewpoints, ally/enemy portrait hover,
active marker and 100-150% text layout in the four-player offline fixture. Surface motion was
softened following visual inspection. No four-human LAN soak or measured FPS claim is made.
The developer presentation fixture supports V for team perspective and O for Reduced Motion,
restoring these presentation preferences on close; it is excluded from distribution.
