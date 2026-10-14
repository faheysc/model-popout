package com.modelpopout;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import net.runelite.api.Texture;
import net.runelite.api.TextureProvider;

/** Immutable copies of referenced game textures. Native cache reads happen only on the client thread. */
final class TextureSet
{
    static final TextureSet EMPTY = new TextureSet(Collections.emptyMap(),0,false);
    final Map<Integer, Pixels> textures;
    private final Pixels[] byId;
    final int cycle;
    final boolean gpu;
    TextureSet(Map<Integer, Pixels> textures, int cycle, boolean gpu)
    {
        this.textures = Collections.unmodifiableMap(new HashMap<>(textures));
        int maximum = -1;
        for (int id : textures.keySet()) if (id >= 0 && id <= 32767) maximum = Math.max(maximum,id);
        byId = new Pixels[maximum+1];
        for (Map.Entry<Integer,Pixels> entry : textures.entrySet()) if (entry.getKey() >= 0 && entry.getKey() < byId.length) byId[entry.getKey()] = entry.getValue();
        this.cycle = cycle; this.gpu = gpu;
    }

    static TextureSet capture(TextureProvider provider, ModelSnapshot player, ModelSnapshot npc,
        int cycle, boolean gpu)
    {
        return capture(provider,player,npc,cycle,gpu,EMPTY);
    }

    static TextureSet capture(TextureProvider provider,ModelSnapshot player,ModelSnapshot npc,
        int cycle,boolean gpu,TextureSet previous)
    {
        if (provider == null) return previous;
        Map<Integer, Pixels> copied = new HashMap<>();
        Texture[] metadata = provider.getTextures();
        Set<Integer> visited = new HashSet<>();
        for (ModelSnapshot model : new ModelSnapshot[]{player,npc})
        {
            if (model == null || model.textureIds == null) continue;
            for (int face=0;face<model.textureIds.length;face++)
            {
                int id = model.textureIds[face];
                if (id < 0 || model.colorC[face] < 0 || (model.alpha[face]&255)==255 || visited.contains(id) || visited.size() >= 32) continue;
                visited.add(id);
                Pixels old = previous.textures.get(id);
                if (metadata == null || id >= metadata.length || metadata[id] == null)
                { if (old != null) copied.put(id,old); continue; }
                int[] pixels;
                try { pixels = provider.load(id); }
                catch (RuntimeException unavailable) { pixels = null; }
                if (pixels == null) { if (old != null) copied.put(id,old); continue; }
                Texture texture = metadata[id];
                int direction=texture.getAnimationDirection(),speed=texture.getAnimationSpeed();
                float u=texture.getU(),v=texture.getV();
                Pixels copy = old != null && previous.gpu==gpu && old.direction==direction && old.speed==speed && old.u==u && old.v==v
                    && Arrays.equals(old.rgb,pixels) ? old : new Pixels(pixels,direction,speed,u,v,cycle);
                if (copy.size > 0) copied.put(id,copy);
            }
        }
        return new TextureSet(copied,cycle,gpu);
    }

    Pixels texture(int id) { return id >= 0 && id < byId.length ? byId[id] : null; }

    boolean animated()
    {
        for (Pixels pixels : textures.values()) if (pixels.moving) return true;
        return false;
    }

    boolean samePixels(TextureSet other)
    {
        if (other == null || gpu != other.gpu || !textures.keySet().equals(other.textures.keySet())) return false;
        for (Map.Entry<Integer,Pixels> entry : textures.entrySet())
            if (!entry.getValue().same(other.textures.get(entry.getKey()))) return false;
        return true;
    }

    static final class Pixels
    {
        final int[] rgb;
        final int size, direction, speed, originCycle;
        final boolean anchored;
        final float u, v;
        final boolean moving,cutouts;
        Pixels(int[] rgb,int direction,int speed,float u,float v)
        { this(rgb,direction,speed,u,v,0,false); }
        Pixels(int[] rgb,int direction,int speed,float u,float v,int cycle)
        { this(rgb,direction,speed,u,v,cycle,true); }
        private Pixels(int[] rgb,int direction,int speed,float u,float v,int cycle,boolean anchored)
        {
            this.originCycle=cycle;this.anchored=anchored;
            int dimension = (int)Math.sqrt(rgb.length);
            size = dimension * dimension == rgb.length && dimension > 0 && dimension <= 256
                && (dimension & (dimension-1)) == 0 ? dimension : 0;
            this.rgb = size == 0 ? new int[0] : rgb.clone();
            this.direction = direction; this.speed = speed;
            this.u = Float.isFinite(u) ? u : 0; this.v = Float.isFinite(v) ? v : 0;
            boolean varied=false,clear=false;
            for (int pixel : this.rgb) { if (pixel != this.rgb[0]) varied=true; if (pixel == 0) clear=true; }
            cutouts=clear;
            moving=varied && direction>=1 && direction<=4 && speed>0;
        }
        boolean same(Pixels other)
        {
            return other != null && direction == other.direction && speed == other.speed
                && u == other.u && v == other.v && Arrays.equals(rgb,other.rgb);
        }
        int sampleArgb(double baseU,double baseV,double phase,TextureSet set)
        {
            double cycles = set.gpu ? phase : phase-(anchored ? originCycle : set.cycle);
            double unit = set.gpu ? 1.0/128 : 1.0/size;
            double uOffset = (direction == 2 ? -1 : direction == 4 ? 1 : 0)*speed*cycles*unit;
            double vOffset = (direction == 1 ? -1 : direction == 3 ? 1 : 0)*speed*cycles*unit;
            double u = baseU+this.u+uOffset, v = baseV+this.v+vOffset;
            u-=Math.floor(u);v-=Math.floor(v);
            if (direction < 1 || direction > 4 || speed == 0)
            {
                int pixel = sample(u,v);
                return pixel == 0 ? 0 : 0xff000000 | pixel;
            }
            // Linear filtering of real texels gives continuous scrolling, with premultiplied cutouts.
            double px=u*size-.5,py=v*size-.5;
            int x=(int)Math.floor(px),y=(int)Math.floor(py);
            double fx=px-x,fy=py-y;
            double alpha=0,r=0,g=0,b=0;
            for(int dy=0;dy<2;dy++) for(int dx=0;dx<2;dx++)
            {
                int pixel=rgb[((y+dy)&(size-1))*size+((x+dx)&(size-1))];
                double weight=(dx==0?1-fx:fx)*(dy==0?1-fy:fy);
                if(pixel==0) continue;
                alpha+=weight;r+=((pixel>>16)&255)*weight;g+=((pixel>>8)&255)*weight;b+=(pixel&255)*weight;
            }
            if(alpha<1e-9) return 0;
            return ((int)Math.round(alpha*255)<<24) | ((int)Math.round(r/alpha)<<16)
                | ((int)Math.round(g/alpha)<<8) | (int)Math.round(b/alpha);
        }

        int sample(double u, double v)
        {
            int x = (int)Math.floor(u*size), y = (int)Math.floor(v*size);
            return rgb[(y & (size-1))*size+(x & (size-1))];
        }
    }
}
