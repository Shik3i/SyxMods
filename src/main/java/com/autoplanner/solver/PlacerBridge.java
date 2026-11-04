package com.autoplanner.solver;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import settlement.room.main.Room;
import settlement.room.main.furnisher.FurnisherItem;
import settlement.room.main.placement.RoomPlacer;
import snake2d.util.datatypes.RECTANGLE;

/**
 * High-performance, zero-allocation reflection bridge for Songs of Syx RoomPlacer internals.
 * Resolves package-private engine fields and methods at class initialization time.
 */
public final class PlacerBridge {

    private static Field fieldInstance;
    private static Field fieldPlacerArea;
    private static Method methodPlacerAreaClear;
    private static Field fieldResources;
    private static Method methodResourcesUpdatee;
    private static Field fieldHistory;
    private static Method methodHistoryPlaceItem;
    private static Method methodHistoryClear;

    private static boolean initialized = false;

    static {
        try {
            fieldInstance = RoomPlacer.class.getDeclaredField("instance");
            fieldInstance.setAccessible(true);

            fieldPlacerArea = RoomPlacer.class.getDeclaredField("placerArea");
            fieldPlacerArea.setAccessible(true);
            methodPlacerAreaClear = fieldPlacerArea.getType().getDeclaredMethod("clear", int.class, int.class);
            methodPlacerAreaClear.setAccessible(true);

            fieldResources = RoomPlacer.class.getDeclaredField("resources");
            fieldResources.setAccessible(true);
            methodResourcesUpdatee = fieldResources.getType().getDeclaredMethod("updatee");
            methodResourcesUpdatee.setAccessible(true);

            fieldHistory = RoomPlacer.class.getDeclaredField("history");
            fieldHistory.setAccessible(true);
            methodHistoryPlaceItem = fieldHistory.getType().getDeclaredMethod("placeItem", FurnisherItem.class, int.class, int.class, int.class);
            methodHistoryPlaceItem.setAccessible(true);
            methodHistoryClear = fieldHistory.getType().getDeclaredMethod("clear");
            methodHistoryClear.setAccessible(true);

            initialized = true;
        } catch (Throwable t) {
            System.err.println("[SyxAutoPlanner] Failed to bind RoomPlacer reflection bridge: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private PlacerBridge() {}

    public static boolean isAvailable() {
        return initialized;
    }

    public static Room.RoomInstanceImp getInstance(RoomPlacer placer) {
        if (!initialized || placer == null) return null;
        try {
            return (Room.RoomInstanceImp) fieldInstance.get(placer);
        } catch (Throwable t) {
            return null;
        }
    }

    public static int getArea(RoomPlacer placer) {
        if (placer == null) return 0;
        try {
            return placer.size();
        } catch (Throwable t) {
            Room.RoomInstanceImp ins = getInstance(placer);
            return ins != null ? ins.area() : 0;
        }
    }

    public static RECTANGLE getBounds(RoomPlacer placer) {
        Room.RoomInstanceImp ins = getInstance(placer);
        return ins != null ? ins.body() : null;
    }

    public static boolean isTileInRoom(RoomPlacer placer, int tx, int ty) {
        Room.RoomInstanceImp ins = getInstance(placer);
        return ins != null && ins.is(tx, ty);
    }

    public static void clearTile(RoomPlacer placer, int tx, int ty) {
        if (!initialized || placer == null) return;
        try {
            Object pa = fieldPlacerArea.get(placer);
            if (pa != null) {
                methodPlacerAreaClear.invoke(pa, tx, ty);
            }
        } catch (Throwable t) {
            System.err.println("[SyxAutoPlanner] clearTile failed: " + t.getMessage());
        }
    }

    public static void updateResources(RoomPlacer placer) {
        if (!initialized || placer == null) return;
        try {
            Object res = fieldResources.get(placer);
            if (res != null) {
                methodResourcesUpdatee.invoke(res);
            }
        } catch (Throwable t) {
            System.err.println("[SyxAutoPlanner] updateResources failed: " + t.getMessage());
        }
    }

    public static void recordHistoryItem(RoomPlacer placer, FurnisherItem item, int tx, int ty, int change) {
        if (!initialized || placer == null) return;
        try {
            Object hist = fieldHistory.get(placer);
            if (hist != null) {
                methodHistoryPlaceItem.invoke(hist, item, tx, ty, change);
            }
        } catch (Throwable t) {
            System.err.println("[SyxAutoPlanner] recordHistoryItem failed: " + t.getMessage());
        }
    }

    public static void clearHistory(RoomPlacer placer) {
        if (!initialized || placer == null) return;
        try {
            Object hist = fieldHistory.get(placer);
            if (hist != null) {
                methodHistoryClear.invoke(hist);
            }
        } catch (Throwable t) {
            System.err.println("[SyxAutoPlanner] clearHistory failed: " + t.getMessage());
        }
    }
}
