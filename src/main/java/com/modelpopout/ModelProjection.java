package com.modelpopout;

/** Cached projection and UVs. Texture-only updates do not recapture, convert or reproject geometry. */
final class ModelProjection
{
    final CameraView camera;
    final ModelFrame frame;
    final double[] x,y,z,uv;
    final int[] faces;
    private TextureRasterPlan rasterPlan;
    private boolean rasterAttempted;
    ModelProjection(ModelSnapshot mesh,CameraView camera,ModelFrame frame)
    {
        this.camera=camera; this.frame=frame;
        int n=mesh.x.length; x=new double[n]; y=new double[n]; z=new double[n];
        double scale=frame.scale(camera), cy=Math.cos(camera.yaw), sy=Math.sin(camera.yaw);
        double cp=Math.cos(camera.pitch), sp=Math.sin(camera.pitch);
        for(int i=0;i<n;i++)
        {
            double mx=mesh.x[i]-frame.centerX, my=mesh.y[i]-frame.centerY, mz=mesh.z[i]-frame.centerZ;
            double rx=mx*cy+mz*sy, rz=mz*cy-mx*sy;
            x[i]=ModelRenderer.SIZE/2.0+rx*scale;
            y[i]=ModelRenderer.SIZE/2.0+(my*cp-rz*sp)*scale;
            z[i]=my*sp+rz*cp;
        }
        uv=mesh.uvs(); faces=FaceOrdering.order(mesh,z);
    }
    TextureRasterPlan rasterPlan(ModelSnapshot mesh)
    {
        if (!rasterAttempted) { rasterPlan = TextureRasterPlan.build(mesh,this); rasterAttempted = true; }
        return rasterPlan;
    }

    boolean matches(CameraView view,ModelFrame reference)
    {
        return frame == reference && camera.yaw == view.yaw && camera.pitch == view.pitch && camera.zoom == view.zoom;
    }
}
