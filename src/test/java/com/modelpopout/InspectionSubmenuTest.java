package com.modelpopout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.Actor;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.modelpopout.InspectionFixtures.*;

public class InspectionSubmenuTest
{
    private Entry group(Menus root,Actor actor,String... visualOptions)
    {
        Entry parent=new Entry("",actor);Menus child=new Menus(false);parent.submenu=child;
        for (int i=visualOptions.length-1;i>=0;i--) child.add(visualOptions[i],actor);
        root.entries.add(parent);return parent;
    }
    private List<String> visual(Menus menu)
    {
        List<String> options=new ArrayList<>();
        for (int i=menu.entries.size()-1;i>=0;i--) options.add(menu.entries.get(i).getOption());
        return options;
    }
    private List<MenuEntry> withoutPopout(Menus menu)
    {
        List<MenuEntry> result=new ArrayList<>(menu.entries);result.removeAll(menu.popouts());return result;
    }

    @Test public void addsInsideTheExistingPlayerSubmenuBeforeReportWithoutRecreatingAnyActions()
    {
        World world=new World(-1);Person player=new Person(world,1);Menus root=new Menus();
        Entry parent=group(root,player,"Follow","Trade with","Lookup","Kill Clog","Report");
        Menus child=(Menus)parent.getSubMenu();List<MenuEntry> originals=new ArrayList<>(child.entries);
        AtomicInteger existingClicks=new AtomicInteger();
        for (MenuEntry entry:originals) entry.onClick(ignored -> existingClicks.incrementAndGet());
        List<InspectionActor> selected=new ArrayList<>();
        InspectionMenu inspection=new InspectionMenu(() -> true,id -> world,selected::add);
        inspection.add(root);inspection.add(root);
        assertTrue(root.popouts().isEmpty());assertEquals(1,child.popouts().size());
        assertSame(child,parent.getSubMenu());assertEquals(originals,withoutPopout(child));
        assertEquals(Arrays.asList("Follow","Trade with","Lookup","Kill Clog","Popout","Report"),visual(child));
        for (MenuEntry entry:originals) entry.onClick().accept(entry);
        assertEquals(5,existingClicks.get());
        MenuEntry popout=child.popouts().get(0);assertNull(popout.getSubMenu());popout.onClick().accept(popout);
        assertSame(player,selected.get(0).resolve(id -> world));assertEquals(2,root.entries.size());
    }

    @Test public void overlappingPlayersWithIdenticalDisplayTextRemainSeparateAndUseApiIdentity()
    {
        World world=new World(-1);Person a=new Person(world,1),b=new Person(world,2);Menus root=new Menus();
        Entry pa=group(root,a,"Follow","Trade with","Report"),pb=group(root,b,"Follow","Lookup","Report");
        pa.setTarget("same text (level-92)");pb.setTarget(pa.getTarget());
        List<InspectionActor> selected=new ArrayList<>();
        new InspectionMenu(() -> true,id -> world,selected::add).add(root);
        Menus ca=(Menus)pa.submenu,cb=(Menus)pb.submenu;
        assertTrue(root.popouts().isEmpty());assertEquals(1,ca.popouts().size());assertEquals(1,cb.popouts().size());
        ca.popouts().get(0).onClick().accept(ca.popouts().get(0));cb.popouts().get(0).onClick().accept(cb.popouts().get(0));
        assertSame(a,selected.get(0).resolve(id -> world));assertSame(b,selected.get(1).resolve(id -> world));
    }

    @Test public void actorMayBeIdentifiedByChildrenWhenTheGroupingParentHasNoActor()
    {
        World world=new World(-1);Person actor=new Person(world,1);Menus root=new Menus();
        Entry parent=group(root,actor,"Follow","Lookup");parent.actor=null;parent.target="An unrelated display label";
        new InspectionMenu(() -> true,id -> world,binding -> {}).add(root);
        Menus child=(Menus)parent.submenu;
        assertEquals(Arrays.asList("Follow","Lookup","Popout"),visual(child));assertTrue(root.popouts().isEmpty());
    }

    @Test public void npcUsesItsExistingSubmenuWhileAnotherNpcKeepsTheOuterFallback()
    {
        World world=new World(-1);Npc grouped=new Npc(world,1),plain=new Npc(world,2);Menus root=new Menus();
        Entry parent=group(root,grouped,"Talk-to","Examine");root.add("Examine",plain);
        List<InspectionActor> selected=new ArrayList<>();
        new InspectionMenu(() -> true,id -> world,selected::add).add(root);
        Menus child=(Menus)parent.submenu;
        assertEquals(Arrays.asList("Talk-to","Examine","Popout"),visual(child));assertEquals(1,root.popouts().size());
        MenuEntry entry=child.popouts().get(0);entry.onClick().accept(entry);
        root.popouts().get(0).onClick().accept(root.popouts().get(0));
        assertSame(grouped,selected.get(0).resolve(id -> world));assertSame(plain,selected.get(1).resolve(id -> world));
    }

    @Test public void conflictingParentAndChildIdentitiesNeverPlaceAnActorInsideAnotherActorsMenu()
    {
        World world=new World(-1);Person a=new Person(world,1),b=new Person(world,2);Menus root=new Menus();
        Entry parent=group(root,a,"Follow");Menus child=(Menus)parent.submenu;child.add("Trade with",b);
        new InspectionMenu(() -> true,id -> world,binding -> {}).add(root);
        assertTrue(child.popouts().isEmpty());assertEquals(2,root.popouts().size());
        assertEquals(Arrays.asList("Trade with","Follow"),visual(child));
    }

    @Test public void nestedActorGroupsWorkButUnrelatedNpcColorMenusAreNotActorSubmenus()
    {
        World world=new World(-1);Person a=new Person(world,1);Npc npc=new Npc(world,2);Menus root=new Menus();
        Entry container=new Entry("Players",null);Menus list=new Menus(false);container.submenu=list;root.entries.add(container);
        Entry actorGroup=group(list,a,"Follow","Report");
        Entry color=new Entry("Tag color",npc);Menus colors=new Menus(false);colors.add("Red",null);color.submenu=colors;root.entries.add(color);
        new InspectionMenu(() -> true,id -> world,binding -> {}).add(root);
        assertEquals(1,((Menus)actorGroup.submenu).popouts().size());assertTrue(list.popouts().isEmpty());
        assertTrue(colors.popouts().isEmpty());assertEquals(1,root.popouts().size());
    }

    @Test public void enableDisableRemovesOnlyOwnedEntriesAtEveryDepthAndFencesOldCallbacks()
    {
        World world=new World(-1);Person actor=new Person(world,1);Menus root=new Menus();
        Entry parent=group(root,actor,"Follow","Lookup","Report");Menus child=(Menus)parent.submenu;
        List<MenuEntry> originals=new ArrayList<>(child.entries);boolean[] enabled={true};AtomicInteger clicks=new AtomicInteger();
        InspectionMenu inspection=new InspectionMenu(() -> enabled[0],id -> world,binding -> clicks.incrementAndGet());
        inspection.add(root);MenuEntry old=child.popouts().get(0);
        enabled[0]=false;old.onClick().accept(old);InspectionMenu.remove(root);
        assertEquals(0,clicks.get());assertEquals(originals,child.entries);assertSame(child,parent.submenu);
        inspection.add(root);assertTrue(child.popouts().isEmpty());
        enabled[0]=true;inspection.add(root);assertEquals(1,child.popouts().size());
    }

    @Test public void onlyOwnedFallbackMigratesWhenAnActorSubmenuBecomesAvailableLater()
    {
        World world=new World(-1);Person actor=new Person(world,1);Menus root=new Menus();root.add("Follow",actor);
        MenuEntry original=root.entries.get(1);InspectionMenu inspection=new InspectionMenu(() -> true,id -> world,binding -> {});
        inspection.add(root);assertEquals(1,root.popouts().size());
        Entry parent=group(root,actor,"Follow","Report");Menus child=(Menus)parent.submenu;
        inspection.add(root);inspection.add(root);
        assertTrue(root.popouts().isEmpty());assertEquals(1,child.popouts().size());assertTrue(root.entries.contains(original));
    }

    @Test public void submenuClickStillCannotFollowAReusedActorIndex()
    {
        World world=new World(-1);Person original=new Person(world,1);Menus root=new Menus();
        Entry parent=group(root,original,"Follow","Report");List<InspectionActor> selected=new ArrayList<>();
        new InspectionMenu(() -> true,id -> world,selected::add).add(root);new Person(world,1);
        MenuEntry entry=((Menus)parent.submenu).popouts().get(0);entry.onClick().accept(entry);
        assertNull(selected.get(0).resolve(id -> world));
    }

    @Test public void cyclicOrSharedSubmenuReferencesDoNotLoopOrDuplicateEntries()
    {
        World world=new World(-1);Person actor=new Person(world,1);Menus root=new Menus();
        Entry parent=group(root,actor,"Follow","Report");Menus child=(Menus)parent.submenu;
        Entry loop=new Entry("More",null);loop.submenu=root;child.entries.add(loop);
        new InspectionMenu(() -> true,id -> world,binding -> {}).add(root);
        assertEquals(1,child.popouts().size());InspectionMenu.remove(root);assertTrue(child.popouts().isEmpty());
    }

    @Test public void emptyActorSubmenuAcceptsPopoutWithoutRecreatingItsParent()
    {
        World world=new World(-1);Person actor=new Person(world,1);Menus root=new Menus();Entry parent=group(root,actor);
        new InspectionMenu(() -> true,id -> world,binding -> {}).add(root);
        assertEquals(Arrays.asList("Popout"),visual((Menus)parent.submenu));assertEquals(2,root.entries.size());
    }

    @Test public void groupingHandlersRunBeforeTheInspectionHandler() throws Exception
    {
        World world=new World(-1);Person actor=new Person(world,1);Menus root=new Menus();
        EventBus bus=new EventBus();
        bus.register(MenuOpened.class,event -> group(root,actor,"Follow","Report"),1);
        float priority=ModelPopoutPlugin.class.getMethod("onMenuOpened",MenuOpened.class).getAnnotation(Subscribe.class).priority();
        assertTrue(priority<0);
        bus.register(MenuOpened.class,event -> new InspectionMenu(() -> true,id -> world,binding -> {}).add(root),priority);
        bus.post(new MenuOpened());
        assertTrue(root.popouts().isEmpty());assertEquals(1,((Menus)root.entries.get(1).getSubMenu()).popouts().size());
    }
}
