package com.treezones.tools;

import com.treezones.config.TreeZoneConfig;
import com.treezones.util.I18n;
import com.treezones.zone.GardenerActions;
import com.treezones.zone.TreeZoneManager;
import init.sprite.SPRITES;
import settlement.main.SETT;
import snake2d.util.datatypes.AREA;
import snake2d.util.sprite.SPRITE;
import view.tool.PLACER_TYPE;
import view.tool.PlacableMulti;

/**
 * Drag & Drop Tool for Instant Flora & Vegetation Clearing.
 * Clears trees, flowers, bushes, mushrooms, and herbs within the selected area.
 */
public final class AreaClearPlacer extends PlacableMulti {

    private final TreeZoneConfig config;
    private final TreeZoneManager zoneManager;

    public AreaClearPlacer(TreeZoneConfig config, TreeZoneManager zoneManager, SPRITE icon) {
        super(I18n.get("tool.clear_area.name"), I18n.get("tool.clear_area.desc"), icon != null ? icon : ((SPRITES.icons() != null && SPRITES.icons().m != null) ? SPRITES.icons().m.clear_food : null));
        this.config = config;
        this.zoneManager = zoneManager;
    }

    @Override
    public CharSequence name() {
        return I18n.get("tool.clear_area.name");
    }

    @Override
    public CharSequence desc() {
        return I18n.get("tool.clear_area.desc");
    }

    @Override
    public CharSequence isPlacable(int tx, int ty, AREA area, PLACER_TYPE type) {
        if (!SETT.IN_BOUNDS(tx, ty)) {
            return I18n.get("status.out_of_bounds");
        }
        return null;
    }

    @Override
    public void place(int tx, int ty, AREA area, PLACER_TYPE type) {
        GardenerActions.clearTileFlora(tx, ty, config.isDropWood());
        if (config.isAutoNoGrowth()) {
            zoneManager.applyNoTree(tx, ty, false, false);
        }
    }
}
