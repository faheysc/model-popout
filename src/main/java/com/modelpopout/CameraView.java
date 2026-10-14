package com.modelpopout;

/** Immutable viewer-only camera settings, passed from the EDT to the renderer. */
final class CameraView
{
    static final double MIN_PITCH = Math.toRadians(-75);
    static final double MAX_PITCH = Math.toRadians(75);
    static final double MIN_ZOOM = .4;
    static final double MAX_ZOOM = 3;
    // Native player models face negative Z; yaw zero looks at that front surface.
    static final CameraView DEFAULT = new CameraView(0, Math.toRadians(10), 1);
    final double yaw, pitch, zoom;

    CameraView(double yaw, double pitch, double zoom)
    {
        if (!Double.isFinite(yaw) || !Double.isFinite(pitch) || !Double.isFinite(zoom))
        {
            throw new IllegalArgumentException("Camera settings must be finite");
        }
        double turn = Math.PI * 2;
        this.yaw = (yaw % turn + turn) % turn;
        this.pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch));
        this.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
    }

    CameraView drag(int dx, int dy)
    {
        return new CameraView(yaw + dx * .01, pitch + dy * .01, zoom);
    }

    CameraView wheel(double turns)
    {
        // Clamp wheel input before exponentiation, including high-resolution wheels.
        double exponent = Math.max(-20, Math.min(20, -turns * .12));
        return new CameraView(yaw, pitch, zoom * Math.exp(exponent));
    }
}
