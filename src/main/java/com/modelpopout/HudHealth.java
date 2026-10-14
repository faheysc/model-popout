package com.modelpopout;

/** Visibility follows available native health-bar data; NPC fractions never imply exact HP. */
final class HudHealth
{
    static final HudHealth UNKNOWN=new HudHealth(false,0,false,-1,-1);
    final boolean visible,exact;
    final double fraction;
    final int current,maximum;

    private HudHealth(boolean visible,double fraction,boolean exact,int current,int maximum)
    { this.visible=visible;this.fraction=fraction;this.exact=exact;this.current=current;this.maximum=maximum; }

    static HudHealth scaled(int ratio,int scale)
    {
        return ratio<0 || scale<=0 ? UNKNOWN : new HudHealth(true,Math.min(1,ratio/(double)scale),false,-1,-1);
    }

    static HudHealth player(int ratio,int scale,int current,int maximum)
    {
        if(ratio<0 || scale<=0) return UNKNOWN;
        if(current<0 || maximum<=0) return scaled(ratio,scale);
        return new HudHealth(true,Math.min(1,current/(double)maximum),true,current,maximum);
    }

    boolean same(HudHealth other)
    { return other!=null && visible==other.visible && exact==other.exact && fraction==other.fraction && current==other.current && maximum==other.maximum; }
}
