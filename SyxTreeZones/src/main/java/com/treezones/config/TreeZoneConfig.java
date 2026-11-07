package com.treezones.config;

import java.io.IOException;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;

/**
 * Configuration state for Syx Tree Zones.
 * Supports backward-compatible binary save and load.
 */
public final class TreeZoneConfig {

    public static final int VERSION = 2;

    private boolean instantClear = true;
    private boolean dropWood = true;
    private boolean instantPlant = true;
    private boolean showOverlay = true;
    private boolean showHudButton = true;
    private boolean autoNoGrowth = false;

    public boolean isAutoNoGrowth() {
        return autoNoGrowth;
    }

    public void setAutoNoGrowth(boolean autoNoGrowth) {
        this.autoNoGrowth = autoNoGrowth;
    }

    public void toggleAutoNoGrowth() {
        this.autoNoGrowth = !this.autoNoGrowth;
    }

    public boolean isInstantClear() {
        return instantClear;
    }

    public void setInstantClear(boolean instantClear) {
        this.instantClear = instantClear;
    }

    public void toggleInstantClear() {
        this.instantClear = !this.instantClear;
    }

    public boolean isDropWood() {
        return dropWood;
    }

    public void setDropWood(boolean dropWood) {
        this.dropWood = dropWood;
    }

    public void toggleDropWood() {
        this.dropWood = !this.dropWood;
    }

    public boolean isInstantPlant() {
        return instantPlant;
    }

    public void setInstantPlant(boolean instantPlant) {
        this.instantPlant = instantPlant;
    }

    public void toggleInstantPlant() {
        this.instantPlant = !this.instantPlant;
    }

    public boolean isShowOverlay() {
        return showOverlay;
    }

    public void setShowOverlay(boolean showOverlay) {
        this.showOverlay = showOverlay;
    }

    public void toggleOverlay() {
        this.showOverlay = !this.showOverlay;
    }

    public boolean isShowHudButton() {
        return showHudButton;
    }

    public void setShowHudButton(boolean showHudButton) {
        this.showHudButton = showHudButton;
    }

    public void toggleHudButton() {
        this.showHudButton = !this.showHudButton;
    }

    public void save(FilePutter file) {
        file.i(VERSION);
        file.bool(instantClear);
        file.bool(dropWood);
        file.bool(instantPlant);
        file.bool(showOverlay);
        file.bool(showHudButton);
        file.bool(autoNoGrowth);
    }

    public void load(FileGetter file) throws IOException {
        int v = file.i();
        if (v >= 1) {
            instantClear = file.bool();
            dropWood = file.bool();
            instantPlant = file.bool();
            showOverlay = file.bool();
            showHudButton = file.bool();
        }
        if (v >= 2) {
            autoNoGrowth = file.bool();
        }
    }
}
