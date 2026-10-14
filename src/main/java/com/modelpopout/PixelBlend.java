package com.modelpopout;

final class PixelBlend
{
    static int over(int source,int destination)
    {
        int sa=source>>>24;
        if(sa==255) return source;
        if(sa==0) return destination;
        double s=sa/255.0,d=(destination>>>24)/255.0,out=s+d*(1-s);
        int result=(int)Math.round(out*255)<<24;
        for(int shift=0;shift<=16;shift+=8)
        {
            int value=(int)Math.round((((source>>>shift)&255)*s+((destination>>>shift)&255)*d*(1-s))/out);
            result |= value<<shift;
        }
        return result;
    }
    static int shade(int texel,double light)
    {
        int value=texel & 0xff000000;
        for(int shift=0;shift<=16;shift+=8)
            value |= Math.max(0,Math.min(255,(int)Math.round(((texel>>>shift)&255)*light)))<<shift;
        return value;
    }
}
