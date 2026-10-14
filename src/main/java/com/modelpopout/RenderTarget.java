package com.modelpopout;

public enum RenderTarget
{
    FPS_60(60), FPS_120(120), FPS_144(144), FPS_165(165), FPS_240(240), UNLIMITED(0);
    final int fps;
    RenderTarget(int fps) { this.fps = fps; }
    @Override public String toString() { return fps == 0 ? "Unlimited (on demand)" : fps + " FPS"; }
}
