package com.modelpopout;

/** Explicit public-API test double; unexpected reads and mutations fail immediately. */
@SuppressWarnings("deprecation")
abstract class TestWorldView implements net.runelite.api.WorldView
{
    @Override public int getId() { throw new UnsupportedOperationException("getId"); }
    @Override public boolean isTopLevel() { throw new UnsupportedOperationException("isTopLevel"); }
    @Override public net.runelite.api.Scene getScene() { throw new UnsupportedOperationException("getScene"); }
    @Override public net.runelite.api.IndexedObjectSet<? extends net.runelite.api.Player> players() { throw new UnsupportedOperationException("players"); }
    @Override public net.runelite.api.IndexedObjectSet<? extends net.runelite.api.NPC> npcs() { throw new UnsupportedOperationException("npcs"); }
    @Override public net.runelite.api.IndexedObjectSet<? extends net.runelite.api.WorldEntity> worldEntities() { throw new UnsupportedOperationException("worldEntities"); }
    @Override public net.runelite.api.IndexedObjectSet<? extends net.runelite.api.WorldView> worldViews() { throw new UnsupportedOperationException("worldViews"); }
    @Override public net.runelite.api.CollisionData[] getCollisionMaps() { throw new UnsupportedOperationException("getCollisionMaps"); }
    @Override public int getPlane() { throw new UnsupportedOperationException("getPlane"); }
    @Override public int[][][] getTileHeights() { throw new UnsupportedOperationException("getTileHeights"); }
    @Override public byte[][][] getTileSettings() { throw new UnsupportedOperationException("getTileSettings"); }
    @Override public int getSizeX() { throw new UnsupportedOperationException("getSizeX"); }
    @Override public int getSizeY() { throw new UnsupportedOperationException("getSizeY"); }
    @Override public int getBaseX() { throw new UnsupportedOperationException("getBaseX"); }
    @Override public int getBaseY() { throw new UnsupportedOperationException("getBaseY"); }
    @Override public net.runelite.api.Projectile createProjectile(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, net.runelite.api.Actor p10, int p11, int p12) { throw new UnsupportedOperationException("createProjectile"); }
    @Override public net.runelite.api.Deque<net.runelite.api.GraphicsObject> getGraphicsObjects() { throw new UnsupportedOperationException("getGraphicsObjects"); }
    @Override public net.runelite.api.Tile getSelectedSceneTile() { throw new UnsupportedOperationException("getSelectedSceneTile"); }
    @Override public boolean isInstance() { throw new UnsupportedOperationException("isInstance"); }
    @Override public int[][][] getInstanceTemplateChunks() { throw new UnsupportedOperationException("getInstanceTemplateChunks"); }
    @Override public int[] getMapRegions() { throw new UnsupportedOperationException("getMapRegions"); }
    @Override public boolean contains(net.runelite.api.coords.WorldPoint p0) { throw new UnsupportedOperationException("contains"); }
    @Override public boolean contains(net.runelite.api.coords.LocalPoint p0) { throw new UnsupportedOperationException("contains"); }
    @Override public net.runelite.api.Projection getMainWorldProjection() { throw new UnsupportedOperationException("getMainWorldProjection"); }
    @Override public net.runelite.api.Projection getCanvasProjection() { throw new UnsupportedOperationException("getCanvasProjection"); }
    @Override public int getYellowClickAction() { throw new UnsupportedOperationException("getYellowClickAction"); }
    @Override public int getTileHeight(int p0, int p1, int p2) { throw new UnsupportedOperationException("getTileHeight"); }
}
