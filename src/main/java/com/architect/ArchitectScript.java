package com.architect;

import com.architect.util.I18n;

import script.SCRIPT;
import snake2d.util.misc.ACTION;
import view.interrupter.IDebugPanel;

/**
 * Main script factory and lifecycle entry point for Songs of Syx mod loader.
 */
public final class ArchitectScript implements SCRIPT {

    private static ArchitectInstance currentInstance;

    @Override
    public CharSequence name() {
        return I18n.get("mod.name");
    }

    @Override
    public CharSequence desc() {
        return I18n.get("mod.desc");
    }

    @Override
    public void initBeforeGameCreated() {
        // Early hook before game world initialization
    }

    @Override
    public void initBeforeGameInited() {
        // Register in-game debug panel hook for easy access
        IDebugPanel.add("Syx Architect", new ACTION() {
            @Override
            public void exe() {
                if (currentInstance != null) {
                    currentInstance.openUI();
                }
            }
        });
    }

    @Override
    public SCRIPT_INSTANCE createInstance() {
        currentInstance = new ArchitectInstance();
        return currentInstance;
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean forceInit() {
        return false;
    }

    public static ArchitectInstance getCurrentInstance() {
        return currentInstance;
    }
}
