package com.modelpopout;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.ui.FontManager;

/** Client-thread public archive/sprite reads; small complete hit images are built once per event. */
final class HudAssets
{
    private final IntFunction<byte[]> definitions;
    private final IntFunction<BufferedImage> sprites;
    private final IntUnaryOperator varbits,varps;
    private final Map<Integer,HitsplatStyle> styles=new LinkedHashMap<Integer,HitsplatStyle>(64,.75f,true)
    {
        @Override protected boolean removeEldestEntry(Map.Entry<Integer,HitsplatStyle> entry) { return size()>64; }
    };
    private HealthSkin healthSkin=HealthSkin.FALLBACK;
    private int nextHealthRetry;
    private boolean healthAttempted;
    HudAssets(IntFunction<byte[]> definitions,IntFunction<BufferedImage> sprites,IntUnaryOperator varbits,IntUnaryOperator varps)
    { this.definitions=definitions;this.sprites=sprites;this.varbits=varbits;this.varps=varps; }

    HitsplatStyle style(int type)
    {
        try
        {
            int id=type;
            for(int depth=0;depth<8;depth++)
            {
                if(id<0) return null; // Native transformation explicitly hides this hit.
                if(id>4095) break;
                HitsplatStyle style=styles.get(id);
                if(style==null)
                {
                    byte[] data=definitions.apply(id);
                    if(data==null) break;
                    style=HitsplatStyle.decode(data);styles.put(id,style);
                }
                if(style.transforms==null) return style;
                int value=style.varbit>=0?varbits.applyAsInt(style.varbit):style.varp>=0?varps.applyAsInt(style.varp):-1;
                id=style.transforms[value>=0 && value<style.transforms.length-1?value:style.transforms.length-1];
            }
        }
        catch(RuntimeException unavailable) { /* unsupported/missing data uses an explicit fallback */ }
        return HitsplatStyle.fallback(type);
    }

    BufferedImage hitImage(int type,HitsplatStyle style,int amount)
    {
        BufferedImage[] parts=new BufferedImage[4];boolean nativeArt=false;
        for(int i=0;i<4;i++) if(style.sprites[i]>=0)
        {
            parts[i]=sprite(style.sprites[i]);nativeArt|=parts[i]!=null;
        }
        BufferedImage measuring=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
        Graphics2D probe=measuring.createGraphics();FontMetrics metrics;
        try { probe.setFont(FontManager.getRunescapeSmallFont());metrics=probe.getFontMetrics(); }
        finally { probe.dispose(); }
        String text=style.text(amount);
        int textWidth=Math.min(160,metrics.stringWidth(text));
        int icon=width(parts[0]),left=width(parts[1]),right=width(parts[3]);
        int tile=width(parts[2]);
        int middle=tile>0?Math.max(1,(textWidth+4+tile-1)/tile)*tile:textWidth+4;
        int gap=icon>0?2:0;
        int width=Math.max(20,Math.min(256,icon+gap+left+middle+right));
        int height=metrics.getHeight()+4;
        for(BufferedImage part:parts) if(part!=null) height=Math.max(height,part.getHeight());
        height=Math.min(128,height+Math.min(16,Math.abs(style.textOffset)));
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();
        try
        {
            g.setFont(FontManager.getRunescapeSmallFont());
            if(!nativeArt)
            {
                g.setColor(new Color(HitsplatStyle.fallback(type).color));g.fillRoundRect(0,0,width,height,10,10);
                g.setColor(Color.BLACK);g.drawRoundRect(0,0,width-1,height-1,10,10);
            }
            int x=0;
            draw(g,parts[0],x,height);x+=icon+gap;
            draw(g,parts[1],x,height);x+=left;
            int textX=x+(middle-textWidth)/2;
            if(tile>0) for(int repeat=0;repeat<middle/tile;repeat++) draw(g,parts[2],x+repeat*tile,height);
            x+=middle;draw(g,parts[3],x,height);
            int baseline=(height-metrics.getHeight())/2+metrics.getAscent()+Math.max(-16,Math.min(16,style.textOffset));
            g.setColor(Color.BLACK);g.drawString(text,textX+1,baseline+1);
            g.setColor(nativeArt?new Color(style.color):Color.WHITE);g.drawString(text,textX,baseline);
        }
        finally { g.dispose(); }
        return image;
    }

    HealthSkin healthSkin(int cycle)
    {
        if(healthSkin==HealthSkin.FALLBACK && (!healthAttempted || cycle-nextHealthRetry>=0))
        {
            healthAttempted=true;nextHealthRetry=cycle+50;
            BufferedImage front=sprite(SpriteID.StandardHealth30.FRONT),back=sprite(SpriteID.StandardHealth30.BACK);
            if(front!=null && back!=null) healthSkin=new HealthSkin(front,back);
        }
        return healthSkin;
    }
    private BufferedImage sprite(int id)
    {
        try
        {
            BufferedImage image=sprites.apply(id);
            return image!=null && image.getWidth()<=256 && image.getHeight()<=128?image:null;
        }
        catch(RuntimeException unavailable) { return null; }
    }
    private static int width(BufferedImage image) { return image==null?0:image.getWidth(); }
    private static void draw(Graphics2D g,BufferedImage image,int x,int height)
    { if(image!=null) g.drawImage(image,x,(height-image.getHeight())/2,null); }

    static final class HealthSkin
    {
        static final HealthSkin FALLBACK=new HealthSkin(null,null);
        final BufferedImage front,back;
        HealthSkin(BufferedImage front,BufferedImage back) { this.front=front;this.back=back; }
        int width() { return back==null?30:back.getWidth(); }
        int height() { return back==null?5:back.getHeight(); }
    }
}
