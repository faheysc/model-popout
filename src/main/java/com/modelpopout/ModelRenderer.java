package com.modelpopout;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/** Small orthographic CPU renderer. No RuneLite access or global rasterizer state. */
final class ModelRenderer
{
    static final int BACKGROUND = 0x20242a;
    static final int SIZE = 512;

    /** Single-snapshot helpers for static renderer checks; live rendering requires retained framing. */
    BufferedImage renderStatic(ModelSnapshot mesh)
    {
        return renderStatic(mesh, CameraView.DEFAULT);
    }

    BufferedImage renderStatic(ModelSnapshot mesh, CameraView camera)
    {
        return BackgroundOpacity.compose(render(mesh, camera, ModelFrame.fit(mesh)), 100);
    }

    BufferedImage render(ModelSnapshot mesh, CameraView camera, ModelFrame frame)
    {
        return render(mesh,camera,frame,TextureSet.EMPTY,0);
    }

    BufferedImage render(ModelSnapshot mesh,CameraView camera,ModelFrame frame,TextureSet textures,double phase)
    {
        ModelSnapshot rgb = mesh.toRgb();
        boolean textured = rgb.textureIds != null && java.util.stream.IntStream.range(0,rgb.textureIds.length).anyMatch(i -> rgb.textureIds[i] >= 0);
        TextureSet relevant = textured ? textures : TextureSet.EMPTY;
        double time = textured ? phase : 0;
        BufferedImage cached = rgb.cachedImage(camera,frame,relevant,time);
        if (cached != null) return cached;
        BufferedImage image = renderRgb(rgb,camera,frame,relevant,time);
        rgb.rememberImage(camera,frame,image,relevant,time);
        return image;
    }

    BufferedImage renderScene(LiveViewState.RenderRequest request)
    {
        BufferedImage player = render(request.model,request.camera,request.framing,request.textures,request.texturePhase);
        if (!request.npcEnabled) return player;
        BufferedImage scene = new BufferedImage(SIZE * 2, SIZE, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = scene.createGraphics();
        try
        {
            g.drawImage(player, 0, 0, null);
            if (request.npcModel != null)
            {
                BufferedImage npc = render(request.npcModel,request.camera,request.npcFraming,request.textures,request.texturePhase);
                g.drawImage(npc, SIZE, 0, null);
            }
        }
        finally { g.dispose(); }
        return scene;
    }

    private BufferedImage renderRgb(ModelSnapshot mesh, CameraView camera, ModelFrame frame,TextureSet textures,double phase)
    {
        ModelProjection projection = mesh.projection(camera,frame);
        TextureRasterPlan plan = projection.rasterPlan(mesh);
        if (plan != null) return plan.render(textures,phase);
        return rasterize(mesh,projection,textures,phase);
    }

    // The uncached path also serves as the regression oracle for the bounded texture raster cache.
    BufferedImage rasterize(ModelSnapshot mesh,ModelProjection projection,TextureSet textures,double phase)
    {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        double[] depth = new double[pixels.length];
        Arrays.fill(depth, Double.POSITIVE_INFINITY);
        long remainingWork = 16000000;
        // Opaque depth first. Transparent faces test against it but never overwrite it.
        for (int pass=0;pass<2;pass++) for (int face : projection.faces)
        {
            int alpha = mesh.alpha[face] & 255;
            if (mesh.colorC[face] < 0 || alpha == 255 || ((alpha == 0) != (pass == 0))) continue;
            remainingWork -= triangle(mesh,face,projection.x,projection.y,projection.z,pixels,depth,remainingWork,projection.uv,textures,phase);
        }
        return image;
    }

    private int triangle(ModelSnapshot m, int face, double[] x, double[] y, double[] z,
        int[] pixels, double[] depth, long remainingWork,double[] uv,TextureSet textures,double phase)
    {
        int a = m.a[face], b = m.b[face], c = m.c[face];
        double area = edge(x[a], y[a], x[b], y[b], x[c], y[c]);
        // Native software rendering rejects reverse-wound faces. Cape backs occupy
        // the same surface as their textured fronts and must not enter the depth test.
        if (area <= 1e-8) return 0;
        int minX = Math.max(0, (int) Math.floor(Math.min(x[a], Math.min(x[b], x[c]))));
        int maxX = Math.min(SIZE - 1, (int) Math.ceil(Math.max(x[a], Math.max(x[b], x[c]))));
        int minY = Math.max(0, (int) Math.floor(Math.min(y[a], Math.min(y[b], y[c]))));
        int maxY = Math.min(SIZE - 1, (int) Math.ceil(Math.max(y[a], Math.max(y[b], y[c]))));
        int work = Math.max(0, maxX - minX + 1) * Math.max(0, maxY - minY + 1);
        if (work > remainingWork) throw new IllegalArgumentException("Model exceeds the rendering work limit");
        int transparency = m.alpha[face] & 255;
        double sourceAlpha = 1-transparency/255.0;
        int textureId = m.textureIds == null ? -1 : m.textureIds[face];
        TextureSet.Pixels texture = textures.texture(textureId);
        int uvOffset = face*6;
        boolean textured = texture != null && Double.isFinite(uv[uvOffset]);
        for (int py = minY; py <= maxY; py++)
        {
            for (int px = minX; px <= maxX; px++)
            {
                double wa = edge(x[b], y[b], x[c], y[c], px + .5, py + .5) / area;
                double wb = edge(x[c], y[c], x[a], y[a], px + .5, py + .5) / area;
                double wc = 1 - wa - wb;
                if (wa < -1e-9 || wb < -1e-9 || wc < -1e-9) continue;
                double dz = wa * z[a] + wb * z[b] + wc * z[c];
                int index = py * SIZE + px;
                if (dz >= depth[index]) continue;
                int texel = 0;
                double light = 1;
                if (textured)
                {
                    double u = wa*uv[uvOffset]+wb*uv[uvOffset+2]+wc*uv[uvOffset+4];
                    double v = wa*uv[uvOffset+1]+wb*uv[uvOffset+3]+wc*uv[uvOffset+5];
                    texel = texture.sampleArgb(u,v,phase,textures);
                    if (texel == 0) continue; // Native zero texels are cutouts, not black paint.
                    light = Math.max(0,(wa*m.lightA[face]+wb*m.lightB[face]+wc*m.lightC[face])/127.0);
                }
                double pixelAlpha = sourceAlpha*(textured ? (texel>>>24)/255.0 : 1);
                double destAlpha = (pixels[index]>>>24)/255.0;
                double outAlpha = pixelAlpha+destAlpha*(1-pixelAlpha);
                int rgb = (int) Math.round(outAlpha * 255) << 24;
                for (int shift = 0; shift <= 16; shift += 8)
                {
                    int value = (int) Math.round(wa * ((m.colorA[face] >> shift) & 255)
                        + wb * ((m.colorB[face] >> shift) & 255)
                        + wc * ((m.colorC[face] >> shift) & 255));
                    if (textured) value = (int)Math.round(((texel >> shift) & 255)*light);
                    if (pixelAlpha < 1) value = (int)Math.round((value*pixelAlpha
                        + ((pixels[index]>>shift)&255)*destAlpha*(1-pixelAlpha))/outAlpha);
                    rgb |= Math.max(0, Math.min(255, value)) << shift;
                }
                pixels[index] = rgb;
                if (transparency == 0) depth[index] = dz;
            }
        }
        return work;
    }

    private static double edge(double ax, double ay, double bx, double by, double px, double py)
    {
        return (px - ax) * (by - ay) - (py - ay) * (bx - ax);
    }
}
