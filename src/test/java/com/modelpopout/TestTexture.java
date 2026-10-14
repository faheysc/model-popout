package com.modelpopout;

import net.runelite.api.Node;
import net.runelite.api.Texture;

final class TestTexture implements Texture
{
    final int[] pixels; final int direction,speed;
    TestTexture(int[] pixels,int direction,int speed) { this.pixels=pixels; this.direction=direction; this.speed=speed; }
    @Override public int[] getPixels(){return pixels;}
    @Override public int getAnimationDirection(){return direction;}
    @Override public int getAnimationSpeed(){return speed;}
    @Override public boolean isLoaded(){return true;}
    @Override public float getU(){return 0;}
    @Override public float getV(){return 0;}
    @Override public void setU(float value){throw new AssertionError("Must not animate native textures");}
    @Override public void setV(float value){throw new AssertionError("Must not animate native textures");}
    @Override public Node getNext(){throw new UnsupportedOperationException();}
    @Override public Node getPrevious(){throw new UnsupportedOperationException();}
    @Override public long getHash(){return 0;}
}
