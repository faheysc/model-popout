package com.modelpopout;

import java.awt.Point;

/** Stable initial head anchor transformed exactly like its model, independent of later pose bounds. */
final class HudAnchor
{
    private final double x,y,z;
    private final ModelFrame frame;
    private HudAnchor(double x,double y,double z,ModelFrame frame) { this.x=x;this.y=y;this.z=z;this.frame=frame; }
    static HudAnchor initial(ModelSnapshot mesh,ModelFrame frame)
    {
        double top=Double.POSITIVE_INFINITY;
        for(float y:mesh.y) top=Math.min(top,y);
        return new HudAnchor(frame.centerX,top-12,frame.centerZ,frame);
    }
    Point screen(CameraView camera,int width,int height,boolean npcEnabled,int slot,boolean mirrored)
    {
        int slots=npcEnabled?2:1,size=Math.min(width/slots,height);
        int left=(width-size*slots)/2+size*slot,top=(height-size)/2;
        double mx=x-frame.centerX,my=y-frame.centerY,mz=z-frame.centerZ;
        double rx=mx*Math.cos(camera.yaw)+mz*Math.sin(camera.yaw);
        double rz=mz*Math.cos(camera.yaw)-mx*Math.sin(camera.yaw);
        double px=ModelRenderer.SIZE/2.0+rx*frame.scale(camera);
        if(mirrored) px=ModelRenderer.SIZE-px;
        double py=ModelRenderer.SIZE/2.0+(my*Math.cos(camera.pitch)-rz*Math.sin(camera.pitch))*frame.scale(camera);
        int sx=left+(int)Math.round(px*size/ModelRenderer.SIZE),sy=top+(int)Math.round(py*size/ModelRenderer.SIZE);
        return new Point(Math.max(48,Math.min(width-48,sx)),Math.max(64,Math.min(height-8,sy)));
    }
}
