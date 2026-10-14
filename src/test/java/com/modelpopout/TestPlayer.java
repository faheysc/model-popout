package com.modelpopout;

/** Explicit public-API test double; unexpected reads and mutations fail immediately. */
@SuppressWarnings("deprecation")
abstract class TestPlayer extends TestActor implements net.runelite.api.Player
{
    @Override public int getId() { throw new UnsupportedOperationException("getId"); }
    @Override public int getCombatLevel() { throw new UnsupportedOperationException("getCombatLevel"); }
    @Override public net.runelite.api.PlayerComposition getPlayerComposition() { throw new UnsupportedOperationException("getPlayerComposition"); }
    @Override public int getTeam() { throw new UnsupportedOperationException("getTeam"); }
    @Override public boolean isFriendsChatMember() { throw new UnsupportedOperationException("isFriendsChatMember"); }
    @Override public boolean isFriend() { throw new UnsupportedOperationException("isFriend"); }
    @Override public boolean isClanMember() { throw new UnsupportedOperationException("isClanMember"); }
    @Override public net.runelite.api.HeadIcon getOverheadIcon() { throw new UnsupportedOperationException("getOverheadIcon"); }
    @Override public int getSkullIcon() { throw new UnsupportedOperationException("getSkullIcon"); }
    @Override public void setSkullIcon(int p0) { throw new UnsupportedOperationException("setSkullIcon"); }
}
