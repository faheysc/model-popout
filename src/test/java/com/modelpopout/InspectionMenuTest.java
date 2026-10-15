package com.modelpopout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.modelpopout.InspectionFixtures.*;

public class InspectionMenuTest
{
    @Test public void addsOneExactOptionPerPlayerAndNpcWithoutChangingOtherActions()
    {
        World world=new World(-1);Person player=new Person(world,5);Npc npc=new Npc(world,5);
        Menus menu=new Menus();menu.add("Trade with",player);menu.add("Follow",player);
        menu.add("Kill Clog",player);menu.add("Lookup",player);menu.add("Talk-to",npc);menu.add("Examine",npc);
        List<MenuEntry> originals=Arrays.asList(menu.getMenuEntries());
        List<InspectionActor> selected=new ArrayList<>();
        InspectionMenu inspection=new InspectionMenu(() -> true,id -> world,selected::add);
        inspection.add(menu);inspection.add(menu); // Reentrant notification must not duplicate options.
        assertEquals(2,menu.popouts().size());
        List<MenuEntry> retained=new ArrayList<>(menu.entries);retained.removeAll(menu.popouts());
        assertEquals(originals,retained);
        for (MenuEntry entry:menu.popouts())
        {
            assertEquals("Popout",entry.getOption());assertEquals(MenuAction.RUNELITE,entry.getType());
            assertFalse(entry.isForceLeftClick());assertNull(entry.getSubMenu());assertEquals(-1,entry.getWorldViewId());
            entry.onClick().accept(entry);
        }
        assertEquals(2,selected.size());
        assertTrue(selected.stream().anyMatch(binding -> binding.resolve(id -> world)==player));
        assertTrue(selected.stream().anyMatch(binding -> binding.resolve(id -> world)==npc));
        // The menu does not read or overwrite native player option slots at all.
        assertEquals("Kill Clog",originals.get(3).getOption());assertEquals("Lookup",originals.get(4).getOption());
    }

    @Test public void disableRemovesOnlyOurEntriesAndFencesAnAlreadyOpenCallback()
    {
        World world=new World(-1);Person actor=new Person(world,1);boolean[] enabled={false};
        Menus menu=new Menus();menu.add("Trade with",actor);menu.add("Kill Clog",actor);
        List<InspectionActor> opens=new ArrayList<>();
        InspectionMenu inspection=new InspectionMenu(() -> enabled[0],id -> world,opens::add);
        inspection.add(menu);assertTrue(menu.popouts().isEmpty());
        enabled[0]=true;inspection.add(menu);MenuEntry popout=menu.popouts().get(0);
        enabled[0]=false;popout.onClick().accept(popout);assertTrue(opens.isEmpty());
        inspection.add(menu);assertEquals(3,menu.entries.size());assertTrue(menu.popouts().isEmpty());
        enabled[0]=true;inspection.add(menu);assertEquals(1,menu.popouts().size());
    }

    @Test public void excludesInvalidActorsAndDistinguishesWorldsWithTheSameIndex()
    {
        World top=new World(-1),sub=new World(7);Person a=new Person(top,1),b=new Person(sub,1),stale=new Person(top,2);
        top.players.remove(2);
        Menus menu=new Menus();menu.add("Follow",a);menu.add("Follow",b);menu.add("Follow",stale);menu.add("Message",null);
        new InspectionMenu(() -> true,id -> id==-1?top:sub,binding -> {}).add(menu);
        assertEquals(2,menu.popouts().size());
        assertEquals(7,menu.popouts().get(0).getWorldViewId());assertEquals(-1,menu.popouts().get(1).getWorldViewId());
    }

    @Test public void menuSelectionRetainsOriginalIdentityAndEachClickGetsAnIndependentBinding()
    {
        World world=new World(-1);Person original=new Person(world,3);Menus menu=new Menus();menu.add("Follow",original);
        List<InspectionActor> selections=new ArrayList<>();
        new InspectionMenu(() -> true,id -> world,selections::add).add(menu);
        MenuEntry popout=menu.popouts().get(0);popout.onClick().accept(popout);popout.onClick().accept(popout);
        selections.get(0).release();assertSame(original,selections.get(1).resolve(id -> world));
        new Person(world,3);popout.onClick().accept(popout);
        assertNull(selections.get(2).resolve(id -> world)); // Never re-resolve by index alone.
    }
}
