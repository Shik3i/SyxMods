package com.treezones.tools;

import com.treezones.util.I18n;
import com.treezones.zone.TreeZoneManager;
import init.sprite.SPRITES;
import settlement.main.SETT;
import snake2d.util.datatypes.AREA;
import snake2d.util.sprite.SPRITE;
import view.tool.PLACER_TYPE;
import view.tool.PlacableMulti;

/**
 * Drag & Drop Tool for Resetting / Clearing Custom Zones.
 * Restores natural terrain growth state.
 */
public final class ResetPlacer extends PlacableMulti {

    private final TreeZoneManager zoneManager;

    public ResetPlacer(TreeZoneManager zoneManager, SPRITE icon) {
        super(I18n.get("tool.reset.name"), I18n.get("tool.reset.desc"), icon);
        this.zoneManager = zoneManager;
    }

    @Override
    public CharSequence name() {
        return I18n.get("tool.reset.name");
    }

    @Override
    public CharSequence desc() {
        return I18n.get("tool.reset.desc");
    }

    @Override
    public CharSequence isPlacable(int tx, int ty, AREA area, PLACER_TYPE type) {
        if (!SETT.IN_BOUNDS(tx, ty)) {
            return I18n.get("status.out_of_bounds");
        }
        return null; // Valid tile
    }

    @Override
    public void place(int tx, int ty, AREA area, PLACER_TYPE type) {
        zoneManager.applyReset(tx, ty);
    }
}
