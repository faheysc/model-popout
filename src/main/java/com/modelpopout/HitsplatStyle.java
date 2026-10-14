package com.modelpopout;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import net.runelite.api.HitsplatID;

/** Bounded decoding of the public config archive's hitsplat format, not internal client objects. */
final class HitsplatStyle
{
    final int color,duration,fadeStart,replaceMode,offsetX,offsetY,textOffset,varbit,varp;
    final int[] sprites,transforms;
    final String format;

    private HitsplatStyle(Builder b)
    {
        color=b.color;duration=b.duration;fadeStart=b.fadeStart;replaceMode=b.replaceMode;
        offsetX=b.offsetX;offsetY=b.offsetY;textOffset=b.textOffset;varbit=b.varbit;varp=b.varp;
        sprites=b.sprites.clone();transforms=b.transforms==null?null:b.transforms.clone();format=b.format;
    }

    static HitsplatStyle decode(byte[] data)
    {
        if(data==null || data.length==0 || data.length>4096) throw new IllegalArgumentException("Invalid hitsplat definition size");
        ByteBuffer in=ByteBuffer.wrap(data);Builder b=new Builder();
        while(in.hasRemaining())
        {
            int opcode=in.get()&255;
            switch(opcode)
            {
                case 0:return new HitsplatStyle(b);
                case 1:smart(in);break; // Native font ID: no offscreen native-font API is used.
                case 2:b.color=((in.get()&255)<<16)|((in.get()&255)<<8)|(in.get()&255);break;
                case 3:b.sprites[0]=smart(in);break;
                case 4:b.sprites[1]=smart(in);break;
                case 5:b.sprites[2]=smart(in);break;
                case 6:b.sprites[3]=smart(in);break;
                case 7:b.offsetX=in.getShort();break;
                case 8:
                    if(in.get()!=0) throw new IllegalArgumentException("Invalid hitsplat string marker");
                    int start=in.position();
                    while(in.get()!=0) { /* bounded by the input buffer */ }
                    b.format=new String(data,start,in.position()-start-1,Charset.forName("windows-1252"));
                    break;
                case 9:b.duration=Math.max(1,in.getShort()&65535);break;
                case 10:b.offsetY=in.getShort();break;
                case 11:b.fadeStart=0;break;
                case 12:b.replaceMode=in.get()&255;break;
                case 13:b.textOffset=in.getShort();break;
                case 14:b.fadeStart=in.getShort()&65535;break;
                case 17:case 18:
                    b.varbit=id(in);b.varp=id(in);
                    int fallback=opcode==18?id(in):-1;
                    int count=in.get()&255;b.transforms=new int[count+2];
                    for(int i=0;i<=count;i++) b.transforms[i]=id(in);
                    b.transforms[count+1]=fallback;
                    break;
                default:throw new IllegalArgumentException("Unsupported hitsplat opcode: "+opcode);
            }
        }
        throw new IllegalArgumentException("Unterminated hitsplat definition");
    }

    private static int id(ByteBuffer in) { int value=in.getShort()&65535;return value==65535?-1:value; }
    private static int smart(ByteBuffer in)
    {
        if(in.get(in.position())<0) return in.getInt()&Integer.MAX_VALUE;
        int value=in.getShort()&65535;return value==32767?-1:value;
    }

    static HitsplatStyle fallback(int type)
    {
        Builder b=new Builder();b.replaceMode=0;
        switch(type)
        {
            case HitsplatID.BLOCK_ME:case HitsplatID.BLOCK_OTHER:b.color=0x438cff;break;
            case HitsplatID.POISON:b.color=0x39a83a;break;
            case HitsplatID.VENOM:b.color=0x146126;break;
            case HitsplatID.HEAL:case HitsplatID.SANITY_RESTORE:b.color=0x4ddd78;break;
            case HitsplatID.DISEASE:case HitsplatID.DISEASE_BLOCKED:b.color=0xe8bb34;break;
            case HitsplatID.DAMAGE_ME_CYAN:case HitsplatID.DAMAGE_OTHER_CYAN:case HitsplatID.DAMAGE_MAX_ME_CYAN:
            case HitsplatID.CYAN_UP:case HitsplatID.CYAN_DOWN:b.color=0x42cbd9;break;
            case HitsplatID.DAMAGE_ME_ORANGE:case HitsplatID.DAMAGE_OTHER_ORANGE:case HitsplatID.DAMAGE_MAX_ME_ORANGE:b.color=0xf09731;break;
            case HitsplatID.DAMAGE_ME_YELLOW:case HitsplatID.DAMAGE_OTHER_YELLOW:case HitsplatID.DAMAGE_MAX_ME_YELLOW:b.color=0xf3db43;break;
            case HitsplatID.DAMAGE_ME_WHITE:case HitsplatID.DAMAGE_OTHER_WHITE:case HitsplatID.DAMAGE_MAX_ME_WHITE:b.color=0xffffff;break;
            case HitsplatID.DAMAGE_ME_POISE:case HitsplatID.DAMAGE_OTHER_POISE:case HitsplatID.DAMAGE_MAX_ME_POISE:b.color=0xb287dc;break;
            case HitsplatID.DAMAGE_ME:case HitsplatID.DAMAGE_OTHER:case HitsplatID.DAMAGE_MAX_ME:
            case HitsplatID.BLEED:case HitsplatID.BURN:b.color=0xe44736;break;
            default:b.color=0xb0b0b0;break;
        }
        return new HitsplatStyle(b);
    }

    String text(int amount) { return (format.isEmpty()?"%1":format).replace("%1",Integer.toString(amount)); }

    private static final class Builder
    {
        int color=0xffffff,duration=70,fadeStart=-1,replaceMode=-1,offsetX,offsetY,textOffset,varbit=-1,varp=-1;
        final int[] sprites={-1,-1,-1,-1};int[] transforms;String format="%1";
    }
}
