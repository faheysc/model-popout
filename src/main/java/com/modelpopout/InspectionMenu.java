package com.modelpopout;

import java.util.Collections;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.runelite.api.Actor;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.WorldView;

/** Additive scene-only menu entries; no native player option slots or synthetic clicks. */
final class InspectionMenu
{
    static final String OPTION = "Popout";
    private final BooleanSupplier enabled;
    private final IntFunction<WorldView> worlds;
    private final Consumer<InspectionActor> open;

    InspectionMenu(BooleanSupplier enabled, IntFunction<WorldView> worlds, Consumer<InspectionActor> open)
    { this.enabled = enabled; this.worlds = worlds; this.open = open; }

    void add(Menu menu)
    {
        if (!enabled.getAsBoolean()) { remove(menu);return; }
        List<Node> nodes=tree(menu);
        Map<Actor,Candidate> actors=new IdentityHashMap<>();
        List<Candidate> ordered=new ArrayList<>();
        for (Node node:nodes)
        {
            for (MenuEntry entry:node.entries)
            {
                Actor actor=identity(entry);
                if (actor==null) continue;
                Candidate candidate=actors.get(actor);
                if (candidate==null)
                { candidate=new Candidate(actor,entry);actors.put(actor,candidate);ordered.add(candidate); }
                if (entry.onClick() instanceof Choice) candidate.owned.add(new Owned(node.menu,entry));
            }
            Actor subject=subject(node);
            Candidate candidate=actors.get(subject);
            if (candidate!=null && candidate.preferred==null) candidate.preferred=node.menu;
        }
        for (Candidate candidate:ordered)
        {
            InspectionActor binding=InspectionActor.bind(candidate.actor,worlds);
            Menu destination=candidate.preferred==null?menu:candidate.preferred;
            if (binding!=null && candidate.owned.size()==1 && candidate.owned.get(0).menu==destination)
            { binding.release();continue; }
            // Only our own entries may migrate when a submenu appears on a later notification.
            for (Owned own:candidate.owned) remove(own.menu,own.entry);
            if (binding==null) continue;
            int index=destination==menu?Math.min(1,destination.getMenuEntries().length):insertionIndex(destination);
            destination.createMenuEntry(index)
                .setOption(OPTION).setTarget(candidate.target).setType(MenuAction.RUNELITE)
                .setIdentifier(binding.index).setWorldViewId(binding.worldId).setForceLeftClick(false)
                .onClick(new Choice(binding));
        }
    }

    /** A menu belongs to one actor only when API identities agree. Text never identifies the actor. */
    private static Actor subject(Node node)
    {
        if (node.parent==null) return null;
        Actor actor=node.parent.getActor();
        boolean evidence=actor!=null && "".equals(node.parent.getOption()); // RuneLite's condensed actor row.
        for (MenuEntry entry:node.entries)
        {
            Actor child=identity(entry);
            if (child==null) continue;
            if (actor!=null && actor!=child) return null; // Shared/mixed menu: use actor submenus or outer fallback.
            actor=child;
            if (!(entry.onClick() instanceof Choice) && entry.getSubMenu()==null) evidence=true;
        }
        return evidence?actor:null;
    }

    private static Actor identity(MenuEntry entry)
    {
        return entry.onClick() instanceof Choice?((Choice)entry.onClick()).binding.identity():entry.getActor();
    }

    private static int insertionIndex(Menu menu)
    {
        MenuEntry[] entries=menu.getMenuEntries();
        // The menu array is reversed: insert immediately above Report in visual order.
        for (int i=0;i<entries.length;i++) if ("Report".equalsIgnoreCase(entries[i].getOption())) return i+1;
        return 0; // Otherwise appear below existing actions, without reordering any of them.
    }

    static void remove(Menu menu)
    {
        for (Node node:tree(menu)) for (MenuEntry entry:node.entries)
            if (entry.onClick() instanceof Choice) remove(node.menu,entry);
    }

    private static void remove(Menu menu,MenuEntry entry)
    {
        ((Choice)entry.onClick()).binding.release();
        menu.removeMenuEntry(entry);
    }

    /** Snapshot the existing tree before editing; tolerate shared/cyclic references without recreating menus. */
    private static List<Node> tree(Menu root)
    {
        List<Node> result=new ArrayList<>();
        Set<Menu> visited=Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<Node> pending=new ArrayDeque<>();pending.add(new Node(root,null));
        while (!pending.isEmpty())
        {
            Node node=pending.remove();
            if (!visited.add(node.menu)) continue;
            result.add(node);
            for (MenuEntry entry:node.entries)
                if (entry.getSubMenu()!=null) pending.add(new Node(entry.getSubMenu(),entry));
        }
        return result;
    }

    private static final class Node
    {
        final Menu menu;
        final MenuEntry parent;
        final MenuEntry[] entries;
        Node(Menu menu,MenuEntry parent) { this.menu=menu;this.parent=parent;entries=menu.getMenuEntries(); }
    }

    private static final class Candidate
    {
        final Actor actor;
        final String target;
        final List<Owned> owned=new ArrayList<>();
        Menu preferred;
        Candidate(Actor actor,MenuEntry exemplar) { this.actor=actor;target=exemplar.getTarget(); }
    }

    private static final class Owned
    {
        final Menu menu;
        final MenuEntry entry;
        Owned(Menu menu,MenuEntry entry) { this.menu=menu;this.entry=entry; }
    }

    private final class Choice implements Consumer<MenuEntry>
    {
        private final InspectionActor binding;
        Choice(InspectionActor binding) { this.binding=binding; }
        @Override public void accept(MenuEntry entry)
        { if (enabled.getAsBoolean()) open.accept(binding.copy()); }
    }
}
