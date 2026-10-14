package com.modelpopout;

/** Observe native cycles; fractional texture motion may advance at most one 20ms cycle ahead. */
final class TextureClock
{
    private int cycle;
    private long observedAt;
    private boolean observed;
    void observe(int next, long now)
    {
        if (!observed || next != cycle) { cycle = next; observedAt = now; observed = true; }
    }
    double phase(long now)
    {
        return observed ? cycle + Math.max(0,Math.min(1,(now-observedAt)/20_000_000.0)) : 0;
    }
    boolean advancing(long now) { return observed && now-observedAt < 20_000_000L; }
    void clear() { observed = false; }
}
