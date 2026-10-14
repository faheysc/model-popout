package com.modelpopout;

/** Selection policy over copied relationship flags; identity, not reused NPC slot numbers, wins. */
final class NpcSelector
{
    static final class Candidate<T>
    {
        final T npc;
        final int index;
        final boolean present, attacking, dead, combat, deathAnimating;
        Candidate(T npc, int index, boolean present, boolean attacking, boolean dead, boolean combat)
        {
            this(npc,index,present,attacking,dead,combat,dead);
        }
        Candidate(T npc, int index, boolean present, boolean attacking, boolean dead, boolean combat, boolean deathAnimating)
        {
            this.deathAnimating = deathAnimating;
            this.npc = npc; this.index = index; this.present = present;
            this.attacking = attacking; this.dead = dead; this.combat = combat;
        }
    }

    static <T> T select(T direct, T previous, Iterable<Candidate<T>> candidates)
    {
        Candidate<T> old = null, fallback = null;
        for (Candidate<T> candidate : candidates)
        {
            if (!candidate.present || (candidate.dead && !candidate.deathAnimating)) continue;
            if (candidate.npc == direct) return direct;
            if (candidate.npc == previous) old = candidate;
            if (candidate.attacking && candidate.combat && !candidate.dead
                && (fallback == null || candidate.index < fallback.index)) fallback = candidate;
        }
        // Keep the selected NPC only while its actual death action is running, or until despawn.
        // A new explicit player target always takes precedence above.
        if (old != null && (old.dead || (old.attacking && old.combat))) return previous;
        return fallback == null ? null : fallback.npc;
    }
}
