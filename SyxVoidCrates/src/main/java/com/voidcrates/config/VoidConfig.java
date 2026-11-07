package com.voidcrates.config;

import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;

import java.io.IOException;

public final class VoidConfig {

    public static final int SAVE_MAGIC = 0x564F4944; // 'VOID'
    public static final int SAVE_VERSION = 1;

    private boolean active = true;
    private boolean showHudButton = true;
    private String language = "de";

    // Auto-void resource toggles
    private boolean autoVoidStone = true;
    private boolean autoVoidClay = true;
    private boolean autoVoidCoal = false;
    private boolean autoVoidLivestock = false;
    private boolean autoVoidWood = false;
    private boolean autoVoidOres = false;

    // Threshold & buffer settings
    // 0 = 100% full, 1 = 90% full, 2 = 75% full, 3 = instant
    private int thresholdIndex = 0;
    // 0 = 0% retain, 1 = 20% retain, 2 = 50% retain
    private int retainBufferIndex = 0;

    // Scope
    private boolean designatedOnly = false;

    // Lifetime statistic counter
    private long totalItemsVoided = 0L;

    public void save(FilePutter file) {
        file.i(SAVE_MAGIC);
        file.i(SAVE_VERSION);

        file.bool(active);
        file.bool(showHudButton);
        file.chars(language != null ? language : "de");

        file.bool(autoVoidStone);
        file.bool(autoVoidClay);
        file.bool(autoVoidCoal);
        file.bool(autoVoidLivestock);
        file.bool(autoVoidWood);
        file.bool(autoVoidOres);

        file.i(thresholdIndex);
        file.i(retainBufferIndex);
        file.bool(designatedOnly);
        file.l(totalItemsVoided);
    }

    public void load(FileGetter file) throws IOException {
        int magic = file.i();
        if (magic != SAVE_MAGIC) {
            return;
        }
        int version = file.i();
        if (version >= 1) {
            active = file.bool();
            showHudButton = file.bool();
            language = file.chars();

            autoVoidStone = file.bool();
            autoVoidClay = file.bool();
            autoVoidCoal = file.bool();
            autoVoidLivestock = file.bool();
            autoVoidWood = file.bool();
            autoVoidOres = file.bool();

            thresholdIndex = file.i();
            retainBufferIndex = file.i();
            designatedOnly = file.bool();
            totalItemsVoided = file.l();
        }
    }

    public double getTriggerRatio() {
        switch (thresholdIndex) {
            case 1: return 0.90;
            case 2: return 0.75;
            case 3: return 0.01; // Instant void on deposit
            case 0:
            default: return 1.00;
        }
    }

    public double getRetainRatio() {
        switch (retainBufferIndex) {
            case 1: return 0.20;
            case 2: return 0.50;
            case 0:
            default: return 0.0;
        }
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isShowHudButton() { return showHudButton; }
    public void setShowHudButton(boolean show) { this.showHudButton = show; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public boolean isAutoVoidStone() { return autoVoidStone; }
    public void setAutoVoidStone(boolean val) { this.autoVoidStone = val; }

    public boolean isAutoVoidClay() { return autoVoidClay; }
    public void setAutoVoidClay(boolean val) { this.autoVoidClay = val; }

    public boolean isAutoVoidCoal() { return autoVoidCoal; }
    public void setAutoVoidCoal(boolean val) { this.autoVoidCoal = val; }

    public boolean isAutoVoidLivestock() { return autoVoidLivestock; }
    public void setAutoVoidLivestock(boolean val) { this.autoVoidLivestock = val; }

    public boolean isAutoVoidWood() { return autoVoidWood; }
    public void setAutoVoidWood(boolean val) { this.autoVoidWood = val; }

    public boolean isAutoVoidOres() { return autoVoidOres; }
    public void setAutoVoidOres(boolean val) { this.autoVoidOres = val; }

    public int getThresholdIndex() { return thresholdIndex; }
    public void setThresholdIndex(int index) { this.thresholdIndex = (index >= 0 && index <= 3) ? index : 0; }
    public void cycleThreshold() { this.thresholdIndex = (thresholdIndex + 1) % 4; }

    public int getRetainBufferIndex() { return retainBufferIndex; }
    public void setRetainBufferIndex(int index) { this.retainBufferIndex = (index >= 0 && index <= 2) ? index : 0; }
    public void cycleRetainBuffer() { this.retainBufferIndex = (retainBufferIndex + 1) % 3; }

    public boolean isDesignatedOnly() { return designatedOnly; }
    public void setDesignatedOnly(boolean val) { this.designatedOnly = val; }

    public long getTotalItemsVoided() { return totalItemsVoided; }
    public void addVoided(long amount) { if (amount > 0) totalItemsVoided += amount; }
    public void setTotalItemsVoided(long val) { this.totalItemsVoided = Math.max(0, val); }
}
