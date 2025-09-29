/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.util;

import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class CoordAdapter {
    private final Map<AdvancementKey, Coord> advancementCoords;
    private float lowestX;
    private float lowestY;

    public CoordAdapter(@NotNull Map<AdvancementKey, Coord> advancementCoords) {
        this.advancementCoords = Objects.requireNonNull(advancementCoords, "Advancement coords is null.");
        for (Map.Entry<AdvancementKey, Coord> e : advancementCoords.entrySet()) {
            Preconditions.checkNotNull((Object)e.getKey(), (Object)"An AdvancementKey is null.");
            Coord coords = e.getValue();
            Preconditions.checkNotNull((Object)coords, (Object)(String.valueOf(e.getKey()) + "'s coords entry is null."));
            if (coords.x < this.lowestX) {
                this.lowestX = coords.x;
            }
            if (!(coords.y < this.lowestY)) continue;
            this.lowestY = coords.y;
        }
        if (this.lowestX < 0.0f) {
            this.lowestX = -this.lowestX;
        }
        if (this.lowestY < 0.0f) {
            this.lowestY = -this.lowestY;
        }
    }

    public float getX(@NotNull AdvancementKey key) throws IllegalArgumentException {
        Coord coord = this.advancementCoords.get(Objects.requireNonNull(key, "Key is null."));
        Preconditions.checkArgument((coord != null ? 1 : 0) != 0, (Object)("Couldn't find key \"" + String.valueOf(key) + "\"."));
        return coord.x + this.lowestX;
    }

    public float getY(@NotNull AdvancementKey key) throws IllegalArgumentException {
        Coord coord = this.advancementCoords.get(Objects.requireNonNull(key, "Key is null."));
        Preconditions.checkArgument((coord != null ? 1 : 0) != 0, (Object)("Couldn't find key \"" + String.valueOf(key) + "\"."));
        return coord.y + this.lowestY;
    }

    @NotNull
    public Coord getXAndY(@NotNull AdvancementKey key) throws IllegalArgumentException {
        Coord coord = this.advancementCoords.get(Objects.requireNonNull(key, "Key is null."));
        Preconditions.checkArgument((coord != null ? 1 : 0) != 0, (Object)("Couldn't find key \"" + String.valueOf(key) + "\"."));
        return new Coord(coord.x + this.lowestX, coord.y + this.lowestY);
    }

    public float getOriginalX(@NotNull AdvancementDisplay display) {
        return this.getOriginalX(Objects.requireNonNull(display, "AdvancementDisplay is null.").getX());
    }

    public float getOriginalX(@NotNull AdvancementKey key) throws IllegalArgumentException {
        Coord coord = this.advancementCoords.get(Objects.requireNonNull(key, "Key is null."));
        Preconditions.checkArgument((coord != null ? 1 : 0) != 0, (Object)("Couldn't find key \"" + String.valueOf(key) + "\"."));
        return coord.x;
    }

    public float getOriginalX(float x) {
        Preconditions.checkArgument((boolean)Float.isFinite(x), (Object)"y coordinate is not finite.");
        return x - this.lowestX;
    }

    public float getOriginalY(@NotNull AdvancementDisplay display) {
        return this.getOriginalY(Objects.requireNonNull(display, "AdvancementDisplay is null.").getY());
    }

    public float getOriginalY(@NotNull AdvancementKey key) throws IllegalArgumentException {
        Coord coord = this.advancementCoords.get(Objects.requireNonNull(key, "Key is null."));
        Preconditions.checkArgument((coord != null ? 1 : 0) != 0, (Object)("Couldn't find key \"" + String.valueOf(key) + "\"."));
        return coord.y;
    }

    public float getOriginalY(float y) {
        Preconditions.checkArgument((boolean)Float.isFinite(y), (Object)"y coordinate is not finite.");
        return y - this.lowestY;
    }

    @NotNull
    public Coord getOriginalXAndY(@NotNull AdvancementDisplay display) {
        Preconditions.checkNotNull((Object)display, (Object)"AdvancementDisplay is null.");
        return this.getOriginalXAndY(display.getX(), display.getY());
    }

    @NotNull
    public Coord getOriginalXAndY(@NotNull AdvancementKey key) throws IllegalArgumentException {
        Coord coord = this.advancementCoords.get(Objects.requireNonNull(key, "Key is null."));
        Preconditions.checkArgument((coord != null ? 1 : 0) != 0, (Object)("Couldn't find key \"" + String.valueOf(key) + "\"."));
        return coord;
    }

    @NotNull
    public Coord getOriginalXAndY(@NotNull Coord coord) {
        Preconditions.checkNotNull((Object)coord, (Object)"Coord is null.");
        return this.getOriginalXAndY(coord.x, coord.y);
    }

    @NotNull
    public Coord getOriginalXAndY(float x, float y) {
        return new Coord(this.getOriginalX(x), this.getOriginalY(y));
    }

    @NotNull
    @Contract(pure=true, value="-> new")
    public static CoordAdapterBuilder builder() {
        return new CoordAdapterBuilder();
    }

    public record Coord(float x, float y) {
        public Coord {
            Preconditions.checkArgument((boolean)Float.isFinite(x), (Object)"x coordinate is not finite.");
            Preconditions.checkArgument((boolean)Float.isFinite(y), (Object)"y coordinate is not finite.");
        }
    }

    public static final class CoordAdapterBuilder {
        private final Map<AdvancementKey, Coord> advancementCoords = new HashMap<AdvancementKey, Coord>();

        @NotNull
        public CoordAdapterBuilder add(@NotNull AdvancementKey key, float x, float y) {
            Preconditions.checkNotNull((Object)key, (Object)"Key is null");
            Preconditions.checkArgument((boolean)Float.isFinite(x), (Object)(String.valueOf(key) + "'s x value is not finite."));
            Preconditions.checkArgument((boolean)Float.isFinite(y), (Object)(String.valueOf(key) + "'s y value is not finite."));
            this.advancementCoords.put(key, new Coord(x, y));
            return this;
        }

        @NotNull
        public CoordAdapterBuilder offset(@NotNull AdvancementKey key, @NotNull AdvancementKey keyOfParent, float offsetX, float offsetY) throws IllegalArgumentException {
            Preconditions.checkNotNull((Object)key, (Object)"Key is null");
            Preconditions.checkNotNull((Object)keyOfParent, (Object)"Key of parent is null");
            Preconditions.checkArgument((boolean)Float.isFinite(offsetX), (Object)(String.valueOf(key) + "'s offsetX value is not finite."));
            Preconditions.checkArgument((boolean)Float.isFinite(offsetY), (Object)(String.valueOf(key) + "'s offsetY value is not finite."));
            Coord coord = this.advancementCoords.get(keyOfParent);
            if (coord == null) {
                throw new IllegalArgumentException("Cannot find key \"" + String.valueOf(keyOfParent) + "\".");
            }
            this.advancementCoords.put(key, new Coord(coord.x + offsetX, coord.y + offsetY));
            return this;
        }

        @NotNull
        public CoordAdapter build() {
            return new CoordAdapter(this.advancementCoords);
        }
    }
}

