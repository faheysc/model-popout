package com.modelpopout;

import java.util.Locale;

/** Counts distinct work stages; presentation means completed Swing paints, not monitor refresh. */
final class ViewerPerformance
{
    private long captures, poses, renders, paints, textureFrames;
    private boolean sampled;
    private long captureBase, poseBase, renderBase, paintBase, textureBase, sampleTime;
    synchronized void capture(boolean unique) { captures++; if (unique) poses++; }
    synchronized void rendered(boolean texture) { renders++; if (texture) textureFrames++; }
    synchronized void presented() { paints++; }
    synchronized String sample(long now)
    {
        if (!sampled) { sampled = true; sampleTime = now; return "Waiting for measurements…"; }
        double seconds = Math.max(.001,(now-sampleTime)/1e9);
        String text = String.format(Locale.ROOT,
            "Capture %.1f/s · unique poses %.1f/s · completed renders %.1f/s · Swing paints %.1f/s · texture frames %.1f/s · viewer pose interpolation off",
            (captures-captureBase)/seconds,(poses-poseBase)/seconds,(renders-renderBase)/seconds,
            (paints-paintBase)/seconds,(textureFrames-textureBase)/seconds);
        captureBase = captures; poseBase = poses; renderBase = renders; paintBase = paints; textureBase = textureFrames; sampleTime = now;
        return text;
    }
}
