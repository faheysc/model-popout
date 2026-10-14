package com.modelpopout;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/** Bounded opaque-model raster cache: reuse static pixels and texture coverage/UVs on texture-only frames. */
final class TextureRasterPlan
{
    static final int MAX_FRAGMENTS = 65536;
    private final int[] base,baseRank,workingRank;
    private final double[] baseDepth;
    private final double[] workingDepth;
    private final int[] pixel,material,fallback,rank;
    private final double[] depth,u,v,light;
    private final int count;
    private final int[] usedMaterials;
    private final ModelSnapshot mesh;
    private final ModelProjection projection;

    private TextureRasterPlan(Builder b,ModelSnapshot mesh,ModelProjection projection)
    {
        this.mesh=mesh;this.projection=projection;
        usedMaterials=java.util.stream.IntStream.range(0,mesh.textureIds.length).map(i -> mesh.textureIds[i]).filter(id -> id>=0).distinct().toArray();
        base=b.base; baseRank=b.baseRank;workingRank=new int[base.length];baseDepth=b.baseDepth; workingDepth=new double[baseDepth.length]; count=b.count;
        rank=Arrays.copyOf(b.rank,count);pixel=Arrays.copyOf(b.pixel,count); material=Arrays.copyOf(b.material,count); fallback=Arrays.copyOf(b.fallback,count);
        depth=Arrays.copyOf(b.depth,count);u=Arrays.copyOf(b.u,count);v=Arrays.copyOf(b.v,count);light=Arrays.copyOf(b.light,count);
    }

    static TextureRasterPlan build(ModelSnapshot mesh,ModelProjection projection)
    {
        if (mesh.textureIds == null) return null;
        boolean texturedFace=false;
        for(int face=0;face<mesh.textureIds.length;face++) if(mesh.textureIds[face]>=0 && Double.isFinite(projection.uv[face*6])) { texturedFace=true;break; }
        if(!texturedFace) return null;
        for (byte alpha : mesh.alpha) if ((alpha&255) != 0 && (alpha&255) != 255) return null;
        Builder builder=new Builder();
        long work=0;
        int[] ranks=new int[mesh.a.length];
        for(int i=0;i<projection.faces.length;i++) ranks[projection.faces[i]]=i;
        for(int pass=0;pass<2;pass++) for(int face:projection.faces)
        {
            if(mesh.colorC[face]<0 || (mesh.alpha[face]&255)==255) continue;
            int uvOffset=face*6;
            boolean textured=mesh.textureIds[face]>=0 && Double.isFinite(projection.uv[uvOffset]);
            if(textured != (pass==1)) continue;
            int a=mesh.a[face],b=mesh.b[face],c=mesh.c[face];
            double area=edge(projection.x[a],projection.y[a],projection.x[b],projection.y[b],projection.x[c],projection.y[c]);
            // Match the general/native path before caching any face coverage.
            if(area<=1e-8) continue;
            int minX=Math.max(0,(int)Math.floor(Math.min(projection.x[a],Math.min(projection.x[b],projection.x[c]))));
            int maxX=Math.min(511,(int)Math.ceil(Math.max(projection.x[a],Math.max(projection.x[b],projection.x[c]))));
            int minY=Math.max(0,(int)Math.floor(Math.min(projection.y[a],Math.min(projection.y[b],projection.y[c]))));
            int maxY=Math.min(511,(int)Math.ceil(Math.max(projection.y[a],Math.max(projection.y[b],projection.y[c]))));
            work+=(long)Math.max(0,maxX-minX+1)*Math.max(0,maxY-minY+1);
            if(work>16000000) return null; // Full renderer preserves the existing work-limit rejection.
            for(int py=minY;py<=maxY;py++) for(int px=minX;px<=maxX;px++)
            {
                double wa=edge(projection.x[b],projection.y[b],projection.x[c],projection.y[c],px+.5,py+.5)/area;
                double wb=edge(projection.x[c],projection.y[c],projection.x[a],projection.y[a],px+.5,py+.5)/area;
                double wc=1-wa-wb;
                if(wa < -1e-9 || wb < -1e-9 || wc < -1e-9) continue;
                int index=py*512+px;
                double z=wa*projection.z[a]+wb*projection.z[b]+wc*projection.z[c];
                if(z>builder.baseDepth[index] || (z==builder.baseDepth[index] && ranks[face]>=builder.baseRank[index])) continue;
                int color=0xff000000;
                for(int shift=0;shift<=16;shift+=8) color|=(int)Math.round(wa*((mesh.colorA[face]>>shift)&255)
                    +wb*((mesh.colorB[face]>>shift)&255)+wc*((mesh.colorC[face]>>shift)&255))<<shift;
                if(!textured) { builder.base[index]=color; builder.baseDepth[index]=z;builder.baseRank[index]=ranks[face]; continue; }
                if(builder.count==MAX_FRAGMENTS) return null;
                builder.ensureCapacity();
                int i=builder.count++;
                builder.rank[i]=ranks[face];builder.pixel[i]=index;builder.material[i]=mesh.textureIds[face];builder.depth[i]=z;builder.fallback[i]=color;
                builder.u[i]=wa*projection.uv[uvOffset]+wb*projection.uv[uvOffset+2]+wc*projection.uv[uvOffset+4];
                builder.v[i]=wa*projection.uv[uvOffset+1]+wb*projection.uv[uvOffset+3]+wc*projection.uv[uvOffset+5];
                builder.light[i]=Math.max(0,(wa*mesh.lightA[face]+wb*mesh.lightB[face]+wc*mesh.lightC[face])/127.0);
            }
        }
        return builder.count==0 ? null : new TextureRasterPlan(builder,mesh,projection);
    }

    BufferedImage render(TextureSet textures,double phase)
    {
        // Filtered moving cutouts create partial alpha; preserve exact priority/depth blending via the general path.
        for(int id:usedMaterials)
        {
            TextureSet.Pixels texture=textures.texture(id);
            if(texture!=null && texture.moving && texture.cutouts) return new ModelRenderer().rasterize(mesh,projection,textures,phase);
        }
        BufferedImage image=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);
        int[] pixels=((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        System.arraycopy(base,0,pixels,0,base.length);
        double[] z=workingDepth;
        System.arraycopy(baseDepth,0,z,0,baseDepth.length);
        System.arraycopy(baseRank,0,workingRank,0,baseRank.length);
        for(int i=0;i<count;i++)
        {
            int index=pixel[i];
            if(depth[i]>z[index] || (depth[i]==z[index] && rank[i]>=workingRank[index])) continue;
            TextureSet.Pixels texture=textures.texture(material[i]);
            int color=texture==null ? fallback[i] : PixelBlend.shade(texture.sampleArgb(u[i],v[i],phase,textures),light[i]);
            if((color>>>24)==0) continue;
            pixels[index]=PixelBlend.over(color,pixels[index]);z[index]=depth[i];workingRank[index]=rank[i];
        }
        return image;
    }
    int fragments() { return count; }

    private static double edge(double ax,double ay,double bx,double by,double px,double py)
    { return (px-ax)*(by-ay)-(py-ay)*(bx-ax); }

    private static final class Builder
    {
        int count;
        final int[] base=new int[512*512],baseRank=new int[512*512];
        final double[] baseDepth=new double[512*512];
        int[] rank=new int[2048],pixel=new int[2048],material=new int[2048],fallback=new int[2048];
        double[] depth=new double[2048],u=new double[2048],v=new double[2048],light=new double[2048];
        Builder() { Arrays.fill(baseDepth,Double.POSITIVE_INFINITY);Arrays.fill(baseRank,Integer.MAX_VALUE); }
        void ensureCapacity()
        {
            if(count<pixel.length) return;
            int next=Math.min(MAX_FRAGMENTS,pixel.length*2);
            rank=Arrays.copyOf(rank,next);pixel=Arrays.copyOf(pixel,next);material=Arrays.copyOf(material,next);fallback=Arrays.copyOf(fallback,next);
            depth=Arrays.copyOf(depth,next);u=Arrays.copyOf(u,next);v=Arrays.copyOf(v,next);light=Arrays.copyOf(light,next);
        }
    }
}
