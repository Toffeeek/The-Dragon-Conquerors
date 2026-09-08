// File Location: shared/src/main/java/com/shared/shared/model/world/Environment.java
package com.shared.shared.model.world;

import com.shared.shared.model.effect.StatusEffectType;

/**
 * The three battlefields players vote for, and the hazard each one imposes.
 *
 * <p>Design document mapping:</p>
 * <ul>
 *   <li>{@link #BOG} — poison tiles. A character standing on a hazard tile at
 *       the start of its turn is poisoned. Localised: position matters.</li>
 *   <li>{@link #LAVA} — <b>every</b> player takes burn damage each turn,
 *       regardless of position. Global: a race against attrition.</li>
 *   <li>{@link #CANYON} — the neutral map. No periodic damage, but falling off
 *       an edge is instant death.</li>
 * </ul>
 *
 * <h2>Why the map is named as a String</h2>
 *
 * <p>This enum lives in {@code shared} so the server can validate an environment
 * vote and the client can load the matching map. Referring to libGDX's
 * {@code TiledMap} or the client's {@code MapAssets} here would drag rendering
 * types into the server, so the Tiled file is identified by name and the client
 * resolves it.</p>
 *
 * <h2>Hazard layers</h2>
 *
 * <p>{@link #getHazardLayerName()} is the Tiled <em>object layer</em> that marks
 * hazardous ground. {@code NavGrid} already reads {@code DropZones} and
 * {@code CliffEdges} for collision, so hazard tiles follow the same convention:
 * draw rectangles on a layer with this name and the environment engine will pick
 * them up without code changes.</p>
 */
public enum Environment {

    BOG("Bog",
        "Sunken marshland. The pale water is toxic — do not linger in it.",
        "bog.tmx",
        "PoisonTiles",
        StatusEffectType.POISON,
        HazardScope.TILE_BASED,
        false),

    LAVA("Lava",
        "Volcanic flats. The air itself scorches; nobody escapes the heat.",
        "lava.tmx",
        null,
        StatusEffectType.BURN,
        HazardScope.EVERY_PLAYER,
        false),

    CANYON("Canyon",
        "A neutral highland shelf. No hazards underfoot — but the drop is fatal.",
        "canyon.tmx",
        "CliffEdges",
        null,
        HazardScope.NONE,
        true);

    /** How widely an environment's hazard applies each turn. */
    public enum HazardScope {

        /** No periodic hazard at all. */
        NONE,

        /** Only characters standing on a hazard tile are affected. */
        TILE_BASED,

        /** Every character on the map is affected, wherever they stand. */
        EVERY_PLAYER
    }

    /** Public final to mirror the other display-name fields in this package. */
    public final String displayName;

    private final String description;
    private final String mapFileName;
    private final String hazardLayerName;
    private final StatusEffectType hazardEffect;
    private final HazardScope hazardScope;
    private final boolean fallingIsLethal;

    Environment(String displayName, String description, String mapFileName,
                String hazardLayerName, StatusEffectType hazardEffect,
                HazardScope hazardScope, boolean fallingIsLethal) {
        this.displayName = displayName;
        this.description = description;
        this.mapFileName = mapFileName;
        this.hazardLayerName = hazardLayerName;
        this.hazardEffect = hazardEffect;
        this.hazardScope = hazardScope;
        this.fallingIsLethal = fallingIsLethal;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Flavour text for the environment-voting screen. */
    public String getDescription() {
        return description;
    }

    /**
     * Tiled map file for this environment.
     *
     * <p>Only {@code canyon.tmx} currently exists in {@code assets/maps/}; the
     * bog and lava maps are still to be authored, and the client falls back to
     * the default map until they are added.</p>
     */
    public String getMapFileName() {
        return mapFileName;
    }

    /** Tiled object layer marking hazardous ground, or {@code null} if none. */
    public String getHazardLayerName() {
        return hazardLayerName;
    }

    /** Status effect this environment inflicts, or {@code null} if none. */
    public StatusEffectType getHazardEffect() {
        return hazardEffect;
    }

    public HazardScope getHazardScope() {
        return hazardScope;
    }

    /** True when leaving the walkable area kills outright (Canyon). */
    public boolean isFallingLethal() {
        return fallingIsLethal;
    }

    /** True when the hazard applies to everyone every turn (Lava). */
    public boolean affectsEveryone() {
        return hazardScope == HazardScope.EVERY_PLAYER;
    }

    /** True when only characters on marked tiles are affected (Bog). */
    public boolean affectsHazardTilesOnly() {
        return hazardScope == HazardScope.TILE_BASED;
    }

    /** One-line hazard summary for the voting screen. */
    public String hazardSummary() {
        switch (hazardScope) {
            case EVERY_PLAYER:
                return "Every player suffers " + hazardEffect.getDisplayName() + " each turn";
            case TILE_BASED:
                return hazardEffect.getDisplayName() + " while standing on hazard tiles";
            default:
                return fallingIsLethal ? "No hazards — but falling is instant death"
                    : "No hazards";
        }
    }

    /** Case-insensitive lookup that returns {@code null} instead of throwing. */
    public static Environment fromName(String name) {
        if (name == null) return null;
        for (Environment environment : values()) {
            if (environment.name().equalsIgnoreCase(name)
                || environment.displayName.equalsIgnoreCase(name)) {
                return environment;
            }
        }
        return null;
    }
}
