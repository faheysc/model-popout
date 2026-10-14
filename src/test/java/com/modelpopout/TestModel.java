package com.modelpopout;

/** Explicit API test double: unexpected model reads or mutations fail immediately. */
@SuppressWarnings("deprecation")
class TestModel implements net.runelite.api.Model
{
    @Override public int[] getFaceColors1() { throw new UnsupportedOperationException("getFaceColors1"); }
    @Override public int[] getFaceColors2() { throw new UnsupportedOperationException("getFaceColors2"); }
    @Override public int[] getFaceColors3() { throw new UnsupportedOperationException("getFaceColors3"); }
    @Override public short[] getUnlitFaceColors() { throw new UnsupportedOperationException("getUnlitFaceColors"); }
    @Override public int getSceneId() { throw new UnsupportedOperationException("getSceneId"); }
    @Override public void setSceneId(int p0) { throw new UnsupportedOperationException("setSceneId"); }
    @Override public int getBufferOffset() { throw new UnsupportedOperationException("getBufferOffset"); }
    @Override public void setBufferOffset(int p0) { throw new UnsupportedOperationException("setBufferOffset"); }
    @Override public int getUvBufferOffset() { throw new UnsupportedOperationException("getUvBufferOffset"); }
    @Override public void setUvBufferOffset(int p0) { throw new UnsupportedOperationException("setUvBufferOffset"); }
    @Override public int getBottomY() { throw new UnsupportedOperationException("getBottomY"); }
    @Override public void calculateBoundsCylinder() { throw new UnsupportedOperationException("calculateBoundsCylinder"); }
    @Override public byte getTransparency() { return 0; }
    @Override public byte[] getFaceRenderPriorities() { return null; }
    @Override public byte[] getFaceBias() { throw new UnsupportedOperationException("getFaceBias"); }
    @Override public int getRadius() { throw new UnsupportedOperationException("getRadius"); }
    @Override public int getDiameter() { throw new UnsupportedOperationException("getDiameter"); }
    @Override public void calculateExtreme(int p0) { throw new UnsupportedOperationException("calculateExtreme"); }
    @Override public net.runelite.api.AABB getAABB(int p0) { throw new UnsupportedOperationException("getAABB"); }
    @Override public int getXYZMag() { throw new UnsupportedOperationException("getXYZMag"); }
    @Override public boolean useBoundingBox() { throw new UnsupportedOperationException("useBoundingBox"); }
    @Override public int[] getVertexNormalsX() { throw new UnsupportedOperationException("getVertexNormalsX"); }
    @Override public int[] getVertexNormalsY() { throw new UnsupportedOperationException("getVertexNormalsY"); }
    @Override public int[] getVertexNormalsZ() { throw new UnsupportedOperationException("getVertexNormalsZ"); }
    @Override public byte getOverrideAmount() { return 0; }
    @Override public byte getOverrideHue() { return -1; }
    @Override public byte getOverrideSaturation() { return -1; }
    @Override public byte getOverrideLuminance() { return -1; }
    @Override public byte[] getTextureFaces() { return null; }
    @Override public int[] getTexIndices1() { return null; }
    @Override public int[] getTexIndices2() { return null; }
    @Override public int[] getTexIndices3() { return null; }
    @Override public net.runelite.api.Model getUnskewedModel() { throw new UnsupportedOperationException("getUnskewedModel"); }
    @Override public void drawFrustum(int p0, int p1, int p2, int p3, int p4, int p5, int p6) { throw new UnsupportedOperationException("drawFrustum"); }
    @Override public void drawOrtho(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7) { throw new UnsupportedOperationException("drawOrtho"); }
    @Override public int getVerticesCount() { throw new UnsupportedOperationException("getVerticesCount"); }
    @Override public float[] getVerticesX() { throw new UnsupportedOperationException("getVerticesX"); }
    @Override public float[] getVerticesY() { throw new UnsupportedOperationException("getVerticesY"); }
    @Override public float[] getVerticesZ() { throw new UnsupportedOperationException("getVerticesZ"); }
    @Override public int getFaceCount() { throw new UnsupportedOperationException("getFaceCount"); }
    @Override public int[] getFaceIndices1() { throw new UnsupportedOperationException("getFaceIndices1"); }
    @Override public int[] getFaceIndices2() { throw new UnsupportedOperationException("getFaceIndices2"); }
    @Override public int[] getFaceIndices3() { throw new UnsupportedOperationException("getFaceIndices3"); }
    @Override public byte[] getFaceTransparencies() { throw new UnsupportedOperationException("getFaceTransparencies"); }
    @Override public short[] getFaceTextures() { throw new UnsupportedOperationException("getFaceTextures"); }
    @Override public net.runelite.api.Model rotateY90Ccw() { throw new UnsupportedOperationException("rotateY90Ccw"); }
    @Override public net.runelite.api.Model rotateY180Ccw() { throw new UnsupportedOperationException("rotateY180Ccw"); }
    @Override public net.runelite.api.Model rotateY270Ccw() { throw new UnsupportedOperationException("rotateY270Ccw"); }
    @Override public net.runelite.api.Model translate(int p0, int p1, int p2) { throw new UnsupportedOperationException("translate"); }
    @Override public net.runelite.api.Model scale(int p0, int p1, int p2) { throw new UnsupportedOperationException("scale"); }
    @Override public net.runelite.api.Model getModel() { throw new UnsupportedOperationException("getModel"); }
    @Override public int getModelHeight() { throw new UnsupportedOperationException("getModelHeight"); }
    @Override public void setModelHeight(int p0) { throw new UnsupportedOperationException("setModelHeight"); }
    @Override public int getAnimationHeightOffset() { throw new UnsupportedOperationException("getAnimationHeightOffset"); }
    @Override public int getRenderMode() { throw new UnsupportedOperationException("getRenderMode"); }
    @Override public net.runelite.api.Node getNext() { throw new UnsupportedOperationException("getNext"); }
    @Override public net.runelite.api.Node getPrevious() { throw new UnsupportedOperationException("getPrevious"); }
    @Override public long getHash() { throw new UnsupportedOperationException("getHash"); }
}
