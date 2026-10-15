package com.modelpopout;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.modelpopout.InspectionFixtures.*;

public class InspectionActorTest
{
    @Test public void playerAndNpcIndexReuseCannotRedirectAnInspector()
    {
        World world=new World(-1);Person player=new Person(world,4);Npc npc=new Npc(world,4);
        InspectionActor p=InspectionActor.bind(player,id -> world),n=InspectionActor.bind(npc,id -> world);
        assertSame(player,p.resolve(id -> world));assertSame(npc,n.resolve(id -> world));
        new Person(world,4);new Npc(world,4);
        assertNull(p.resolve(id -> world));assertNull(n.resolve(id -> world));
        assertTrue(p.matches(player));assertFalse(p.matches(world.players.get(4)));
    }

    @Test public void replacementWorldActorWorldMoveAndReleaseInvalidateTheBinding()
    {
        World original=new World(-1),replacement=new World(-1);Person player=new Person(original,2);
        InspectionActor binding=InspectionActor.bind(player,id -> original);
        assertNull(binding.resolve(id -> replacement));
        player.world=replacement;assertNull(binding.resolve(id -> original));
        player.world=original;binding.release();assertNull(binding.resolve(id -> original));assertFalse(binding.matches(player));
    }

    @Test public void equipmentAndNpcTransformationsDoNotChangeActorIdentity()
    {
        World world=new World(-1);Npc npc=new Npc(world,1);Person player=new Person(world,2);
        InspectionActor n=InspectionActor.bind(npc,id -> world),p=InspectionActor.bind(player,id -> world);
        npc.id=999;npc.mesh=new Mesh();player.mesh=new Mesh();player.mesh.color=1200;
        assertSame(npc,n.resolve(id -> world));assertSame(player,p.resolve(id -> world));
        assertNull(InspectionActor.bind(null,id -> world));world.players.remove(2);
        assertNull(InspectionActor.bind(player,id -> world));
    }
}
