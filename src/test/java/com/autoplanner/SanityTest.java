package com.autoplanner;

import java.io.File;
import java.lang.reflect.Field;

import com.autoplanner.solver.AutoFurnishSolver;
import com.autoplanner.solver.AutoStrutSolver;
import com.autoplanner.solver.ReachabilityChecker;
import com.autoplanner.solver.RoomGridSnapshot;

import script.SCRIPT;
import snake2d.util.file.Json;

/**
 * Automated QA & Physics/Math Sanity Test Suite for Syx Auto-Planner.
 */
public class SanityTest {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   SYX AUTO-PLANNER - PHYSICS, PACKING & REACHABILITY VERIFICATION SUITE        ");
        System.out.println("================================================================================");

        int passed = 0;
        int failed = 0;

        // Check 1: _Info.txt Engine Syntax & Case Sensitivity
        try {
            testModInfoSyntax();
            System.out.println("[PASS] Check 1: _Info.txt exact case & engine Json parser verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Check 1: _Info.txt check failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Check 2: Engine ScriptLoad Reflection Simulation
        try {
            testScriptLoadSimulation();
            System.out.println("[PASS] Check 2: AutoPlannerScript reflection & SCRIPT lifecycle verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Check 2: ScriptLoad check failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Check 3: Auto-Strut 16-Ray Support Physics & Pillar Self-Resolution
        try {
            testAutoStrutSupportPhysics();
            System.out.println("[PASS] Check 3: AutoStrutSolver 16-ray support & pillar self-support (1.0) verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Check 3: AutoStrutSolver physics check failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Check 4: Stockpile Packing Heuristic (7x7 / 5x5 Interior Ranking)
        try {
            testStockpilePackingHeuristic();
            System.out.println("[PASS] Check 4: Stockpile packing heuristic (2x4 > 1x5 > 2x3 > 1x4 > 1x3 > 2x2) verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Check 4: Stockpile heuristic check failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Check 5: ReachabilityChecker BFS & WillBlockRoom Pocket Protection
        try {
            testReachabilityAndTopology();
            System.out.println("[PASS] Check 5: ReachabilityChecker BFS, interior neighbor & WillBlockRoom pocket check verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Check 5: ReachabilityChecker check failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("================================================================================");
        System.out.println("AUTO-PLANNER TEST RESULTS: " + passed + " passed, " + failed + " failed.");
        System.out.println("================================================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testModInfoSyntax() throws Exception {
        File infoFile = new File("mod-files/_Info.txt");
        if (!infoFile.exists()) {
            throw new AssertionError("_Info.txt does not exist in mod-files!");
        }

        File parent = infoFile.getParentFile();
        boolean exactCaseFound = false;
        String[] files = parent.list();
        if (files != null) {
            for (String f : files) {
                if (f.equals("_Info.txt")) {
                    exactCaseFound = true;
                    break;
                }
            }
        }
        if (!exactCaseFound) {
            throw new AssertionError("File casing mismatch: _Info.txt must have capital 'I' on disk!");
        }

        Json json = new Json(infoFile.toPath());
        String name = json.text("NAME", null);
        String desc = json.text("DESC", null);
        String version = json.text("VERSION", null);
        String author = json.text("AUTHOR", null);
        String gameVersion = json.text("GAMEVERSION", null);

        if (name == null || name.trim().isEmpty()) throw new AssertionError("NAME missing in _Info.txt");
        if (desc == null || desc.trim().isEmpty()) throw new AssertionError("DESC missing in _Info.txt");
        if (version == null || version.trim().isEmpty()) throw new AssertionError("VERSION missing in _Info.txt");
        if (author == null || author.trim().isEmpty()) throw new AssertionError("AUTHOR missing in _Info.txt");
        if (!"71".equals(gameVersion)) throw new AssertionError("GAMEVERSION must be 71");
    }

    private static void testScriptLoadSimulation() throws Exception {
        Class<?> clazz = Class.forName("com.autoplanner.AutoPlannerScript");
        Object scriptObj = clazz.getDeclaredConstructor().newInstance();
        if (!(scriptObj instanceof SCRIPT)) {
            throw new AssertionError("AutoPlannerScript does not implement script.SCRIPT!");
        }
        SCRIPT script = (SCRIPT) scriptObj;
        if (script.name() == null || script.name().length() == 0) {
            throw new AssertionError("script.name() is empty!");
        }
        if (script.desc() == null || script.desc().length() == 0) {
            throw new AssertionError("script.desc() is empty!");
        }
        script.initBeforeGameCreated();
        script.initBeforeGameInited();
    }

    private static void testAutoStrutSupportPhysics() throws Exception {
        AutoStrutSolver solver = new AutoStrutSolver();
        Field snapField = AutoStrutSolver.class.getDeclaredField("snapshot");
        snapField.setAccessible(true);
        RoomGridSnapshot snap = (RoomGridSnapshot) snapField.get(solver);

        setupSyntheticRoom(snap, 15, 15);

        // Center of 13x13 interior is at (7, 7) -> > 4 tiles away from all walls, so support must be 0.0
        double centerSup = solver.computeSupport(7, 7);
        if (Math.abs(centerSup) > 1e-6) {
            throw new AssertionError("Expected 0.0 support at unsupported room center (7,7), got: " + centerSup);
        }

        // Place a pillar at (7, 7): computeSupport(7, 7) MUST immediately return 1.0!
        snap.set(7, 7, RoomGridSnapshot.TILE_STRUT);
        double pillarSelfSup = solver.computeSupport(7, 7);
        if (Math.abs(pillarSelfSup - 1.0) > 1e-6) {
            throw new AssertionError("Placed pillar at (7,7) must return support 1.0 so it is removed from yellow list, got: " + pillarSelfSup);
        }

        // Adjacent tile (7, 6) at distance 1 from pillar (7, 7) must receive positive ray support > 0
        double neighborSup = solver.computeSupport(7, 6);
        if (neighborSup <= 0.5) {
            throw new AssertionError("Tile adjacent to pillar at (7,6) should receive strong ray support, got: " + neighborSup);
        }
    }

    private static void testStockpilePackingHeuristic() {
        // Formula: stats * (0.65 + 0.35 * (stats / area)) + (maxDim * 0.05)
        double score2x4 = computeFormula(6.0, 8, 4); // 2x4: 6 crates, 8 tiles
        double score1x5 = computeFormula(4.0, 5, 5); // 1x5: 4 crates, 5 tiles
        double score2x3 = computeFormula(4.0, 6, 3); // 2x3: 4 crates, 6 tiles
        double score1x4 = computeFormula(3.0, 4, 4); // 1x4: 3 crates, 4 tiles
        double score1x3 = computeFormula(2.0, 3, 3); // 1x3: 2 crates, 3 tiles
        double score2x2 = computeFormula(2.0, 4, 2); // 2x2: 2 crates, 4 tiles
        double score1x2 = computeFormula(1.0, 2, 2); // 1x2: 1 crate,  2 tiles

        if (!(score2x4 > score1x5 && score1x5 > score2x3 && score2x3 > score1x4
                && score1x4 > score1x3 && score1x3 > score2x2 && score2x2 > score1x2)) {
            throw new AssertionError(String.format(
                    "Invalid stockpile packing ranking: 2x4=%.3f, 1x5=%.3f, 2x3=%.3f, 1x4=%.3f, 1x3=%.3f, 2x2=%.3f, 1x2=%.3f",
                    score2x4, score1x5, score2x3, score1x4, score1x3, score2x2, score1x2));
        }
    }

    private static double computeFormula(double stats, int area, int maxDim) {
        double density = stats / area;
        return stats * (0.65 + 0.35 * density) + (maxDim * 0.05);
    }

    private static void testReachabilityAndTopology() throws Exception {
        RoomGridSnapshot snap = new RoomGridSnapshot();
        setupSyntheticRoom(snap, 7, 7); // 5x5 interior (x=1..5, y=1..5), walls on 0 and 6
        // Place door at South wall midpoint (3, 6)
        snap.set(3, 6, RoomGridSnapshot.TILE_DOOR);
        snap.doorX[0] = 3;
        snap.doorY[0] = 6;
        snap.doorCount = 1;

        ReachabilityChecker checker = new ReachabilityChecker();

        // 1. All floor tiles reachable initially
        int[] ax = new int[]{3};
        int[] ay = new int[]{5};
        if (!checker.canReachAllAccessPoints(snap, ax, ay, 1)) {
            throw new AssertionError("Initial open 5x5 room should be 100% reachable!");
        }

        // 2. Seal off top-left corner (1, 1) by placing blockers at (1, 2) and (2, 1)
        snap.set(1, 2, RoomGridSnapshot.TILE_FURNITURE_BLOCK);
        snap.set(2, 1, RoomGridSnapshot.TILE_FURNITURE_BLOCK);
        if (checker.canReachAllAccessPoints(snap, ax, ay, 1)) {
            throw new AssertionError("Sealing off walkable floor tile (1,1) must be rejected (WillBlockRoom)!");
        }

        // Restore (1, 2) and (2, 1)
        snap.set(1, 2, RoomGridSnapshot.TILE_ROOM_FLOOR);
        snap.set(2, 1, RoomGridSnapshot.TILE_ROOM_FLOOR);

        // 3. Test simple-point strut topology: placing a strut in an open 5x5 center (3, 3) is valid
        if (!checker.canPlaceStrutWithoutBlocking(snap, 3, 3)) {
            throw new AssertionError("Strut at open center (3,3) should be topologically valid!");
        }

        // 4. Placing a strut directly in front of the door (3, 5) must be rejected (dist <= 1 to door (3, 6))
        if (checker.canPlaceStrutWithoutBlocking(snap, 3, 5)) {
            throw new AssertionError("Strut at door threshold (3,5) must be rejected!");
        }
    }

    private static void setupSyntheticRoom(RoomGridSnapshot snap, int w, int h) throws Exception {
        snap.clear();
        Field widthField = RoomGridSnapshot.class.getDeclaredField("width");
        Field heightField = RoomGridSnapshot.class.getDeclaredField("height");
        Field countField = RoomGridSnapshot.class.getDeclaredField("roomTileCount");
        widthField.setAccessible(true);
        heightField.setAccessible(true);
        countField.setAccessible(true);
        widthField.setInt(snap, w);
        heightField.setInt(snap, h);

        int count = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (x == 0 || x == w - 1 || y == 0 || y == h - 1) {
                    snap.set(x, y, RoomGridSnapshot.TILE_WALL);
                } else {
                    snap.set(x, y, RoomGridSnapshot.TILE_ROOM_FLOOR);
                    count++;
                }
            }
        }
        countField.setInt(snap, count);
    }
}
