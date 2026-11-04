package com.architect.modules;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.architect.config.ArchitectState;
import com.architect.config.CheatTiers.ConstructionTier;
import com.architect.config.CheatTiers.MaintenanceTier;

import game.GAME;
import game.faction.FResources.RTYPE;
import init.resources.RESOURCE;
import init.resources.RESOURCES;
import settlement.job.JOBS;
import settlement.job.Job;
import settlement.job.JobBuild;
import settlement.main.SETT;
import settlement.maintenance.MAINTENANCE;
import settlement.room.infra.janitor.ROOM_JANITOR;
import settlement.room.main.Room;
import settlement.room.main.RoomInstance;
import settlement.room.main.construction.CONSTRUCTION;
import snake2d.util.sets.Bitmap1D;
import snake2d.util.sets.Bitsmap1D;

/**
 * Creative construction and maintenance automation subsystem.
 * Supports:
 * - Instant Build (immediate completion of structures, rooms, roads, walls, ceilings)
 * - Free Materials / Keep Work (0 resource costs, oddjobbers still physically build)
 * - Janitors Work / Free Mats (maintenance simulation preserved, zero resource cost)
 * - Zero Decay (continuous pristine condition)
 */
public final class ConstructionModule implements ArchitectModule {

    private double tickTimer = 0.0;
    private static final double TICK_STEP = 0.5;

    // Cached reflection handles for package-private engine classes
    private static Method jobFinsihMethod;
    private static Method totalResourcesNeededMethod;

    private static Field bitsField;
    private static Method resAmMethod;
    private static Method resIncMethod;

    // Reflection handles for settlement jobs (roads, walls, ceilings, fortifications)
    private static Method jobBuildConstructMethod;
    private static Field jobsProgressField;
    private static Field jobsWantsResField;

    @Override
    public void init() {
        // Scaffolding initialization
    }

    @Override
    public void update(double ds, ArchitectState state) {
        tickTimer += ds;
        if (tickTimer < TICK_STEP) {
            return;
        }
        tickTimer = 0.0;

        if (!SETT.exists()) {
            return;
        }

        // 1. Instant Construction
        if (state.constructionTier == ConstructionTier.INSTANT_BUILD) {
            triggerInstantBuildAll();
        }
        // 2. Free Materials / Keep Work (or Fast & Free)
        else if (state.constructionTier == ConstructionTier.FREE_MATERIALS_KEEP_WORK ||
                 state.constructionTier == ConstructionTier.FAST_AND_FREE) {
            processFreeMaterials(state.constructionTier == ConstructionTier.FAST_AND_FREE);
            processOutsideJobs(state.constructionTier == ConstructionTier.FAST_AND_FREE);
        }

        // 3. Maintenance Logic
        if (state.maintenanceTier == MaintenanceTier.NO_DECAY && SETT.MAINTENANCE() != null) {
            MAINTENANCE maintenance = SETT.MAINTENANCE();
            if (maintenance.needs != null) {
                int width = SETT.TWIDTH;
                int height = SETT.THEIGHT;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        if (maintenance.needs.is(x, y)) {
                            maintenance.maintain(x, y);
                        }
                    }
                }
            }
        } else if (state.maintenanceTier == MaintenanceTier.JANITOR_WORK_FREE_MATS) {
            keepJanitorSuppliesFull();
        }
    }

    /**
     * Supplies room construction sites with needed materials without deducting from the player,
     * allowing oddjobbers to immediately begin physical construction.
     */
    private void processFreeMaterials(boolean fastSpeed) {
        if (SETT.ROOMS() == null || SETT.ROOMS().construction == null) {
            return;
        }
        CONSTRUCTION construction = SETT.ROOMS().construction;
        if (construction.instances() == 0 || construction.isser == null) {
            return;
        }

        int width = SETT.TWIDTH;
        int height = SETT.THEIGHT;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (construction.isser.is(x, y)) {
                    if (fastSpeed) {
                        construction.construct(x, y);
                    } else {
                        Room room = SETT.ROOMS().map.get(x, y);
                        if (room != null) {
                            fulfillTileResources(room, x, y);
                        }
                    }
                }
            }
        }
    }

    /**
     * Supplies outside jobs (roads, squares, walls, ceilings, fortifications) with needed materials
     * directly at the job site so oddjobbers do not need to fetch resources from player stockpiles.
     */
    private void processOutsideJobs(boolean fastSpeed) {
        if (SETT.JOBS() == null || SETT.JOBS().getter == null) {
            return;
        }

        try {
            if (jobsProgressField == null) {
                jobsProgressField = JOBS.class.getDeclaredField("progress");
                jobsProgressField.setAccessible(true);
                jobsWantsResField = JOBS.class.getDeclaredField("wantsRes");
                jobsWantsResField.setAccessible(true);
            }
            Bitsmap1D progress = (Bitsmap1D) jobsProgressField.get(SETT.JOBS());
            Bitmap1D wantsRes = (Bitmap1D) jobsWantsResField.get(SETT.JOBS());

            int width = SETT.TWIDTH;
            int height = SETT.THEIGHT;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (SETT.JOBS().getter.is(x, y)) {
                        Job job = SETT.JOBS().getter.get(x, y);
                        if (job instanceof JobBuild) {
                            JobBuild jb = (JobBuild) job;
                            if (fastSpeed) {
                                completeJobBuild(jb, x, y);
                            } else {
                                int tile = y * width + x;
                                int needed = jb.resAmount();
                                if (progress != null && progress.get(tile) < needed) {
                                    progress.set(tile, needed);
                                    if (wantsRes != null) {
                                        wantsRes.set(tile, false);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Protected against reflection edge cases
        }
    }

    private void completeJobBuild(JobBuild job, int x, int y) {
        try {
            if (jobBuildConstructMethod == null) {
                jobBuildConstructMethod = JobBuild.class.getDeclaredMethod("construct", int.class, int.class);
                jobBuildConstructMethod.setAccessible(true);
            }
            RESOURCE res = job.res();
            int amount = job.resAmount();
            jobBuildConstructMethod.invoke(job, x, y);
            if (SETT.JOBS() != null && SETT.JOBS().clearer != null) {
                SETT.JOBS().clearer.set(x, y);
            }
            // Refund any resources deducted during construct so player stockpile remains 100% free
            if (res != null && amount > 0 && GAME.player() != null && GAME.player().res() != null) {
                GAME.player().res().inc(res, RTYPE.CONSTRUCTION, amount);
            }
        } catch (Exception ignored) {
        }
    }

    private void fulfillTileResources(Room room, int x, int y) {
        try {
            if (jobFinsihMethod == null) {
                Class<?> clazz = room.getClass();
                jobFinsihMethod = clazz.getMethod("jobFinsih", int.class, int.class, RESOURCE.class, int.class);
                totalResourcesNeededMethod = clazz.getMethod("totalResourcesNeeded", int.class, int.class);
            }
            int needed = (int) totalResourcesNeededMethod.invoke(room, x, y);
            if (needed > 0 && room.constructor() != null) {
                int resCount = room.constructor().resources();
                for (int i = 0; i < resCount; i++) {
                    RESOURCE res = room.constructor().resource(i);
                    jobFinsihMethod.invoke(room, x, y, res, needed);
                    if (GAME.player() != null && GAME.player().res() != null) {
                        GAME.player().res().inc(res, RTYPE.CONSTRUCTION, needed);
                    }
                }
            }
        } catch (Exception ignored) {
            // Protected against reflection edge cases during deconstruction
        }
    }

    /**
     * Refills janitor room maintenance reserves so janitors repair buildings without consuming player supplies.
     */
    private void keepJanitorSuppliesFull() {
        try {
            if (SETT.ROOMS() == null) return;
            ROOM_JANITOR janitorBlueprint = SETT.ROOMS().JANITOR;
            if (janitorBlueprint == null) return;

            int count = janitorBlueprint.instancesSize();
            for (int i = 0; i < count; i++) {
                RoomInstance ins = janitorBlueprint.getInstance(i);
                if (ins == null) continue;

                if (bitsField == null) {
                    bitsField = ins.getClass().getDeclaredField("bits");
                    bitsField.setAccessible(true);
                }
                Object bits = bitsField.get(ins);
                if (bits != null) {
                    if (resAmMethod == null) {
                        resAmMethod = bits.getClass().getMethod("resAm", RESOURCE.class);
                        resIncMethod = bits.getClass().getMethod("resInc", ins.getClass(), RESOURCE.class, int.class);
                    }
                    for (RESOURCE res : RESOURCES.ALL()) {
                        int current = (int) resAmMethod.invoke(bits, res);
                        if (current < 20) {
                            resIncMethod.invoke(bits, ins, res, 20);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Instantly finishes all ongoing construction sites settlement-wide
     * (Rooms, Road paving, Wall building, Ceilings, Fortifications).
     */
    public void triggerInstantBuildAll() {
        if (!SETT.exists()) {
            return;
        }

        // 1. Room construction sites
        if (SETT.ROOMS() != null && SETT.ROOMS().construction != null) {
            CONSTRUCTION construction = SETT.ROOMS().construction;
            if (construction.isser != null && construction.instances() > 0) {
                int width = SETT.TWIDTH;
                int height = SETT.THEIGHT;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        if (construction.isser.is(x, y)) {
                            construction.construct(x, y);
                        }
                    }
                }
            }
        }

        // 2. Outside jobs (Roads, Floors, Walls, Ceilings, Fortifications)
        if (SETT.JOBS() != null && SETT.JOBS().getter != null) {
            int width = SETT.TWIDTH;
            int height = SETT.THEIGHT;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (SETT.JOBS().getter.is(x, y)) {
                        Job job = SETT.JOBS().getter.get(x, y);
                        if (job instanceof JobBuild) {
                            completeJobBuild((JobBuild) job, x, y);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onStateChanged(ArchitectState state) {
        if (state.constructionTier == ConstructionTier.INSTANT_BUILD) {
            triggerInstantBuildAll();
        }
    }
}
