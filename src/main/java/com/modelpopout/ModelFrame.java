package com.modelpopout;

/** Immutable model-space framing, established once and independent of animation pose. */
final class ModelFrame
{
    // Reserve motion space even when the first sample is a crouched/compact pose.
    private static final double MIN_ANIMATION_RADIUS = 256;
    private static final double ANIMATION_HEADROOM = 2;
    final double centerX, centerY, centerZ, radius;

    private ModelFrame(double centerX, double centerY, double centerZ, double radius)
    {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
    }

    static ModelFrame initial(ModelSnapshot mesh)
    {
        ModelFrame fitted = fit(mesh);
        return new ModelFrame(fitted.centerX, fitted.centerY, fitted.centerZ,
            Math.max(MIN_ANIMATION_RADIUS, fitted.radius * ANIMATION_HEADROOM));
    }

    /** Tight framing for isolated static renderer tests; live frames use initial() once. */
    static ModelFrame fit(ModelSnapshot mesh)
    {
        double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        for (int i = 0; i < mesh.x.length; i++)
        {
            minX = Math.min(minX, mesh.x[i]); maxX = Math.max(maxX, mesh.x[i]);
            minY = Math.min(minY, mesh.y[i]); maxY = Math.max(maxY, mesh.y[i]);
            minZ = Math.min(minZ, mesh.z[i]); maxZ = Math.max(maxZ, mesh.z[i]);
        }
        double cx = (minX + maxX) / 2, cy = (minY + maxY) / 2, cz = (minZ + maxZ) / 2;
        double radius = 0;
        for (int i = 0; i < mesh.x.length; i++)
        {
            double x = mesh.x[i] - cx, y = mesh.y[i] - cy, z = mesh.z[i] - cz;
            radius = Math.max(radius, Math.sqrt(x * x + y * y + z * z));
        }
        return new ModelFrame(cx, cy, cz, Math.max(.5, radius));
    }

    double scale(CameraView camera)
    {
        return (ModelRenderer.SIZE - 48) / (2 * radius) * camera.zoom;
    }
}
