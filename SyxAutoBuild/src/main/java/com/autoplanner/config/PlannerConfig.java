package com.autoplanner.config;

import java.io.IOException;

import com.autoplanner.util.I18n;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;

/**
 * Configuration state for Syx Auto-Planner.
 * Supports binary serialization into savegames with version check.
 */
public final class PlannerConfig {

    private static final int MAGIC = 0x53595850; // "SYXP"
    private static final int VERSION = 2;

    public enum LayoutStyle {
        COMPACT,
        BALANCED;

        public LayoutStyle next() {
            return this == COMPACT ? BALANCED : COMPACT;
        }
    }

    private LayoutStyle style = LayoutStyle.COMPACT;
    private double targetEfficiency = 1.0; // 100%
    private double storageRatio = 0.05;     // 5% storage allowance
    private boolean autoStrutOnPlace = false;
    private boolean showOverlay = true;
    private String language = "en";

    public LayoutStyle getStyle() {
        return style;
    }

    public void setStyle(LayoutStyle style) {
        if (style != null) {
            this.style = style;
        }
    }

    public void cycleStyle() {
        this.style = this.style.next();
    }

    public double getTargetEfficiency() {
        return targetEfficiency;
    }

    public void setTargetEfficiency(double targetEfficiency) {
        this.targetEfficiency = Math.max(0.5, Math.min(2.0, targetEfficiency));
    }

    public void cycleTargetEfficiency() {
        if (targetEfficiency <= 0.85) {
            targetEfficiency = 1.0;
        } else if (targetEfficiency <= 1.05) {
            targetEfficiency = 1.2;
        } else {
            targetEfficiency = 0.8;
        }
    }

    public double getStorageRatio() {
        return storageRatio;
    }

    public void setStorageRatio(double storageRatio) {
        this.storageRatio = Math.max(0.0, Math.min(0.5, storageRatio));
    }

    public void cycleStorageRatio() {
        if (storageRatio < 0.04) {
            storageRatio = 0.05;
        } else if (storageRatio < 0.09) {
            storageRatio = 0.10;
        } else if (storageRatio < 0.19) {
            storageRatio = 0.20;
        } else {
            storageRatio = 0.0;
        }
    }

    public void resetDefaults() {
        this.style = LayoutStyle.COMPACT;
        this.targetEfficiency = 1.0;
        this.storageRatio = 0.05;
        this.language = "en";
        I18n.setLanguage(I18n.Language.EN);
    }

    public boolean isAutoStrutOnPlace() {
        return autoStrutOnPlace;
    }

    public void setAutoStrutOnPlace(boolean autoStrutOnPlace) {
        this.autoStrutOnPlace = autoStrutOnPlace;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String lang) {
        if (lang != null) {
            this.language = lang;
            I18n.setLanguage(I18n.Language.fromCode(lang));
        }
    }

    public void save(FilePutter file) {
        file.i(MAGIC);
        file.i(VERSION);
        file.i(style.ordinal());
        file.d(targetEfficiency);
        file.d(storageRatio);
        file.bool(autoStrutOnPlace);
        file.bool(showOverlay);
        file.chars(language != null ? language : "en");
    }

    public void load(FileGetter file) throws IOException {
        int magic = file.i();
        if (magic != MAGIC) {
            return;
        }
        int ver = file.i();
        if (ver >= 1) {
            int styleOrd = file.i();
            LayoutStyle[] styles = LayoutStyle.values();
            if (styleOrd >= 0 && styleOrd < styles.length) {
                this.style = styles[styleOrd];
            }
            this.targetEfficiency = file.d();
            this.storageRatio = file.d();
            this.autoStrutOnPlace = file.bool();
            this.showOverlay = file.bool();

            if (ver >= 2) {
                this.language = file.chars();
                I18n.setLanguage(I18n.Language.fromCode(language));
            }
        }
    }
}
