package com.treezones.tools;

import com.treezones.config.TreeZoneConfig;
import com.treezones.util.I18n;
import com.treezones.zone.TreeZoneManager;
import init.sprite.SPRITES;
import settlement.main.SETT;
import snake2d.util.datatypes.AREA;
import snake2d.util.sprite.SPRITE;
import view.tool.PLACER_TYPE;
import view.tool.PlacableMulti;

/**
 * Drag & Drop Tool for Forest (Cultivation / Forced Growth) Zones.
 * Integrates natively with Songs of Syx rectangle, line, and brush placers.
 */
public final class ForestPlacer extends PlacableMulti {

    private final TreeZoneManager zoneManager;
    private final TreeZoneConfig config;

    public ForestPlacer(TreeZoneManager zoneManager, TreeZoneConfig config, SPRITE icon) {
        super(I18n.get("tool.forest.name"), I18n.get("tool.forest.desc"), icon);
        this.zoneManager = zoneManager;
        this.config = config;
    }

    @Override
    public CharSequence name() {
        return I18n.get("tool.forest.name");
    }

    @Override
    public CharSequence desc() {
        return I18n.get("tool.forest.desc");
    }

    @Override
    public CharSequence isPlacable(int tx, int ty, AREA area, PLACER_TYPE type) {
        if (!SETT.IN_BOUNDS(tx, ty)) {
            return I18n.get("status.out_of_bounds");
        }
        if (SETT.TERRAIN() != null && SETT.TERRAIN().WATER != null && SETT.TERRAIN().WATER.is != null && SETT.TERRAIN().WATER.is.is(tx, ty)) {
            return I18n.get("status.water_blocked");
        }
        if (SETT.TERRAIN() != null && SETT.TERRAIN().MOUNTAIN != null && SETT.TERRAIN().MOUNTAIN.isMountain(tx, ty)) {
            return I18n.get("status.mountain_blocked");
        }
        return null; // Valid tile
    }

    @Override
    public void place(int tx, int ty, AREA area, PLACER_TYPE type) {
        zoneManager.applyForest(tx, ty, config.isInstantPlant());
    }
}
