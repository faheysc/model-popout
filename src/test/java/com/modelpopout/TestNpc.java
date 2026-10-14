package com.modelpopout;

/** Explicit public-API test double; unexpected reads and mutations fail immediately. */
@SuppressWarnings("deprecation")
abstract class TestNpc extends TestActor implements net.runelite.api.NPC
{
    @Override public int getId() { throw new UnsupportedOperationException("getId"); }
    @Override public java.lang.String getName() { throw new UnsupportedOperationException("getName"); }
    @Override public int getCombatLevel() { throw new UnsupportedOperationException("getCombatLevel"); }
    @Override public int getIndex() { throw new UnsupportedOperationException("getIndex"); }
    @Override public net.runelite.api.NPCComposition getComposition() { throw new UnsupportedOperationException("getComposition"); }
    @Override public net.runelite.api.NPCComposition getTransformedComposition() { throw new UnsupportedOperationException("getTransformedComposition"); }
    @Override public net.runelite.api.NpcOverrides getModelOverrides() { throw new UnsupportedOperationException("getModelOverrides"); }
    @Override public net.runelite.api.NpcOverrides getChatheadOverrides() { throw new UnsupportedOperationException("getChatheadOverrides"); }
    @Override public int[] getOverheadArchiveIds() { throw new UnsupportedOperationException("getOverheadArchiveIds"); }
    @Override public short[] getOverheadSpriteIds() { throw new UnsupportedOperationException("getOverheadSpriteIds"); }
}
