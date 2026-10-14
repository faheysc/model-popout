package com.modelpopout;

/** Explicit public-API test double; unexpected reads and mutations fail immediately. */
@SuppressWarnings("deprecation")
abstract class TestActor implements net.runelite.api.Actor
{
    @Override public net.runelite.api.WorldView getWorldView() { throw new UnsupportedOperationException("getWorldView"); }
    @Override public int getCombatLevel() { throw new UnsupportedOperationException("getCombatLevel"); }
    @Override public java.lang.String getName() { throw new UnsupportedOperationException("getName"); }
    @Override public boolean isInteracting() { throw new UnsupportedOperationException("isInteracting"); }
    @Override public net.runelite.api.Actor getInteracting() { throw new UnsupportedOperationException("getInteracting"); }
    @Override public int getHealthRatio() { throw new UnsupportedOperationException("getHealthRatio"); }
    @Override public int getHealthScale() { throw new UnsupportedOperationException("getHealthScale"); }
    @Override public net.runelite.api.coords.WorldPoint getWorldLocation() { throw new UnsupportedOperationException("getWorldLocation"); }
    @Override public net.runelite.api.coords.LocalPoint getLocalLocation() { throw new UnsupportedOperationException("getLocalLocation"); }
    @Override public int getOrientation() { throw new UnsupportedOperationException("getOrientation"); }
    @Override public int getCurrentOrientation() { throw new UnsupportedOperationException("getCurrentOrientation"); }
    @Override public int getAnimation() { throw new UnsupportedOperationException("getAnimation"); }
    @Override public int getPoseAnimation() { throw new UnsupportedOperationException("getPoseAnimation"); }
    @Override public void setPoseAnimation(int p0) { throw new UnsupportedOperationException("setPoseAnimation"); }
    @Override public int getPoseAnimationFrame() { throw new UnsupportedOperationException("getPoseAnimationFrame"); }
    @Override public void setPoseAnimationFrame(int p0) { throw new UnsupportedOperationException("setPoseAnimationFrame"); }
    @Override public int getIdlePoseAnimation() { throw new UnsupportedOperationException("getIdlePoseAnimation"); }
    @Override public void setIdlePoseAnimation(int p0) { throw new UnsupportedOperationException("setIdlePoseAnimation"); }
    @Override public int getIdleRotateLeft() { throw new UnsupportedOperationException("getIdleRotateLeft"); }
    @Override public void setIdleRotateLeft(int p0) { throw new UnsupportedOperationException("setIdleRotateLeft"); }
    @Override public int getIdleRotateRight() { throw new UnsupportedOperationException("getIdleRotateRight"); }
    @Override public void setIdleRotateRight(int p0) { throw new UnsupportedOperationException("setIdleRotateRight"); }
    @Override public int getWalkAnimation() { throw new UnsupportedOperationException("getWalkAnimation"); }
    @Override public void setWalkAnimation(int p0) { throw new UnsupportedOperationException("setWalkAnimation"); }
    @Override public int getWalkRotateLeft() { throw new UnsupportedOperationException("getWalkRotateLeft"); }
    @Override public void setWalkRotateLeft(int p0) { throw new UnsupportedOperationException("setWalkRotateLeft"); }
    @Override public int getWalkRotateRight() { throw new UnsupportedOperationException("getWalkRotateRight"); }
    @Override public void setWalkRotateRight(int p0) { throw new UnsupportedOperationException("setWalkRotateRight"); }
    @Override public int getWalkRotate180() { throw new UnsupportedOperationException("getWalkRotate180"); }
    @Override public void setWalkRotate180(int p0) { throw new UnsupportedOperationException("setWalkRotate180"); }
    @Override public int getRunAnimation() { throw new UnsupportedOperationException("getRunAnimation"); }
    @Override public void setRunAnimation(int p0) { throw new UnsupportedOperationException("setRunAnimation"); }
    @Override public void setAnimation(int p0) { throw new UnsupportedOperationException("setAnimation"); }
    @Override public int getAnimationFrame() { throw new UnsupportedOperationException("getAnimationFrame"); }
    @Override public void setActionFrame(int p0) { throw new UnsupportedOperationException("setActionFrame"); }
    @Override public void setAnimationFrame(int p0) { throw new UnsupportedOperationException("setAnimationFrame"); }
    @Override public net.runelite.api.IterableHashTable<net.runelite.api.ActorSpotAnim> getSpotAnims() { throw new UnsupportedOperationException("getSpotAnims"); }
    @Override public boolean hasSpotAnim(int p0) { throw new UnsupportedOperationException("hasSpotAnim"); }
    @Override public void createSpotAnim(int p0, int p1, int p2, int p3) { throw new UnsupportedOperationException("createSpotAnim"); }
    @Override public void removeSpotAnim(int p0) { throw new UnsupportedOperationException("removeSpotAnim"); }
    @Override public void clearSpotAnims() { throw new UnsupportedOperationException("clearSpotAnims"); }
    @Override public int getGraphic() { throw new UnsupportedOperationException("getGraphic"); }
    @Override public void setGraphic(int p0) { throw new UnsupportedOperationException("setGraphic"); }
    @Override public int getGraphicHeight() { throw new UnsupportedOperationException("getGraphicHeight"); }
    @Override public void setGraphicHeight(int p0) { throw new UnsupportedOperationException("setGraphicHeight"); }
    @Override public int getSpotAnimFrame() { throw new UnsupportedOperationException("getSpotAnimFrame"); }
    @Override public void setSpotAnimFrame(int p0) { throw new UnsupportedOperationException("setSpotAnimFrame"); }
    @Override public java.awt.Polygon getCanvasTilePoly() { throw new UnsupportedOperationException("getCanvasTilePoly"); }
    @Override public net.runelite.api.Point getCanvasTextLocation(java.awt.Graphics2D p0, java.lang.String p1, int p2) { throw new UnsupportedOperationException("getCanvasTextLocation"); }
    @Override public net.runelite.api.Point getCanvasImageLocation(java.awt.image.BufferedImage p0, int p1) { throw new UnsupportedOperationException("getCanvasImageLocation"); }
    @Override public net.runelite.api.Point getCanvasSpriteLocation(net.runelite.api.SpritePixels p0, int p1) { throw new UnsupportedOperationException("getCanvasSpriteLocation"); }
    @Override public net.runelite.api.Point getMinimapLocation() { throw new UnsupportedOperationException("getMinimapLocation"); }
    @Override public int getLogicalHeight() { throw new UnsupportedOperationException("getLogicalHeight"); }
    @Override public java.awt.Shape getConvexHull() { throw new UnsupportedOperationException("getConvexHull"); }
    @Override public net.runelite.api.coords.WorldArea getWorldArea() { throw new UnsupportedOperationException("getWorldArea"); }
    @Override public java.lang.String getOverheadText() { throw new UnsupportedOperationException("getOverheadText"); }
    @Override public void setOverheadText(java.lang.String p0) { throw new UnsupportedOperationException("setOverheadText"); }
    @Override public int getOverheadCycle() { throw new UnsupportedOperationException("getOverheadCycle"); }
    @Override public void setOverheadCycle(int p0) { throw new UnsupportedOperationException("setOverheadCycle"); }
    @Override public boolean isDead() { throw new UnsupportedOperationException("isDead"); }
    @Override public void setDead(boolean p0) { throw new UnsupportedOperationException("setDead"); }
    @Override public int getFootprintSize() { throw new UnsupportedOperationException("getFootprintSize"); }
    @Override public int getAnimationHeightOffset() { throw new UnsupportedOperationException("getAnimationHeightOffset"); }
    @Override public net.runelite.api.Model getModel() { throw new UnsupportedOperationException("getModel"); }
    @Override public int getModelHeight() { throw new UnsupportedOperationException("getModelHeight"); }
    @Override public void setModelHeight(int p0) { throw new UnsupportedOperationException("setModelHeight"); }
    @Override public int getRenderMode() { throw new UnsupportedOperationException("getRenderMode"); }
    @Override public net.runelite.api.Node getNext() { throw new UnsupportedOperationException("getNext"); }
    @Override public net.runelite.api.Node getPrevious() { throw new UnsupportedOperationException("getPrevious"); }
    @Override public long getHash() { throw new UnsupportedOperationException("getHash"); }
    @Override public net.runelite.api.coords.LocalPoint getCameraFocus() { throw new UnsupportedOperationException("getCameraFocus"); }
}
