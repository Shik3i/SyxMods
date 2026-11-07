package com.architect.modules;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.architect.config.ArchitectState;
import com.architect.config.CheatTiers.ConstructionTier;
import com.architect.config.CheatTiers.JobYieldTier;
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
import settlement.thing.ThingsResources.ScatteredResource;
import settlement.tilemap.terrain.Terrain.TerrainTile;
import snake2d.util.sets.Bitmap1D;
import snake2d.util.sets.Bitsmap1D;

/**
 * Creative construction and maintenance automation subsystem.
 * Supports:
 * - Instant Build (immediate completion of structures, rooms, roads, walls, ceilings)
 * - Free Materials / Keep Work (0 resource costs, oddjobbers still physically build)
 * - Janitors Work / Free Mats (maintenance simulation preserved, zero resource cost)
 * - Zero Decay (continuous pristine condition)
 *
 * PERFORMANCE:
 * Uses a rolling chunk scan (budgeted row scanning) across ticks to eliminate frame-time spikes
 * on colossal 768x768 maps.
 */
public final class ConstructionModule implements ArchitectModule {

    // Wall-clock rate limiting: ensures constant 10 Hz execution whether running at 1x or 250x simulation speed
    private long lastUpdateMs = 0;
    private static final long UPDATE_INTERVAL_MS = 100; // 100ms = 10 executions per real second

    // Rolling chunk cursor for lag-free updates across colossal maps
    private int scanRowCursor = 0;
    private static final int ROWS_PER_TICK = 48; // Scans 48 rows per tick

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
    private static Field jobsMapField;
    private static Field bitsmap2DMapField;
    private static Field bitsField1D;
    private static Field constructionBlueprintField;
    private static Field blueprintAllField;
    private static boolean bitsReflectionFailed = false;
    private static boolean roomReflectionFailed = false;
    private static boolean outsideJobsReflectionFailed = false;
    private static boolean janitorReflectionFailed = false;
    private static boolean constructionBlueprintReflectionFailed = false;
    private ArchitectState stateRef;

    @Override
    public void init() {
        // Scaffolding initialization
    }

    /**
     * Fast-path check: inspects the underlying 64-bit BitMap array in settlement.job.JOBS.
     * Takes ~2 microseconds to determine if ANY outside jobs exist on the entire map.
     */
    public boolean hasAnyOutsideJobs() {
        if (!SETT.exists() || SETT.JOBS() == null) return false;
        if (bitsReflectionFailed) return true;
        try {
            if (jobsMapField == null) {
                jobsMapField = settlement.job.JOBS.class.getDeclaredField("map");
                jobsMapField.setAccessible(true);
            }
            Object bitsmap2D = jobsMapField.get(SETT.JOBS());
            if (bitsmap2D != null) {
                if (bitsmap2DMapField == null) {
                    bitsmap2DMapField = bitsmap2D.getClass().getDeclaredField("map");
                    bitsmap2DMapField.setAccessible(true);
                }
                Object bitsmap1D = bitsmap2DMapField.get(bitsmap2D);
                if (bitsmap1D != null) {
                    if (bitsField1D == null) {
                        bitsField1D = bitsmap1D.getClass().getDeclaredField("bits");
                        bitsField1D.setAccessible(true);
                    }
                    long[] bits = (long[]) bitsField1D.get(bitsmap1D);
                    if (bits != null) {
                        for (int i = 0; i < bits.length; i++) {
                            if (bits[i] != 0L) {
                                return true;
                            }
                        }
                        return false;
                    }
                }
            }
        } catch (Throwable t) {
            bitsReflectionFailed = true;
        }
        return true;
    }

    /**
     * Fast-path check: checks if any room construction sites currently exist in the settlement.
     */
    public boolean hasAnyRoomConstruction() {
        if (!SETT.exists() || SETT.ROOMS() == null || SETT.ROOMS().construction == null) {
            return false;
        }
        return SETT.ROOMS().construction.instances() > 0;
    }

    @Override
    public void update(double ds, ArchitectState state) {
        this.stateRef = state;

        if (!SETT.exists()) {
            return;
        }

        // 1. Wall-clock rate limiting: exactly 10 executions per real-world second, regardless of whether simulation is 1x or 250x
        long now = System.currentTimeMillis();
        if (now - lastUpdateMs < UPDATE_INTERVAL_MS) {
            return;
        }
        lastUpdateMs = now;

        boolean hasRooms = hasAnyRoomConstruction();
        boolean hasJobs = hasAnyOutsideJobs();

        // 2. Early return if zero construction and zero jobs are pending (99.9% of the time). Takes ~2 microseconds!
        if (!hasRooms && !hasJobs && state.maintenanceTier != MaintenanceTier.JANITOR_WORK_FREE_MATS) {
            return;
        }

        int height = SETT.THEIGHT;
        int width = SETT.TWIDTH;
        int startY = scanRowCursor;
        int endY = Math.min(height, startY + (state.constructionTier == ConstructionTier.INSTANT_BUILD ? 256 : (state.instantJobs ? 96 : ROWS_PER_TICK)));

        // 3. Instant Construction (Optimized rolling sweep)
        if (state.constructionTier == ConstructionTier.INSTANT_BUILD) {
            // Fast room sweep
            if (hasRooms && SETT.ROOMS().construction.isser != null) {
                CONSTRUCTION construction = SETT.ROOMS().construction;
                if (!processInstantRoomsFast(construction, state.jobYieldTier)) {
                    for (int y = startY; y < endY; y++) {
                        for (int x = 0; x < width; x++) {
                            if (construction.isser.is(x, y)) {
                                clearTileForConstruction(x, y, state.jobYieldTier);
                                construction.construct(x, y);
                            }
                        }
                    }
                }
            }

            // Outside jobs sweep
            if (hasJobs && SETT.JOBS() != null && SETT.JOBS().getter != null) {
                for (int y = startY; y < endY; y++) {
                    for (int x = 0; x < width; x++) {
                        if (SETT.JOBS().getter.is(x, y)) {
                            Job job = SETT.JOBS().getter.get(x, y);
                            if (job instanceof JobBuild) {
                                completeJobBuild((JobBuild) job, x, y);
                            } else if (job != null) {
                                completeJobClear(job, x, y, state.jobYieldTier);
                            }
                        }
                    }
                }
            }

            scanRowCursor = (endY >= height) ? 0 : endY;
            return;
        }

        // 4. Free Materials / Keep Work (or Fast & Free) with rolling chunk cursor
        if (state.constructionTier == ConstructionTier.FREE_MATERIALS_KEEP_WORK ||
            state.constructionTier == ConstructionTier.FAST_AND_FREE) {
            boolean fastSpeed = (state.constructionTier == ConstructionTier.FAST_AND_FREE);
            if (hasRooms) {
                processFreeMaterialsSlice(fastSpeed, width, startY, endY);
            }
            if (hasJobs) {
                processOutsideJobsSlice(fastSpeed, width, startY, endY, state.instantJobs, state.jobYieldTier);
            }
        } else if (state.instantJobs && hasJobs) {
            processOutsideJobsSlice(false, width, startY, endY, true, state.jobYieldTier);
        }

        // 5. Janitor supplies maintenance
        if (state.maintenanceTier == MaintenanceTier.JANITOR_WORK_FREE_MATS) {
            keepJanitorSuppliesFull();
        }

        // Advance cursor
        scanRowCursor = (endY >= height) ? 0 : endY;
    }

    /**
     * Budgeted row slice scan for room construction sites.
     */
    private void processFreeMaterialsSlice(boolean fastSpeed, int width, int startY, int endY) {
        if (SETT.ROOMS() == null || SETT.ROOMS().construction == null) {
            return;
        }
        CONSTRUCTION construction = SETT.ROOMS().construction;
        if (construction.instances() == 0 || construction.isser == null) {
            return;
        }

        for (int y = startY; y < endY; y++) {
            for (int x = 0; x < width; x++) {
                if (construction.isser.is(x, y)) {
                    if (fastSpeed) {
                        clearTileForConstruction(x, y, stateRef != null ? stateRef.jobYieldTier : JobYieldTier.DROP_ON_GROUND);
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
     * Budgeted row slice scan for outside jobs (roads, walls, ceilings, fortifications, clearings).
     */
    private void processOutsideJobsSlice(boolean fastSpeed, int width, int startY, int endY, boolean instantJobs, JobYieldTier yieldTier) {
        if (SETT.JOBS() == null || SETT.JOBS().getter == null) {
            return;
        }

        Bitsmap1D progress = null;
        Bitmap1D wantsRes = null;
        if (!outsideJobsReflectionFailed) {
            try {
                if (jobsProgressField == null) {
                    jobsProgressField = JOBS.class.getDeclaredField("progress");
                    jobsProgressField.setAccessible(true);
                    jobsWantsResField = JOBS.class.getDeclaredField("wantsRes");
                    jobsWantsResField.setAccessible(true);
                }
                progress = (Bitsmap1D) jobsProgressField.get(SETT.JOBS());
                wantsRes = (Bitmap1D) jobsWantsResField.get(SETT.JOBS());
            } catch (Throwable t) {
                outsideJobsReflectionFailed = true;
                System.err.println("[SyxArchitect] Reflection disabled for OutsideJobs: " + t.getMessage());
            }
        }

        for (int y = startY; y < endY; y++) {
            for (int x = 0; x < width; x++) {
                if (SETT.JOBS().getter.is(x, y)) {
                    Job job = SETT.JOBS().getter.get(x, y);
                    if (job instanceof JobBuild) {
                        JobBuild jb = (JobBuild) job;
                        if (fastSpeed) {
                            completeJobBuild(jb, x, y);
                        } else if (progress != null) {
                            int tile = y * width + x;
                            int needed = jb.resAmount();
                            if (progress.get(tile) < needed) {
                                progress.set(tile, needed);
                                if (wantsRes != null) {
                                    wantsRes.set(tile, false);
                                }
                            }
                        }
                    } else if (instantJobs && job != null) {
                        completeJobClear(job, x, y, yieldTier);
                    }
                }
            }
        }
    }

    /**
     * Instantly completes an outside clearing or demolition job (tree felling, rock clearing,
     * road destruction, structure/wall disassembly, tunneling, wild food harvesting).
     * Uses a multi-pass loop to deplete all HP/stages, directing yielded resources according to
     * JobYieldTier (drop on floor, void/discard, or direct to stockpile).
     */
    private void completeJobClear(Job job, int x, int y, JobYieldTier yieldTier) {
        if (job == null) return;
        int maxSteps = 30;
        while (maxSteps-- > 0 && SETT.JOBS() != null && SETT.JOBS().getter != null && SETT.JOBS().getter.is(x, y)) {
            Job current = SETT.JOBS().getter.get(x, y);
            if (current == null || current instanceof JobBuild) {
                break;
            }
            try {
                RESOURCE res = current.jobPerform(null, null, 0);
                if (res != null) {
                    handleJobYield(res, x, y, yieldTier);
                }
            } catch (Throwable t) {
                break;
            }
        }
        // Robust cleanup: if job still lingering after multi-pass attempts, force clear tile
        if (SETT.JOBS() != null && SETT.JOBS().getter != null && SETT.JOBS().getter.is(x, y)) {
            Job rem = SETT.JOBS().getter.get(x, y);
            if (rem != null && !(rem instanceof JobBuild)) {
                try {
                    if (SETT.TERRAIN() != null) {
                        settlement.tilemap.terrain.Terrain.TerrainTile tt = SETT.TERRAIN().get(x, y);
                        if (tt != null && tt.clearing() != null) {
                            tt.clearing().destroy(x, y);
                        }
                    }
                    if (SETT.FLOOR() != null && SETT.FLOOR().getter != null && SETT.FLOOR().getter.is(x, y)) {
                        if (SETT.FLOOR().clearer != null) {
                            SETT.FLOOR().clearer.clear(x, y);
                        }
                    }
                    if (SETT.JOBS().clearer != null) {
                        SETT.JOBS().clearer.set(x, y);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        clearTileForConstruction(x, y, yieldTier);
    }

    /**
     * Directs yielded clearing resources according to the player's JobYieldTier preference:
     * - DROP_ON_GROUND: Spawns scattered item crates on the ground next to the clearing tile.
     * - VOID_DISCARD: Discards resources completely (True Creative mode, zero clutter).
     * - STOCKPILE: Keeps yielded resources directly in the global player stockpile.
     */
    private void handleJobYield(RESOURCE res, int x, int y, JobYieldTier yieldTier) {
        if (res == null) return;

        if (yieldTier == JobYieldTier.VOID_DISCARD) {
            // Revert engine's automatic +1 to stockpile so resource vanishes
            if (GAME.player() != null && GAME.player().res() != null) {
                GAME.player().res().inc(res, RTYPE.PRODUCED, -1);
            }
        } else if (yieldTier == JobYieldTier.DROP_ON_GROUND) {
            // Revert engine's automatic +1 to stockpile and spawn physical item on floor
            if (GAME.player() != null && GAME.player().res() != null) {
                GAME.player().res().inc(res, RTYPE.PRODUCED, -1);
            }
            if (SETT.THINGS() != null && SETT.THINGS().resources != null) {
                SETT.THINGS().resources.create(x, y, res, 1);
            }
        }
        // If STOCKPILE, keep the engine's +1 in the player's stockpile directly
    }

    /**
     * Completely levels and clears ("planiert") a tile before construction of roads,
     * floors, walls, structures, rooms, or agricultural fields.
     * Eradicates:
     * - Standing flora & terrain clutter: trees, rocks, bushes, flowers, mushrooms, wild herbs/crops.
     * - Floor growth: growth sprites, saplings, fungi, undergrowth.
     * - Surface vegetation: tall grass coverage.
     * - Scattered ground debris: directs yielded items according to JobYieldTier.
     */
    private void clearTileForConstruction(int x, int y, JobYieldTier yieldTier) {
        if (!SETT.IN_BOUNDS(x, y)) {
            return;
        }

        // 1. Terrain layer (Trees, Rocks, Bushes, Flowers, Mushrooms, Growables/Herbs, Ruins)
        if (SETT.TERRAIN() != null) {
            TerrainTile tt = SETT.TERRAIN().get(x, y);
            boolean isMountain = (tt == SETT.TERRAIN().MOUNTAIN || tt == SETT.TERRAIN().CAVE);
            boolean isWater = (SETT.TERRAIN().WATER != null && SETT.TERRAIN().WATER.is != null && SETT.TERRAIN().WATER.is.is(x, y));
            if (!isMountain && !isWater) {
                if (tt != null && tt != SETT.TERRAIN().NADA) {
                    try {
                        if (tt.clearing() != null) {
                            if (yieldTier == JobYieldTier.STOCKPILE) {
                                while (SETT.TERRAIN().get(x, y).clearing() != null && SETT.TERRAIN().get(x, y).clearing().can()) {
                                    RESOURCE res = SETT.TERRAIN().get(x, y).clearing().clear1(x, y);
                                    if (res != null && GAME.player() != null && GAME.player().res() != null) {
                                        GAME.player().res().inc(res, RTYPE.PRODUCED, 1);
                                    }
                                }
                            } else if (yieldTier == JobYieldTier.DROP_ON_GROUND) {
                                while (SETT.TERRAIN().get(x, y).clearing() != null && SETT.TERRAIN().get(x, y).clearing().can()) {
                                    RESOURCE res = SETT.TERRAIN().get(x, y).clearing().clear1(x, y);
                                    if (res != null && SETT.THINGS() != null && SETT.THINGS().resources != null) {
                                        SETT.THINGS().resources.create(x, y, res, 1);
                                    }
                                }
                            } else {
                                tt.clearing().destroy(x, y);
                            }
                        }
                    } catch (Throwable ignored) {}

                    try {
                        SETT.TERRAIN().NADA.placeFixed(x, y);
                    } catch (Throwable ignored) {}
                }

                // Explicit safety pass for rogue flora
                try {
                    if (SETT.TERRAIN().TREES != null && SETT.TERRAIN().TREES.isTree(x, y)) {
                        SETT.TERRAIN().NADA.placeFixed(x, y);
                    }
                    if (SETT.TERRAIN().BUSH != null && SETT.TERRAIN().BUSH.is(x, y)) {
                        SETT.TERRAIN().NADA.placeFixed(x, y);
                    }
                    if (SETT.TERRAIN().FLOWER != null && SETT.TERRAIN().FLOWER.is(x, y)) {
                        SETT.TERRAIN().NADA.placeFixed(x, y);
                    }
                    if (SETT.TERRAIN().MUSHROOM != null && SETT.TERRAIN().MUSHROOM.is(x, y)) {
                        SETT.TERRAIN().NADA.placeFixed(x, y);
                    }
                    if (SETT.TERRAIN().GROWABLES != null) {
                        for (int gi = 0; gi < SETT.TERRAIN().GROWABLES.size(); gi++) {
                            settlement.tilemap.terrain.TGrowable tg = SETT.TERRAIN().GROWABLES.get(gi);
                            if (tg != null && tg.is(x, y)) {
                                SETT.TERRAIN().NADA.placeFixed(x, y);
                                break;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        // 2. Floor Growth layer (Flower sprites, herbs, bushes, mushrooms, undergrowth)
        if (SETT.TILE_MAP() != null && SETT.TILE_MAP().growth != null) {
            try {
                if (SETT.TILE_MAP().growth.tree != null) {
                    SETT.TILE_MAP().growth.tree.set(x, y, 0.0);
                }
                if (SETT.TILE_MAP().growth.flower != null) {
                    SETT.TILE_MAP().growth.flower.set(x, y, 0.0);
                }
                if (SETT.TILE_MAP().growth.bush != null) {
                    SETT.TILE_MAP().growth.bush.set(x, y, 0.0);
                }
                if (SETT.TILE_MAP().growth.mushroom != null) {
                    SETT.TILE_MAP().growth.mushroom.set(x, y, 0.0);
                }
                if (SETT.TILE_MAP().growth.nothing != null) {
                    SETT.TILE_MAP().growth.nothing.set(x, y, 0.0);
                }
                SETT.TILE_MAP().growth.tear(x, y);
            } catch (Throwable ignored) {}
        }

        // 3. Grass layer (Grass visual density & blades)
        if (SETT.GRASS() != null && SETT.GRASS().currentI != null) {
            try {
                SETT.GRASS().currentI.set(x, y, 0);
            } catch (Throwable ignored) {}
        }

        // 4. Ground Clutter / Scattered resources
        try {
            if (SETT.THINGS() != null && SETT.THINGS().resources != null) {
                ScatteredResource sr = SETT.THINGS().resources.get(x, y);
                if (sr != null && sr.amount() > 0 && sr.amountReserved() == 0) {
                    if (yieldTier == JobYieldTier.VOID_DISCARD) {
                        sr.removeUnreserved(sr.amount());
                    } else if (yieldTier == JobYieldTier.STOCKPILE) {
                        if (GAME.player() != null && GAME.player().res() != null && sr.resource() != null) {
                            GAME.player().res().inc(sr.resource(), RTYPE.PRODUCED, sr.amount());
                        }
                        sr.removeUnreserved(sr.amount());
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private void completeJobBuild(JobBuild job, int x, int y) {
        if (outsideJobsReflectionFailed) return;
        try {
            if (jobBuildConstructMethod == null) {
                jobBuildConstructMethod = JobBuild.class.getDeclaredMethod("construct", int.class, int.class);
                jobBuildConstructMethod.setAccessible(true);
            }
            RESOURCE res = job.res();
            int amount = job.resAmount();

            clearTileForConstruction(x, y, stateRef != null ? stateRef.jobYieldTier : JobYieldTier.DROP_ON_GROUND);

            jobBuildConstructMethod.invoke(job, x, y);
            if (SETT.JOBS() != null && SETT.JOBS().clearer != null) {
                SETT.JOBS().clearer.set(x, y);
            }
            // Refund any resources deducted during construct so player stockpile remains 100% free
            if (res != null && amount > 0 && GAME.player() != null && GAME.player().res() != null) {
                GAME.player().res().inc(res, RTYPE.CONSTRUCTION, amount);
            }
        } catch (Throwable t) {
            if (!outsideJobsReflectionFailed) {
                outsideJobsReflectionFailed = true;
                System.err.println("[SyxArchitect] Reflection disabled for JobBuild.construct: " + t.getMessage());
            }
        }
    }

    private void fulfillTileResources(Room room, int x, int y) {
        if (outsideJobsReflectionFailed) return;
        try {
            clearTileForConstruction(x, y, stateRef != null ? stateRef.jobYieldTier : JobYieldTier.DROP_ON_GROUND);
            if (jobFinsihMethod == null) {
                Class<?> clazz = room.getClass();
                jobFinsihMethod = clazz.getMethod("jobFinsih", int.class, int.class, RESOURCE.class, int.class);
                jobFinsihMethod.setAccessible(true);
                totalResourcesNeededMethod = clazz.getMethod("totalResourcesNeeded", int.class, int.class);
                totalResourcesNeededMethod.setAccessible(true);
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
        } catch (Throwable t) {
            if (!roomReflectionFailed) {
                roomReflectionFailed = true;
                System.err.println("[SyxArchitect] Reflection disabled for Room.fulfillTileResources: " + t.getMessage());
            }
        }
    }

    /**
     * Refills janitor room maintenance reserves so janitors repair buildings without consuming player supplies.
     */
    private void keepJanitorSuppliesFull() {
        if (janitorReflectionFailed || SETT.ROOMS() == null) return;
        try {
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
                        resAmMethod.setAccessible(true);
                        resIncMethod = bits.getClass().getMethod("resInc", ins.getClass(), RESOURCE.class, int.class);
                        resIncMethod.setAccessible(true);
                    }
                    for (int r = 0; r < RESOURCES.ALL().size(); r++) {
                        RESOURCE res = RESOURCES.ALL().get(r);
                        int current = (int) resAmMethod.invoke(bits, res);
                        if (current < 20) {
                            resIncMethod.invoke(bits, ins, res, 20);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            if (!janitorReflectionFailed) {
                janitorReflectionFailed = true;
                System.err.println("[SyxArchitect] Reflection disabled for Janitor.bits: " + t.getMessage());
            }
        }
    }


    /**
     * High-speed direct instance room solver: directly accesses bounding boxes of
     * active room construction sites instead of sweeping hundreds of thousands of world tiles.
     */
    private boolean processInstantRoomsFast(CONSTRUCTION construction, JobYieldTier yieldTier) {
        if (constructionBlueprintReflectionFailed || construction == null) return false;
        try {
            if (constructionBlueprintField == null) {
                constructionBlueprintField = CONSTRUCTION.class.getDeclaredField("construction");
                constructionBlueprintField.setAccessible(true);
            }
            Object bp = constructionBlueprintField.get(construction);
            if (bp != null) {
                if (blueprintAllField == null) {
                    blueprintAllField = bp.getClass().getDeclaredField("all");
                    blueprintAllField.setAccessible(true);
                }
                snake2d.util.sets.LIST<?> all = (snake2d.util.sets.LIST<?>) blueprintAllField.get(bp);
                if (all != null) {
                    int count = all.size();
                    for (int i = 0; i < count; i++) {
                        Object ins = all.get(i);
                        if (ins instanceof settlement.room.main.ROOMA) {
                            snake2d.util.datatypes.RECTANGLE body = ((settlement.room.main.ROOMA) ins).body();
                            if (body != null) {
                                int x1 = Math.max(0, body.x1());
                                int x2 = Math.min(SETT.TWIDTH, body.x2());
                                int y1 = Math.max(0, body.y1());
                                int y2 = Math.min(SETT.THEIGHT, body.y2());
                                for (int y = y1; y < y2; y++) {
                                    for (int x = x1; x < x2; x++) {
                                        if (construction.isser != null && construction.isser.is(x, y)) {
                                            clearTileForConstruction(x, y, yieldTier);
                                            construction.construct(x, y);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return true;
                }
            }
        } catch (Throwable t) {
            constructionBlueprintReflectionFailed = true;
        }
        return false;
    }

    /**
     * Instantly finishes all ongoing construction sites settlement-wide
     * (Rooms, Road paving, Wall building, Ceilings, Fortifications, Tree felling, Demolition, Excavation).
     * Full sweep executed on 1-click button or tier activation.
     */
    public void triggerInstantBuildAll() {
        triggerInstantBuildAll(stateRef != null ? stateRef.jobYieldTier : JobYieldTier.DROP_ON_GROUND);
    }

    public void triggerInstantBuildAll(JobYieldTier yieldTier) {
        if (!SETT.exists()) {
            return;
        }

        int width = SETT.TWIDTH;
        int height = SETT.THEIGHT;

        // 1. Room construction sites
        if (SETT.ROOMS() != null && SETT.ROOMS().construction != null) {
            CONSTRUCTION construction = SETT.ROOMS().construction;
            if (construction.isser != null && construction.instances() > 0) {
                if (!processInstantRoomsFast(construction, yieldTier)) {
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            if (construction.isser.is(x, y)) {
                                clearTileForConstruction(x, y, yieldTier);
                                construction.construct(x, y);
                            }
                        }
                    }
                }
            }
        }

        // 2. Outside jobs (Roads, Floors, Walls, Ceilings, Fortifications, Tree felling, Demolition, Excavation)
        if (SETT.JOBS() != null && SETT.JOBS().getter != null) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (SETT.JOBS().getter.is(x, y)) {
                        Job job = SETT.JOBS().getter.get(x, y);
                        if (job instanceof JobBuild) {
                            completeJobBuild((JobBuild) job, x, y);
                        } else if (job != null) {
                            completeJobClear(job, x, y, yieldTier);
                        }
                    }
                }
            }
        }
    }

    /**
     * Executes a one-time maintenance sweep across the map to restore all existing damaged
     * buildings to 100%. Subsequent decay is permanently prevented by BoostModule's 100x booster.
     */
    public void maintainAll() {
        if (!SETT.exists() || SETT.MAINTENANCE() == null || SETT.MAINTENANCE().needs == null) {
            return;
        }
        MAINTENANCE maintenance = SETT.MAINTENANCE();
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

    @Override
    public void onStateChanged(ArchitectState state) {
        this.stateRef = state;
        if (state.constructionTier == ConstructionTier.INSTANT_BUILD || state.instantJobs) {
            triggerInstantBuildAll(state.jobYieldTier);
        }
        if (state.maintenanceTier == MaintenanceTier.NO_DECAY) {
            maintainAll();
        }
    }
}
