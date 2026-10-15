package com.modelpopout;

import com.google.gson.Gson;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;

/** Real RuneLite config serialization/events, isolated to a test profile with no remote services. */
final class ConfigTestSupport implements AutoCloseable
{
    final EventBus events=new EventBus();
    final ConfigManager manager;
    final ModelPopoutConfig config;
    private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor();
    private final Object data;

    ConfigTestSupport(File profile, Client client) throws Exception
    {
        Constructor<?> constructor=ConfigManager.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        manager=(ConfigManager)constructor.newInstance(null,executor,events,client,new Gson(),null,null,null);
        // Do not run ConfigManager's periodic remote-sync task in an isolated test.
        executor.shutdownNow();
        Field field=ConfigManager.class.getDeclaredField("configProfile");field.setAccessible(true);
        Constructor<?> dataConstructor=field.getType().getDeclaredConstructor(File.class);dataConstructor.setAccessible(true);
        data=dataConstructor.newInstance(profile);field.set(manager,data);
        config=manager.getConfig(ModelPopoutConfig.class);
    }

    void save() throws Exception
    {
        Method swap=data.getClass().getDeclaredMethod("swapChanges");swap.setAccessible(true);
        Method patch=data.getClass().getDeclaredMethod("patch",Map.class);patch.setAccessible(true);
        patch.invoke(data,swap.invoke(data));
    }

    static void set(Object owner,String name,Object value) throws Exception
    {
        Field field=owner.getClass().getDeclaredField(name);field.setAccessible(true);field.set(owner,value);
    }

    @SuppressWarnings("unchecked") static <T> T get(Object owner,String name) throws Exception
    {
        Field field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return (T)field.get(owner);
    }

    @Override public void close() { executor.shutdownNow(); }
}
