package com.voidcrates;

import com.voidcrates.util.I18n;
import script.SCRIPT;

/**
 * Entry-point for Syx Void Crates mod.
 * Discovered dynamically by Songs of Syx via script.ScriptLoad.
 */
public final class VoidCratesScript implements SCRIPT {

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
        return new VoidCratesInstance();
    }
}
