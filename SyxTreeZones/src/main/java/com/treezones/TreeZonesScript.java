package com.treezones;

import com.treezones.util.I18n;
import script.SCRIPT;

/**
 * Entry-point for Syx Tree Zones mod.
 * Discovered dynamically by Songs of Syx via script.ScriptLoad.
 */
public final class TreeZonesScript implements SCRIPT {

    @Override
    public CharSequence name() {
        return I18n.get("mod.name");
    }

    @Override
    public CharSequence desc() {
        return I18n.get("mod.desc");
    }

    @Override
    public boolean forceInit() {
        return true;
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public SCRIPT_INSTANCE createInstance() {
        return new TreeZonesInstance();
    }
}
