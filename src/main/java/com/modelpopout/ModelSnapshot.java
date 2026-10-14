package com.modelpopout;

import java.util.Arrays;
import java.awt.image.BufferedImage;
import net.runelite.api.Model;
import net.runelite.api.TextureProvider;

/** Owned arrays only: never retain a client Model across threads or frames. */
final class ModelSnapshot
{
    final float[] x, y, z;
    final int[] a, b, c, colorA, colorB, colorC;
    final byte[] alpha;
    final short[] textureIds;
    final byte[] textureFaces, priorities;
    final int[] texA, texB, texC, lightA, lightB, lightC;
    final double brightness;
    private volatile double[] uv;
    private volatile ModelProjection projection;
    private final boolean packedColors;
    private volatile ModelSnapshot rgb;
    private volatile Rendered rendered;

    ModelSnapshot(float[] x, float[] y, float[] z, int[] a, int[] b, int[] c,
        int[] colorA, int[] colorB, int[] colorC, byte[] alpha)
    {
        this(x.clone(), y.clone(), z.clone(), a.clone(), b.clone(), c.clone(),
            colorA.clone(), colorB.clone(), colorC.clone(), alpha.clone(), false, null,null,null,null,null,null,null,null,null,.8);
    }

    private ModelSnapshot(float[] x, float[] y, float[] z, int[] a, int[] b, int[] c,
        int[] colorA, int[] colorB, int[] colorC, byte[] alpha, boolean packedColors,
        short[] textureIds, byte[] textureFaces, int[] texA, int[] texB, int[] texC,
        byte[] priorities, int[] lightA, int[] lightB, int[] lightC, double brightness)
    {
        this.packedColors = packedColors;
        this.x = x; this.y = y; this.z = z;
        this.a = a; this.b = b; this.c = c;
        this.colorA = colorA; this.colorB = colorB; this.colorC = colorC;
        this.alpha = alpha;
        this.textureIds = textureIds; this.textureFaces = textureFaces;
        this.texA = texA; this.texB = texB; this.texC = texC; this.priorities = priorities;
        this.lightA = lightA; this.lightB = lightB; this.lightC = lightC; this.brightness = brightness;
    }

    /** Must be called on the client thread. Does not mutate any client array. */
    static ModelSnapshot capture(Model model, TextureProvider textures)
    {
        int vertices = model.getVerticesCount(), faces = model.getFaceCount();
        if (vertices < 1 || vertices > 50000 || faces < 1 || faces > 30000)
        {
            throw new IllegalArgumentException("Model exceeds the snapshot size limit");
        }
        float[] x = copyRequired(model.getVerticesX(), vertices);
        float[] y = copyRequired(model.getVerticesY(), vertices);
        float[] z = copyRequired(model.getVerticesZ(), vertices);
        int[] a = copyRequired(model.getFaceIndices1(), faces);
        int[] b = copyRequired(model.getFaceIndices2(), faces);
        int[] c = copyRequired(model.getFaceIndices3(), faces);
        int[] ca = copyRequired(model.getFaceColors1(), faces);
        int[] cb = copyRequired(model.getFaceColors2(), faces);
        int[] cc = copyRequired(model.getFaceColors3(), faces);
        byte[] transparencies = copyOptional(model.getFaceTransparencies(), faces);
        byte[] alpha = transparencies == null ? new byte[faces] : transparencies;
        short[] faceTextures = copyOptional(model.getFaceTextures(), faces);
        byte[] textureFaces = copyOptional(model.getTextureFaces(), faces);
        int[] texA = copy(model.getTexIndices1()), texB = copy(model.getTexIndices2()), texC = copy(model.getTexIndices3());
        if (texA != null && (texB == null || texC == null || texA.length != texB.length || texA.length != texC.length))
            throw new IllegalArgumentException("Invalid texture mapping arrays");
        byte[] priorities = copyOptional(model.getFaceRenderPriorities(), faces);
        int[] lightA = ca.clone(), lightB = cb.clone(), lightC = cc.clone();
        int modelAlpha = model.getTransparency() & 255;
        byte override = model.getOverrideAmount();
        byte overrideHue=override==0 ? -1 : model.getOverrideHue();
        byte overrideSat=override==0 ? -1 : model.getOverrideSaturation();
        byte overrideLum=override==0 ? -1 : model.getOverrideLuminance();
        double brightness = textures == null ? .8 : textures.getBrightness();
        for (int i = 0; i < vertices; i++)
        {
            if (!Float.isFinite(x[i]) || !Float.isFinite(y[i]) || !Float.isFinite(z[i]))
            {
                throw new IllegalArgumentException("Non-finite player model vertex");
            }
        }
        for (int i = 0; i < faces; i++)
        {
            if (a[i] < 0 || a[i] >= vertices || b[i] < 0 || b[i] >= vertices || c[i] < 0 || c[i] >= vertices)
            {
                throw new IllegalArgumentException("Invalid player model face");
            }
            if (cc[i] == -2) // Native hidden-face sentinel.
            {
                ca[i] = cb[i] = cc[i] = -1;
                continue;
            }
            int faceAlpha = alpha[i] & 255;
            if (modelAlpha == 255) alpha[i] = (byte)255;
            else if (modelAlpha > 0 && faceAlpha < 253) alpha[i] = (byte)(faceAlpha + ((253-faceAlpha)*modelAlpha >> 8));
            boolean flat = cc[i] == -1;
            if (flat) lightB[i] = lightC[i] = lightA[i];
            if (faceTextures != null && faceTextures[i] != -1 && textures != null)
            {
                // Textured face colors are lighting intensities, not packed HSL.
                // Use the texture provider's fallback HSL; no texture cache loads.
                int base = textures.getDefaultColor(faceTextures[i] & 0xffff);
                ca[i] = shadeTexture(base, ca[i]);
                cb[i] = flat ? ca[i] : shadeTexture(base, cb[i]);
                cc[i] = flat ? ca[i] : shadeTexture(base, cc[i]);
            }
            else
            {
                cb[i] = flat ? ca[i] : cb[i];
                cc[i] = flat ? ca[i] : cc[i];
                if ((faceTextures == null || faceTextures[i] == -1) && override > 0)
                {
                    ca[i] = overrideHsl(ca[i],overrideHue,overrideSat,overrideLum,override);
                    cb[i] = overrideHsl(cb[i],overrideHue,overrideSat,overrideLum,override);
                    cc[i] = overrideHsl(cc[i],overrideHue,overrideSat,overrideLum,override);
                }
            }
        }
        return new ModelSnapshot(x,y,z,a,b,c,ca,cb,cc,alpha,true,faceTextures,textureFaces,texA,texB,texC,priorities,lightA,lightB,lightC,brightness);
    }

    /** Exact array comparison, not Model identity or an animation-ID-only heuristic. */
    boolean sameAppearance(ModelSnapshot other)
    {
        return other != null && packedColors == other.packedColors
            && Arrays.equals(x, other.x) && Arrays.equals(y, other.y) && Arrays.equals(z, other.z)
            && Arrays.equals(a, other.a) && Arrays.equals(b, other.b) && Arrays.equals(c, other.c)
            && Arrays.equals(colorA, other.colorA) && Arrays.equals(colorB, other.colorB)
            && Arrays.equals(colorC, other.colorC) && Arrays.equals(alpha, other.alpha)
            && Arrays.equals(textureIds,other.textureIds) && Arrays.equals(textureFaces,other.textureFaces)
            && Arrays.equals(texA,other.texA) && Arrays.equals(texB,other.texB) && Arrays.equals(texC,other.texC)
            && Arrays.equals(priorities,other.priorities) && Arrays.equals(lightA,other.lightA)
            && Arrays.equals(lightB,other.lightB) && Arrays.equals(lightC,other.lightC) && brightness == other.brightness;
    }

    /** Palette work runs on the render worker, cached for subsequent camera-only renders. */
    ModelSnapshot toRgb()
    {
        if (!packedColors) return this;
        ModelSnapshot cached = rgb;
        if (cached != null) return cached;
        int[] ca = colorA.clone(), cb = colorB.clone(), cc = colorC.clone();
        for (int i = 0; i < ca.length; i++)
        {
            if (cc[i] < 0) continue;
            ca[i] = hslToRgb(ca[i],brightness);
            cb[i] = hslToRgb(cb[i],brightness);
            cc[i] = hslToRgb(cc[i],brightness);
        }
        cached = new ModelSnapshot(x,y,z,a,b,c,ca,cb,cc,alpha,false,textureIds,textureFaces,texA,texB,texC,priorities,lightA,lightB,lightC,brightness);
        rgb = cached;
        return cached;
    }

    /** One bounded raster cache per owned pose; unchanged actors need not reraster for the other actor. */
    BufferedImage cachedImage(CameraView camera, ModelFrame frame, TextureSet textures, double phase)
    {
        Rendered cached = rendered;
        return cached != null && cached.frame == frame && cached.yaw == camera.yaw
            && cached.pitch == camera.pitch && cached.zoom == camera.zoom
            && cached.textures == textures && cached.phase == phase ? cached.image : null;
    }

    void rememberImage(CameraView camera,ModelFrame frame,BufferedImage image,TextureSet textures,double phase)
    {
        rendered = new Rendered(camera,frame,image,textures,phase);
    }

    private static final class Rendered
    {
        final ModelFrame frame;
        final double yaw, pitch, zoom, phase;
        final TextureSet textures;
        final BufferedImage image;
        Rendered(CameraView camera, ModelFrame frame, BufferedImage image,TextureSet textures,double phase)
        {
            this.textures = textures; this.phase = phase;
            this.frame = frame; yaw = camera.yaw; pitch = camera.pitch; zoom = camera.zoom; this.image = image;
        }
    }

    ModelProjection projection(CameraView camera, ModelFrame frame)
    {
        ModelProjection cached = projection;
        if (cached == null || !cached.matches(camera,frame)) projection = cached = new ModelProjection(this,camera,frame);
        return cached;
    }

    double[] uvs()
    {
        double[] cached = uv;
        if (cached == null) uv = cached = TextureCoordinates.compute(this);
        return cached;
    }

    private static float[] copyRequired(float[] values, int count)
    {
        if (values == null || values.length < count) throw new IllegalArgumentException("Incomplete model vertex arrays");
        return Arrays.copyOf(values, count);
    }

    private static int[] copyRequired(int[] values, int count)
    {
        if (values == null || values.length < count) throw new IllegalArgumentException("Incomplete model face arrays");
        return Arrays.copyOf(values, count);
    }

    private static byte[] copyOptional(byte[] values, int count)
    {
        if (values == null) return null;
        if (values.length < count) throw new IllegalArgumentException("Incomplete model face metadata");
        return Arrays.copyOf(values, count);
    }

    private static short[] copyOptional(short[] values, int count)
    {
        if (values == null) return null;
        if (values.length < count) throw new IllegalArgumentException("Incomplete model texture assignments");
        return Arrays.copyOf(values, count);
    }

    private static int[] copy(int[] values)
    {
        if (values == null) return null;
        if (values.length > 50000) throw new IllegalArgumentException("Texture mapping limit exceeded");
        return values.clone();
    }

    private static int overrideHsl(int hsl, byte hue, byte saturation, byte luminance, byte amount)
    {
        int[] components = {hsl>>10 & 63,hsl>>7 & 7,hsl & 127};
        int[] target = {hue,saturation,luminance};
        for (int i=0;i<3;i++) if (target[i] != -1) components[i] += ((amount & 255)*(target[i]-components[i]))>>7;
        return ((components[0]<<10) | (components[1]<<7) | components[2]) & 65535;
    }

    private static int shadeTexture(int hsl, int light)
    {
        return (hsl & 0xff80) | Math.max(2, Math.min(126, (hsl & 127) * light / 128));
    }

    static int hslToRgb(int packed) { return hslToRgb(packed,.8); }

    static int hslToRgb(int packed, double brightness)
    {
        double h = ((packed >> 10) & 63) / 64.0 + 1.0 / 128;
        double s = ((packed >> 7) & 7) / 8.0 + 1.0 / 16;
        double l = (packed & 127) / 128.0;
        double q = l < .5 ? l * (1 + s) : l + s - l * s;
        double p = 2 * l - q;
        return channel(hue(p, q, h + 1.0 / 3),brightness) << 16
            | channel(hue(p, q, h),brightness) << 8 | channel(hue(p, q, h - 1.0 / 3),brightness);
    }

    private static double hue(double p, double q, double t)
    {
        t -= Math.floor(t);
        if (t < 1.0 / 6) return p + (q - p) * 6 * t;
        if (t < .5) return q;
        if (t < 2.0 / 3) return p + (q - p) * (2.0 / 3 - t) * 6;
        return p;
    }

    private static int channel(double value,double brightness)
    {
        return Math.max(0, Math.min(255, (int) (Math.pow(value, brightness) * 256)));
    }
}
