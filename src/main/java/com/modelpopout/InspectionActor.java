package com.modelpopout;

import java.util.function.IntFunction;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;

/** Native reads are client-thread-only. A released binding can never attach to a reused slot. */
final class InspectionActor
{
    private Actor actor;
    private WorldView world;
    final int index, worldId;
    final String name;

    private InspectionActor(Actor actor, WorldView world, int index)
    {
        this.actor = actor;
        this.world = world;
        this.index = index;
        worldId = world.getId();
        String observed = actor.getName();
        name = observed == null || observed.isEmpty() ? (actor instanceof NPC ? "NPC" : "Player") : observed;
    }

    private InspectionActor(InspectionActor original)
    { actor=original.actor;world=original.world;index=original.index;worldId=original.worldId;name=original.name; }

    InspectionActor copy() { return new InspectionActor(this); }

    static InspectionActor bind(Actor actor, IntFunction<WorldView> worlds)
    {
        if (!(actor instanceof Player) && !(actor instanceof NPC)) return null;
        WorldView world = actor.getWorldView();
        if (world == null) return null;
        int index = actor instanceof Player ? ((Player)actor).getId() : ((NPC)actor).getIndex();
        if (index < 0) return null;
        InspectionActor binding = new InspectionActor(actor,world,index);
        return binding.resolve(worlds) == null ? null : binding;
    }

    Actor resolve(IntFunction<WorldView> worlds)
    {
        Actor current = actor;
        WorldView original = world;
        if (current == null || original == null) return null;
        if (worlds.apply(worldId) != original || current.getWorldView() != original) return null;
        Actor occupant = current instanceof Player ? original.players().byIndex(index) : original.npcs().byIndex(index);
        return occupant == current ? current : null;
    }

    boolean matches(Actor other) { return actor != null && actor == other; }
    Actor identity() { return actor; }
    void release() { actor = null; world = null; }
}
