package com.modelpopout;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.runelite.api.Actor;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.widgets.Widget;

final class InspectionFixtures
{
    static final class World extends TestWorldView
    {
        final Map<Integer,Player> players=new HashMap<>();
        final Map<Integer,NPC> npcs=new HashMap<>();
        final int id;
        World(int id) { this.id=id; }
        @Override public int getId() { return id; }
        @Override public IndexedObjectSet<Player> players() { return set(players); }
        @Override public IndexedObjectSet<NPC> npcs() { return set(npcs); }
        private static <T> IndexedObjectSet<T> set(Map<Integer,T> values)
        {
            return new IndexedObjectSet<T>()
            {
                @Override public T byIndex(int index) { return values.get(index); }
                @Override public Iterator<T> iterator() { throw new AssertionError("No full-world scans"); }
            };
        }
    }

    static final class Mesh extends TestModel
    {
        final float[] x={-80,80,0}, y={0,0,-180}, z={0,0,0};
        int color=5000, texture=-1;
        @Override public int getVerticesCount() { return 3; }
        @Override public float[] getVerticesX() { return x; }
        @Override public float[] getVerticesY() { return y; }
        @Override public float[] getVerticesZ() { return z; }
        @Override public int getFaceCount() { return 1; }
        @Override public int[] getFaceIndices1() { return new int[]{0}; }
        @Override public int[] getFaceIndices2() { return new int[]{1}; }
        @Override public int[] getFaceIndices3() { return new int[]{2}; }
        @Override public int[] getFaceColors1() { return new int[]{color}; }
        @Override public int[] getFaceColors2() { return new int[]{color}; }
        @Override public int[] getFaceColors3() { return new int[]{color}; }
        @Override public byte[] getFaceTransparencies() { return null; }
        @Override public short[] getFaceTextures() { return texture<0?null:new short[]{(short)texture}; }
    }

    static final class Person extends TestPlayer
    {
        final int index;
        World world;
        Mesh mesh=new Mesh();
        int reads, ratio=15, scale=30;
        Person(World world,int index) { this.world=world;this.index=index;world.players.put(index,this); }
        @Override public int getId() { return index; }
        @Override public WorldView getWorldView() { return world; }
        @Override public String getName() { return "Player "+index; }
        @Override public net.runelite.api.Model getModel() { reads++;return mesh; }
        @Override public int getHealthRatio() { return ratio; }
        @Override public int getHealthScale() { return scale; }
    }

    static final class Npc extends TestNpc
    {
        final int index;
        final World world;
        Mesh mesh=new Mesh();
        int id=1, reads;
        Npc(World world,int index) { this.world=world;this.index=index;world.npcs.put(index,this); }
        @Override public int getIndex() { return index; }
        @Override public int getId() { return id; }
        @Override public WorldView getWorldView() { return world; }
        @Override public net.runelite.api.NPCComposition getTransformedComposition()
        {
            return (net.runelite.api.NPCComposition)java.lang.reflect.Proxy.newProxyInstance(
                net.runelite.api.NPCComposition.class.getClassLoader(),new Class<?>[]{net.runelite.api.NPCComposition.class},
                (proxy,method,args) -> {
                    if (method.getName().equals("getId")) return id;
                    throw new AssertionError("Unexpected NPC definition read: "+method.getName());
                });
        }
        @Override public String getName() { return "NPC "+id; }
        @Override public net.runelite.api.Model getModel() { reads++;return mesh; }
        @Override public int getHealthRatio() { return 10; }
        @Override public int getHealthScale() { return 20; }
    }

    static final class Entry implements MenuEntry
    {
        Actor actor;
        String option="",target="";
        MenuAction type=MenuAction.RUNELITE;
        Consumer<MenuEntry> click;
        int id,world,param0,param1,item;
        boolean force,low;
        Menu submenu;
        Entry(String option,Actor actor) { this.option=option;this.actor=actor;target=actor==null?"":actor.getName(); }
        @Override public String getOption() { return option; }
        @Override public MenuEntry setOption(String value) { option=value;return this; }
        @Override public String getTarget() { return target; }
        @Override public MenuEntry setTarget(String value) { target=value;return this; }
        @Override public int getIdentifier() { return id; }
        @Override public MenuEntry setIdentifier(int value) { id=value;return this; }
        @Override public MenuAction getType() { return type; }
        @Override public MenuEntry setType(MenuAction value) { type=value;return this; }
        @Override public int getParam0() { return param0; }
        @Override public MenuEntry setParam0(int value) { param0=value;return this; }
        @Override public int getParam1() { return param1; }
        @Override public MenuEntry setParam1(int value) { param1=value;return this; }
        @Override public boolean isForceLeftClick() { return force; }
        @Override public MenuEntry setForceLeftClick(boolean value) { force=value;return this; }
        @Override public int getWorldViewId() { return world; }
        @Override public MenuEntry setWorldViewId(int value) { world=value;return this; }
        @Override public boolean isDeprioritized() { return low; }
        @Override public MenuEntry setDeprioritized(boolean value) { low=value;return this; }
        @Override public MenuEntry onClick(Consumer<MenuEntry> value) { click=value;return this; }
        @Override public Consumer<MenuEntry> onClick() { return click; }
        @Override public boolean isItemOp() { return false; }
        @Override public int getItemOp() { return -1; }
        @Override public int getItemId() { return item; }
        @Override public MenuEntry setItemId(int value) { item=value;return this; }
        @Override public Widget getWidget() { return null; }
        @Override public NPC getNpc() { return actor instanceof NPC?(NPC)actor:null; }
        @Override public Player getPlayer() { return actor instanceof Player?(Player)actor:null; }
        @Override public Actor getActor() { return actor; }
        @Override public Menu getSubMenu() { return submenu; }
        @Override public Menu createSubMenu() { throw new AssertionError("No submenus"); }
        @Override public void deleteSubMenu() { throw new AssertionError("Do not alter other menus"); }
    }

    static final class Menus implements Menu
    {
        final List<MenuEntry> entries=new ArrayList<>();
        Menus() { this(true); }
        Menus(boolean cancel) { if (cancel) entries.add(new Entry("Cancel",null)); }
        void add(String option,Actor actor) { entries.add(new Entry(option,actor)); }
        List<MenuEntry> popouts()
        {
            List<MenuEntry> result=new ArrayList<>();
            for (MenuEntry entry:entries) if ("Popout".equals(entry.getOption())) result.add(entry);
            return result;
        }
        @Override public MenuEntry createMenuEntry(int index)
        { Entry entry=new Entry("",null);entries.add(index,entry);return entry; }
        @Override public MenuEntry[] getMenuEntries() { return entries.toArray(new MenuEntry[0]); }
        @Override public void setMenuEntries(MenuEntry[] value) { throw new AssertionError("No menu replacement"); }
        @Override public void removeMenuEntry(MenuEntry entry) { entries.remove(entry); }
        @Override public int getMenuX() { return 0; }
        @Override public int getMenuY() { return 0; }
        @Override public int getMenuWidth() { return 0; }
        @Override public int getMenuHeight() { return 0; }
    }

    static final class Worker extends AbstractExecutorService
    {
        final Queue<Runnable> tasks=new ArrayDeque<>();
        boolean closed;
        @Override public void execute(Runnable task)
        { if (closed) throw new java.util.concurrent.RejectedExecutionException();tasks.add(task); }
        @Override public void shutdown() { closed=true; }
        @Override public List<Runnable> shutdownNow() { throw new AssertionError("Do not interrupt raster work"); }
        @Override public boolean isShutdown() { return closed; }
        @Override public boolean isTerminated() { return closed && tasks.isEmpty(); }
        @Override public boolean awaitTermination(long time,TimeUnit unit) { return isTerminated(); }
        void run() { tasks.remove().run(); }
    }
}
